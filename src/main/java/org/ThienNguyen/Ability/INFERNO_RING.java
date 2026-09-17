package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * INFERNO_RING
 * Một vòng lửa bùng lên và lan nhanh ra ngoài từ mục tiêu trong một đợt duy nhất, rồi tắt.
 * Sát thương 1 lần khi vòng lửa quét qua entity (không phải sát thương liên tục).
 */
public class INFERNO_RING implements IAbility {

    @Override
    public String getName() {
        return "INFERNO_RING";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 23% + 7%/level
        final double damagePercent = 23.0 + (skillLevel - 1) * 7.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double maxRadius = 5.0 + (skillLevel * 0.15);

        final Location center = target.getLocation().clone();
        final Set<UUID> hitEntities = new HashSet<>();

        center.getWorld().playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.1f, 0.8f);
        center.getWorld().spawnParticle(Particle.FLASH, center.clone().add(0, 0.3, 0), 1, 0, 0, 0, 0);

        new BukkitRunnable() {

            double radius = 0.0;
            int safetyCounter = 0;

            @Override
            public void run() {

                if (center.getWorld() == null || radius >= maxRadius || safetyCounter++ > 20) {
                    this.cancel();
                    return;
                }

                // Lan nhanh: hết bán kính trong ~10 tick (0.5s)
                radius += maxRadius / 10.0;

                int points = Math.min(140, Math.max(30, (int) (radius * 18)));

                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    Location p = center.clone().add(x, 0.15, z);
                    if (p.getWorld() != center.getWorld()) continue;

                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(Color.fromRGB(255, 100, 20), 1.4f)
                    );
                    if (Math.random() < 0.3) {
                        p.getWorld().spawnParticle(Particle.FLAME, p, 1, 0.05, 0.05, 0.05, 0.01);
                    }
                }

                double radiusSquared = radius * radius;

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (hitEntities.contains(entity.getUniqueId())) continue;

                    Location eLoc = entity.getLocation();
                    if (Math.abs(eLoc.getY() - center.getY()) > 2.0) continue;

                    double dx = eLoc.getX() - center.getX();
                    double dz = eLoc.getZ() - center.getZ();
                    if ((dx * dx) + (dz * dz) > radiusSquared) continue;

                    hitEntities.add(entity.getUniqueId());

                    entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    entity.damage(damage, attacker);
                    final LivingEntity ref = entity;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (ref.isValid()) ref.removeMetadata("IS_ABILITY", Main.getInstance());
                        }
                    }.runTaskLater(Main.getInstance(), 1L);

                    // Đốt cháy nhất thời (khác với sát thương liên tục có tick riêng)
                    entity.setFireTicks(Math.max(entity.getFireTicks(), 20));

                    entity.getWorld().spawnParticle(Particle.FLAME,
                            eLoc.clone().add(0, 1, 0), 16, 0.25, 0.4, 0.25, 0.02);
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
