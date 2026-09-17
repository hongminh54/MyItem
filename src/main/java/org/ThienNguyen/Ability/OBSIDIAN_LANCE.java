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
 * OBSIDIAN_LANCE
 * Một mũi giáo đá obsidian sắc nhọn phóng thẳng lên từ dưới chân mục tiêu với tốc độ cực
 * nhanh, xuyên thấu và tỏa các vết nứt đá quanh điểm đâm. Sát thương 1 lần.
 */
public class OBSIDIAN_LANCE implements IAbility {

    @Override
    public String getName() {
        return "OBSIDIAN_LANCE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 28% + 9%/level
        final double damagePercent = 28.0 + (skillLevel - 1) * 9.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double lanceHeight = 4.0 + (skillLevel * 0.1);
        final double crackRadius = 2.5;

        final Location center = target.getLocation().clone();
        final Color obsidian = Color.fromRGB(35, 15, 55);
        final Color edgeGlow = Color.fromRGB(160, 80, 220);

        center.getWorld().playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.1f, 0.5f);

        new BukkitRunnable() {

            int ticks = 0;
            final int growTicks = 5; // Cực nhanh, gây bất ngờ
            final int totalTicks = 12;
            boolean struck = false;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                if (ticks <= growTicks) {
                    double progress = (double) ticks / growTicks;
                    double currentHeight = lanceHeight * progress;

                    // Thân giáo góc cạnh: 4 mặt phẳng hẹp dần lên đỉnh nhọn
                    for (int face = 0; face < 4; face++) {
                        double faceAngle = (Math.PI / 2 * face);
                        for (double y = 0; y <= currentHeight; y += 0.25) {
                            double taper = 1.0 - (y / lanceHeight) * 0.9;
                            double r = 0.4 * taper;
                            Location p = center.clone().add(
                                    Math.cos(faceAngle) * r, y, Math.sin(faceAngle) * r);
                            if (p.getWorld() != center.getWorld()) continue;

                            boolean edge = taper < 0.2;
                            p.getWorld().spawnParticle(
                                    Particle.DUST, p, 1,
                                    new Particle.DustOptions(edge ? edgeGlow : obsidian, 1.2f)
                            );
                        }
                    }

                    if (!struck && currentHeight >= 1.0) {
                        struck = true;
                        strike();
                    }

                } else {
                    center.getWorld().spawnParticle(
                            Particle.DUST, center.clone().add(0, 1, 0), 5, 0.4, 0.4, 0.4, 0,
                            new Particle.DustOptions(obsidian, 1.0f)
                    );
                }

                ticks++;
            }

            private void strike() {
                center.getWorld().playSound(center, Sound.BLOCK_STONE_BREAK, 1.0f, 0.6f);
                center.getWorld().spawnParticle(Particle.CRIT, center.clone().add(0, 1, 0),
                        20, 0.3, 0.5, 0.3, 0.2);

                // Vết nứt đá tỏa ra quanh điểm đâm
                int cracks = 6;
                for (int c = 0; c < cracks; c++) {
                    double angle = (2 * Math.PI * c) / cracks;
                    for (double d = 0.3; d <= crackRadius; d += 0.3) {
                        Location p = center.clone().add(Math.cos(angle) * d, 0.05, Math.sin(angle) * d);
                        if (p.getWorld() != center.getWorld()) continue;
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(edgeGlow, 1.1f)
                        );
                    }
                }

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (entity.getLocation().distanceSquared(center) > 1.3 * 1.3) continue;

                    entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    entity.damage(damage, attacker);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (entity.isValid()) entity.removeMetadata("IS_ABILITY", Main.getInstance());
                        }
                    }.runTaskLater(Main.getInstance(), 1L);
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
