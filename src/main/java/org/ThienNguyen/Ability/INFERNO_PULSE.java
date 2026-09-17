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
 * INFERNO_PULSE
 * 3 vòng lửa đồng tâm bùng lên và lan rộng nối tiếp nhau quanh mục tiêu, mỗi vòng
 * bùng nổ một lần gây sát thương trong bán kính nhỏ trước khi tắt dần.
 */
public class INFERNO_PULSE implements IAbility {

    private static final int HITS = 3;
    private static final double BASE_PERCENT = 30.0;
    private static final double PERCENT_PER_LEVEL = 18.0;

    @Override
    public String getName() {
        return "INFERNO_PULSE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);
        final double damagePercent = BASE_PERCENT + (skillLevel - 1) * PERCENT_PER_LEVEL;
        final double hitDamage = baseDamage * (damagePercent / 100.0);

        final double radius = 2.4;
        final double radiusSquared = radius * radius;

        final Location center = target.getLocation().clone().add(0, 0.1, 0);
        final Color fireColor = Color.fromRGB(255, 120, 30);
        final Color emberColor = Color.fromRGB(255, 210, 90);

        final int[] pulseTicks = {6, 14, 22};
        final int totalTicks = 26;

        center.getWorld().playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.9f);

        new BukkitRunnable() {

            int ticks = 0;
            int pulseIndex = 0;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                double cyclePos = (ticks % 8) / 8.0;
                double ringRadius = radius * cyclePos;
                int points = Math.max(16, (int) (ringRadius * 14));
                for (int i = 0; i < points; i++) {
                    double a = (2 * Math.PI * i) / points;
                    Location p = center.clone().add(Math.cos(a) * ringRadius, 0.1, Math.sin(a) * ringRadius);
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(Particle.FLAME, p, 1, 0, 0.05, 0, 0.0);
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(fireColor, 1.3f)
                    );
                }

                if (pulseIndex < pulseTicks.length && ticks == pulseTicks[pulseIndex]) {
                    ignite();
                    pulseIndex++;
                }

                ticks++;
            }

            private void ignite() {
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.5f);
                for (int i = 0; i < 35; i++) {
                    double a = (2 * Math.PI * i) / 35;
                    double r = radius * Math.random();
                    for (double y = 0; y <= 1.5; y += 0.4) {
                        Location p = center.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                        if (p.getWorld() != center.getWorld()) continue;
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(y > 0.7 ? emberColor : fireColor, 1.4f)
                        );
                    }
                }
                center.getWorld().spawnParticle(Particle.LAVA, center.clone().add(0, 0.3, 0), 8, 0.4, 0.2, 0.4, 0.0);

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
