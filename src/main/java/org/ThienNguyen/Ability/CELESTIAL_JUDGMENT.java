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
 * CELESTIAL_JUDGMENT
 * 3 trụ ánh sáng thần thánh giáng xuống nối tiếp nhau ngay tại vị trí mục tiêu,
 * mỗi trụ sáng đánh trúng một lần gây sát thương trong bán kính nhỏ.
 */
public class CELESTIAL_JUDGMENT implements IAbility {

    private static final int HITS = 3;
    private static final double BASE_PERCENT = 35.0;
    private static final double PERCENT_PER_LEVEL = 22.0;

    @Override
    public String getName() {
        return "CELESTIAL_JUDGMENT";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);
        final double damagePercent = BASE_PERCENT + (skillLevel - 1) * PERCENT_PER_LEVEL;
        final double hitDamage = baseDamage * (damagePercent / 100.0);

        final double radius = 1.8;
        final double radiusSquared = radius * radius;
        final double beamHeight = 7.0;

        final Location center = target.getLocation().clone();
        final Color lightColor = Color.fromRGB(255, 250, 210);
        final Color goldColor = Color.fromRGB(255, 215, 100);

        final int[] beamTicks = {6, 14, 22};
        final int totalTicks = 26;

        center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.2f);

        new BukkitRunnable() {

            int ticks = 0;
            int beamIndex = 0;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                // Vòng sáng nhẹ xoay quanh mục tiêu trong lúc chờ trụ sáng tiếp theo
                double spin = ticks * 0.25;
                for (int i = 0; i < 20; i++) {
                    double a = (2 * Math.PI * i) / 20 + spin;
                    Location p = center.clone().add(Math.cos(a) * radius, 0.05, Math.sin(a) * radius);
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(goldColor, 1.0f)
                    );
                }

                if (beamIndex < beamTicks.length && ticks == beamTicks[beamIndex]) {
                    judge();
                    beamIndex++;
                }

                ticks++;
            }

            private void judge() {
                center.getWorld().playSound(center, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.2f, 1.1f);
                for (double y = 0; y <= beamHeight; y += 0.25) {
                    Location p = center.clone().add(0, y, 0);
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(Particle.END_ROD, p, 1, 0.08, 0, 0.08, 0.0);
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(lightColor, 1.4f)
                    );
                }
                center.getWorld().spawnParticle(Particle.FLASH, center.clone().add(0, 0.5, 0), 1, 0, 0, 0, 0);
                center.getWorld().spawnParticle(Particle.ENCHANT, center.clone().add(0, 1.5, 0), 40, 1.0, 1.0, 1.0, 0.5);

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
