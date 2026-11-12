package org.ThienNguyen.Listener.Passive.Editor.Gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * Chặn mọi thao tác lấy/đặt item trong menu editor, chuyển click về {@link EditorMenu},
 * và cảnh báo khi đóng menu (ESC) lúc còn thay đổi chưa lưu.
 * Dùng {@code e.getInventory()} thay {@code e.getView()}: InventoryView đổi từ class sang interface ở 1.21.
 */
public final class EditorGuiListener implements Listener {

    /** Gọi 1 lần ở onEnable (cùng chỗ đăng ký các listener Passive khác). */
    public static void register(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new EditorGuiListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new ChatPrompt.PromptListener(), plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof EditorMenu menu)) return;
        e.setCancelled(true); // chặn cả shift-click từ túi đồ người chơi
        menu.handleClick(e);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof EditorMenu) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof EditorMenu)) return;
        if (e.getPlayer() instanceof Player p) EditorSessions.handleClose(p);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        EditorSessions.forget(e.getPlayer().getUniqueId());
    }
}
