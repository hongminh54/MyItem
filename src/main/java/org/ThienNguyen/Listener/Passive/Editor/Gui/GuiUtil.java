package org.ThienNguyen.Listener.Passive.Editor.Gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Tiện ích dựng ItemStack / màu cho GUI editor (dùng chuỗi legacy '&' để tương thích Spigot + Paper). */
public final class GuiUtil {

    private GuiUtil() {}

    public static String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }

    public static ItemStack item(Material mat, String name, String... lore) {
        return item(mat, name, Arrays.asList(lore));
    }

    public static ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat == null ? Material.PAPER : mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            List<String> l = new ArrayList<>(lore.size());
            for (String s : lore) l.add(color(s));
            meta.setLore(l);
            meta.addItemFlags(ItemFlag.values());
            it.setItemMeta(meta);
        }
        return it;
    }

    public static ItemStack filler() {
        return item(Material.GRAY_STAINED_GLASS_PANE, " ");
    }

    /** Cắt chuỗi dài để vừa lore (thêm "..."). */
    public static String clip(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 3)) + "...";
    }

    /** Giá trị hiển thị trong lore: rỗng -> "(trống)". */
    public static String show(Object v) {
        if (v == null) return "&8(trống)";
        String s = String.valueOf(v);
        return s.isEmpty() ? "&8(trống)" : "&f" + clip(s, 40);
    }

    /** Ngắt dòng mô tả cho lore (theo từ), mỗi dòng có tiền tố màu. */
    public static List<String> wrap(String text, int width, String prefix) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;
        StringBuilder line = new StringBuilder();
        for (String w : text.split(" ")) {
            if (line.length() > 0 && line.length() + 1 + w.length() > width) {
                out.add(prefix + line);
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(w);
        }
        if (line.length() > 0) out.add(prefix + line);
        return out;
    }
}
