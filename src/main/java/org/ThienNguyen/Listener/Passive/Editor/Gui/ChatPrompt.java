package org.ThienNguyen.Listener.Passive.Editor.Gui;

import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Nhập giá trị qua chat. Người chơi gõ "huy" / "cancel" để hủy; tự hủy sau 60 giây.
 * Mọi callback chạy trên main thread.
 */
public final class ChatPrompt {

    private static final long TIMEOUT_TICKS = 20L * 60;
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private record Pending(Function<String, String> validator, Consumer<String> onValue,
                           Runnable onCancel, BukkitTask timeout) {}

    private ChatPrompt() {}

    /**
     * @param hint      dòng hướng dẫn gửi cho người chơi
     * @param validator trả null nếu hợp lệ, ngược lại trả thông báo lỗi (người chơi được nhập lại); null = nhận mọi giá trị
     * @param onValue   nhận chuỗi đã trim
     * @param onCancel  chạy khi hủy/hết giờ (thường là mở lại menu)
     */
    public static void ask(Player p, String hint, Function<String, String> validator,
                           Consumer<String> onValue, Runnable onCancel) {
        cancelPending(p.getUniqueId(), false);
        BukkitTask t = Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            Pending pd = PENDING.remove(p.getUniqueId());
            if (pd != null) {
                p.sendMessage(GuiUtil.color("&cHết thời gian nhập."));
                pd.onCancel().run();
            }
        }, TIMEOUT_TICKS);
        PENDING.put(p.getUniqueId(), new Pending(validator, onValue, onCancel, t));
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> p.closeInventory());
        p.sendMessage(GuiUtil.color("&e&l» &f" + hint));
        p.sendMessage(GuiUtil.color("&7Gõ &chuy &7để quay lại."));
    }

    public static boolean isPrompting(UUID id) {
        return PENDING.containsKey(id);
    }

    private static void cancelPending(UUID id, boolean runCancel) {
        Pending old = PENDING.remove(id);
        if (old == null) return;
        old.timeout().cancel();
        if (runCancel) old.onCancel().run();
    }

    /** Listener chat; đăng ký qua {@link EditorGuiListener#register}. */
    static final class PromptListener implements Listener {

        @EventHandler(priority = EventPriority.LOWEST)
        public void onChat(AsyncPlayerChatEvent e) {
            Player p = e.getPlayer();
            Pending pd = PENDING.get(p.getUniqueId());
            if (pd == null) return;
            e.setCancelled(true);
            String msg = e.getMessage().trim();

            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                if (!PENDING.containsKey(p.getUniqueId())) return;
                if (msg.equalsIgnoreCase("huy") || msg.equalsIgnoreCase("cancel")) {
                    cancelPending(p.getUniqueId(), true);
                    return;
                }
                String err = pd.validator() == null ? null : pd.validator().apply(msg);
                if (err != null) {
                    p.sendMessage(GuiUtil.color("&c" + err + " &7(nhập lại hoặc gõ &chuy&7)"));
                    return;
                }
                cancelPending(p.getUniqueId(), false);
                pd.onValue().accept(msg);
            });
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent e) {
            cancelPending(e.getPlayer().getUniqueId(), false);
        }
    }
}
