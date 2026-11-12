package org.ThienNguyen.Listener.Passive.Editor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Một action trong danh sách {@code actions}: {@code type} + các field thường + các nhánh con.
 *
 * <ul>
 *   <li>{@code fields}: mọi key thường (target, amount, ...), kể cả key lạ chưa biết - được giữ nguyên.</li>
 *   <li>{@code children}: key -> danh sách action con (children / actions / on-death / on-empty ...).</li>
 *   <li>{@code keyedChildren}: key -> (khóa -> danh sách action con), dùng cho {@code milestones}.</li>
 * </ul>
 *
 * Thứ tự key của file gốc được ghi nhớ ({@code keyOrder}) và dùng lại khi {@link #toMap()}.
 * Key mới thêm vào sẽ nằm ở cuối.
 */
public final class ActionNode {

    /** Các key luôn chứa danh sách action con (theo kiến trúc editor). */
    static final Set<String> CONTAINER_KEYS = Set.of("children", "actions", "on-death", "on-empty");
    static final String MILESTONES_KEY = "milestones";

    /** Key chứa list-of-map có 'type' nhưng KHÔNG phải action con (vd POTION_ZONE.effects) -> giữ nguyên làm field thường. */
    static final Set<String> RAW_LIST_KEYS = Set.of("effects");

    private String type;
    private final LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
    private final LinkedHashMap<String, List<ActionNode>> children = new LinkedHashMap<>();
    private final LinkedHashMap<String, LinkedHashMap<String, List<ActionNode>>> keyedChildren = new LinkedHashMap<>();
    private final List<String> keyOrder = new ArrayList<>();

    public ActionNode() {}

    public ActionNode(String type) {
        this.type = type;
    }

    // ------------------------------------------------------------------ type / fields

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    /** Map sống: sửa trực tiếp sẽ sửa node. Không chứa 'type' và các nhánh con. */
    public LinkedHashMap<String, Object> fields() { return fields; }

    public Object getField(String key) { return fields.get(key); }

    /** value == null sẽ xóa key (về mặc định). */
    public void setField(String key, Object value) {
        if (value == null) fields.remove(key);
        else fields.put(key, value);
    }

    public void removeField(String key) { fields.remove(key); }

    // ------------------------------------------------------------------ children

    /** Danh sách con tại key (tạo mới nếu chưa có). List sống. */
    public List<ActionNode> children(String key) {
        return children.computeIfAbsent(key, k -> new ArrayList<>());
    }

    public boolean hasChildren(String key) { return children.containsKey(key); }

    public void removeChildren(String key) { children.remove(key); }

    public Map<String, List<ActionNode>> allChildren() { return children; }

    /** Map nhánh con theo khóa (milestones). Map sống, tạo mới nếu chưa có. */
    public LinkedHashMap<String, List<ActionNode>> keyedChildren(String key) {
        return keyedChildren.computeIfAbsent(key, k -> new LinkedHashMap<>());
    }

    public Map<String, LinkedHashMap<String, List<ActionNode>>> allKeyedChildren() { return keyedChildren; }

    public void removeKeyedChildren(String key) { keyedChildren.remove(key); }

    // ------------------------------------------------------------------ raw <-> node

    /** Nhận diện danh sách action con: key container hoặc list không rỗng toàn map có 'type'. */
    static boolean isActionList(String key, Object v) {
        if (!(v instanceof List<?> l)) return false;
        if (RAW_LIST_KEYS.contains(key)) return false;
        boolean container = CONTAINER_KEYS.contains(key);
        if (l.isEmpty()) return container;
        for (Object o : l) {
            if (!(o instanceof Map<?, ?> m)) return false;
            if (!container && !m.containsKey("type")) return false;
        }
        return true;
    }

    /** Nhận diện map khóa -> danh sách action (milestones). */
    static boolean isActionMap(String key, Object v) {
        if (!(v instanceof Map<?, ?> m)) return false;
        boolean milestones = MILESTONES_KEY.equals(key);
        if (m.isEmpty()) return milestones;
        for (Map.Entry<?, ?> e : m.entrySet()) {
            if (!isActionList(milestones ? "children" : key + "#", e.getValue())) return false;
        }
        return true;
    }

    public static ActionNode fromMap(Map<?, ?> raw) {
        ActionNode n = new ActionNode();
        for (Map.Entry<?, ?> e : raw.entrySet()) {
            String key = String.valueOf(e.getKey());
            Object v = e.getValue();
            n.keyOrder.add(key);

            if ("type".equals(key)) {
                n.type = v == null ? null : String.valueOf(v);
            } else if (isActionList(key, v)) {
                n.children.put(key, parseList((List<?>) v));
            } else if (isActionMap(key, v)) {
                LinkedHashMap<String, List<ActionNode>> keyed = new LinkedHashMap<>();
                for (Map.Entry<?, ?> me : ((Map<?, ?>) v).entrySet()) {
                    keyed.put(String.valueOf(me.getKey()), parseList((List<?>) me.getValue()));
                }
                n.keyedChildren.put(key, keyed);
            } else {
                n.fields.put(key, YamlTree.toRawValue(v));
            }
        }
        return n;
    }

    private static List<ActionNode> parseList(List<?> raw) {
        List<ActionNode> out = new ArrayList<>(raw.size());
        for (Object o : raw) out.add(fromMap((Map<?, ?>) o));
        return out;
    }

    /** Dựng lại Map để ghi YAML. Giữ thứ tự gốc; key mới nằm cuối ('type' mới luôn đứng đầu). */
    public LinkedHashMap<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (type != null && !keyOrder.contains("type")) out.put("type", type);

        for (String k : keyOrder) emit(out, k);
        for (String k : fields.keySet()) emit(out, k);
        for (String k : children.keySet()) emit(out, k);
        for (String k : keyedChildren.keySet()) emit(out, k);
        return out;
    }

    private void emit(LinkedHashMap<String, Object> out, String k) {
        if (out.containsKey(k)) return;
        if ("type".equals(k)) {
            if (type != null) out.put("type", type);
        } else if (fields.containsKey(k)) {
            out.put(k, YamlTree.toRawValue(fields.get(k)));
        } else if (children.containsKey(k)) {
            out.put(k, listToMaps(children.get(k)));
        } else if (keyedChildren.containsKey(k)) {
            LinkedHashMap<String, Object> m = new LinkedHashMap<>();
            for (Map.Entry<String, List<ActionNode>> e : keyedChildren.get(k).entrySet()) {
                m.put(e.getKey(), listToMaps(e.getValue()));
            }
            out.put(k, m);
        }
    }

    static List<Object> listToMaps(List<ActionNode> nodes) {
        List<Object> out = new ArrayList<>(nodes.size());
        for (ActionNode n : nodes) out.add(n.toMap());
        return out;
    }

    public ActionNode deepCopy() {
        return fromMap(toMap());
    }

    @Override
    public String toString() {
        return "ActionNode{" + type + "}";
    }
}
