package org.ThienNguyen.Listener.Passive.Editor;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Một field khai báo của action. Bất biến: các hàm với-giá-trị trả về bản sao mới.
 *
 * @param key           khóa trong YAML (vd "damage-type")
 * @param kind          loại giá trị
 * @param def           giá trị mặc định (null = không có / phụ thuộc field khác); EXPRESSION lưu String
 * @param allowed       giá trị cho phép (ENUM); rỗng nếu không giới hạn
 * @param nameVN        tên hiển thị tiếng Việt
 * @param descVN        mô tả tiếng Việt (hiển thị làm lore, tự ngắt dòng theo từ nên có thể dài vài câu)
 * @param required      bắt buộc phải có (không có default)
 * @param icon          icon trong GUI
 * @param min           cận dưới (INT/DOUBLE), null = không giới hạn
 * @param max           cận trên (INT/DOUBLE), null = không giới hạn
 * @param visibleKey    field điều khiển hiển thị (null = luôn hiện)
 * @param visibleValues field này chỉ hiện khi visibleKey nhận một trong các giá trị này
 */
public record FieldDef(String key, FieldKind kind, Object def, List<String> allowed,
                       String nameVN, String descVN, boolean required, Material icon,
                       Double min, Double max, String visibleKey, List<String> visibleValues) {

    public FieldDef {
        allowed = allowed == null ? List.of() : List.copyOf(allowed);
        visibleValues = visibleValues == null ? List.of() : List.copyOf(visibleValues);
        if (icon == null) icon = kind.defaultIcon();
    }

    public static FieldDef of(String key, FieldKind kind, Object def, String nameVN, String descVN) {
        return new FieldDef(key, kind, def, List.of(), nameVN, descVN, false, null, null, null, null, List.of());
    }

    public FieldDef withAllowed(String... values) {
        return new FieldDef(key, kind, def, Arrays.asList(values), nameVN, descVN, required, icon, min, max, visibleKey, visibleValues);
    }

    public FieldDef asRequired() {
        return new FieldDef(key, kind, def, allowed, nameVN, descVN, true, icon, min, max, visibleKey, visibleValues);
    }

    public FieldDef withIcon(Material m) {
        return new FieldDef(key, kind, def, allowed, nameVN, descVN, required, m, min, max, visibleKey, visibleValues);
    }

    public FieldDef withRange(Double min, Double max) {
        return new FieldDef(key, kind, def, allowed, nameVN, descVN, required, icon, min, max, visibleKey, visibleValues);
    }

    /** Chỉ hiện field này khi field {@code otherKey} có giá trị thuộc {@code values} (không phân biệt hoa/thường). */
    public FieldDef visibleIf(String otherKey, String... values) {
        return new FieldDef(key, kind, def, allowed, nameVN, descVN, required, icon, min, max, otherKey, Arrays.asList(values));
    }

    public boolean hasCondition() {
        return visibleKey != null;
    }

    /** @param controllingValue giá trị hiện tại (hoặc mặc định) của {@link #visibleKey()} */
    public boolean isVisible(Object controllingValue) {
        if (visibleKey == null) return true;
        if (controllingValue == null) return false;
        String s = String.valueOf(controllingValue).trim().toLowerCase(Locale.ROOT);
        for (String v : visibleValues) {
            if (v.toLowerCase(Locale.ROOT).equals(s)) return true;
        }
        return false;
    }
}
