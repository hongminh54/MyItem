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
 * THUNDER_CASCADE
 * 4 tia sét thị giác giáng xuống liên tiếp tại các điểm lệch nhẹ quanh mục tiêu,
 * mỗi tia đánh trúng gây 1 lần sát thương trong bán kính nhỏ tại điểm rơi.
 */
public class THUNDER_CASCADE implements IAbility {

    private static final int HITS = 4;
    private static final double BASE_PERCENT = 22.0;
    private static final double PERCENT_PER_LEVEL = 16.0;

    @Override
    public String getName() {
        return "THUNDER_CASCADE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);
        final double damagePercent = BASE_PERCENT + (skillLevel - 1) * PERCENT_PER_LEVEL;
        final double hitDamage = baseDamage * (damagePercent / 100.0);

        final double radius = 1.7;
        final double radiusSquared = radius * radius;
        final double strikeHeight = 6.0;

        final Location baseCenter = target.getLocation().clone();
        final Color boltColor = Color.fromRGB(255, 255, 210);
        final Color glowColor = Color.fromRGB(180, 220, 255);

        // 4 điểm rơi lệch nhẹ quanh mục tiêu, mỗi điểm đánh vào 1 mốc thời gian khác nhau
        final Location[] strikePoints = new Location[4];
        for (int i = 0; i < 4; i++) {
            double a = (2 * Math.PI * i) / 4;
            strikePoints[i] = baseCenter.clone().add(Math.cos(a) * 0.9, 0, Math.sin(a) * 0.9);
        }
        final int[] strikeTicks = {3, 8, 13, 18};

        baseCenter.getWorld().playSound(baseCenter, Sound.ENTITY_CREEPER_PRIMED, 0.6f, 0.5f);

        new BukkitRunnable() {

            int ticks = 0;
            int strikeIndex = 0;
            final int totalTicks = 20;

            @Override
            public void run() {

                if (baseCenter.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                if (strikeIndex < strikeTicks.length && ticks == strikeTicks[strikeIndex] - 2) {
                    // Cảnh báo: quầng sáng xuất hiện trước khi sét đánh
                    Location warn = strikePoints[strikeIndex];
                    for (int i = 0; i < 16; i++) {
                        double a = (2 * Math.PI * i) / 16;
                        Location p = warn.clone().add(Math.cos(a) * 0.6, 0.05, Math.sin(a) * 0.6);
                        if (p.getWorld() != baseCenter.getWorld()) continue;
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(glowColor, 1.0f)
                        );
                    }
                }

                if (strikeIndex < strikeTicks.length && ticks == strikeTicks[strikeIndex]) {
                    strike(strikePoints[strikeIndex]);
                    strikeIndex++;
                }

                ticks++;
            }

            private void strike(Location point) {
                point.getWorld().playSound(point, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.4f);
                for (double y = 0; y <= strikeHeight; y += 0.3) {
                    Location p = point.clone().add(0, y, 0);
                    if (p.getWorld() != baseCenter.getWorld()) continue;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(boltColor, 1.3f)
                    );
                    p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, p, 2, 0.05, 0.05, 0.05, 0.02);
                }
                point.getWorld().spawnParticle(Particle.FLASH, point.clone().add(0, 0.2, 0), 1, 0, 0, 0, 0);
                point.getWorld().spawnParticle(Particle.CLOUD, point, 12, 0.4, 0.1, 0.4, 0.02);

                for (LivingEntity entity : baseCenter.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;

                    Location eLoc = entity.getLocation();
                    if (Math.abs(eLoc.getY() - point.getY()) > 2.5) continue;

                    double dx = eLoc.getX() - point.getX();
                    double dz = eLoc.getZ() - point.getZ();
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
