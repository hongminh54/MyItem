package org.ThienNguyen.Listener.Passive.Editor;

import org.bukkit.Material;

/**
 * Loại giá trị của một field trong schema action.
 * <p>
 * EXPRESSION = số / biểu thức toán / PlaceholderAPI / nội bộ ({damage}...), luôn lưu dạng String
 * (DROP_ITEM.amount còn nhận cả khoảng "1-3").
 */
public enum FieldKind {
    INT(Material.GOLD_NUGGET, true, false),
    DOUBLE(Material.GOLD_INGOT, true, false),
    EXPRESSION(Material.WRITABLE_BOOK, true, false),
    STRING(Material.PAPER, false, false),
    BOOLEAN(Material.LEVER, false, true),
    ENUM(Material.COMPARATOR, false, true),
    MATERIAL(Material.CHEST, false, false),
    SOUND(Material.NOTE_BLOCK, false, false),
    PARTICLE(Material.BLAZE_POWDER, false, false),
    POTION_EFFECT(Material.POTION, false, false),
    STRING_LIST(Material.BOOK, false, false),
    ACTION_LIST(Material.COMMAND_BLOCK, false, false),
    /** List các map hiệu ứng thuốc {type, amplifier, duration-seconds, ambient, particles, icon} (POTION_ZONE.effects). */
    EFFECT_LIST(Material.BREWING_STAND, false, false);

    private final Material defaultIcon;
    private final boolean numeric;
    private final boolean cycled;

    FieldKind(Material defaultIcon, boolean numeric, boolean cycled) {
        this.defaultIcon = defaultIcon;
        this.numeric = numeric;
        this.cycled = cycled;
    }

    /** Icon mặc định khi FieldDef không chỉ định icon riêng. */
    public Material defaultIcon() {
        return defaultIcon;
    }

    /** INT / DOUBLE / EXPRESSION. */
    public boolean isNumeric() {
        return numeric;
    }

    /** true = đổi giá trị bằng click (không cần chat): BOOLEAN, ENUM. */
    public boolean isCycled() {
        return cycled;
    }

    /** true = field nhận biểu thức / placeholder. */
    public boolean acceptsExpression() {
        return this == EXPRESSION;
    }
}
