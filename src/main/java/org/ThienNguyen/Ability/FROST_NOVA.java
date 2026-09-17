package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * FROST_NOVA
 * Nổ băng lan nhanh quanh mục tiêu, làm chậm và giảm tốc độ đào của kẻ địch trong bán kính.
 */
public class FROST_NOVA implements IAbility {

    @Override
    public String getName() {
        return "FROST_NOVA";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 18% + 7%/level
        final double damagePercent = 18.0 + (skillLevel - 1) * 7.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        // Bán kính tối đa của nova
        final double maxRadius = 3.0 + (skillLevel * 0.25);

        // Mức độ làm chậm, tối đa Slowness IV
        final int slowAmplifier = Math.min(3, skillLevel / 2);

        final Location center = target.getLocation().clone();
        final Set<UUID> hitEntities = new HashSet<>();

        new BukkitRunnable() {

            double radius = 0.0;
            int safetyCounter = 0;

            @Override
            public void run() {

                if (center.getWorld() == null || radius >= maxRadius || safetyCounter++ > 20) {
                    this.cancel();
                    return;
                }

                // Nova lan nhanh: hết bán kính trong khoảng 8 tick (~0.4s)
                radius += maxRadius / 8.0;

                int points = Math.min(150, Math.max(30, (int) (radius * 18)));

                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    Location particleLoc = center.clone().add(x, 0.3, z);
                    if (particleLoc.getWorld() != center.getWorld()) continue;

                    center.getWorld().spawnParticle(
                            Particle.DUST,
                            particleLoc,
                            1,
                            new Particle.DustOptions(Color.fromRGB(120, 220, 255), 1.4f)
                    );
                }

                double radiusSquared = radius * radius;

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {

                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (hitEntities.contains(entity.getUniqueId())) continue;

                    Location entityLoc = entity.getLocation();
                    if (Math.abs(entityLoc.getY() - center.getY()) > 2.0) continue;

                    double dx = entityLoc.getX() - center.getX();
                    double dz = entityLoc.getZ() - center.getZ();
                    double distSquared = (dx * dx) + (dz * dz);

                    if (distSquared <= radiusSquared) {

                        hitEntities.add(entity.getUniqueId());

                        // Đánh dấu damage đến từ ability để tránh vòng lặp vô hạn
                        entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                        entity.damage(damage, attacker);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (entity.isValid()) entity.removeMetadata("IS_ABILITY", Main.getInstance());
                            }
                        }.runTaskLater(Main.getInstance(), 1L);

                        entity.addPotionEffect(new PotionEffect(
                                PotionEffectType.SLOWNESS, 60, slowAmplifier, false, false, true));
                        entity.addPotionEffect(new PotionEffect(
                                PotionEffectType.MINING_FATIGUE, 60, 1, false, false, true));
                    }
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 2L);
    }
}
