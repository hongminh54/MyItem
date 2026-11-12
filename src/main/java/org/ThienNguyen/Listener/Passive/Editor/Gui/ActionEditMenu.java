package org.ThienNguyen.Listener.Passive.Editor.Gui;

import org.ThienNguyen.Listener.Passive.Editor.ActionNode;
import org.ThienNguyen.Listener.Passive.Editor.ActionSchema;
import org.ThienNguyen.Listener.Passive.Editor.ActionSchemaRegistry;
import org.ThienNguyen.Listener.Passive.Editor.FieldDef;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Sửa các field của một action theo {@link ActionSchema}. Chỉ hiện field đang visible (visibleIf).
 * Shift+Click (field thường) = xóa về mặc định. Không tự xóa giá trị trùng mặc định (tránh lệch với plugin).
 */
public final class ActionEditMenu extends EditorMenu {

    private final ActionTree tree;
    private final ActionNode node;
    private final ActionSchema schema;

    public ActionEditMenu(Player viewer, EditorMenu parent, ActionTree tree, ActionNode node) {
        super(viewer, parent, "&8" + title(node), 6);
        this.tree = tree;
        this.node = node;
        this.schema = ActionSchemaRegistry.get(node.getType());
    }

    private static String title(ActionNode n) {
        ActionSchema s = ActionSchemaRegistry.get(n.getType());
        return s == null ? String.valueOf(n.getType()) : s.nameVN();
    }

    // ------------------------------------------------------------------ build

    @Override
    protected void build() {
        if (schema == null) { back(); return; }

        List<FieldDef> fields = schema.visibleFields(node.fields());
        for (int i = 0; i < fields.size() && i < 45; i++) {
            FieldDef f = fields.get(i);
            set(i, fieldItem(f), e -> onField(f, e));
        }

        set(45, GuiUtil.item(Material.ARROW, "&eQuay lại"), e -> back());
        set(47, GuiUtil.item(Material.MAP, "&bXem YAML", "&7In YAML action này ra chat."),
                e -> YamlPreview.send(viewer, this, "action " + node.getType(), node.toMap()));
        List<String> info = new ArrayList<>(GuiUtil.wrap(schema.descVN(), 38, "&7"));
        info.add("");
        info.add("&8Type: " + schema.type());
        set(49, GuiUtil.item(schema.icon(), "&e" + schema.nameVN(), info));

        List<String> unknown = new ArrayList<>();
        for (String k : node.fields().keySet()) if (schema.field(k) == null) unknown.add("&7- " + k);
        if (!unknown.isEmpty()) {
            unknown.add(0, "&7Được giữ nguyên khi lưu:");
            set(53, GuiUtil.item(Material.BOOK, "&7Key lạ (" + (unknown.size() - 1) + ")", unknown));
        }
        fillEmpty();
    }

    private org.bukkit.inventory.ItemStack fieldItem(FieldDef f) {
        Object cur = node.getField(f.key());
        boolean has = cur != null;
        boolean missing = f.required() && !has && f.def() == null;

        List<String> lore = new ArrayList<>(GuiUtil.wrap(f.descVN(), 38, "&7"));
        lore.add("");
        switch (f.kind()) {
            case ACTION_LIST -> {
                int n = "milestones".equals(f.key())
                        ? (node.allKeyedChildren().containsKey("milestones") ? node.keyedChildren("milestones").size() : 0)
                        : (node.hasChildren(f.key()) ? node.children(f.key()).size() : 0);
                lore.add("&7Hiện có: &f" + n + ("milestones".equals(f.key()) ? " mốc" : " action"));
                lore.add("&aClick để mở");
            }
            case EFFECT_LIST -> {
                lore.add("&7Hiện có: &f" + (cur instanceof List<?> x ? x.size() : 0) + " hiệu ứng");
                lore.add("&aClick để mở");
            }
            case STRING_LIST -> {
                List<?> l = cur instanceof List<?> x ? x : List.of();
                if (l.isEmpty()) lore.add("&8(trống)");
                for (int i = 0; i < l.size() && i < 6; i++) lore.add("&7" + (i + 1) + ". &f" + GuiUtil.clip(String.valueOf(l.get(i)), 36));
                if (l.size() > 6) lore.add("&8... +" + (l.size() - 6));
                lore.add("");
                lore.add("&aTrái: thêm dòng  &aPhải: xóa dòng cuối");
                lore.add("&cShift+Phải: xóa hết");
            }
            default -> {
                if (has) lore.add("&7Giá trị: " + GuiUtil.show(cur));
                else if (f.def() != null) lore.add("&7Giá trị: &8(mặc định " + f.def() + ")");
                else lore.add(f.required() ? "&cBắt buộc - chưa có giá trị" : "&7Giá trị: &8(trống)");
                if (f.min() != null || f.max() != null)
                    lore.add("&8Khoảng: " + (f.min() == null ? "-∞" : f.min()) + " → " + (f.max() == null ? "∞" : f.max()));
                if (!f.allowed().isEmpty()) lore.add("&8Cho phép: " + GuiUtil.clip(String.join(", ", f.allowed()), 38));
                if (f.kind() == org.ThienNguyen.Listener.Passive.Editor.FieldKind.EXPRESSION) {
                    lore.add("&8Nhận: số, biểu thức (vd %player_level% * 2),");
                    lore.add("&8%placeholder% PAPI, {damage}, {victim_hp}...");
                }
                lore.add("");
                lore.add(f.kind().isCycled() ? "&aTrái: kế tiếp  &aPhải: trước đó" : "&aClick để nhập/chọn");
                lore.add("&cShift+Click: về mặc định");
            }
        }

        Material icon = f.icon();
        if (f.kind() == org.ThienNguyen.Listener.Passive.Editor.FieldKind.MATERIAL && has) {
            Material m = Material.matchMaterial(String.valueOf(cur));
            if (m != null && m.isItem()) icon = m;
        }
        String name = (missing ? "&c" : "&e") + f.nameVN() + " &8(" + f.key() + ")";
        return GuiUtil.item(icon, name, lore);
    }

