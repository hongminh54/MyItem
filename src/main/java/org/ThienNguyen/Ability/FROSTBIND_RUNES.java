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
 * FROSTBIND_RUNES
 * 3 vòng chữ rune băng giá hiện lên quanh mục tiêu, mỗi vòng khép lại siết chặt
 * và bung ra một đợt gai băng, gây sát thương theo 3 nhịp liên tiếp.
 */
public class FROSTBIND_RUNES implements IAbility {

    private static final int NUM_HITS = 3;
    private static final int HIT_INTERVAL = 7;
    private static final int RUNE_COUNT = 6;

    @Override
    public String getName() {
        return "FROSTBIND_RUNES";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 18% + 10%/level, chia đều cho 3 nhịp
        final double damagePercent = 18.0 + (skillLevel - 1) * 10.0;
        final double totalDamage = baseDamage * (damagePercent / 100.0);
        final double perHitDamage = totalDamage / NUM_HITS;

        final double radius = 1.8 + (skillLevel * 0.04);

        final Location center = target.getLocation().clone().add(0, 0.1, 0);

        final Color iceColor = Color.fromRGB(150, 220, 255);
        final Color deepColor = Color.fromRGB(80, 150, 255);

        center.getWorld().playSound(center, Sound.BLOCK_GLASS_PLACE, 1.0f, 0.7f);

        new BukkitRunnable() {

            int ticks = 0;
            int hitsDone = 0;
            final int totalTicks = (NUM_HITS - 1) * HIT_INTERVAL + 10;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                int cyclePos = ticks % HIT_INTERVAL;
                double closeProgress = Math.min(1.0, (double) cyclePos / (HIT_INTERVAL - 2));
                double ringRadius = radius * (1.0 - 0.4 * closeProgress);

                drawRunes(ringRadius, ticks * 0.15);

                boolean isHitTick = (ticks % HIT_INTERVAL == HIT_INTERVAL - 1) && hitsDone < NUM_HITS;
                if (isHitTick) {
                    pulseHit(ringRadius);
                    hitsDone++;
                }

                ticks++;
            }

            private void drawRunes(double r, double spin) {
                for (int i = 0; i < RUNE_COUNT; i++) {
                    double a = (2 * Math.PI * i) / RUNE_COUNT + spin;
                    double rx = Math.cos(a) * r;
                    double rz = Math.sin(a) * r;
                    Location p = center.clone().add(rx, 0.1, rz);
                    if (p.getWorld() != center.getWorld()) continue;

                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 2, 0.05, 0.15, 0.05,
                            new Particle.DustOptions(iceColor, 1.1f)
                    );
                    p.getWorld().spawnParticle(Particle.SNOWFLAKE, p, 1, 0.05, 0.1, 0.05, 0.0);

                    // Vạch nối vào tâm mờ dần, gợi cảm giác "trói buộc"
                    for (double t = 0.3; t <= 1.0; t += 0.25) {
                        Location link = center.clone().add(rx * t, 0.05, rz * t);
                        if (link.getWorld() != center.getWorld()) continue;
                        link.getWorld().spawnParticle(
                                Particle.DUST, link, 1,
                                new Particle.DustOptions(deepColor, 0.7f)
                        );
                    }
                }
            }

            private void pulseHit(double r) {
                center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.1f);

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
                            Particle.CLOUD, eLoc.clone().add(0, 1, 0), 8, 0.3, 0.4, 0.3, 0.02);
                    entity.getWorld().spawnParticle(
                            Particle.DUST, eLoc.clone().add(0, 1, 0), 12, 0.3, 0.5, 0.3,
                            new Particle.DustOptions(iceColor, 1.0f)
                    );
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}