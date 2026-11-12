package org.ThienNguyen.Listener.Passive.Editor;

import org.ThienNguyen.Listener.Passive.Trigger.PassiveTrigger;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bản nháp của một passive: bọc quanh {@code LinkedHashMap<String,Object>} giữ đúng cây YAML.
 *
 * <p>Mọi key lạ (top-level, trong condition, trong action) đều nằm sẵn trong map nên được giữ khi ghi lại.
 * Key mới thêm được chèn đúng vị trí chuẩn (id, display-name, trigger, chance, cooldown, condition, actions);
 * key đã có thì giữ nguyên vị trí.
 */
public final class PassiveDraft {

    private static final List<String> CANONICAL_ORDER =
            List.of("id", "display-name", "trigger", "chance", "cooldown", "condition", "actions");

    private final LinkedHashMap<String, Object> root;
    private File sourceFile; // null = chưa lưu lần nào

    private PassiveDraft(LinkedHashMap<String, Object> root, File sourceFile) {
        this.root = root;
        this.sourceFile = sourceFile;
    }

    // ------------------------------------------------------------------ factories

    public static PassiveDraft fromYaml(ConfigurationSection cfg, File sourceFile) {
        return new PassiveDraft(YamlTree.toRaw(cfg), sourceFile);
    }

