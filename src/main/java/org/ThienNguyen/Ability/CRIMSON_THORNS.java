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
 * CRIMSON_THORNS
 * 3 đợt gai máu trồi lên từ mặt đất quanh mục tiêu theo từng vòng mở rộng, mỗi đợt
 * trồi lên đâm trúng những ai đứng trong bán kính nhỏ rồi rút xuống trước đợt kế tiếp.
 */
public class CRIMSON_THORNS implements IAbility {

    private static final int HITS = 3;
    private static final double BASE_PERCENT = 28.0;
    private static final double PERCENT_PER_LEVEL = 20.0;

    @Override
    public String getName() {
        return "CRIMSON_THORNS";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);
        final double damagePercent = BASE_PERCENT + (skillLevel - 1) * PERCENT_PER_LEVEL;
        final double hitDamage = baseDamage * (damagePercent / 100.0);

        final double radius = 2.1;
        final double radiusSquared = radius * radius;

        final Location center = target.getLocation().clone();
        final Color thornColor = Color.fromRGB(150, 10, 20);
        final Color bloodColor = Color.fromRGB(220, 40, 40);

        final int[] waveTicks = {5, 12, 19};
        final double[] waveRadii = {0.8, 1.5, 2.1};
        final int totalTicks = 24;

        center.getWorld().playSound(center, Sound.BLOCK_ROOTED_DIRT_BREAK, 1.0f, 0.6f);

        new BukkitRunnable() {

            int ticks = 0;
            int waveIndex = 0;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                if (waveIndex < waveTicks.length && ticks == waveTicks[waveIndex]) {
                    erupt(waveRadii[waveIndex]);
                    waveIndex++;
                }

                ticks++;
            }

            private void erupt(double ringRadius) {
                center.getWorld().playSound(center, Sound.ENTITY_HOSTILE_BIG_FALL, 0.8f, 0.7f);
                int spikes = Math.max(10, (int) (ringRadius * 10));
                for (int i = 0; i < spikes; i++) {
                    double a = (2 * Math.PI * i) / spikes;
                    double px = Math.cos(a) * ringRadius;
                    double pz = Math.sin(a) * ringRadius;
                    for (double y = 0; y <= 1.4; y += 0.2) {
                        Location p = center.clone().add(px, y, pz);
                        if (p.getWorld() != center.getWorld()) continue;
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(thornColor, 1.3f)
                        );
                    }
                    Location tip = center.clone().add(px, 1.4, pz);
                    if (tip.getWorld() == center.getWorld()) {
                        tip.getWorld().spawnParticle(
                                Particle.DUST, tip, 2,
                                new Particle.DustOptions(bloodColor, 1.1f)
                        );
                    }
                }

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;

                    Location eLoc = entity.getLocation();
                    if (Math.abs(eLoc.getY() - center.getY()) > 2.5) continue;

                    double dx = eLoc.getX() - center.getX();
                    double dz = eLoc.getZ() - center.getZ();
                    double distSq = (dx * dx) + (dz * dz);
                    if (distSq > radiusSquared) continue;

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
