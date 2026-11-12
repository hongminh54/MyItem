package org.ThienNguyen.Listener.Passive.Editor.Gui;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Giữ bản nháp (PassiveEditMenu) theo người chơi để: (1) cảnh báo khi đóng GUI bằng ESC lúc còn thay đổi chưa lưu,
 * (2) giữ bản nháp để bỏ bằng /passiveeditor discard. Bản nháp mất khi người chơi thoát server.
 */
public final class EditorSessions {

    private static final Map<UUID, PassiveEditMenu> SESSIONS = new ConcurrentHashMap<>();
    /** Đếm số lần mở menu editor; đổi giá trị = vừa chuyển sang menu khác (không phải ESC). */
    private static final Map<UUID, Integer> OPEN_COUNT = new ConcurrentHashMap<>();
    /** Lần đóng tiếp theo do plugin chủ động (đọc chat) -> không cảnh báo. */
    private static final Set<UUID> QUIET = ConcurrentHashMap.newKeySet();
    /** Menu editor mở gần nhất của từng người chơi (giữ lại để tham chiếu). */
    private static final Map<UUID, EditorMenu> LAST = new ConcurrentHashMap<>();

    private EditorSessions() {}

    static void track(PassiveEditMenu m) {
        SESSIONS.put(m.viewer().getUniqueId(), m);
    }

    /** Bỏ bản nháp (và menu gần nhất, vì có thể thuộc bản nháp đó). */
    public static void clear(UUID id) {
        SESSIONS.remove(id);
        LAST.remove(id);
    }

    /** Bản nháp đang có thay đổi chưa lưu, hoặc null. */
    public static PassiveEditMenu pending(UUID id) {
        PassiveEditMenu m = SESSIONS.get(id);
        return m != null && m.isDirty() ? m : null;
    }

    static void markOpened(EditorMenu m) {
        UUID id = m.viewer().getUniqueId();
        OPEN_COUNT.merge(id, 1, Integer::sum);
        LAST.put(id, m);
    }

    /** Menu editor mở gần nhất của người chơi, hoặc null. */
    public static EditorMenu last(UUID id) {
        return LAST.get(id);
    }

    /** Gọi ngay trước viewer.closeInventory() khi chủ động đóng (vd để in lỗi ra chat). */
    static void quietClose(Player p) {
        QUIET.add(p.getUniqueId());
    }

    static void forget(UUID id) {
        SESSIONS.remove(id);
        OPEN_COUNT.remove(id);
        QUIET.remove(id);
        LAST.remove(id);
        ActionClipboard.clear(id);
    }

    /**
     * Gọi từ InventoryCloseEvent của menu editor. Chờ 1 tick: nếu trong lúc đó không mở menu editor khác
     * và không đang nhập chat -> người chơi đã thoát bằng ESC -> cảnh báo.
     */
    static void handleClose(Player p) {
        UUID id = p.getUniqueId();
        if (QUIET.remove(id)) return;
        if (pending(id) == null) return;
        int snap = OPEN_COUNT.getOrDefault(id, 0);
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            if (!p.isOnline()) return;
            if (OPEN_COUNT.getOrDefault(id, 0) != snap) return; // đã mở menu khác
            if (ChatPrompt.isPrompting(id)) return;             // đang nhập chat
            PassiveEditMenu cur = pending(id);
            if (cur != null) warn(p, cur);
        });
    }

    private static TextComponent legacy(String s) {
        return new TextComponent(TextComponent.fromLegacyText(GuiUtil.color(s)));
    }

    private static void warn(Player p, PassiveEditMenu s) {
        TextComponent line = legacy("&e⚠ Bạn vừa đóng editor khi passive &f" + s.draft().getId() + " &echưa lưu. ");
        TextComponent discard = legacy(" &c[Bỏ]");
        discard.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/passiveeditor discard"));
        line.addExtra((BaseComponent) discard);
        p.spigot().sendMessage(line);
        p.sendMessage(GuiUtil.color("&7Bản nháp được giữ đến khi bạn thoát server."));
    }
}
