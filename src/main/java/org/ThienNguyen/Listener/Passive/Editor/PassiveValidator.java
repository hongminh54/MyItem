package org.ThienNguyen.Listener.Passive.Editor;

import org.ThienNguyen.Listener.Passive.TargetType;
import org.ThienNguyen.Listener.Passive.Trigger.PassiveTrigger;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Kiểm tra draft trước khi lưu. ERROR chặn lưu; WARN chỉ cảnh báo. */
public final class PassiveValidator {

    public enum Level { ERROR, WARN }

    public record Issue(Level level, String path, String message) {
        public boolean isError() { return level == Level.ERROR; }
        @Override public String toString() { return (isError() ? "§c✖ " : "§e⚠ ") + "§7" + path + ": §f" + message; }
    }

    private PassiveValidator() {}

    public static List<Issue> validate(PassiveDraft d) {
        List<Issue> out = new ArrayList<>();

        if (!PassiveFileStore.ID_PATTERN.matcher(d.getId()).matches())
            out.add(err("id", "ID rỗng hoặc có ký tự không hợp lệ (chỉ chữ/số/_/-)."));
        try {
            PassiveTrigger.valueOf(d.getTrigger().toUpperCase());
        } catch (IllegalArgumentException ex) {
            out.add(err("trigger", "Trigger không hợp lệ: \"" + d.getTrigger() + "\"."));
        }
        if (d.getChance().isBlank()) out.add(err("chance", "Chance không được trống."));
        else if (isNumber(d.getChance()) && (num(d.getChance()) < 0 || num(d.getChance()) > 100))
            out.add(warn("chance", "Chance nằm ngoài 0-100 (sẽ bị ép về khoảng này)."));

        if (!d.getCooldown().matches("\\d{1,9}"))
            out.add(err("cooldown", "Cooldown phải là số nguyên giây >= 0 (plugin không đọc biểu thức ở đây)."));

        try {
            TargetType.valueOf(d.getTargetType().toUpperCase());
        } catch (IllegalArgumentException ex) {
            out.add(warn("condition.target-type", "Giá trị lạ \"" + d.getTargetType() + "\" → plugin dùng BOTH."));
        }
        for (String expr : d.getConditionExpressions()) {
            if (!expr.matches(".*(>=|<=|==|!=|>|<).*"))
                out.add(err("condition.expressions", "Thiếu toán tử so sánh trong \"" + expr + "\"."));
        }

        List<ActionNode> actions = d.getActions();
        if (actions.isEmpty()) out.add(warn("actions", "Passive chưa có action nào."));
        validateList(actions, "actions", out);
        checkCounterKeys(actions, out);
        return out;
    }

    public static boolean hasErrors(List<Issue> issues) {
        for (Issue i : issues) if (i.isError()) return true;
        return false;
    }

    private static void validateList(List<ActionNode> list, String path, List<Issue> out) {
        for (int i = 0; i < list.size(); i++) validateNode(list.get(i), path + "[" + (i + 1) + "]", out);
    }

    private static void validateNode(ActionNode n, String path, List<Issue> out) {
        String type = n.getType();
        ActionSchema s = ActionSchemaRegistry.get(type);
        if (type == null || type.isBlank()) {
            out.add(err(path, "Thiếu 'type'."));
            return;
        }
        String here = path + "(" + type.toUpperCase() + ")";
        if (s == null) {
            out.add(err(here, "Type không có trong MechanicRegistry."));
        } else {
            for (FieldDef f : s.visibleFields(n.fields())) {
                Object v = n.getField(f.key());
                boolean missing = v == null || String.valueOf(v).isBlank();
                if (missing) {
                    if (f.required() && f.def() == null) out.add(err(here + "." + f.key(), "Field bắt buộc chưa có giá trị."));
                    continue;
                }
                checkValue(f, v, here + "." + f.key(), out);
            }
            typeSpecific(n, here, out);
        }
        for (Map.Entry<String, List<ActionNode>> e : n.allChildren().entrySet())
            validateList(e.getValue(), here + "." + e.getKey(), out);
        for (Map.Entry<String, java.util.LinkedHashMap<String, List<ActionNode>>> e : n.allKeyedChildren().entrySet())
            for (Map.Entry<String, List<ActionNode>> m : e.getValue().entrySet())
                validateList(m.getValue(), here + "." + e.getKey() + "." + m.getKey(), out);
    }

