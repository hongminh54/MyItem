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
 * TIDAL_CRUSH
 * Một cột nước cuộn cao lên phía trên mục tiêu, uốn cong thành hình sóng biển rồi đổ sập
 * xuống. Sát thương 1 lần kèm đẩy lùi mạnh theo phương ngang.
 */
public class TIDAL_CRUSH implements IAbility {

    @Override
    public String getName() {
        return "TIDAL_CRUSH";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 26% + 9%/level
        final double damagePercent = 26.0 + (skillLevel - 1) * 9.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double crushRadius = 3.5 + (skillLevel * 0.15);
        final double crushRadiusSquared = crushRadius * crushRadius;

        final Location center = target.getLocation().clone();
        final Color waterColor = Color.fromRGB(60, 140, 220);
        final Color foamColor = Color.fromRGB(230, 245, 255);

        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_SWIM, 1.2f, 0.6f);

        new BukkitRunnable() {

            int ticks = 0;
            final int riseTicks = 16; // Cuộn lên & uốn cong
            final int totalTicks = 22;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                if (ticks < riseTicks) {

                    double progress = (double) ticks / riseTicks;
                    double waveHeight = 5.0 * progress;
                    // Sóng cong dần về phía trước khi lên cao (giống sóng biển cuộn)
                    double curl = Math.pow(progress, 2) * 2.0;

                    int columns = 20;
                    for (int c = 0; c < columns; c++) {
                        double lateral = (c - columns / 2.0) * 0.3;

                        for (double h = 0; h <= waveHeight; h += 0.3) {
                            double heightFrac = h / Math.max(0.1, waveHeight);
                            double forwardCurl = curl * heightFrac * heightFrac;

                            Location p = center.clone().add(lateral, h, forwardCurl);
                            if (p.getWorld() != center.getWorld()) continue;

                            Color color = heightFrac > 0.8 ? foamColor : waterColor;
                            p.getWorld().spawnParticle(
                                    Particle.DUST, p, 1,
                                    new Particle.DustOptions(color, 1.2f)
                            );
                        }
                    }

                    center.getWorld().spawnParticle(Particle.SPLASH, center.clone().add(0, waveHeight * 0.5, 0),
                            4, 0.3, waveHeight * 0.3, 0.3, 0.05);

                } else if (ticks == riseTicks) {
                    crash();
                } else {
                    center.getWorld().spawnParticle(Particle.SPLASH, center, 20, crushRadius * 0.5, 0.3, crushRadius * 0.5, 0.05);
                }

                ticks++;
            }

            private void crash() {
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_SPLASH, 1.4f, 0.7f);
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.5f);

                // Vòng nước tràn ra khi sóng đổ sập
                int points = 70;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    Location p = center.clone().add(
                            Math.cos(angle) * crushRadius, 0.2, Math.sin(angle) * crushRadius);
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(foamColor, 1.4f)
                    );
                }
                center.getWorld().spawnParticle(Particle.SPLASH, center.clone().add(0, 1, 0),
                        80, crushRadius * 0.4, 1.0, crushRadius * 0.4, 0.15);

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (entity.getLocation().distanceSquared(center) > crushRadiusSquared) continue;

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
                    if (away.lengthSquared() < 0.01) away = new Vector(0, 0, 1);
                    away.setY(0);
                    Vector push = away.normalize().multiply(0.9);
                    push.setY(0.2);
                    entity.setVelocity(push);
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
