package org.ThienNguyen.Listener;

import org.ThienNguyen.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper dùng chung cho CacheListener + StatsListener.
 * Mục đích: gom các thứ trước đây bị tính lại MỖI LẦN refresh (rất nhiều lần/giây):
 *  - NamespacedKey ("slot_<stat>", "slot_pct_<stat>", "owner_tag") -> tạo 1 lần
 *  - luật stats-slots trong config -> parse 1 lần
 *  - dòng gem (apply.stats / element / ability / BUFF) -> parse 1 lần
 *  - layout jewelry (player-slots / slots) -> parse 1 lần
 * Cache tự reset khi object config bị thay (reloadConfig). Nếu bạn reload config theo kiểu
 * load() tại chỗ (cùng object) thì gọi ItemStatHelper.invalidateAll() trong lệnh reload.
 */
public final class ItemStatHelper {
    private ItemStatHelper() {}

    // ---------------------------------------------------------------- keys
    private static final Map<String, NamespacedKey> KEYS = new ConcurrentHashMap<>();
    private static final Map<String, NamespacedKey> SLOT_KEYS = new ConcurrentHashMap<>();
    private static final Map<String, NamespacedKey> SLOT_PCT_KEYS = new ConcurrentHashMap<>();

    public static NamespacedKey key(String name) {
        return KEYS.computeIfAbsent(name, n -> new NamespacedKey(Main.getInstance(), n));
    }

    // ---------------------------------------------------------------- slot names
    private static final String[] NORMALIZED;   // "mainhand", "offhand", "head"...
    private static final String[] NAME_LC;      // "hand", "off_hand", "head"...

    static {
        EquipmentSlot[] all = EquipmentSlot.values();
        NORMALIZED = new String[all.length];
        NAME_LC = new String[all.length];
        for (EquipmentSlot s : all) {
            String lc = s.name().toLowerCase();
            String norm = lc.replace("_", "");
            if (norm.equals("hand")) norm = "mainhand";
            NORMALIZED[s.ordinal()] = norm;
            NAME_LC[s.ordinal()] = lc;
        }
    }

    // ---------------------------------------------------------------- stats-slots rules
    private static final Map<String, Set<String>> SLOT_RULES = new ConcurrentHashMap<>();
    private static FileConfiguration slotRulesFor;

    private static Set<String> slotRule(FileConfiguration cfg, String stat) {
        if (cfg != slotRulesFor) {
            SLOT_RULES.clear();
            slotRulesFor = cfg;
        }
        return SLOT_RULES.computeIfAbsent(stat, s -> {
            List<String> list = cfg.getStringList("stats-slots." + s);
            if (list == null || list.isEmpty()) return Collections.emptySet();
            Set<String> out = new HashSet<>();
            for (String v : list) out.add(v.toLowerCase());
            return out;
        });
    }

    /**
     * Item này có được cộng stat `stat` khi đang ở `slot` không?
     * Đọc trực tiếp từ PDC đã lấy sẵn (không gọi item.getItemMeta() nữa).
     */
    public static boolean allowed(PersistentDataContainer pdc, FileConfiguration cfg, String stat,
                                  EquipmentSlot slot, boolean percent) {
        Map<String, NamespacedKey> cache = percent ? SLOT_PCT_KEYS : SLOT_KEYS;
        String prefix = percent ? "slot_pct_" : "slot_";
        NamespacedKey key = cache.computeIfAbsent(stat, s -> new NamespacedKey(Main.getInstance(), prefix + s.toLowerCase()));
        String required = pdc.get(key, PersistentDataType.STRING);
        int o = slot.ordinal();
        if (required != null) {
            if (required.equalsIgnoreCase("any")) return true;
            String cur = NORMALIZED[o];
            for (String s : required.split(",")) {
                if (s.trim().equalsIgnoreCase(cur)) return true;
            }
            return false;
        }
        if (cfg == null) return true;
        Set<String> rule = slotRule(cfg, stat);
        if (rule.isEmpty()) return true;
        return rule.contains(NORMALIZED[o]) || rule.contains(NAME_LC[o]);
    }

