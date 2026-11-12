package org.ThienNguyen.Listener.Passive.Editor.Gui;

import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** In YAML của passive/action ra chat (đúng nội dung sẽ ghi, chưa gồm comment) rồi mở lại editor ngay. */
public final class YamlPreview {

    private static final int MAX_LINES = 120;
    private static final Pattern KEY_LINE = Pattern.compile("^(\\s*(?:-\\s+)?)([^:\\s][^:]*?):(\\s.*)?$");

    private YamlPreview() {}

    public static String dump(Map<String, Object> map) {
        YamlConfiguration c = new YamlConfiguration();
        for (Map.Entry<String, Object> e : map.entrySet()) c.set(e.getKey(), e.getValue());
        return c.saveToString();
    }

    /**
     * Đóng GUI (không cảnh báo ESC), in preview ra chat rồi MỞ LẠI NGAY menu {@code reopen}.
     * Dump ngay lúc gọi để phản ánh đúng trạng thái hiện tại.
     */
    public static void send(Player p, EditorMenu reopen, String title, Map<String, Object> map) {
        String[] lines = dump(map).split("\n");
        // Tránh đóng inventory ngay trong InventoryClickEvent -> trì hoãn 1 tick
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            if (!p.isOnline()) return;
            EditorSessions.quietClose(p);
            p.closeInventory();
            p.sendMessage(GuiUtil.color("&8&m-----&r &bYAML: &f" + title + " &8&m-----"));
            int shown = Math.min(lines.length, MAX_LINES);
            for (int i = 0; i < shown; i++) p.sendMessage(colorize(lines[i]));
            if (lines.length > shown) {
                p.sendMessage(GuiUtil.color("&8... còn " + (lines.length - shown) + " dòng nữa (đã ẩn)."));
            }
            reopen.open();
        });
    }

    private static String colorize(String line) {
        Matcher m = KEY_LINE.matcher(line);
        if (!m.matches()) return "\u00a7f" + line;
        String rest = m.group(3) == null ? "" : m.group(3);
        return "\u00a7r" + m.group(1) + "\u00a7e" + m.group(2) + "\u00a77:\u00a7f" + rest;
    }
}
