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
 * TOXIC_BLOOM
 * Một đóa hoa độc mọc lên từ dưới chân mục tiêu và nở ra 4 lần liên tiếp, mỗi lần nở
 * phun bào tử độc gây sát thương trong bán kính nhỏ quanh tâm hoa.
 */
public class TOXIC_BLOOM implements IAbility {

    private static final int HITS = 4;
    private static final double BASE_PERCENT = 15.0;
    private static final double PERCENT_PER_LEVEL = 10.0;

    @Override
    public String getName() {
        return "TOXIC_BLOOM";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);
        final double damagePercent = BASE_PERCENT + (skillLevel - 1) * PERCENT_PER_LEVEL;
        final double hitDamage = baseDamage * (damagePercent / 100.0);

        final double radius = 2.0;
        final double radiusSquared = radius * radius;

        final Location center = target.getLocation().clone().add(0, 0.1, 0);
        final Color petalColor = Color.fromRGB(140, 220, 60);
        final Color sporeColor = Color.fromRGB(90, 180, 40);

        final int[] bloomTicks = {5, 10, 15, 20};
        final int totalTicks = 24;

        center.getWorld().playSound(center, Sound.ENTITY_SLIME_SQUISH, 1.0f, 0.7f);

        new BukkitRunnable() {

            int ticks = 0;
            int bloomIndex = 0;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                // Cánh hoa lay động nhẹ quanh tâm
                double sway = Math.sin(ticks * 0.3) * 0.15;
                int petals = 6;
                for (int i = 0; i < petals; i++) {
                    double a = (2 * Math.PI * i) / petals;
                    double r = radius * 0.5 + sway;
                    Location p = center.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r);
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(petalColor, 1.2f)
                    );
                }

                if (bloomIndex < bloomTicks.length && ticks == bloomTicks[bloomIndex]) {
                    bloom();
                    bloomIndex++;
                }

                ticks++;
            }

            private void bloom() {
                center.getWorld().playSound(center, Sound.BLOCK_SWEET_BERRY_BUSH_BREAK, 0.8f, 0.6f);
                for (int i = 0; i < 30; i++) {
                    double a = (2 * Math.PI * i) / 30;
                    double r = radius * (0.3 + Math.random() * 0.7);
                    Location p = center.clone().add(Math.cos(a) * r, 0.4 + Math.random() * 0.6, Math.sin(a) * r);
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(sporeColor, 1.3f)
                    );
                }
                center.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, center.clone().add(0, 0.5, 0), 20, 0.6, 0.4, 0.6, 0.0);

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