    public static boolean isOwner(Player player, PersistentDataContainer pdc) {
        String owners = pdc.get(key("owner_tag"), PersistentDataType.STRING);
        if (owners == null || owners.isEmpty()) return true;
        String name = player.getName();
        for (String owner : owners.split(",")) {
            if (owner.trim().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- gems
    public record StatLine(String type, double value) {}
    public record ElemLine(String id, double value) {}
    public record AbilityLine(String name, double level, double chance) {}
    public record BuffLine(PotionEffectType type, int amplifier) {}

    public static final class GemData {
        public final List<StatLine> stats = new ArrayList<>();
        public final List<ElemLine> elements = new ArrayList<>();
        public final List<AbilityLine> abilities = new ArrayList<>();
        public final List<BuffLine> buffs = new ArrayList<>();
    }

    private static final Map<String, GemData> GEMS = new ConcurrentHashMap<>();
    private static FileConfiguration gemsFor;

    public static GemData gem(FileConfiguration gemConfig, String gemId) {
        if (gemConfig != gemsFor) {
            GEMS.clear();
            gemsFor = gemConfig;
        }
        return GEMS.computeIfAbsent(gemId, id -> parseGem(gemConfig, id));
    }

    private static GemData parseGem(FileConfiguration cfg, String id) {
        GemData d = new GemData();
        for (String line : cfg.getStringList(id + ".apply.stats")) {
            String[] parts = line.split(":", 2);
            if (parts.length < 2) continue;
            String type = parts[0].trim().toLowerCase();
            // NamespacedKey("slot_"+type) chỉ nhận [a-z0-9/._-]; dòng sai sẽ bị bỏ (trước đây ném exception rồi nuốt)
            if (!type.matches("[a-z0-9/._-]+")) continue;
            try {
                d.stats.add(new StatLine(type, Double.parseDouble(parts[1].trim())));
            } catch (NumberFormatException ignored) {}
        }
        for (String line : cfg.getStringList(id + ".apply.element")) {
            String[] parts = line.split(":", 2);
            if (parts.length != 2) continue;
            try {
                d.elements.add(new ElemLine(parts[0].trim().toUpperCase(), Double.parseDouble(parts[1].trim())));
            } catch (NumberFormatException ignored) {}
        }
        for (String line : cfg.getStringList(id + ".apply.ability")) {
            String[] p = line.split(":");
            if (p.length < 2) continue;
            String[] vals = p[1].trim().split("\\s+");
            if (vals.length < 2) continue;
            try {
                d.abilities.add(new AbilityLine(p[0].trim().toUpperCase(),
                        Double.parseDouble(vals[0]), Double.parseDouble(vals[1])));
            } catch (NumberFormatException ignored) {}
        }
        for (String line : cfg.getStringList(id + ".apply.BUFF")) {
            String[] parts = line.split(":", 2);
            if (parts.length < 2) continue;
            @SuppressWarnings("deprecation")
            PotionEffectType type = PotionEffectType.getByName(parts[0].trim().toUpperCase());
            if (type == null) continue;
            try {
                d.buffs.add(new BuffLine(type, Math.max(0, Integer.parseInt(parts[1].trim()) - 1)));
            } catch (NumberFormatException ignored) {}
        }
        return d;
    }

    // ---------------------------------------------------------------- jewelry layout
    public static final class JewelryLayout {
        private final Map<Integer, String> playerSlotType = new HashMap<>();
        private final Map<String, Set<Integer>> groupSlots = new HashMap<>();
        /** các key số của jewelry.player-slots */
        public int[] playerSlots = new int[0];
        /** tất cả slot trang sức (player-slots + slots) */
        public int[] allSlots = new int[0];

        public boolean matches(String itemType, int slotIdx) {
            String pt = playerSlotType.get(slotIdx);
            if (pt != null && pt.equalsIgnoreCase(itemType)) return true;
            Set<Integer> s = groupSlots.get(itemType);
            return s != null && s.contains(slotIdx);
        }
    }

    private static JewelryLayout layout;
    private static FileConfiguration layoutFor;

    public static JewelryLayout jewelryLayout(FileConfiguration config) {
        if (layout == null || config != layoutFor) {
            layout = buildLayout(config);
            layoutFor = config;
        }
        return layout;
    }

    private static JewelryLayout buildLayout(FileConfiguration config) {
        JewelryLayout l = new JewelryLayout();
        TreeSet<Integer> all = new TreeSet<>();
        List<Integer> player = new ArrayList<>();

        ConfigurationSection pSlots = config.getConfigurationSection("jewelry.player-slots");
        if (pSlots != null) {
            for (String key : pSlots.getKeys(false)) {
                try {
                    int idx = Integer.parseInt(key);
                    player.add(idx);
                    all.add(idx);
                    String t = pSlots.getString(key + ".type");
                    if (t != null) l.playerSlotType.put(idx, t);
                } catch (NumberFormatException ignored) {}
            }
        }
        ConfigurationSection groups = config.getConfigurationSection("jewelry.slots");
        if (groups != null) {
            for (String type : groups.getKeys(false)) {
                Set<Integer> set = l.groupSlots.computeIfAbsent(type, k -> new HashSet<>());
                if (groups.isList(type)) {
                    List<?> list = groups.getList(type);
                    if (list == null) continue;
                    for (Object obj : list) {
                        if (obj instanceof Map<?, ?> map && map.get("slot") instanceof Number num) {
                            set.add(num.intValue());
                            all.add(num.intValue());
                        }
                    }
                } else {
                    int s = groups.getInt(type + ".slot");
                    set.add(s);
                    if (s > 0) all.add(s);
                }
            }
        }
        l.playerSlots = player.stream().mapToInt(Integer::intValue).toArray();
        l.allSlots = all.stream().mapToInt(Integer::intValue).toArray();
        return l;
    }

    // ---------------------------------------------------------------- reset
    public static void invalidateAll() {
        SLOT_RULES.clear();
        GEMS.clear();
        slotRulesFor = null;
        gemsFor = null;
        layout = null;
        layoutFor = null;
    }
}
