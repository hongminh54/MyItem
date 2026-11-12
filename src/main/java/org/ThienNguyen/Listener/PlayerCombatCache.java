package org.ThienNguyen.Listener;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToDoubleFunction;
import java.util.function.ObjDoubleConsumer;
public class PlayerCombatCache {
    private static final Map<UUID, CombatStats> cache = new ConcurrentHashMap<>();

    /** getter/setter trực tiếp cho từng stat — thay cho java.lang.reflect.Field (chậm + setAccessible mỗi lần). */
    private record Accessor(ToDoubleFunction<CombatStats> get, ObjDoubleConsumer<CombatStats> set) {}
    private static final Map<String, Accessor> ACCESSORS = new HashMap<>();
    private static void reg(String key, ToDoubleFunction<CombatStats> g, ObjDoubleConsumer<CombatStats> s) {
        ACCESSORS.put(key, new Accessor(g, s));
    }
    static {
        reg("damage", s -> s.totalBonusDmg, (s, v) -> s.totalBonusDmg = v);
        reg("pve_damage", s -> s.totalPveBonus, (s, v) -> s.totalPveBonus = v);
        reg("pvp_damage", s -> s.totalPvpBonus, (s, v) -> s.totalPvpBonus = v);
        reg("critical_chance", s -> s.totalCritChance, (s, v) -> s.totalCritChance = v);
        reg("critical_damage", s -> s.totalCritDamage, (s, v) -> s.totalCritDamage = v);
        reg("lifesteal", s -> s.totalLifesteal, (s, v) -> s.totalLifesteal = v);
        reg("dodge_rate", s -> s.totalDodge, (s, v) -> s.totalDodge = v);
        reg("block_rate", s -> s.totalBlock, (s, v) -> s.totalBlock = v);
        reg("penetration", s -> s.totalPenetration, (s, v) -> s.totalPenetration = v);
        reg("true_damage", s -> s.totalTrueDamage, (s, v) -> s.totalTrueDamage = v);
        reg("thorns", s -> s.totalThorns, (s, v) -> s.totalThorns = v);
        reg("max_mana", s -> s.totalMaxMana, (s, v) -> s.totalMaxMana = v);
        reg("mana_regen", s -> s.totalManaRegen, (s, v) -> s.totalManaRegen = v);
        reg("exp_bonus", s -> s.totalExpBonus, (s, v) -> s.totalExpBonus = v);
        reg("movement_speed", s -> s.totalMovementSpeed, (s, v) -> s.totalMovementSpeed = v);
        reg("armor_pen", s -> s.totalArmorPen, (s, v) -> s.totalArmorPen = v);
        reg("health_regen", s -> s.totalHealthRegen, (s, v) -> s.totalHealthRegen = v);
        reg("all_damage", s -> s.totalAllDamage, (s, v) -> s.totalAllDamage = v);
        reg("all_defense", s -> s.totalAllDefense, (s, v) -> s.totalAllDefense = v);
        reg("bow_damage", s -> s.totalBowDamage, (s, v) -> s.totalBowDamage = v);
        reg("knockback_resistance", s -> s.totalKnockbackResist, (s, v) -> s.totalKnockbackResist = v);
        reg("death_damage", s -> s.totalDeathDamage, (s, v) -> s.totalDeathDamage = v);
        reg("accuracy", s -> s.totalAccuracy, (s, v) -> s.totalAccuracy = v);
        reg("crit_damage_reduction", s -> s.totalCritDamageReduction, (s, v) -> s.totalCritDamageReduction = v);
        reg("magic_damage", s -> s.totalMagicDamage, (s, v) -> s.totalMagicDamage = v);
        reg("magic_defense", s -> s.totalMagicDefense, (s, v) -> s.totalMagicDefense = v);
        reg("armor", s -> s.totalArmor, (s, v) -> s.totalArmor = v);
        reg("pve_defense", s -> s.totalPveDef, (s, v) -> s.totalPveDef = v);
        reg("pvp_defense", s -> s.totalPvpDef, (s, v) -> s.totalPvpDef = v);
        reg("deep_wound", s -> s.totalDeepWound, (s, v) -> s.totalDeepWound = v);
        reg("damage_reduction", s -> s.totalDamageReduction, (s, v) -> s.totalDamageReduction = v);
        reg("effect_resistance", s -> s.totalEffectResistance, (s, v) -> s.totalEffectResistance = v);
        reg("health", s -> s.totalHealth, (s, v) -> s.totalHealth = v);
        reg("attack_speed", s -> s.totalAttackSpeed, (s, v) -> s.totalAttackSpeed = v);
        reg("cooldown_reduction", s -> s.totalCooldownReduction, (s, v) -> s.totalCooldownReduction = v);
    }
    public static boolean isKnownStat(String statKey) {
        return ACCESSORS.containsKey(statKey);
    }
    public static java.util.Set<String> getKnownStatKeys() {
        return ACCESSORS.keySet();
    }
    public static boolean addToField(CombatStats stats, String statKey, double amount) {
        Accessor a = ACCESSORS.get(statKey);
        if (a == null) return false;
        a.set().accept(stats, a.get().applyAsDouble(stats) + amount);
        return true;
    }
    public static boolean multiplyField(CombatStats stats, String statKey, double percent) {
        Accessor a = ACCESSORS.get(statKey);
        if (a == null) return false;
        a.set().accept(stats, a.get().applyAsDouble(stats) * (1.0 + percent / 100.0));
        return true;
    }
    public static double getEffective(UUID uuid, String statKey, double baseValue) {
        CombatStats stats = cache.get(uuid);
        if (stats == null || stats.tempBuffs.isEmpty()) return baseValue;
        double bonus = 0.0;
        long now = System.currentTimeMillis();
        for (org.ThienNguyen.Listener.Passive.TempBuff buff : stats.tempBuffs.values()) {
            if (buff.expireAtMillis <= now) continue;
            if (buff.statKey.equals(statKey)) {
                bonus += buff.amount;
            }
        }
        return baseValue + bonus;
    }
    public static double getEffectiveByStatName(UUID uuid, String statKey) {
        CombatStats stats = cache.get(uuid);
        if (stats == null) return 0.0;
        Accessor a = ACCESSORS.get(statKey);
        double base = a != null ? a.get().applyAsDouble(stats) : 0.0;
        return getEffective(uuid, statKey, base);
    }
    public static class CombatStats {
        public double totalCritDamageReduction = 0.0;
        public double totalMagicDamage = 1.0;
        public Map<String, Integer> elementDefenses = new HashMap<>();
        public double totalMagicDefense = 0.0;
        public double totalBonusDmg = 0;
        public double totalPveBonus = 0;
        public double totalPvpBonus = 0;
        public double totalAccuracy = 0;
        public double totalCritChance = 0;
        public double totalCritDamage = 0;
        public double totalLifesteal = 0;
        public double totalPenetration = 0;
        public double totalTrueDamage = 0;
        public double totalArmorPen = 0;
        public double totalElementDamage = 0;
        public double totalDeathDamage = 0;
        public double totalDeepWound = 0.0;
        public double totalBowDamage = 0;
        public double totalAllDamage = 0;
        public double totalMaxMana = 0;
        public double totalManaRegen = 0;
        public double totalArmor = 0;
        public double totalPveDef = 0;
        public double totalPvpDef = 0;
        public double totalDodge = 0;
        public double totalBlock = 0;
        public double totalThorns = 0;
        public double totalAllDefense = 0;
        public double totalExpBonus = 0.0;
        public double totalHealth = 0;
        public double totalHealthRegen = 0;
        public double totalKnockbackResist = 0;
        public double totalMovementSpeed = 0;
        public double totalDamageReduction = 0.0;
        public double totalEffectResistance = 0.0;
        public double totalAttackSpeed = 0;
        public double totalCooldownReduction = 0;
        public Map<String, double[]> bestAbilities = new HashMap<>();
        public Map<String, Double> weaponElementDamage = new HashMap<>();
        public Map<String, Integer> weaponElementLevels = new HashMap<>();
        public Map<String, org.ThienNguyen.Listener.Passive.TempBuff> tempBuffs = new ConcurrentHashMap<>();
        public void clear() {
            totalAccuracy = 0;
            totalDeepWound = 0.0;
            totalDamageReduction = 0.0;
            totalMaxMana = 0;
            totalManaRegen = 0;
            totalDeathDamage = 0;
            totalBonusDmg = totalPveBonus = totalPvpBonus = 0;
            totalCritChance = totalCritDamage = totalLifesteal = 0;
            totalPenetration = totalTrueDamage = totalArmorPen = 0;
            totalElementDamage = totalBowDamage = totalAllDamage = 0;
            elementDefenses.clear();
            totalCritDamageReduction = 0.0;
            totalArmor = totalPveDef = totalPvpDef = 0;
            totalDodge = totalBlock = totalThorns = totalAllDefense = 0;
            totalHealth = 0;
            totalHealthRegen = 0;
            totalKnockbackResist = 0;
            totalMovementSpeed = 0;
            totalExpBonus = 0;
            totalEffectResistance = 0.0;
            totalMagicDamage = 1.0;
            totalMagicDefense = 0.0;
            totalAttackSpeed = 0;
            totalCooldownReduction = 0;
            bestAbilities.clear();
            weaponElementDamage.clear();
            weaponElementLevels.clear();
        }
        public void clearWeaponElements() {
            weaponElementDamage.clear();
            weaponElementLevels.clear();
        }
    }
    public double getRealPower(Player player) {
        var stats = org.ThienNguyen.Listener.PlayerCombatCache.getStats(player.getUniqueId());
        double attackDmg = stats.totalBonusDmg;
        if (attackDmg <= 0) {
            attackDmg = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        }
        return attackDmg;
    }
    public static CombatStats getStats(UUID uuid) {
        return cache.computeIfAbsent(uuid, k -> new CombatStats());
    }
    public static void updateCache(UUID uuid, CombatStats stats) {
        cache.put(uuid, stats);
    }
    public static void invalidate(UUID uuid) {
        cache.remove(uuid);
    }
}