package org.ThienNguyen.Listener.Passive.Editor;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Schema của một loại action (type) trong passive.
 *
 * @param type     tên type trong YAML (vd "DAMAGE")
 * @param category nhóm trong menu chọn loại
 * @param icon     icon trong menu
 * @param nameVN   tên hiển thị tiếng Việt
 * @param descVN   mô tả tiếng Việt (hiển thị làm lore, tự ngắt dòng theo từ nên có thể dài vài câu)
 * @param fields   các field (không gồm "type")
 */
public record ActionSchema(String type, Category category, Material icon,
                           String nameVN, String descVN, List<FieldDef> fields) {

    public ActionSchema {
        fields = List.copyOf(fields);
    }

    /** Nhóm action cho menu chọn loại. */
    public enum Category {
        DAMAGE("Sát Thương", Material.IRON_SWORD),
        SUPPORT("Hỗ Trợ", Material.GOLDEN_APPLE),
        MOVEMENT("Di Chuyển", Material.FEATHER),
        WORLD("Thế Giới & Vật Phẩm", Material.DIAMOND_PICKAXE),
        VISUAL("Hiệu Ứng Hiển Thị", Material.FIREWORK_STAR),
        UTILITY("Tiện Ích", Material.COMMAND_BLOCK);

        private final String nameVN;
        private final Material icon;

        Category(String nameVN, Material icon) {
            this.nameVN = nameVN;
            this.icon = icon;
        }

        public String nameVN() {
            return nameVN;
        }

        public Material icon() {
            return icon;
        }
    }

    public FieldDef field(String key) {
        for (FieldDef f : fields) {
            if (f.key().equals(key)) return f;
        }
        return null;
    }

    /** Giá trị hiệu lực của một key: giá trị đã set trong action, nếu không có thì mặc định của field. */
    public Object effectiveValue(Map<String, Object> action, String key) {
        if (action != null && action.containsKey(key)) return action.get(key);
        FieldDef f = field(key);
        return f == null ? null : f.def();
    }

    /** Các field đang hiện với action này (áp dụng visibleIf). */
    public List<FieldDef> visibleFields(Map<String, Object> action) {
        List<FieldDef> out = new ArrayList<>(fields.size());
        for (FieldDef f : fields) {
            if (!f.hasCondition() || f.isVisible(effectiveValue(action, f.visibleKey()))) out.add(f);
        }
        return out;
    }
}
