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
 * RUNIC_BLAST
 * Một ký hiệu rune lục giác hiện lên và xoay tại vị trí mục tiêu, tích năng lượng rồi nổ
 * tung thành các mảnh ánh sáng góc cạnh bắn ra quanh. Sát thương 1 lần.
 */
public class RUNIC_BLAST implements IAbility {

    @Override
    public String getName() {
        return "RUNIC_BLAST";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 27% + 9%/level
        final double damagePercent = 27.0 + (skillLevel - 1) * 9.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double blastRadius = 4.0 + (skillLevel * 0.2);
        final double blastRadiusSquared = blastRadius * blastRadius;
        final double runeRadius = 1.8;

        final Location center = target.getLocation().clone().add(0, 1.0, 0);
        final Color runeColor = Color.fromRGB(80, 210, 255);

        center.getWorld().playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.4f);

        new BukkitRunnable() {

            int ticks = 0;
            final int chargeTicks = 16;
            final int totalTicks = 20;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                if (ticks < chargeTicks) {
                    double progress = (double) ticks / chargeTicks;
                    double spin = progress * 6.0;

                    // Lục giác rune quay và co nhẹ khi tích năng
                    drawHexagon(runeRadius * (1.0 - progress * 0.2), spin, runeColor);
                    // Lõi năng lượng ở giữa sáng dần
                    center.getWorld().spawnParticle(
                            Particle.DUST, center, 2 + (int) (progress * 6), 0.1, 0.1, 0.1, 0,
                            new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.3f)
                    );

                } else if (ticks == chargeTicks) {
                    detonate();
                } else {
                    center.getWorld().spawnParticle(
                            Particle.DUST, center, 8, blastRadius * 0.4, 0.5, blastRadius * 0.4, 0,
                            new Particle.DustOptions(runeColor, 1.0f)
                    );
                }

                ticks++;
            }

            private void drawHexagon(double r, double rotation, Color color) {
                int vertices = 6;
                for (int i = 0; i < vertices; i++) {
                    double a1 = (2 * Math.PI * i) / vertices + rotation;
                    double a2 = (2 * Math.PI * (i + 1)) / vertices + rotation;

                    double x1 = Math.cos(a1) * r, z1 = Math.sin(a1) * r;
                    double x2 = Math.cos(a2) * r, z2 = Math.sin(a2) * r;

                    for (double t = 0; t <= 1.0; t += 0.1) {
                        Location p = center.clone().add(
                                x1 + (x2 - x1) * t, 0, z1 + (z2 - z1) * t);
                        if (p.getWorld() != center.getWorld()) continue;
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(color, 1.2f)
                        );
                    }
                }
            }

            private void detonate() {
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.3f);

                // Mảnh ánh sáng góc cạnh bắn ra theo các tam giác nhỏ
                int shards = 24;
                for (int i = 0; i < shards; i++) {
                    double angle = (2 * Math.PI * i) / shards;
                    for (double d = 0.3; d <= blastRadius; d += 0.4) {
                        double x = Math.cos(angle) * d;
                        double z = Math.sin(angle) * d;
                        double y = Math.sin(d * 1.5) * 0.3;
                        Location p = center.clone().add(x, y, z);
                        if (p.getWorld() != center.getWorld()) continue;
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(runeColor, 1.3f)
                        );
                    }
                }
                center.getWorld().spawnParticle(Particle.END_ROD, center, 40, 0.4, 0.4, 0.4, 0.3);

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
