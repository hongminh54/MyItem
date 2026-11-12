package org.ThienNguyen.Listener.Passive.Editor.Gui;

import org.ThienNguyen.Listener.Passive.Editor.ActionSchema;
import org.ThienNguyen.Listener.Passive.Editor.ActionSchema.Category;
import org.ThienNguyen.Listener.Passive.Editor.ActionSchemaRegistry;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Chọn loại action: bước 1 chọn nhóm, bước 2 chọn type. {@code onPick} nhận tên type. */
public final class ActionTypeMenu extends EditorMenu {

    private static final int[] CATEGORY_SLOTS = {10, 11, 12, 14, 15, 16};

    private final Consumer<String> onPick;
    private Category category;

    public ActionTypeMenu(Player viewer, EditorMenu parent, Consumer<String> onPick) {
        super(viewer, parent, "&8Chọn loại action", 5);
        this.onPick = onPick;
    }

    @Override
    protected void build() {
        if (category == null) {
            Category[] cats = Category.values();
            for (int i = 0; i < cats.length && i < CATEGORY_SLOTS.length; i++) {
                Category c = cats[i];
                set(CATEGORY_SLOTS[i], GuiUtil.item(c.icon(), "&e" + c.nameVN(),
                        "&7" + ActionSchemaRegistry.byCategory(c).size() + " loại", "", "&aClick để mở"),
                        e -> { category = c; refresh(); });
            }
            set(40, GuiUtil.item(Material.ARROW, "&eQuay lại"), e -> back());
        } else {
            int slot = 0;
            for (ActionSchema s : ActionSchemaRegistry.byCategory(category)) {
                if (slot >= 36) break;
                List<String> lore = new ArrayList<>(GuiUtil.wrap(s.descVN(), 38, "&7"));
                lore.add("");
                lore.add("&aClick để thêm");
                set(slot++, GuiUtil.item(s.icon(), "&e" + s.nameVN() + " &8(" + s.type() + ")", lore),
                        e -> onPick.accept(s.type()));
            }
            set(40, GuiUtil.item(Material.ARROW, "&eĐổi nhóm"), e -> { category = null; refresh(); });
        }
        fillEmpty();
    }
}
