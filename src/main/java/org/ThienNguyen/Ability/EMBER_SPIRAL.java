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
 * EMBER_SPIRAL
 * Một xoáy lửa cuốn từ mặt đất lên cao quanh mục tiêu, tàn lửa bay theo hình trôn ốc.
 * Xoáy siết dần vào giữa và bùng lên 4 đợt sát thương liên tiếp.
 */
public class EMBER_SPIRAL implements IAbility {

    private static final int NUM_HITS = 4;
    private static final int HIT_INTERVAL = 5;
    private static final int ARMS = 3;

    @Override
    public String getName() {
        return "EMBER_SPIRAL";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 20% + 14%/level, chia đều cho 4 nhịp
        final double damagePercent = 20.0 + (skillLevel - 1) * 14.0;
        final double totalDamage = baseDamage * (damagePercent / 100.0);
        final double perHitDamage = totalDamage / NUM_HITS;

        final double radius = 2.2 + (skillLevel * 0.05);

        final Location center = target.getLocation().clone().add(0, 0.1, 0);

        final Color outer = Color.fromRGB(255, 140, 30);
        final Color inner = Color.fromRGB(255, 220, 80);

        center.getWorld().playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.9f, 0.9f);

        new BukkitRunnable() {

            int ticks = 0;
            int hitsDone = 0;
            final int totalTicks = (NUM_HITS - 1) * HIT_INTERVAL + 12;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                drawSpiral();

                boolean isHitTick = (ticks % HIT_INTERVAL == HIT_INTERVAL - 1) && hitsDone < NUM_HITS;
                if (isHitTick) {
                    pulseHit();
                    hitsDone++;
                }

                ticks++;
            }

            private void drawSpiral() {
                double spin = ticks * 0.35;
                double heightMax = 2.0;

                for (int arm = 0; arm < ARMS; arm++) {
                    double armOffset = (2 * Math.PI * arm) / ARMS;
                    for (double h = 0; h <= heightMax; h += 0.2) {
                        double shrink = 1.0 - (h / heightMax) * 0.5;
                        double r = radius * shrink;
                        double a = spin + armOffset + h * 2.2;
                        double px = Math.cos(a) * r;
                        double pz = Math.sin(a) * r;

                        Location p = center.clone().add(px, h, pz);
                        if (p.getWorld() != center.getWorld()) continue;

                        Color c = (h < heightMax * 0.5) ? outer : inner;
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(c, 1.1f)
                        );
                        if (h < 0.4) {
                            p.getWorld().spawnParticle(Particle.FLAME, p, 1, 0.02, 0.02, 0.02, 0.0);
                        }
                    }
                }
            }

            private void pulseHit() {
                center.getWorld().playSound(center, Sound.ENTITY_BLAZE_HURT, 1.0f, 1.2f);
                center.getWorld().spawnParticle(Particle.LAVA, center.clone().add(0, 0.5, 0), 6, 0.3, 0.3, 0.3, 0.0);

                double rSq = radius * radius;
                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;

                    Location eLoc = entity.getLocation();
                    if (Math.abs(eLoc.getY() - center.getY()) > 3.0) continue;

                    double dx = eLoc.getX() - center.getX();
                    double dz = eLoc.getZ() - center.getZ();
                    if ((dx * dx) + (dz * dz) > rSq) continue;

                    entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    entity.damage(perHitDamage, attacker);
                    final LivingEntity ref = entity;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (ref.isValid()) ref.removeMetadata("IS_ABILITY", Main.getInstance());
                        }
                    }.runTaskLater(Main.getInstance(), 1L);

                    entity.getWorld().spawnParticle(
                            Particle.FLAME, eLoc.clone().add(0, 1, 0), 14, 0.3, 0.5, 0.3, 0.02);
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}