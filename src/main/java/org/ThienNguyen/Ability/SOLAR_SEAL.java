package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class SOLAR_SEAL implements IAbility {

    @Override
    public String getName() {
        return "SOLAR_SEAL";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead()) return;

        final int slowLevel = Math.max(1, level);

        // Chốt vị trí ngay lúc cast
        if (target.getWorld() == null) return;
        final Location center = target.getLocation().clone();

        // Mỗi 3 level tăng 1 bán kính
        final double radius = 2.0 + ((Math.max(1, level) - 1) / 3);

        new BukkitRunnable() {

            int ticks = 0;

            @Override
            public void run() {

                // 5 giây = 100 ticks (task chạy mỗi 2 tick nghĩa là lặp 50 lần)
                if (ticks >= 50 || center.getWorld() == null) {
                    this.cancel();
                    return;
                }

                // ==========================================
                // VISUAL - VÒNG TRÒN CỐ ĐỊNH
                // ==========================================

                int points = Math.max(48, (int) (radius * 24));

                for (int i = 0; i < points; i++) {

                    double angle = (2 * Math.PI * i) / points;

                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    Location particleLoc = center.clone().add(
                            x,
                            0.05,
                            z
                    );

                    if (particleLoc.getWorld() != center.getWorld()) continue;

                    center.getWorld().spawnParticle(
                            Particle.DUST,
                            particleLoc,
                            1,
                            new Particle.DustOptions(
                                    Color.fromRGB(255, 215, 0),
                                    1.5f
                            )
                    );
                }

                // ==========================================
                // CHECK ENTITY TRONG VÒNG
                // ==========================================

                double radiusSquared = radius * radius;

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {

                    if (entity.isDead() || !entity.isValid()) continue;

                    // Không tác dụng lên người cast skill
                    if (entity.equals(attacker)) continue;

                    Location entityLocation = entity.getLocation();

                    if (Math.abs(entityLocation.getY() - center.getY()) > 2.0) {
                        continue;
                    }

                    double dx = entityLocation.getX() - center.getX();
                    double dz = entityLocation.getZ() - center.getZ();

                    double distanceSquared = (dx * dx) + (dz * dz);

                    if (distanceSquared <= radiusSquared) {

                        // Đánh dấu entity đang chịu tác động từ ability
                        entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                        // Gán hiệu ứng Slowness
                        entity.addPotionEffect(
                                new PotionEffect(
                                        PotionEffectType.SLOWNESS,
                                        15,
                                        slowLevel - 1,
                                        false,
                                        false,
                                        true
                                )
                        );

                        // Gỡ cờ "IS_ABILITY" sau 3 tick để dọn dẹp sạch sẽ metadata
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (entity.isValid() && entity.hasMetadata("IS_ABILITY")) {
                                    entity.removeMetadata("IS_ABILITY", Main.getInstance());
                                }
                            }
                        }.runTaskLater(Main.getInstance(), 3L);
                    }
                }

                ticks++;
            }

        }.runTaskTimer(Main.getInstance(), 0L, 2L);
    }
}