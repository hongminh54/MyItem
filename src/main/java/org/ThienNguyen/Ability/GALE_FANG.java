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
 * GALE_FANG
 * 3 luồng nanh gió xoáy lao thẳng qua mục tiêu từ các hướng khác nhau liên tiếp,
 * mỗi luồng gió xé qua gây 1 lần sát thương trong bán kính nhỏ dọc đường đi.
 */
public class GALE_FANG implements IAbility {

    private static final int HITS = 3;
    private static final double BASE_PERCENT = 40.0;
    private static final double PERCENT_PER_LEVEL = 25.0;

    @Override
    public String getName() {
        return "GALE_FANG";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);
        final double damagePercent = BASE_PERCENT + (skillLevel - 1) * PERCENT_PER_LEVEL;
        final double hitDamage = baseDamage * (damagePercent / 100.0);

        final double radius = 1.6;
        final double radiusSquared = radius * radius;
        final double fangLength = 4.0;

        final Location center = target.getLocation().clone().add(0, 1.0, 0);
        final Color windColor = Color.fromRGB(230, 250, 245);
        final Color trailColor = Color.fromRGB(180, 230, 220);

        final int[] fangTicks = {4, 10, 16};
        final int totalTicks = 18;

        center.getWorld().playSound(center, Sound.ENTITY_PHANTOM_FLAP, 1.0f, 1.3f);

        new BukkitRunnable() {

            int ticks = 0;
            int fangIndex = 0;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                if (fangIndex < fangTicks.length && ticks == fangTicks[fangIndex]) {
                    double angle = (2 * Math.PI / 3) * fangIndex + (Math.random() * 0.4 - 0.2);
                    tearThrough(angle);
                    fangIndex++;
                }

                ticks++;
            }

            private void tearThrough(double angle) {
                Vector dir = new Vector(Math.cos(angle), 0, Math.sin(angle));
                Location start = center.clone().subtract(dir.clone().multiply(fangLength / 2.0));

                center.getWorld().playSound(center, Sound.ITEM_TRIDENT_RIPTIDE_2, 0.9f, 1.6f);

                for (double d = 0; d <= fangLength; d += 0.25) {
                    Location p = start.clone().add(dir.clone().multiply(d));
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(Particle.CLOUD, p, 1, 0.06, 0.06, 0.06, 0.0);

                    // Đã bổ sung truyền DustOptions đầy đủ cho Particle.DUST
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            0.0, 0.0, 0.0, 0.0,
                            new Particle.DustOptions(windColor, 1.2f)
                    );
                }
                center.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center, 2, 0.2, 0.1, 0.2, 0.0);

                // Đã bổ sung truyền DustOptions đầy đủ cho Particle.DUST ở tâm
                center.getWorld().spawnParticle(
                        Particle.DUST, center, 6,
                        0.3, 0.3, 0.3,
                        0.0,
                        new Particle.DustOptions(windColor, 1.2f)
                );

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;

                    Location eLoc = entity.getLocation();
                    if (Math.abs(eLoc.getY() - center.getY()) > 2.0) continue;

                    double dx = eLoc.getX() - center.getX();
                    double dz = eLoc.getZ() - center.getZ();
                    if ((dx * dx) + (dz * dz) > radiusSquared) continue;

                    entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    entity.damage(hitDamage, attacker);

                    // Đã bổ sung truyền DustOptions đầy đủ khi gây sát thương trúng entity
                    entity.getWorld().spawnParticle(
                            Particle.DUST, eLoc.clone().add(0, 1, 0), 6,
                            0.3, 0.3, 0.3, 0.0,
                            new Particle.DustOptions(trailColor, 1.1f)
                    );

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