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
 * VOID_RIFT
 * Một vết rách không gian xoáy tại vị trí mục tiêu, kéo các hạt vật chất vào tâm rồi
 * co sập lại 4 lần liên tiếp, mỗi lần sập gây sát thương trong bán kính nhỏ quanh tâm.
 */
public class VOID_RIFT implements IAbility {

    private static final double BASE_PERCENT = 25.0;
    private static final double PERCENT_PER_LEVEL = 15.0;

    @Override
    public String getName() {
        return "VOID_RIFT";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);
        final double damagePercent = BASE_PERCENT + (skillLevel - 1) * PERCENT_PER_LEVEL;
        final double hitDamage = baseDamage * (damagePercent / 100.0);

        final double radius = 2.0;
        final double radiusSquared = radius * radius;

        final Location center = target.getLocation().clone().add(0, 1.0, 0);
        final Color voidColor = Color.fromRGB(90, 20, 140);
        final Color coreColor = Color.fromRGB(200, 80, 255);

        final int[] collapseTicks = {5, 10, 15, 20};
        final int totalTicks = 24;

        center.getWorld().playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.4f);

        new BukkitRunnable() {

            int ticks = 0;
            int collapseIndex = 0;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                double spin = ticks * 0.4;

                // Xoáy hút hạt vào tâm giữa các đợt sập
                int spiralPoints = 18;
                for (int i = 0; i < spiralPoints; i++) {
                    double t = (i / (double) spiralPoints);
                    double a = spin + t * 6.0;
                    double r = radius * 1.3 * (1.0 - t);
                    Location p = center.clone().add(Math.cos(a) * r, t * 0.6 - 0.3, Math.sin(a) * r);
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(voidColor, 1.2f)
                    );
                }
                center.getWorld().spawnParticle(Particle.PORTAL, center, 6, 0.4, 0.4, 0.4, 0.4);

                if (collapseIndex < collapseTicks.length && ticks == collapseTicks[collapseIndex]) {
                    collapse();
                    collapseIndex++;
                }

                ticks++;
            }

            private void collapse() {
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.7f);
                center.getWorld().spawnParticle(Particle.SQUID_INK, center, 30, 0.5, 0.5, 0.5, 0.02);
                for (int i = 0; i < 30; i++) {
                    double a = (2 * Math.PI * i) / 30;
                    Location p = center.clone().add(Math.cos(a) * radius * 0.5, 0, Math.sin(a) * radius * 0.5);
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(coreColor, 1.5f)
                    );
                }
                center.getWorld().spawnParticle(Particle.FLASH, center, 1, 0, 0, 0, 0);

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;

                    Location eLoc = entity.getLocation();
                    if (Math.abs(eLoc.getY() - center.getY()) > 2.5) continue;

                    double dx = eLoc.getX() - center.getX();
                    double dz = eLoc.getZ() - center.getZ();
                    if ((dx * dx) + (dz * dz) > radiusSquared) continue;

                    entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    entity.damage(hitDamage, attacker);
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
