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
 * CHRONO_FRACTURE
 * Một bong bóng thời gian bao lấy mục tiêu, các mảnh đồng hồ vỡ vụn xoay chậm rồi nhanh dần,
 * khi bong bóng vỡ tung thời gian "bật lại" tạo ra cú đánh dồn dập tức thời. Sát thương 1 lần.
 */
public class CHRONO_FRACTURE implements IAbility {

    @Override
    public String getName() {
        return "CHRONO_FRACTURE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 24% + 8%/level
        final double damagePercent = 24.0 + (skillLevel - 1) * 8.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double bubbleRadius = 1.6;
        final double aoeRadius = 3.0 + (skillLevel * 0.15);
        final double aoeRadiusSquared = aoeRadius * aoeRadius;

        final Location center = target.getLocation().clone().add(0, 1.0, 0);
        final Color clockColor = Color.fromRGB(220, 200, 130);
        final Color glassColor = Color.fromRGB(180, 220, 255);

        center.getWorld().playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 1.0f, 1.6f);

        // Đóng băng nhẹ mục tiêu trong lúc bong bóng hình thành (hiệu ứng thời gian ngưng đọng)
        target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SLOWNESS, 24, 7, false, false, true));

        new BukkitRunnable() {

            int ticks = 0;
            final int freezeTicks = 20;
            final int totalTicks = 24;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                if (ticks < freezeTicks) {

                    double progress = (double) ticks / freezeTicks;
                    // Kim đồng hồ quay chậm rồi nhanh dần (gia tốc bậc 3)
                    double spin = Math.pow(progress, 3) * 20.0;

                    // Vỏ bong bóng thủy tinh
                    int shellPoints = 40;
                    for (int i = 0; i < shellPoints; i++) {
                        double a = (2 * Math.PI * i) / shellPoints;
                        double b = Math.PI * ((i % 5) / 5.0 - 0.5);
                        Location p = center.clone().add(
                                Math.cos(a) * Math.cos(b) * bubbleRadius,
                                Math.sin(b) * bubbleRadius,
                                Math.sin(a) * Math.cos(b) * bubbleRadius);
                        if (p.getWorld() != center.getWorld()) continue;
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(glassColor, 1.0f)
                        );
                    }

                    // 4 kim đồng hồ vỡ xoay bên trong bong bóng
                    for (int hand = 0; hand < 4; hand++) {
                        double angle = (Math.PI / 2 * hand) + spin;
                        double len = bubbleRadius * 0.85;
                        for (double d = 0.1; d <= len; d += 0.15) {
                            Location p = center.clone().add(Math.cos(angle) * d, 0, Math.sin(angle) * d);
                            if (p.getWorld() != center.getWorld()) continue;
                            p.getWorld().spawnParticle(
                                    Particle.DUST, p, 1,
                                    new Particle.DustOptions(clockColor, 1.2f)
                            );
                        }
                    }

                } else if (ticks == freezeTicks) {
                    snap();
                } else {
                    center.getWorld().spawnParticle(
                            Particle.DUST, center, 8, aoeRadius * 0.3, 0.5, aoeRadius * 0.3, 0,
                            new Particle.DustOptions(clockColor, 1.0f)
                    );
                }

                ticks++;
            }

            private void snap() {
                center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1.4f, 1.3f);
                center.getWorld().playSound(center, Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);

                center.getWorld().spawnParticle(Particle.FLASH, center, 2, 0, 0, 0, 0);

                // Sóng "tua ngược" lan ra: nhiều vòng đồng tâm xuất hiện đồng loạt
                for (int ring = 0; ring < 4; ring++) {
                    double r = aoeRadius * (0.3 + ring * 0.23);
                    int points = Math.max(20, (int) (r * 12));
                    for (int i = 0; i < points; i++) {
                        double a = (2 * Math.PI * i) / points;
                        Location p = center.clone().add(Math.cos(a) * r, -0.5, Math.sin(a) * r);
                        if (p.getWorld() != center.getWorld()) continue;
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(ring % 2 == 0 ? clockColor : glassColor, 1.3f)
                        );
                    }
                }

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (entity.getLocation().distanceSquared(center) > aoeRadiusSquared) continue;

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