    /** Passive mới với giá trị mặc định hợp lý (chưa gắn file). */
    public static PassiveDraft blank(String id) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("display-name", id);
        m.put("trigger", "ON_HIT");
        m.put("chance", "100");
        m.put("cooldown", 0);
        m.put("actions", new ArrayList<>());
        return new PassiveDraft(m, null);
    }

    /** Bản sao sâu, chưa gắn file. */
    public PassiveDraft copy() {
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> m = (LinkedHashMap<String, Object>) YamlTree.toRawValue(root);
        return new PassiveDraft(m, null);
    }

    /** Khôi phục nội dung từ một bản snapshot (Undo). Giữ nguyên đối tượng draft và file nguồn. */
    public void restoreFrom(PassiveDraft snapshot) {
        LinkedHashMap<String, Object> fresh = snapshot.copy().root; // sao sâu: snapshot dùng lại được
        root.clear();
        root.putAll(fresh);
    }

    // ------------------------------------------------------------------ raw access

    /** Cây thô (sống). Dùng cho validate / ghi file. */
    public Map<String, Object> raw() { return root; }

    public File getSourceFile() { return sourceFile; }
    void setSourceFile(File f) { this.sourceFile = f; }

    public Object get(String key) { return root.get(key); }

    /** Đặt key tùy ý (key lạ). value == null sẽ xóa key. */
    public void set(String key, Object value) {
        if (value == null) root.remove(key);
        else putCanonical(key, value);
    }

    // ------------------------------------------------------------------ top-level fields

    public String getId() { return str("id", ""); }
    public void setId(String id) { putCanonical("id", id == null ? "" : id.trim()); }

    public String getDisplayName() { return str("display-name", getId()); }
    public void setDisplayName(String v) { setOrRemove("display-name", v); }

    public String getTrigger() { return str("trigger", ""); }
    public void setTrigger(String v) { setOrRemove("trigger", v == null ? null : v.trim().toUpperCase()); }
    public void setTrigger(PassiveTrigger t) { setTrigger(t == null ? null : t.name()); }

    /** Chance dạng chuỗi thô (số hoặc expression). Mặc định "100". */
    public String getChance() { return str("chance", "100"); }
    public void setChance(String v) { setOrRemove("chance", v == null ? null : v.trim()); }

    /** Cooldown dạng chuỗi thô (giây). Mặc định "0". */
    public String getCooldown() { return str("cooldown", "0"); }

    /** Lưu thành số nguyên nếu parse được, ngược lại giữ chuỗi để validate báo lỗi. */
    public void setCooldown(String v) {
        if (v == null || v.isBlank()) { root.remove("cooldown"); return; }
        String t = v.trim();
        if (t.matches("\\d{1,9}")) putCanonical("cooldown", Integer.parseInt(t));
        else putCanonical("cooldown", t);
    }

    // ------------------------------------------------------------------ condition.*

    /** "BOTH" nếu chưa đặt. */
    public String getTargetType() {
        Map<String, Object> c = condition(false);
        return c == null || c.get("target-type") == null ? "BOTH" : String.valueOf(c.get("target-type"));
    }

    public void setTargetType(String v) {
        if (v == null || v.isBlank()) {
            Map<String, Object> c = condition(false);
            if (c != null) c.remove("target-type");
            pruneCondition();
        } else {
            condition(true).put("target-type", v.trim().toUpperCase());
        }
    }

    public boolean isMustBeCrit() {
        Map<String, Object> c = condition(false);
        return c != null && Boolean.parseBoolean(String.valueOf(c.get("must-be-crit")));
    }

    /** false = mặc định, xóa key. */
    public void setMustBeCrit(boolean v) {
        if (v) {
            condition(true).put("must-be-crit", true);
        } else {
            Map<String, Object> c = condition(false);
            if (c != null) c.remove("must-be-crit");
            pruneCondition();
        }
    }

    /** "-1" nếu chưa đặt (nghĩa là không áp dụng). */
    public String getTargetHpPercentBelow() {
        Map<String, Object> c = condition(false);
        return c == null || c.get("target-hp-percent-below") == null
                ? "-1" : String.valueOf(c.get("target-hp-percent-below"));
    }

    public void setTargetHpPercentBelow(String v) {
        if (v == null || v.isBlank()) {
            Map<String, Object> c = condition(false);
            if (c != null) c.remove("target-hp-percent-below");
            pruneCondition();
        } else {
            condition(true).put("target-hp-percent-below", v.trim());
        }
    }

    public List<String> getConditionExpressions() {
        List<String> out = new ArrayList<>();
        Map<String, Object> c = condition(false);
        if (c != null && c.get("expressions") instanceof List<?> l) {
            for (Object o : l) if (o != null) out.add(String.valueOf(o));
        }
        return out;
    }

    /** Danh sách rỗng/null sẽ xóa key. */
    public void setConditionExpressions(List<String> exprs) {
        if (exprs == null || exprs.isEmpty()) {
            Map<String, Object> c = condition(false);
            if (c != null) c.remove("expressions");
            pruneCondition();
        } else {
            condition(true).put("expressions", new ArrayList<Object>(exprs));
        }
    }

    // ------------------------------------------------------------------ actions

    /** Cây action dạng node (bản dựng mới mỗi lần gọi; sửa xong phải gọi {@link #setActions}). */
    public List<ActionNode> getActions() {
        List<ActionNode> out = new ArrayList<>();
        if (root.get("actions") instanceof List<?> l) {
            for (Object o : l) {
                if (o instanceof Map<?, ?> m) out.add(ActionNode.fromMap(m));
            }
        }
        return out;
    }

    public void setActions(List<ActionNode> actions) {
        putCanonical("actions", ActionNode.listToMaps(actions == null ? List.of() : actions));
    }

    // ------------------------------------------------------------------ internals

    private String str(String key, String def) {
        Object v = root.get(key);
        return v == null ? def : String.valueOf(v);
    }

    private void setOrRemove(String key, String v) {
        if (v == null) root.remove(key);
        else putCanonical(key, v);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> condition(boolean create) {
        Object c = root.get("condition");
        if (c instanceof Map<?, ?> m) return (Map<String, Object>) m;
        if (!create) return null;
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        putCanonical("condition", m);
        return m;
    }

    private void pruneCondition() {
        if (root.get("condition") instanceof Map<?, ?> m && m.isEmpty()) root.remove("condition");
    }

    /** Key đã có: giữ vị trí. Key mới: chèn trước key chuẩn đứng sau nó, nếu không có thì thêm cuối. */
    private void putCanonical(String key, Object value) {
        if (root.containsKey(key)) {
            root.put(key, value);
            return;
        }
        int rank = CANONICAL_ORDER.indexOf(key);
        if (rank < 0) {
            root.put(key, value);
            return;
        }
        String before = null;
        for (String k : root.keySet()) {
            int r = CANONICAL_ORDER.indexOf(k);
            if (r > rank) { before = k; break; }
        }
        if (before == null) {
            root.put(key, value);
            return;
        }
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>(root);
        root.clear();
        for (Map.Entry<String, Object> e : copy.entrySet()) {
            if (e.getKey().equals(before)) root.put(key, value);
            root.put(e.getKey(), e.getValue());
        }
    }
}
