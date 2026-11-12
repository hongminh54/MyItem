package org.ThienNguyen.Listener.Passive;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;

/**
 * Tra PotionEffectType từ tên trong YAML. Ưu tiên key registry (STRENGTH, SLOWNESS, JUMP_BOOST, "minecraft:haste"...),
 * sau đó mới thử tên cũ (INCREASE_DAMAGE, SLOW, FAST_DIGGING...) qua getByName.
 */
public final class EffectLookup {

    private EffectLookup() {}

    @SuppressWarnings("deprecation")
    public static PotionEffectType find(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        try {
            NamespacedKey key = NamespacedKey.fromString(s.toLowerCase(Locale.ROOT));
            if (key != null) {
                PotionEffectType t = Registry.EFFECT.get(key);
                if (t != null) return t;
            }
            return PotionEffectType.getByName(s.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return null;
        }
    }
}
