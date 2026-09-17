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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * VOLCANIC_ERUPTION
 * Mặt đất tại mục tiêu phun trào một cột dung nham khổng lồ, sau đó các mảnh đá nóng bắn
 * lên trời và rơi xuống thành một cơn mưa thiên thạch lửa quanh điểm phun. Sát thương 1 lần
 * cho đòn phun trào chính, cộng thêm sát thương nhỏ 1 lần cho mỗi mảnh đá rơi trúng.
 */
public class VOLCANIC_ERUPTION implements IAbility {

    private static final int DEBRIS_COUNT = 10;

    @Override
    public String getName() {
        return "VOLCANIC_ERUPTION";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage cột chính: 32% + 11%/level. Mảnh đá rơi: sát thương phụ nhỏ, không cộng dồn vô hạn
        final double mainPercent = 32.0 + (skillLevel - 1) * 11.0;
        final double mainDamage = baseDamage * (mainPercent / 100.0);
        final double debrisDamage = baseDamage * 0.03; // Sát thương nhỏ cố định mỗi mảnh, không scale theo level

        final double eruptionRadius = 3.5 + (skillLevel * 0.15);
        final double eruptionRadiusSquared = eruptionRadius * eruptionRadius;
        final double debrisFieldRadius = 6.0;

        final Location center = target.getLocation().clone();
        final Color lavaColor = Color.fromRGB(255, 90, 20);
        final Color emberColor = Color.fromRGB(255, 180, 40);

        center.getWorld().playSound(center, Sound.BLOCK_LAVA_POP, 0.8f, 0.4f);

        new BukkitRunnable() {

            int ticks = 0;
            final int chargeTicks = 12;   // Rung chuyển trước khi phun
            final int eruptAt = chargeTicks;
            final int debrisTicks = 20;   // Mưa thiên thạch sau khi phun
            final int totalTicks = chargeTicks + debrisTicks;

            final Set<UUID> hitByColumn = new HashSet<>();

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                if (ticks < chargeTicks) {
                    // Rung đất trước khi phun
                    double shake = (double) ticks / chargeTicks * 0.3;
                    for (int i = 0; i < 10; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = Math.random() * eruptionRadius * 0.6;
                        Location p = center.clone().add(Math.cos(angle) * dist, shake, Math.sin(angle) * dist);
                        if (p.getWorld() != center.getWorld()) continue;
                        p.getWorld().spawnParticle(Particle.SMOKE, p, 2, 0.05, 0.05, 0.05, 0.01);
                    }

                } else if (ticks == eruptAt) {
                    erupt();
                } else {
                    // Mưa thiên thạch: mảnh lửa rơi ngẫu nhiên trong debrisFieldRadius
                    int debrisTick = ticks - eruptAt;
                    if (debrisTick % 2 == 0 && debrisTick / 2 < DEBRIS_COUNT) {
                        spawnDebris();
                    }
                }

                ticks++;
            }

            private void erupt() {
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.5f);
                center.getWorld().spawnParticle(Particle.FLASH, center.clone().add(0, 0.3, 0), 2, 0, 0, 0, 0);

                double columnHeight = 6.0;
                for (double y = 0; y <= columnHeight; y += 0.3) {
                    double r = 0.5 * (1.0 - y / columnHeight * 0.4);
                    int points = 12;
                    for (int i = 0; i < points; i++) {
                        double a = (2 * Math.PI * i) / points;
                        Location p = center.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                        if (p.getWorld() != center.getWorld()) continue;
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(y > 4 ? emberColor : lavaColor, 1.4f)
                        );
                    }
                }
                center.getWorld().spawnParticle(Particle.LAVA, center, 30, 0.6, 0.3, 0.6, 0);
                center.getWorld().spawnParticle(Particle.FLAME, center, 40, 0.6, 1.0, 0.6, 0.03);

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (entity.getLocation().distanceSquared(center) > eruptionRadiusSquared) continue;

                    hitByColumn.add(entity.getUniqueId());

                    entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    entity.damage(mainDamage, attacker);
                    final LivingEntity ref = entity;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (ref.isValid()) ref.removeMetadata("IS_ABILITY", Main.getInstance());
                        }
                    }.runTaskLater(Main.getInstance(), 1L);

                    Vector v = entity.getVelocity();
                    entity.setVelocity(new Vector(v.getX(), 0.6, v.getZ()));
                }
            }

            private void spawnDebris() {
                double angle = Math.random() * 2 * Math.PI;
                double dist = Math.random() * debrisFieldRadius;
                Location impact = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                if (impact.getWorld() == null) return;

                // Rơi nhanh từ trên cao xuống điểm impact trong khung thời gian ngắn (mô phỏng không đồng bộ,
                // vẽ vệt rơi tức thời rồi áp sát thương ngay, tránh phải mở thêm task lồng nhau)
                for (double y = 5.0; y >= 0; y -= 0.6) {
                    Location p = impact.clone().add(0, y, 0);
                    p.getWorld().spawnParticle(Particle.FLAME, p, 2, 0.05, 0.05, 0.05, 0.01);
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(emberColor, 1.1f)
                    );
                }

                impact.getWorld().playSound(impact, Sound.BLOCK_LAVA_EXTINGUISH, 0.6f, 1.3f);
                impact.getWorld().spawnParticle(Particle.LAVA, impact, 4, 0.2, 0.1, 0.2, 0);

                for (LivingEntity entity : impact.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (entity.getLocation().distanceSquared(impact) > 1.3 * 1.3) continue;

                    entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    entity.damage(debrisDamage, attacker);
                    final LivingEntity ref = entity;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (ref.isValid()) ref.removeMetadata("IS_ABILITY", Main.getInstance());
                        }
                    }.runTaskLater(Main.getInstance(), 1L);

                    entity.setFireTicks(Math.max(entity.getFireTicks(), 15));
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}