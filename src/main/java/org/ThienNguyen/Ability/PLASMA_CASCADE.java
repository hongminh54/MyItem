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
 * PLASMA_CASCADE
 * 5 quả cầu plasma lần lượt rơi từ trên cao xuống các điểm quanh mục tiêu, mỗi quả nổ tung
 * khi chạm đất tạo một đợt sát thương nhỏ. Entity chỉ trúng 1 lần dù đứng gần nhiều điểm nổ.
 */
public class PLASMA_CASCADE implements IAbility {

    private static final int ORB_COUNT = 5;

    @Override
    public String getName() {
        return "PLASMA_CASCADE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 31% + 11%/level, chia đều cho 5 quả plasma
        final double totalPercent = 31.0 + (skillLevel - 1) * 11.0;
        final double perOrbDamage = baseDamage * (totalPercent / ORB_COUNT / 100.0);

        final double spreadRadius = 4.0 + (skillLevel * 0.15);
        final double explosionRadius = 1.8;
        final double explosionRadiusSquared = explosionRadius * explosionRadius;
        final double dropHeight = 8.0;

        final Location center = target.getLocation().clone();
        final Color plasmaCore = Color.fromRGB(255, 60, 200);
        final Color plasmaEdge = Color.fromRGB(120, 40, 255);

        // Điểm rơi của từng quả plasma, cố định ngay lúc cast
        final Location[] impactPoints = new Location[ORB_COUNT];
        for (int i = 0; i < ORB_COUNT; i++) {
            double angle = (2 * Math.PI * i) / ORB_COUNT + (Math.random() * 0.5);
            double dist = spreadRadius * (0.3 + Math.random() * 0.7);
            impactPoints[i] = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
        }

        center.getWorld().playSound(center, Sound.ENTITY_ENDER_EYE_LAUNCH, 1.0f, 0.6f);

        new BukkitRunnable() {

            int orbIndex = 0;
            int tickInOrb = 0;
            final int fallTicks = 6; // Mỗi quả rơi trong 6 tick trước khi nổ
            final Set<UUID> hitEntities = new HashSet<>();

            @Override
            public void run() {

                if (center.getWorld() == null || orbIndex >= ORB_COUNT) {
                    this.cancel();
                    return;
                }

                Location impact = impactPoints[orbIndex];
                double progress = (double) tickInOrb / fallTicks;
                double currentY = impact.getY() + dropHeight * (1.0 - progress);

                Location orbPos = impact.clone();
                orbPos.setY(currentY);

                if (orbPos.getWorld() != null) {
                    orbPos.getWorld().spawnParticle(
                            Particle.DUST, orbPos, 3, 0.1, 0.1, 0.1, 0,
                            new Particle.DustOptions(plasmaCore, 1.4f)
                    );
                    orbPos.getWorld().spawnParticle(
                            Particle.DUST, orbPos, 4, 0.25, 0.25, 0.25, 0,
                            new Particle.DustOptions(plasmaEdge, 1.1f)
                    );
                }

                tickInOrb++;

                if (tickInOrb >= fallTicks) {
                    detonate(impact);
                    orbIndex++;
                    tickInOrb = 0;
                }
            }

            private void detonate(Location impact) {
                if (impact.getWorld() == null) return;

                impact.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 1.4f);
                impact.getWorld().spawnParticle(Particle.FLASH, impact, 1, 0, 0, 0, 0);

                int points = 40;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    Location p = impact.clone().add(
                            Math.cos(angle) * explosionRadius, 0.1, Math.sin(angle) * explosionRadius);
                    if (p.getWorld() != impact.getWorld()) continue;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(plasmaCore, 1.3f)
                    );
                }
                impact.getWorld().spawnParticle(Particle.END_ROD, impact, 20, 0.3, 0.2, 0.3, 0.2);

                for (LivingEntity entity : impact.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (hitEntities.contains(entity.getUniqueId())) continue;
                    if (entity.getLocation().distanceSquared(impact) > explosionRadiusSquared) continue;

                    hitEntities.add(entity.getUniqueId());

                    entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    entity.damage(perOrbDamage, attacker);
                    final LivingEntity ref = entity;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (ref.isValid()) ref.removeMetadata("IS_ABILITY", Main.getInstance());
                        }
                    }.runTaskLater(Main.getInstance(), 1L);
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
