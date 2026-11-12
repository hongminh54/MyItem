package org.ThienNguyen.Listener.Passive.Editor.Gui;

import org.ThienNguyen.Listener.Passive.PassiveManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Gán / gỡ id passive trên vật phẩm đang cầm (tay chính) qua PDC "passive_ids" (chuỗi, ngăn cách bằng dấu phẩy).
 * Dùng đúng NamespacedKey của PassiveManager và gọi invalidatePassiveCache để có hiệu lực ngay.
 */
final class HeldItemPassive {

    private HeldItemPassive() {}

    /** Kết quả thao tác: {@code ok} + thông báo (có mã màu &) để gửi cho người chơi. */
    record Result(boolean ok, String message) {}

    private static NamespacedKey key() {
        return PassiveManager.pdcKey();
    }

    private static boolean isEmpty(ItemStack it) {
        return it == null || it.getType().isAir();
    }

    private static Set<String> parse(String raw) {
        Set<String> ids = new LinkedHashSet<>();
        if (raw == null) return ids;
        for (String s : raw.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) ids.add(t);
        }
        return ids;
    }

    /** Các id đang có trên vật phẩm tay chính (rỗng nếu tay không / không có PDC). */
    static List<String> idsOnHeld(Player p) {
        ItemStack it = p.getInventory().getItemInMainHand();
        if (isEmpty(it)) return List.of();
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return List.of();
        String raw = meta.getPersistentDataContainer().get(key(), PersistentDataType.STRING);
        return new ArrayList<>(parse(raw));
    }

    static boolean holdingItem(Player p) {
        return !isEmpty(p.getInventory().getItemInMainHand());
    }

    /** Thêm id (không trùng). */
    static Result assign(Player p, String id) {
        return modify(p, id, true);
    }

    /** Gỡ id; gỡ hết thì xóa luôn key PDC. */
    static Result remove(Player p, String id) {
        return modify(p, id, false);
    }

    private static Result modify(Player p, String id, boolean add) {
        ItemStack it = p.getInventory().getItemInMainHand();
        if (isEmpty(it)) return new Result(false, "&cHãy cầm vật phẩm cần gán trên tay chính.");
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return new Result(false, "&cVật phẩm này không hỗ trợ dữ liệu PDC.");

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Set<String> ids = parse(pdc.get(key(), PersistentDataType.STRING));

        if (add) {
            if (!ids.add(id)) return new Result(false, "&eVật phẩm đã có passive &f" + id + "&e.");
        } else {
            if (!ids.remove(id)) return new Result(false, "&eVật phẩm không có passive &f" + id + "&e.");
        }

        if (ids.isEmpty()) pdc.remove(key());
        else pdc.set(key(), PersistentDataType.STRING, String.join(",", ids));

        it.setItemMeta(meta);
        p.getInventory().setItemInMainHand(it); // ghi lại chắc chắn (phòng khi it là bản sao)

        PassiveManager pm = PassiveManager.getInstance();
        if (pm != null) pm.invalidatePassiveCache(p.getUniqueId());
        return new Result(true, add ? "&aĐã gán passive &f" + id + "&a vào vật phẩm đang cầm."
                : "&aĐã gỡ passive &f" + id + "&a khỏi vật phẩm đang cầm.");
    }
}
