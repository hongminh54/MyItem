package org.ThienNguyen.Listener.Passive.Editor;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chuyển cây cấu hình của Bukkit thành cây Map/List thuần (giữ nguyên thứ tự key).
 * Cũng dùng để deep-copy: Map -> LinkedHashMap mới, List -> ArrayList mới, scalar giữ nguyên.
 */
final class YamlTree {

    private YamlTree() {}

    static LinkedHashMap<String, Object> toRaw(ConfigurationSection section) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : section.getValues(false).entrySet()) {
            out.put(e.getKey(), toRawValue(e.getValue()));
        }
        return out;
    }

    static Object toRawValue(Object v) {
        if (v instanceof ConfigurationSection cs) {
            return toRaw(cs);
        }
        if (v instanceof Map<?, ?> m) {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                // Key số (vd milestones "3:") được snakeyaml đọc thành Integer -> đưa về String.
                out.put(String.valueOf(e.getKey()), toRawValue(e.getValue()));
            }
            return out;
        }
        if (v instanceof List<?> l) {
            List<Object> out = new ArrayList<>(l.size());
            for (Object o : l) out.add(toRawValue(o));
            return out;
        }
        return v;
    }
}
