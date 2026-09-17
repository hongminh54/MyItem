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

/**
 * PHANTOM_BLADES
 * 8 lưỡi kiếm ma hiện lên xoay quanh mục tiêu, thu hẹp vòng xoáy rồi đồng loạt lao vào tâm.
 * Sát thương nổ 1 lần khi các lưỡi kiếm chạm nhau ở tâm.
 */
public class PHANTOM_BLADES implements IAbility {

    private static final int BLADE_COUNT = 8;

    @Override
    public String getName() {
        return "PHANTOM_BLADES";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 26% + 9%/level
        final double damagePercent = 26.0 + (skillLevel - 1) * 9.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double impactRadius = 3.2 + (skillLevel * 0.15);
        final double impactRadiusSquared = impactRadius * impactRadius;
        final double startRadius = 4.5;

        final Location center = target.getLocation().clone().add(0, 1.0, 0);
        final Color bladeColor = Color.fromRGB(190, 225, 255);

        center.getWorld().playSound(center, Sound.ITEM_TRIDENT_RETURN, 1.0f, 1.4f);

        new BukkitRunnable() {

            int ticks = 0;
            final int convergeTicks = 20;
            final int totalTicks = 26;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                if (ticks < convergeTicks) {

                    double progress = (double) ticks / convergeTicks;
                    // Bán kính thu vào tâm, tốc độ xoay tăng dần
                    double orbitRadius = startRadius * (1.0 - progress);
                    double spin = progress * progress * 14.0;

                    for (int b = 0; b < BLADE_COUNT; b++) {

                        double angle = (2 * Math.PI * b) / BLADE_COUNT + spin;
                        double bx = Math.cos(angle) * orbitRadius;
                        double bz = Math.sin(angle) * orbitRadius;
                        // Lưỡi kiếm hạ thấp dần khi lao vào
                        double by = 1.6 * (1.0 - progress);

                        Location bladeBase = center.clone().add(bx, by, bz);

                        // Vẽ thân kiếm: đoạn thẳng hướng vào tâm
                        double bladeLength = 1.4;
                        for (double t = 0; t <= bladeLength; t += 0.18) {
                            double tx = bx - Math.cos(angle) * t;
                            double tz = bz - Math.sin(angle) * t;
                            Location p = center.clone().add(tx, by, tz);
                            if (p.getWorld() != center.getWorld()) continue;
                            p.getWorld().spawnParticle(
                                    Particle.DUST, p, 1,
                                    new Particle.DustOptions(bladeColor, 1.2f)
                            );
                        }

                        // Chuôi kiếm sáng hơn
                        bladeBase.getWorld().spawnParticle(
                                Particle.DUST, bladeBase, 1,
                                new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.6f)
                        );

                        // Vệt đuôi mờ phía sau
                        double trailAngle = angle - 0.25;
                        Location trail = center.clone().add(
                                Math.cos(trailAngle) * orbitRadius, by,
                                Math.sin(trailAngle) * orbitRadius);
                        trail.getWorld().spawnParticle(
                                Particle.DUST, trail, 1,
                                new Particle.DustOptions(Color.fromRGB(110, 150, 200), 0.9f)
                        );
                    }

                } else if (ticks == convergeTicks) {
                    impact();
                } else {
                    // Dư chấn sau va chạm
                    center.getWorld().spawnParticle(
                            Particle.DUST, center, 10, 1.2, 0.8, 1.2, 0,
                            new Particle.DustOptions(bladeColor, 1.0f)
                    );
                }

                ticks++;
            }

            private void impact() {
                center.getWorld().playSound(center, Sound.ITEM_TRIDENT_THUNDER, 1.2f, 1.2f);
                center.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center, 8, 1.0, 0.6, 1.0, 0);
                center.getWorld().spawnParticle(Particle.CRIT, center, 60, 1.0, 0.8, 1.0, 0.4);

                // Sóng xung kích trắng lan ra mặt đất
                for (int i = 0; i < 60; i++) {
                    double angle = (2 * Math.PI * i) / 60;
                    Location p = center.clone().add(
                            Math.cos(angle) * impactRadius, -0.8, Math.sin(angle) * impactRadius);
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.4f)
                    );
                }

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (entity.getLocation().distanceSquared(center) > impactRadiusSquared) continue;

                    entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    entity.damage(damage, attacker);
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
