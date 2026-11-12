package org.ThienNguyen.Listener.Passive.Editor.Gui;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

/**
 * Chế độ thao tác cho menu danh sách: thường -> XÓA -> SAO CHÉP (nếu allowCopy) -> thường.
 * Cách xóa không cần phím Q (Creative không gửi sự kiện Q): chuột giữa (ClickType.CREATIVE/MIDDLE) hoặc chế độ XÓA,
 * cả hai đều qua {@link ConfirmMenu}.
 */
final class DeleteMode {

    enum Mode { OFF, DELETE, COPY }

    private final boolean allowCopy;
    private Mode mode = Mode.OFF;

    DeleteMode(boolean allowCopy) { this.allowCopy = allowCopy; }

    boolean isDelete() { return mode == Mode.DELETE; }

    boolean isCopy() { return mode == Mode.COPY; }

    void cycle() {
        mode = switch (mode) {
            case OFF -> Mode.DELETE;
            case DELETE -> allowCopy ? Mode.COPY : Mode.OFF;
            case COPY -> Mode.OFF;
        };
    }

    ItemStack button() {
        return switch (mode) {
            case OFF -> GuiUtil.item(Material.GRAY_DYE, "&7Chế độ: thường",
                    "&7Click: chuyển sang chế độ XÓA.",
                    allowCopy ? "&8thường > xóa > sao chép" : "&8Dùng khi phím Q không chạy.");
            case DELETE -> GuiUtil.item(Material.RED_DYE, "&cChế độ: XÓA",
                    "&7Click trái mục để xóa (có xác nhận).", "&8Click nút này để đổi chế độ.");
            case COPY -> GuiUtil.item(Material.LIGHT_BLUE_DYE, "&bChế độ: SAO CHÉP",
                    "&7Click trái mục để chép vào clipboard.", "&8Dán bằng nút Dán bên cạnh.");
        };
    }

    /** Q / Ctrl+Q: xóa ngay (giữ hành vi cũ). */
    static boolean isQuickDelete(ClickType c) {
        return c == ClickType.DROP || c == ClickType.CONTROL_DROP;
    }

    /** Chuột giữa: MIDDLE (Survival) hoặc CREATIVE (Creative) -> xóa có xác nhận. */
    static boolean isAltDelete(ClickType c) {
        return c == ClickType.MIDDLE || c == ClickType.CREATIVE;
    }
}
