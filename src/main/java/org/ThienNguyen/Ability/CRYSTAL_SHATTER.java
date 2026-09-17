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
 * CRYSTAL_SHATTER
 * Một khối pha lê hình bát diện kết tinh quanh mục tiêu, rung lên rồi vỡ tung thành hàng trăm
 * mảnh bắn ra mọi hướng. Sát thương nổ 1 lần duy nhất lúc vỡ.
 */
public class CRYSTAL_SHATTER implements IAbility {

    @Override
    public String getName() {
        return "CRYSTAL_SHATTER";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 30% + 10%/level
        final double damagePercent = 30.0 + (skillLevel - 1) * 10.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double blastRadius = 4.0 + (skillLevel * 0.2);
        final double blastRadiusSquared = blastRadius * blastRadius;

        final Location center = target.getLocation().clone().add(0, 1.0, 0);
        final Color crystalColor = Color.fromRGB(180, 120, 255);

        center.getWorld().playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1.0f, 0.7f);

        new BukkitRunnable() {

            int ticks = 0;
            final int formTicks = 18;   // Kết tinh + rung
            final int totalTicks = 22;  // Vỡ ở tick 18

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                if (ticks < formTicks) {
                    double progress = (double) ticks / formTicks;
                    double size = 1.6 * progress;
                    // Rung mạnh dần trước khi vỡ
                    double shake = progress > 0.6 ? (progress - 0.6) * 0.5 : 0;
                    double jitterX = (Math.random() - 0.5) * shake;
                    double jitterZ = (Math.random() - 0.5) * shake;

                    drawOctahedron(center.clone().add(jitterX, 0, jitterZ), size, crystalColor, progress);

                } else if (ticks == formTicks) {
                    shatter();
                } else {
                    // Bụi pha lê còn đọng lại sau khi vỡ
                    center.getWorld().spawnParticle(
                            Particle.DUST, center, 14, blastRadius * 0.5, 0.6, blastRadius * 0.5, 0,
                            new Particle.DustOptions(crystalColor, 1.0f)
                    );
                }

                ticks++;
            }

            /** Vẽ khung bát diện: 6 đỉnh nối nhau bằng các đường hạt. */
            private void drawOctahedron(Location c, double size, Color color, double progress) {
                double[][] vertices = {
                        {0, size, 0}, {0, -size, 0},
                        {size, 0, 0}, {-size, 0, 0},
                        {0, 0, size}, {0, 0, -size}
                };
                // Nối đỉnh trên/dưới với 4 đỉnh ngang
                int[][] edges = {
                        {0, 2}, {0, 3}, {0, 4}, {0, 5},
                        {1, 2}, {1, 3}, {1, 4}, {1, 5},
                        {2, 4}, {4, 3}, {3, 5}, {5, 2}
                };

                float particleSize = (float) (1.0 + progress * 0.6);

                for (int[] edge : edges) {
                    double[] a = vertices[edge[0]];
                    double[] b = vertices[edge[1]];
                    for (double t = 0; t <= 1.0; t += 0.12) {
                        Location p = c.clone().add(
                                a[0] + (b[0] - a[0]) * t,
                                a[1] + (b[1] - a[1]) * t,
                                a[2] + (b[2] - a[2]) * t
                        );
                        if (p.getWorld() != c.getWorld()) continue;
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(color, particleSize)
                        );
                    }
                }
            }

            private void shatter() {
                center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1.4f, 0.6f);
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.6f);

                // Mảnh vỡ bắn ra theo phân bố cầu
                int shards = 160;
                for (int i = 0; i < shards; i++) {
                    double theta = Math.random() * 2 * Math.PI;
                    double phi = Math.acos(2 * Math.random() - 1);
                    double r = blastRadius * (0.3 + Math.random() * 0.7);

                    double x = r * Math.sin(phi) * Math.cos(theta);
                    double y = r * Math.cos(phi) * 0.6;
                    double z = r * Math.sin(phi) * Math.sin(theta);

                    Location p = center.clone().add(x, y, z);
                    if (p.getWorld() != center.getWorld()) continue;

                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(crystalColor, 1.3f)
                    );
                }
                center.getWorld().spawnParticle(Particle.END_ROD, center, 40, 0.4, 0.4, 0.4, 0.25);

                // Sát thương 1 lần cho mọi entity trong bán kính nổ
                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (entity.getLocation().distanceSquared(center) > blastRadiusSquared) continue;

                    entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    entity.damage(damage, attacker);
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