    private static void checkValue(FieldDef f, Object v, String path, List<Issue> out) {
        String s = String.valueOf(v).trim();
        switch (f.kind()) {
            case INT, DOUBLE -> {
                if (!isNumber(s)) { out.add(err(path, "Phải là số, hiện là \"" + s + "\".")); return; }
                if (f.kind() == FieldKind.INT && !s.matches("-?\\d+")) out.add(err(path, "Phải là số nguyên."));
                rangeCheck(f, num(s), path, out);
            }
            case EXPRESSION -> {
                if (isNumber(s)) rangeCheck(f, num(s), path, out);
            }
            case ENUM -> {
                if (!f.allowed().isEmpty() && f.allowed().stream().noneMatch(a -> a.equalsIgnoreCase(s)))
                    out.add(err(path, "Giá trị \"" + s + "\" không thuộc " + f.allowed() + "."));
            }
            case BOOLEAN -> {
                if (!s.equalsIgnoreCase("true") && !s.equalsIgnoreCase("false"))
                    out.add(err(path, "Phải là true/false."));
            }
            // Tên theo server hiện tại: chỉ WARN vì có thể khác phiên bản
            case MATERIAL -> {
                if (Material.matchMaterial(s) == null) out.add(warn(path, "Material \"" + s + "\" không có trên server này."));
            }
            case SOUND -> {
                if (!soundExists(s)) out.add(warn(path, "Sound \"" + s + "\" không có trên server này."));
            }
            case PARTICLE -> {
                if (!particleExists(s)) out.add(warn(path, "Particle \"" + s + "\" không có trên server này."));
            }
            case POTION_EFFECT -> {
                if (!potionExists(s)) out.add(warn(path, "Hiệu ứng thuốc \"" + s + "\" không có trên server này."));
            }
            case EFFECT_LIST -> {
                if (!(v instanceof List<?> l) || l.isEmpty()) {
                    out.add(err(path, "Cần ít nhất một hiệu ứng."));
                    return;
                }
                for (int i = 0; i < l.size(); i++) {
                    String ep = path + "[" + (i + 1) + "]";
                    if (!(l.get(i) instanceof Map<?, ?> m) || m.get("type") == null || String.valueOf(m.get("type")).isBlank()) {
                        out.add(err(ep, "Thiếu 'type' của hiệu ứng."));
                    } else if (!potionExists(String.valueOf(m.get("type")).trim())) {
                        out.add(warn(ep + ".type", "Hiệu ứng thuốc \"" + m.get("type") + "\" không có trên server này."));
                    }
                }
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ kiểm tra theo type / chéo action

    private static void typeSpecific(ActionNode n, String here, List<Issue> out) {
        switch (n.getType().toUpperCase(Locale.ROOT)) {
            case "DROP_ITEM" -> checkDropAmount(n.getField("amount"), here + ".amount", out);
            case "POTION_ZONE" -> checkColor(n.getField("color"), here + ".color", out);
            default -> { }
        }
    }

    /** DROP_ITEM.amount: số "n" hoặc khoảng "a-b" (biểu thức/placeholder bỏ qua). */
    private static void checkDropAmount(Object v, String path, List<Issue> out) {
        if (v == null) return;
        String s = String.valueOf(v).trim();
        if (s.isEmpty() || s.matches(".*[%{A-Za-z].*")) return;
        if (s.matches("\\d+(\\.\\d+)?")) return;
        Matcher m = Pattern.compile("(\\d{1,9})\\s*-\\s*(\\d{1,9})").matcher(s);
        if (m.matches()) {
            if (Long.parseLong(m.group(1)) > Long.parseLong(m.group(2))) out.add(warn(path, "Khoảng \"a-b\" có a lớn hơn b."));
            return;
        }
        out.add(warn(path, "Số lượng nên là n hoặc khoảng a-b (vd 1-3), hiện là \"" + s + "\"."));
    }

    /** POTION_ZONE.color: "R,G,B" mỗi thành phần 0-255. */
    private static void checkColor(Object v, String path, List<Issue> out) {
        if (v == null) return;
        String s = String.valueOf(v).trim();
        if (s.isEmpty() || s.matches(".*[%{].*")) return;
        Matcher m = Pattern.compile("(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})").matcher(s);
        if (!m.matches()) {
            out.add(warn(path, "Màu phải dạng R,G,B (vd 255,0,0), hiện là \"" + s + "\"."));
            return;
        }
        for (int i = 1; i <= 3; i++) {
            if (Integer.parseInt(m.group(i)) > 255) {
                out.add(warn(path, "Mỗi thành phần màu phải trong khoảng 0-255."));
                return;
            }
        }
    }

    private interface NodeVisitor { void visit(ActionNode n, String path); }

    /** Duyệt toàn cây; path cùng định dạng với validateNode. */
    private static void walk(List<ActionNode> list, String path, NodeVisitor v) {
        for (int i = 0; i < list.size(); i++) {
            ActionNode n = list.get(i);
            String here = path + "[" + (i + 1) + "](" + String.valueOf(n.getType()).toUpperCase(Locale.ROOT) + ")";
            v.visit(n, here);
            for (Map.Entry<String, List<ActionNode>> e : n.allChildren().entrySet())
                walk(e.getValue(), here + "." + e.getKey(), v);
            for (Map.Entry<String, java.util.LinkedHashMap<String, List<ActionNode>>> e : n.allKeyedChildren().entrySet())
                for (Map.Entry<String, List<ActionNode>> m : e.getValue().entrySet())
                    walk(m.getValue(), here + "." + e.getKey() + "." + m.getKey(), v);
        }
    }

    /** CHECK_VALUE.key nên có ADD_VALUE cùng key trong passive (WARN: có thể passive khác thêm giá trị). */
    private static void checkCounterKeys(List<ActionNode> actions, List<Issue> out) {
        Set<String> added = new HashSet<>();
        List<String[]> checks = new ArrayList<>();
        walk(actions, "actions", (n, path) -> {
            Object k = n.getField("key");
            if (k == null || String.valueOf(k).isBlank() || n.getType() == null) return;
            String t = n.getType().toUpperCase(Locale.ROOT);
            if (t.equals("ADD_VALUE")) added.add(String.valueOf(k).trim());
            else if (t.equals("CHECK_VALUE")) checks.add(new String[]{path, String.valueOf(k).trim()});
        });
        for (String[] c : checks) {
            if (!added.contains(c[1]))
                out.add(warn(c[0] + ".key", "Không có ADD_VALUE cùng khóa \"" + c[1]
                        + "\" trong passive này (bỏ qua nếu passive khác thêm giá trị)."));
        }
    }

    private static boolean soundExists(String s) {
        try { Sound.valueOf(s.trim().toUpperCase(Locale.ROOT)); return true; } catch (IllegalArgumentException ex) { return false; }
    }

    private static boolean particleExists(String s) {
        try { Particle.valueOf(s.trim().toUpperCase(Locale.ROOT)); return true; } catch (IllegalArgumentException ex) { return false; }
    }

    private static boolean potionExists(String s) {
        try {
            if (PotionEffectType.getByName(s.trim().toUpperCase(Locale.ROOT)) != null) return true;
            NamespacedKey k = NamespacedKey.fromString(s.trim().toLowerCase(Locale.ROOT));
            return k != null && Registry.EFFECT.get(k) != null;
        } catch (Exception ex) {
            return false;
        }
    }

    private static void rangeCheck(FieldDef f, double x, String path, List<Issue> out) {
        if (f.min() != null && x < f.min()) out.add(warn(path, "Nhỏ hơn mức tối thiểu " + f.min() + "."));
        if (f.max() != null && x > f.max()) out.add(warn(path, "Lớn hơn mức tối đa " + f.max() + "."));
    }

    private static boolean isNumber(String s) {
        try { Double.parseDouble(s.trim()); return true; } catch (NumberFormatException e) { return false; }
    }

    private static double num(String s) { return Double.parseDouble(s.trim()); }

    private static Issue err(String p, String m) { return new Issue(Level.ERROR, p, m); }
    private static Issue warn(String p, String m) { return new Issue(Level.WARN, p, m); }
}
