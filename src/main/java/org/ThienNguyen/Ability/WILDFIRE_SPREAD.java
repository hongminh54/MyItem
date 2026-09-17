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
 * WILDFIRE_SPREAD
 * Một đám cháy nhỏ bùng lên tại mục tiêu rồi lan rộng dần ra xung quanh trong 4 giây, liên
 * tục gây sát thương cho bất kỳ ai đứng trong vùng lửa đang lan. (Loại sát thương liên tục.)
 */
public class WILDFIRE_SPREAD implements IAbility {

    @Override
    public String getName() {
        return "WILDFIRE_SPREAD";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Tổng sát thương liên tục: 25% + 8%/level, chia đều 8 lần tick (5 tick/lần = 2s... xem bên dưới)
        final double totalPercent = 25.0 + (skillLevel - 1) * 8.0;
        final double tickDamage = baseDamage * (totalPercent / 8.0 / 100.0);

        final double maxRadius = 4.5 + (skillLevel * 0.2);

        final Location center = target.getLocation().clone();
        final Color fireOrange = Color.fromRGB(255, 110, 20);
        final Color emberRed = Color.fromRGB(200, 40, 10);

        center.getWorld().playSound(center, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.6f);

        new BukkitRunnable() {

            int ticks = 0;
            final int totalTicks = 80; // 4 giây, lan rộng dần suốt thời gian này
            final int damageInterval = 10; // Gây sát thương mỗi 10 tick -> 8 lần trong 80 tick

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                double progress = (double) ticks / totalTicks;
                // Lan rộng nhanh lúc đầu, chậm dần về sau (giống cháy rừng thật)
                double currentRadius = maxRadius * Math.sqrt(progress);
                double currentRadiusSquared = currentRadius * currentRadius;

                // Vẽ vùng lửa: hạt rải ngẫu nhiên trong bán kính hiện tại, dày hơn ở biên đang lan
                int innerPoints = (int) (currentRadius * 6);
                for (int i = 0; i < innerPoints; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = currentRadius * Math.sqrt(Math.random());
                    Location p = center.clone().add(Math.cos(angle) * dist, 0.1, Math.sin(angle) * dist);
                    if (p.getWorld() != center.getWorld()) continue;
                    if (Math.random() < 0.4) {
                        p.getWorld().spawnParticle(Particle.FLAME, p, 1, 0.05, 0.05, 0.05, 0.005);
                    } else {
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(Math.random() < 0.5 ? fireOrange : emberRed, 1.1f)
                        );
                    }
                }

                // Viền lan đang cháy rõ hơn
                int edgePoints = Math.max(16, (int) (currentRadius * 10));
                for (int i = 0; i < edgePoints; i++) {
                    double a = (2 * Math.PI * i) / edgePoints;
                    Location p = center.clone().add(Math.cos(a) * currentRadius, 0.1, Math.sin(a) * currentRadius);
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(Particle.FLAME, p, 1, 0.03, 0.03, 0.03, 0.005);
                }

                if (ticks % damageInterval == 0) {

                    for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                        if (entity.isDead() || !entity.isValid()) continue;
                        if (entity.equals(attacker)) continue;
                        if (entity.getLocation().distanceSquared(center) > currentRadiusSquared) continue;

                        entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                        entity.damage(tickDamage, attacker);
                        final LivingEntity ref = entity;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (ref.isValid()) ref.removeMetadata("IS_ABILITY", Main.getInstance());
                            }
                        }.runTaskLater(Main.getInstance(), 1L);

                        entity.setFireTicks(Math.max(entity.getFireTicks(), 20));
                        entity.getWorld().spawnParticle(Particle.FLAME,
                                entity.getLocation().add(0, 1, 0), 10, 0.2, 0.35, 0.2, 0.01);
                    }
                }

                ticks++;
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}