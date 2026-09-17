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
import org.bukkit.util.Vector;

/**
 * STELLAR_COLLAPSE
 * Một điểm kỳ dị nhỏ hình thành tại mục tiêu, hút các vệt sáng xoáy vào tâm rồi collapse
 * và bung nổ một lần thành sóng ánh sáng lan tỏa. Sát thương 1 lần (mạnh nhất pack).
 */
public class STELLAR_COLLAPSE implements IAbility {

    @Override
    public String getName() {
        return "STELLAR_COLLAPSE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 33% + 12%/level
        final double damagePercent = 33.0 + (skillLevel - 1) * 12.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double blastRadius = 5.0 + (skillLevel * 0.2);
        final double blastRadiusSquared = blastRadius * blastRadius;

        final Location center = target.getLocation().clone().add(0, 1.0, 0);

        center.getWorld().playSound(center, Sound.AMBIENT_CAVE, 1.2f, 0.4f);

        new BukkitRunnable() {

            int ticks = 0;
            final int pullTicks = 22;
            final int totalTicks = 30;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                if (ticks < pullTicks) {

                    double progress = (double) ticks / pullTicks;
                    double spin = progress * 12.0;
                    int strands = 5;

                    for (int s = 0; s < strands; s++) {
                        double baseAngle = (2 * Math.PI * s) / strands;
                        double maxLen = blastRadius * (1.0 - progress * 0.7);

                        for (double d = maxLen; d >= 0.2; d -= 0.35) {
                            double angle = baseAngle + spin + (d * 0.6);
                            double x = Math.cos(angle) * d;
                            double z = Math.sin(angle) * d;
                            double y = (1.0 - d / blastRadius) * 0.6;

                            Location p = center.clone().add(x, y, z);
                            if (p.getWorld() != center.getWorld()) continue;

                            // Sáng dần thành trắng khi gần tâm
                            double heat = 1.0 - (d / blastRadius);
                            Color color = Color.fromRGB(
                                    (int) (60 + heat * 195),
                                    (int) (30 + heat * 225),
                                    (int) (120 + heat * 135)
                            );

                            p.getWorld().spawnParticle(
                                    Particle.DUST, p, 1,
                                    new Particle.DustOptions(color, 1.3f)
                            );
                        }
                    }

                    // Lõi kỳ dị ở tâm, đen với viền tím
                    center.getWorld().spawnParticle(
                            Particle.DUST, center, 3, 0.15, 0.15, 0.15, 0,
                            new Particle.DustOptions(Color.fromRGB(20, 5, 30), 1.0f + (float) progress)
                    );

                } else if (ticks == pullTicks) {
                    collapse();
                } else {
                    center.getWorld().spawnParticle(Particle.END_ROD, center, 6, 0.5, 0.5, 0.5, 0.2);
                }

                ticks++;
            }

            private void collapse() {
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.3f);
                center.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.3f);

                center.getWorld().spawnParticle(Particle.FLASH, center, 4, 0.2, 0.2, 0.2, 0);
                center.getWorld().spawnParticle(Particle.EXPLOSION, center, 4, 0.6, 0.6, 0.6, 0);

                // Vòng sóng ánh sáng lan tỏa từ tâm
                int points = 100;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    Location p = center.clone().add(
                            Math.cos(angle) * blastRadius, 0, Math.sin(angle) * blastRadius);
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(Color.fromRGB(220, 200, 255), 1.5f)
                    );
                }
                center.getWorld().spawnParticle(Particle.END_ROD, center, 90, 0.5, 0.5, 0.5, 0.7);

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (entity.getLocation().distanceSquared(center) > blastRadiusSquared) continue;

                    entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    entity.damage(damage, attacker);
                    final LivingEntity ref = entity;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (ref.isValid()) ref.removeMetadata("IS_ABILITY", Main.getInstance());
                        }
                    }.runTaskLater(Main.getInstance(), 1L);

                    Vector away = entity.getLocation().toVector().subtract(center.toVector());
                    if (away.lengthSquared() < 0.01) away = new Vector(1, 0, 0);
                    away.setY(0);
                    Vector push = away.normalize().multiply(0.5);
                    push.setY(0.35);
                    entity.setVelocity(push);
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
