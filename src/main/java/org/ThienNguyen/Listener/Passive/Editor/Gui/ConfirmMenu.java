package org.ThienNguyen.Listener.Passive.Editor.Gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Hộp xác nhận Có/Không. "Không" quay về menu cha. */
public final class ConfirmMenu extends EditorMenu {

    private final String question;
    private final Runnable onYes;

    public ConfirmMenu(Player viewer, EditorMenu parent, String question, Runnable onYes) {
        super(viewer, parent, "&8Xác nhận", 3);
        this.question = question;
        this.onYes = onYes;
    }

    @Override
    protected void build() {
        set(11, GuiUtil.item(Material.LIME_CONCRETE, "&aĐồng ý", "&7" + question), e -> onYes.run());
        set(13, GuiUtil.item(Material.PAPER, "&e" + question));
        set(15, GuiUtil.item(Material.RED_CONCRETE, "&cHủy"), e -> back());
        fillEmpty();
    }
}
