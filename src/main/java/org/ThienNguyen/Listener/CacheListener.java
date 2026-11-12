package org.ThienNguyen.Listener;
import org.ThienNguyen.Ability.AbilityData;
import org.ThienNguyen.JewelryManager;
import org.ThienNguyen.Main;
import org.ThienNguyen.GemSocket.GemLogic;
import org.ThienNguyen.Stat.*;
import org.ThienNguyen.Hook.MMOCORE;
import org.ThienNguyen.Element.ElementCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CacheListener implements Listener {
    private static final NamespacedKey KEY_PCT_DAMAGE          = new NamespacedKey(Main.getInstance(), "pct_damage");
    private static final NamespacedKey KEY_PCT_CRIT_DMG_RED    = new NamespacedKey(Main.getInstance(), "pct_critical_damage_reduction");
    private static final NamespacedKey KEY_PCT_ARMOR           = new NamespacedKey(Main.getInstance(), "pct_armor");
    private static final NamespacedKey KEY_PCT_TRUE_DMG        = new NamespacedKey(Main.getInstance(), "pct_true_damage");
    private static final NamespacedKey KEY_PCT_MAGIC_DMG       = new NamespacedKey(Main.getInstance(), "pct_magic_damage");
    private static final NamespacedKey KEY_PCT_MAGIC_DEF       = new NamespacedKey(Main.getInstance(), "pct_magic_defense");
    private static final NamespacedKey KEY_PCT_PVE             = new NamespacedKey(Main.getInstance(), "pct_pve_damage");
    private static final NamespacedKey KEY_PCT_PVP             = new NamespacedKey(Main.getInstance(), "pct_pvp_damage");
    private static final NamespacedKey KEY_PCT_ALL_DMG         = new NamespacedKey(Main.getInstance(), "pct_all_damage");
    private static final NamespacedKey KEY_PCT_BOW_DMG         = new NamespacedKey(Main.getInstance(), "pct_bow_damage");
    private static final NamespacedKey KEY_PCT_DEATH_DMG       = new NamespacedKey(Main.getInstance(), "pct_death_damage");
    private static final NamespacedKey KEY_PCT_EFFECT_RES      = new NamespacedKey(Main.getInstance(), "pct_effect_resistance");
    private static final NamespacedKey KEY_IS_PLACEHOLDER      = new NamespacedKey(Main.getInstance(), "is_placeholder");
    private static final EquipmentSlot[] ALL_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.HAND, EquipmentSlot.OFF_HAND
    };
    private static final org.bukkit.attribute.Attribute KNOCKBACK_RESISTANCE_ATTRIBUTE =
            resolveAttribute("knockback_resistance", "KNOCKBACK_RESISTANCE", "GENERIC_KNOCKBACK_RESISTANCE");
    private static org.bukkit.attribute.Attribute resolveAttribute(String registryKey, String... legacyFieldNames) {
        try {
            org.bukkit.attribute.Attribute attr =
                    org.bukkit.Registry.ATTRIBUTE.get(org.bukkit.NamespacedKey.minecraft(registryKey));
            if (attr != null) return attr;
        } catch (Throwable ignored) {
        }
        for (String name : legacyFieldNames) {
            try {
                java.lang.reflect.Field field = org.bukkit.attribute.Attribute.class.getField(name);
                Object value = field.get(null);
                if (value instanceof org.bukkit.attribute.Attribute attribute) {
                    return attribute;
                }
            } catch (Exception ignored) {
            }
        }
        Bukkit.getLogger().warning("[" + Main.getInstance().getName() +
                "] Could not resolve knockback resistance Attribute (tried registry key '" + registryKey +
                "' and legacy fields: " + String.join(", ", legacyFieldNames) +
                "). Knockback resistance stat will not be applied.");
        return null;
    }

    // ------------------------------------------------------------------ events
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        JewelryManager.loadDataToCache(p);
        scheduleRefresh(p);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        p.removeMetadata("DEEP_WOUND_REDUCTION", Main.getInstance());
        p.removeMetadata("DEEP_WOUND_TASK", Main.getInstance());
        p.removeMetadata("CURSED_REDUCTION", Main.getInstance());
        scheduleRefresh(p);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        PlayerCombatCache.invalidate(uuid);
        JewelryManager.invalidateJewelryCache(uuid);
        PENDING_REFRESH.remove(uuid);
        LAST_COMBO_MESSAGE_TIME.remove(uuid); // trước đây map này phình mãi theo số player từng vào server
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p) {
            scheduleRefresh(p);
        }
        if (e.getInventory().getHolder() instanceof JewelryManager jm) {
            jm.onClick(e);
        }
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p) {
            scheduleRefresh(p);
        }
        if (e.getInventory().getHolder() instanceof JewelryManager jm) {
            jm.onClose(e);
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHold(PlayerItemHeldEvent e) {
        scheduleRefresh(e.getPlayer());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(org.bukkit.event.player.PlayerDropItemEvent e) {
        scheduleRefresh(e.getPlayer());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHandItems(PlayerSwapHandItemsEvent e) {
        scheduleRefresh(e.getPlayer());
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(org.bukkit.event.player.PlayerRespawnEvent e) {
        scheduleRefresh(e.getPlayer());
    }

    /**
     * Debounce: mỗi player chỉ có TỐI ĐA 1 refresh đang chờ.
     * Trước đây mỗi click/đổi slot/thả đồ tạo 1 BukkitRunnable riêng -> kéo chuột trong inventory
     * = hàng chục lần refreshCache (mỗi lần đọc toàn bộ item + config) chồng lên nhau.
     */
    private static final Set<UUID> PENDING_REFRESH = ConcurrentHashMap.newKeySet();
    public static void scheduleRefresh(Player player) {
        UUID uuid = player.getUniqueId();
        if (!PENDING_REFRESH.add(uuid)) return;
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            PENDING_REFRESH.remove(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) refreshCache(p);
        }, 5L);
    }

    private static final ConcurrentHashMap<UUID, Long> LAST_COMBO_MESSAGE_TIME = new ConcurrentHashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 3000L;

    // ------------------------------------------------------------------ percent
    private static final class PercentAccumulator {
        double bonusDmg, armor, trueDmg, critDmgReduction, magicDmg, magicDef;
        double pveBonus, pvpBonus, allDamage, bowDamage, deathDamage, effectResistance;
    }
    private static void applyAccumulatedPercents(PlayerCombatCache.CombatStats stats, PercentAccumulator a) {
        if (a.critDmgReduction != 0.0) stats.totalCritDamageReduction *= (1.0 + (a.critDmgReduction / 100.0));
        if (a.bonusDmg != 0.0)         stats.totalBonusDmg            *= (1.0 + (a.bonusDmg / 100.0));
        if (a.armor != 0.0)            stats.totalArmor               *= (1.0 + (a.armor / 100.0));
        if (a.trueDmg != 0.0)          stats.totalTrueDamage          *= (1.0 + (a.trueDmg / 100.0));
        if (a.magicDmg != 0.0)         stats.totalMagicDamage         *= (1.0 + (a.magicDmg / 100.0));
        if (a.magicDef != 0.0)         stats.totalMagicDefense        *= (1.0 + (a.magicDef / 100.0));
        if (a.pveBonus != 0.0)         stats.totalPveBonus            *= (1.0 + (a.pveBonus / 100.0));
        if (a.pvpBonus != 0.0)         stats.totalPvpBonus            *= (1.0 + (a.pvpBonus / 100.0));
        if (a.allDamage != 0.0)        stats.totalAllDamage           *= (1.0 + (a.allDamage / 100.0));
        if (a.bowDamage != 0.0)        stats.totalBowDamage           *= (1.0 + (a.bowDamage / 100.0));
        if (a.deathDamage != 0.0)      stats.totalDeathDamage         *= (1.0 + (a.deathDamage / 100.0));
        if (a.effectResistance != 0.0) stats.totalEffectResistance    *= (1.0 + (a.effectResistance / 100.0));
    }
    /** slot == null -> trang sức (không kiểm tra slot). */
    private static double pct(PersistentDataContainer pdc, NamespacedKey key, FileConfiguration cfg, String stat, EquipmentSlot slot) {
        Double v = pdc.get(key, PersistentDataType.DOUBLE);
        if (v == null) return 0.0;
        if (slot != null && !ItemStatHelper.allowed(pdc, cfg, stat, slot, true)) return 0.0;
        return v;
    }
    private static void accumulatePercents(PersistentDataContainer pdc, PercentAccumulator a, FileConfiguration cfg, EquipmentSlot slot) {
        a.bonusDmg         += pct(pdc, KEY_PCT_DAMAGE, cfg, "damage", slot);
        a.critDmgReduction += pct(pdc, KEY_PCT_CRIT_DMG_RED, cfg, "critical_damage_reduction", slot);
        a.armor            += pct(pdc, KEY_PCT_ARMOR, cfg, "armor", slot);
        a.trueDmg          += pct(pdc, KEY_PCT_TRUE_DMG, cfg, "true_damage", slot);
        a.magicDmg         += pct(pdc, KEY_PCT_MAGIC_DMG, cfg, "magic_damage", slot);
        a.magicDef         += pct(pdc, KEY_PCT_MAGIC_DEF, cfg, "magic_defense", slot);
        a.pveBonus         += pct(pdc, KEY_PCT_PVE, cfg, "pve_damage", slot);
        a.pvpBonus         += pct(pdc, KEY_PCT_PVP, cfg, "pvp_damage", slot);
        a.allDamage        += pct(pdc, KEY_PCT_ALL_DMG, cfg, "all_damage", slot);
        a.bowDamage        += pct(pdc, KEY_PCT_BOW_DMG, cfg, "bow_damage", slot);
        a.deathDamage      += pct(pdc, KEY_PCT_DEATH_DMG, cfg, "death_damage", slot);
        a.effectResistance += pct(pdc, KEY_PCT_EFFECT_RES, cfg, "effect_resistance", slot);
    }

    private static boolean ok(PersistentDataContainer pdc, FileConfiguration cfg, String stat, EquipmentSlot slot) {
        return ItemStatHelper.allowed(pdc, cfg, stat, slot, false);
    }

    // ------------------------------------------------------------------ refresh
    public static void refreshCache(Player player) {
        if (player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();
        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(uuid);
        stats.clear();
        Main main = Main.getInstance();
        FileConfiguration slotConfig  = main.getStatsSettingsConfig();
        FileConfiguration gemConfig   = main.getGemConfig();
        FileConfiguration eConfig     = main.getElementConfig();
        FileConfiguration comboConfig = main.getComboConfig();

        String currentComboId = org.ThienNguyen.Listener.ItemCombo.ComboListener.getFullSetComboId(player);
        if (currentComboId == null) currentComboId = "";
        String comboMetadataKey = "last_notified_combo";
        String lastNotifiedId = "";
        if (player.hasMetadata(comboMetadataKey)) {
            lastNotifiedId = player.getMetadata(comboMetadataKey).get(0).asString();
        }
        if (!currentComboId.equals(lastNotifiedId)) {
            if (!currentComboId.isEmpty()) {
                player.setMetadata(comboMetadataKey, new FixedMetadataValue(main, currentComboId));
            } else {
                player.removeMetadata(comboMetadataKey, main);
            }
            long now = System.currentTimeMillis();
            long lastSent = LAST_COMBO_MESSAGE_TIME.getOrDefault(uuid, 0L);
            if (now - lastSent >= MESSAGE_COOLDOWN_MS) {
                if (!currentComboId.isEmpty()) {
                    String msg = comboConfig.getString(currentComboId + ".message");
                    if (msg != null && !msg.trim().isEmpty()) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                    }
                }
                LAST_COMBO_MESSAGE_TIME.put(uuid, now);
            }
        }
        if (!currentComboId.isEmpty()) {
            ConfigurationSection comboStats = comboConfig.getConfigurationSection(currentComboId + ".stats");
            if (comboStats != null) {
                for (String key : comboStats.getKeys(false)) {
                    updateStat(stats, key, comboStats.getDouble(key));
                }
            }
        }
        stats.clearWeaponElements();
        stats.bestAbilities.clear();
        PercentAccumulator pctAcc = new PercentAccumulator();
        boolean mmoCore = isMMOCoreAvailable(); // hoist: trước đây gọi lại cho từng item

        for (EquipmentSlot slot : ALL_SLOTS) {
            ItemStack item = player.getInventory().getItem(slot);
            // item không có meta = không có PDC/lore -> không thể mang stat custom => bỏ qua toàn bộ
            if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) continue;
            ItemMeta meta = item.getItemMeta();                      // CHỈ 1 lần clone meta / item
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (!ItemStatHelper.isOwner(player, pdc)) continue;

            Map<String, Integer> itemDefs = ElementCore.getItemDefenses(item);
            itemDefs.forEach((elem, lv) -> stats.elementDefenses.merge(elem, lv, Integer::sum));
            if (mmoCore && !MMOCORE.canUse(player, item)) continue;

            accumulatePercents(pdc, pctAcc, slotConfig, slot);

            if (ok(pdc, slotConfig, "accuracy", slot))       stats.totalAccuracy += Accuracy.get(item);
            if (ok(pdc, slotConfig, "exp_bonus", slot))      stats.totalExpBonus += ExpBonus.get(item);
            if (ok(pdc, slotConfig, "movement_speed", slot)) stats.totalMovementSpeed += MovementSpeed.get(item);
            if (ok(pdc, slotConfig, "damage", slot))         stats.totalBonusDmg += Damage.getDamage(item);
            if (ok(pdc, slotConfig, "critical_damage_reduction", slot)) stats.totalCritDamageReduction += CritDamageReduction.get(item);
            if (ok(pdc, slotConfig, "damage_reduction", slot)) stats.totalDamageReduction += DamageReduction.get(item);
            if (ok(pdc, slotConfig, "deep_wound", slot))     stats.totalDeepWound += DeepWound.get(item);
            if (ok(pdc, slotConfig, "pve_damage", slot))     stats.totalPveBonus += PveDamage.get(item);
            if (ok(pdc, slotConfig, "pvp_damage", slot))     stats.totalPvpBonus += PvpDamage.get(item);
            if (ok(pdc, slotConfig, "all_damage", slot))     stats.totalAllDamage += AllDamage.get(item);
            if (ok(pdc, slotConfig, "all_defense", slot))    stats.totalAllDefense += AllDefense.get(item);
            if (ok(pdc, slotConfig, "bow_damage", slot))     stats.totalBowDamage += BowDamage.get(item);
            if (ok(pdc, slotConfig, "critical_chance", slot)) stats.totalCritChance += CriticalChance.get(item);
            if (ok(pdc, slotConfig, "critical_damage", slot)) stats.totalCritDamage += CriticalDamage.get(item);
            if (ok(pdc, slotConfig, "lifesteal", slot))      stats.totalLifesteal += Lifesteal.get(item);
            if (ok(pdc, slotConfig, "true_damage", slot))    stats.totalTrueDamage += TrueDamage.get(item);
            if (ok(pdc, slotConfig, "penetration", slot))    stats.totalPenetration += Penetration.get(item);
            if (ok(pdc, slotConfig, "armor_pen", slot))      stats.totalArmorPen += ArmorPen.get(item);
            if (ok(pdc, slotConfig, "armor", slot))          stats.totalArmor += Armor.getArmor(item);
            if (ok(pdc, slotConfig, "pve_defense", slot))    stats.totalPveDef += PveDefense.get(item);
            if (ok(pdc, slotConfig, "pvp_defense", slot))    stats.totalPvpDef += PvpDefense.get(item);
            if (ok(pdc, slotConfig, "dodge_rate", slot))     stats.totalDodge += DodgeRate.get(item);
            if (ok(pdc, slotConfig, "block_rate", slot))     stats.totalBlock += BlockRate.get(item);
            if (ok(pdc, slotConfig, "thorns", slot))         stats.totalThorns += Thorns.get(item);
            if (ok(pdc, slotConfig, "knockback_resistance", slot)) stats.totalKnockbackResist += KnockbackResistance.get(item);
            if (ok(pdc, slotConfig, "death_damage", slot))   stats.totalDeathDamage += DeathDamage.get(item);
            if (ok(pdc, slotConfig, "magic_damage", slot))   stats.totalMagicDamage += MagicDamage.get(item);
            if (ok(pdc, slotConfig, "magic_defense", slot))  stats.totalMagicDefense += MagicDefense.get(item);
            if (ok(pdc, slotConfig, "effect_resistance", slot)) stats.totalEffectResistance += EffectResistance.get(item);
            if (ok(pdc, slotConfig, "cooldown_reduction", slot)) stats.totalCooldownReduction += CooldownReduction.get(item);

            addElements(stats, ElementCore.getAllElements(item), eConfig);

            for (String gemId : GemLogic.getGemsOnItem(item)) {
                ItemStatHelper.GemData gem = ItemStatHelper.gem(gemConfig, gemId);
                for (ItemStatHelper.StatLine line : gem.stats) {
                    if (ok(pdc, slotConfig, line.type(), slot)) updateStat(stats, line.type(), line.value());
                }
                for (ItemStatHelper.ElemLine el : gem.elements) {
                    stats.weaponElementDamage.merge(el.id(), el.value(), Double::sum);
                }
                for (ItemStatHelper.AbilityLine ab : gem.abilities) {
                    putBestAbility(stats.bestAbilities, ab.name(), ab.level(), ab.chance());
                }
            }
            for (String data : AbilityData.getAbilityList(item)) {
                processAbilityLineLegacy(data, stats.bestAbilities);
            }
        }

        Map<Integer, ItemStack> jewelryItems = JewelryManager.getCachedJewelry(uuid);
        for (ItemStack jItem : jewelryItems.values()) {
            applyJewelryStats(player, stats, pctAcc, jItem, eConfig, gemConfig);
        }
        FileConfiguration mainConfig = main.getConfig();
        if (mainConfig.getBoolean("jewelry.enable-player-inventory", false)) {
            for (int slotIdx : ItemStatHelper.jewelryLayout(mainConfig).playerSlots) {
                try {
                    ItemStack item = player.getInventory().getItem(slotIdx);
                    if (item != null && item.getType() != Material.AIR && item.hasItemMeta() && !isPlaceholder(item)) {
                        applyJewelryStats(player, stats, pctAcc, item, eConfig, gemConfig);
                    }
                } catch (Exception ignored) {}
            }
        }

        double finalKB = Math.max(0.0, Math.min(1.0, stats.totalKnockbackResist));
        if (KNOCKBACK_RESISTANCE_ATTRIBUTE != null) {
            org.bukkit.attribute.AttributeInstance kbAttr = player.getAttribute(KNOCKBACK_RESISTANCE_ATTRIBUTE);
            // chỉ set khi đổi: setBaseValue luôn đánh dấu attribute dirty + gửi packet
            if (kbAttr != null && kbAttr.getBaseValue() != finalKB) {
                kbAttr.setBaseValue(finalKB);
            }
        }
        applyAccumulatedPercents(stats, pctAcc);
        org.ThienNguyen.API.StatBoostAPI.apply(uuid, stats);
        StatsListener.getInstance().updatePlayerStats(player, currentComboId.isEmpty() ? null : currentComboId);
        PlayerCombatCache.updateCache(uuid, stats);
    }

    private static void addElements(PlayerCombatCache.CombatStats stats, Map<String, Integer> elements, FileConfiguration eConfig) {
        if (elements == null || elements.isEmpty()) return;
        for (Map.Entry<String, Integer> entry : elements.entrySet()) {
            String eId = entry.getKey().toUpperCase();
            if (eConfig.contains(eId)) {
                double base = eConfig.getDouble(eId + ".base-damage", 0.0);
                double per  = eConfig.getDouble(eId + ".damage-per", 0.0);
                stats.weaponElementDamage.merge(eId, base + (entry.getValue() * per), Double::sum);
                stats.weaponElementLevels.merge(eId, entry.getValue(), Integer::sum);
            }
        }
    }

    private static void updateStat(PlayerCombatCache.CombatStats stats, String type, double val) {
        if (val == 0) return;
        switch (type.toLowerCase()) {
            case "exp_bonus", "exp" -> stats.totalExpBonus += val;
            case "critical_damage_reduction" -> stats.totalCritDamageReduction += val;
            case "movement_speed", "speed" -> stats.totalMovementSpeed += val;
            case "damage_reduction" -> stats.totalDamageReduction += val;
            case "damage" -> stats.totalBonusDmg += val;
            case "deep_wound" -> stats.totalDeepWound += val;
            case "pve_damage" -> stats.totalPveBonus += val;
            case "pvp_damage" -> stats.totalPvpBonus += val;
            case "all_damage" -> stats.totalAllDamage += val;
            case "magic_damage" -> stats.totalMagicDamage += val;
            case "bow_damage" -> stats.totalBowDamage += val;
            case "true_damage" -> stats.totalTrueDamage += val;
            case "death_damage" -> stats.totalDeathDamage += val;
            case "critical_chance", "crit_chance" -> stats.totalCritChance += val;
            case "critical_damage", "crit_damage" -> stats.totalCritDamage += val;
            case "penetration" -> stats.totalPenetration += val;
            case "armor_pen" -> stats.totalArmorPen += val;
            case "lifesteal" -> stats.totalLifesteal += val;
            case "accuracy" -> stats.totalAccuracy += val;
            case "armor" -> stats.totalArmor += val;
            case "pve_defense", "pve_def" -> stats.totalPveDef += val;
            case "pvp_defense", "pvp_def" -> stats.totalPvpDef += val;
            case "all_defense" -> stats.totalAllDefense += val;
            case "magic_defense", "magic_def" -> stats.totalMagicDefense += val;
            case "dodge_rate" -> stats.totalDodge += val;
            case "block_rate" -> stats.totalBlock += val;
            case "thorns" -> stats.totalThorns += val;
            case "knockback_resistance" -> stats.totalKnockbackResist += val;
            case "effect_resistance" -> stats.totalEffectResistance += val;
            case "cooldown_reduction" -> stats.totalCooldownReduction += val;
        }
    }

    private static void putBestAbility(Map<String, double[]> best, String name, double lv, double ch) {
        double[] cur = best.get(name);
        if (cur == null || lv > cur[0]) best.put(name, new double[]{lv, ch});
    }

    private static void applyJewelryStats(Player player, PlayerCombatCache.CombatStats stats, PercentAccumulator pctAcc,
                                          ItemStack jItem, FileConfiguration eConfig, FileConfiguration gemConfig) {
        if (jItem == null || jItem.getType() == Material.AIR || !jItem.hasItemMeta()) return;
        PersistentDataContainer pdc = jItem.getItemMeta().getPersistentDataContainer();
        if (!ItemStatHelper.isOwner(player, pdc)) return;
        accumulatePercents(pdc, pctAcc, null, null);

        stats.totalAccuracy += Accuracy.get(jItem);
        stats.totalExpBonus += ExpBonus.get(jItem);
        stats.totalCritDamageReduction += CritDamageReduction.get(jItem);
        stats.totalBonusDmg     += Damage.getDamage(jItem);
        stats.totalDamageReduction += DamageReduction.get(jItem);
        stats.totalPveBonus     += PveDamage.get(jItem);
        stats.totalPvpBonus     += PvpDamage.get(jItem);
        stats.totalDeepWound    += DeepWound.get(jItem);
        stats.totalCritChance   += CriticalChance.get(jItem);
        stats.totalCritDamage   += CriticalDamage.get(jItem);
        stats.totalLifesteal    += Lifesteal.get(jItem);
        stats.totalTrueDamage   += TrueDamage.get(jItem);
        stats.totalPenetration  += Penetration.get(jItem);
        stats.totalArmorPen     += ArmorPen.get(jItem);
        stats.totalArmor        += Armor.getArmor(jItem);
        stats.totalPveDef       += PveDefense.get(jItem);
        stats.totalPvpDef       += PvpDefense.get(jItem);
        stats.totalDodge        += DodgeRate.get(jItem);
        stats.totalBlock        += BlockRate.get(jItem);
        stats.totalThorns       += Thorns.get(jItem);
        stats.totalAllDamage    += AllDamage.get(jItem);
        stats.totalAllDefense   += AllDefense.get(jItem);
        stats.totalBowDamage    += BowDamage.get(jItem);
        stats.totalDeathDamage  += DeathDamage.get(jItem);
        stats.totalMagicDamage  += MagicDamage.get(jItem);
        stats.totalMagicDefense += MagicDefense.get(jItem);
        stats.totalEffectResistance += EffectResistance.get(jItem);
        stats.totalCooldownReduction += CooldownReduction.get(jItem);

        addElements(stats, ElementCore.getAllElements(jItem), eConfig);

        for (String gemId : GemLogic.getGemsOnItem(jItem)) {
            ItemStatHelper.GemData gem = ItemStatHelper.gem(gemConfig, gemId);
            for (ItemStatHelper.StatLine line : gem.stats) updateStat(stats, line.type(), line.value());
            for (ItemStatHelper.AbilityLine ab : gem.abilities) putBestAbility(stats.bestAbilities, ab.name(), ab.level(), ab.chance());
        }
    }

    private static boolean isPlaceholder(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY_IS_PLACEHOLDER, PersistentDataType.BOOLEAN);
    }
    private static void processAbilityLineLegacy(String data, Map<String, double[]> best) {
        String[] parts = data.split(":");
        if (parts.length < 3) return;
        try {
            putBestAbility(best, parts[0].toUpperCase(), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
        } catch (Exception ignored) {}
    }
    private static boolean isMMOCoreAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("MMOCore");
    }
}