    // ------------------------------------------------------------------ click

    private void onField(FieldDef f, InventoryClickEvent e) {
        switch (f.kind()) {
            case ACTION_LIST -> openChildren(f);
            case EFFECT_LIST -> new EffectListMenu(viewer, this, tree, node, f).open();
            case STRING_LIST -> editStringList(f, e);
            default -> {
                if (e.isShiftClick()) { node.removeField(f.key()); changed(); return; }
                switch (f.kind()) {
                    case BOOLEAN -> {
                        Object cur = node.getField(f.key());
                        boolean b = cur != null ? Boolean.parseBoolean(String.valueOf(cur))
                                : f.def() != null && Boolean.parseBoolean(String.valueOf(f.def()));
                        node.setField(f.key(), !b);
                        changed();
                    }
                    case ENUM -> cycleEnum(f, e.isRightClick() ? -1 : 1);
                    case INT, DOUBLE -> promptNumber(f);
                    case EXPRESSION -> prompt(f, "Nhập " + f.nameVN() + " (số, biểu thức hoặc %placeholder%):", null, ExprValue::of);
                    case STRING -> prompt(f, "Nhập " + f.nameVN() + ":", null, s -> s);
                    default -> pick(f);
                }
            }
        }
    }

    private void cycleEnum(FieldDef f, int dir) {
        if (f.allowed().isEmpty()) {
            prompt(f, "Nhập " + f.nameVN() + ":", null, s -> s);
            return;
        }
        Object cur = node.getField(f.key());
        String curS = cur != null ? String.valueOf(cur) : f.def() == null ? "" : String.valueOf(f.def());
        List<String> a = f.allowed();
        int idx = -1;
        for (int i = 0; i < a.size(); i++) if (a.get(i).equalsIgnoreCase(curS)) idx = i;
        int next = idx < 0 ? (dir > 0 ? 0 : a.size() - 1) : Math.floorMod(idx + dir, a.size());
        node.setField(f.key(), a.get(next));
        changed();
    }

    private void promptNumber(FieldDef f) {
        boolean isInt = f.kind() == org.ThienNguyen.Listener.Passive.Editor.FieldKind.INT;
        Function<String, String> check = s -> {
            double v;
            try {
                v = isInt ? Long.parseLong(s) : Double.parseDouble(s);
            } catch (NumberFormatException ex) {
                return isInt ? "Phải là số nguyên." : "Phải là số.";
            }
            if (f.min() != null && v < f.min()) return "Phải >= " + f.min() + ".";
            if (f.max() != null && v > f.max()) return "Phải <= " + f.max() + ".";
            return null;
        };
        prompt(f, "Nhập " + f.nameVN() + " (" + (isInt ? "số nguyên" : "số") + "):", check,
                s -> isInt ? (Object) Integer.valueOf((int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, Long.parseLong(s))))
                        : (Object) Double.valueOf(s));
    }

    /** Hỏi chat -> set field -> commit -> mở lại menu. {@code parse} nhận chuỗi đã qua validator. */
    private void prompt(FieldDef f, String hint, Function<String, String> validator, Function<String, Object> parse) {
        Function<String, String> v = s -> s.isBlank() ? "Không được trống." : validator == null ? null : validator.apply(s);
        ChatPrompt.ask(viewer, hint, v, s -> {
            node.setField(f.key(), parse.apply(s));
            tree.commit();
            open();
        }, this::open);
    }

    private void pick(FieldDef f) {
        Consumer<String> done = v -> {
            node.setField(f.key(), v);
            tree.commit();
            open();
        };
        PickerMenu p = PickerMenu.forKind(viewer, this, f.kind(), done);
        if (p != null) p.open();
    }

    @SuppressWarnings("unchecked")
    private void editStringList(FieldDef f, InventoryClickEvent e) {
        List<Object> cur = new ArrayList<>(node.getField(f.key()) instanceof List<?> l ? (List<Object>) l : List.of());
        if (e.isRightClick()) {
            if (e.isShiftClick()) cur.clear();
            else if (!cur.isEmpty()) cur.remove(cur.size() - 1);
            node.setField(f.key(), cur.isEmpty() ? null : cur);
            changed();
            return;
        }
        ChatPrompt.ask(viewer, "Nhập dòng mới cho " + f.nameVN() + " (hỗ trợ &màu):",
                s -> s.isBlank() ? "Không được trống." : null, s -> {
                    cur.add(s);
                    node.setField(f.key(), cur);
                    tree.commit();
                    open();
                }, this::open);
    }

    private void openChildren(FieldDef f) {
        String key = f.key();
        if ("milestones".equals(key)) {
            new MilestonesMenu(viewer, this, tree, node).open();
            return;
        }
        List<ActionNode> l = node.children(key);
        new ActionListMenu(viewer, this, tree, l, "&8" + f.nameVN(), () -> {
            if (l.isEmpty() && node.hasChildren(key)) { // không để lại "key: []" thừa
                node.removeChildren(key);
                tree.commit();
            }
        }).open();
    }

    private void changed() {
        tree.commit();
        refresh();
    }
}
