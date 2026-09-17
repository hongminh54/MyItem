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
 * METEOR_STRIKE
 * Vẽ vòng cảnh báo đỏ tại vị trí mục tiêu trong 1.5 giây, sau đó giáng một đòn AoE mạnh
 * kèm hất tung lên không.
 */
public class METEOR_STRIKE implements IAbility {

    @Override
    public String getName() {
        return "METEOR_STRIKE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        final double radius = 3.0 + (skillLevel * 0.2);
        final double radiusSquared = radius * radius;
        final double damagePercent = 30.0 + (skillLevel - 1) * 10.0;
        final double damage = baseDamage * (damagePercent / 100.0);
        final double knockupVelocity = Math.min(1.2, 0.6 + skillLevel * 0.02);

        final Location center = target.getLocation().clone();

        new BukkitRunnable() {

            int ticks = 0;
            final int telegraphTicks = 30; // 1.5 giây cảnh báo

            @Override
            public void run() {

                if (center.getWorld() == null) {
                    this.cancel();
                    return;
                }

                if (ticks >= telegraphTicks) {
                    impact();
                    this.cancel();
                    return;
                }

                // Vòng cảnh báo thu nhỏ dần
                double shrinkProgress = (double) ticks / telegraphTicks;
                double displayRadius = radius * (1.5 - 0.5 * shrinkProgress);

                int points = 40;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    double x = Math.cos(angle) * displayRadius;
                    double z = Math.sin(angle) * displayRadius;
                    Location p = center.clone().add(x, 0.05, z);
                    p.getWorld().spawnParticle(
                            Particle.DUST,
                            p,
                            1,
                            new Particle.DustOptions(Color.fromRGB(220, 30, 20), 1.3f)
                    );
                }

                ticks++;
            }

            private void impact() {
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);

                int points = 80;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location p = center.clone().add(x, 0.3, z);
                    p.getWorld().spawnParticle(Particle.FLAME, p, 2, 0.05, 0.1, 0.05, 0.01);
                }

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;

                    if (entity.getLocation().distanceSquared(center) <= radiusSquared) {
                        entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                        entity.damage(damage, attacker);
                        final LivingEntity ref = entity;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (ref.isValid()) ref.removeMetadata("IS_ABILITY", Main.getInstance());
                            }
                        }.runTaskLater(Main.getInstance(), 1L);

                        Vector velocity = entity.getVelocity();
                        entity.setVelocity(new Vector(velocity.getX(), knockupVelocity, velocity.getZ()));
                    }
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
