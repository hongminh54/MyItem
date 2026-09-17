package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RADIANT_WAVE implements IAbility {

    @Override
    public String getName() {
        return "RADIANT_WAVE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead()) return;

        final int skillLevel = Math.max(1, level);

        // Level 1 = 15%, mỗi level +5%
        final double damagePercent = 15.0 + ((skillLevel - 1) * 5.0);
        final double damage = baseDamage * (damagePercent / 100.0);

        // Vị trí cố định lúc cast (clone world để tránh lỗi null)
        if (target.getWorld() == null) return;
        final Location center = target.getLocation().clone();

        // Bán kính tối đa 8 block
        final double maxRadius = 8.0;

        // Entity đã bị wave đánh
        Set<UUID> hitEntities = new HashSet<>();

        new BukkitRunnable() {

            double radius = 0.0;
            int safetyCounter = 0; // Chống lỗi vòng lặp vô hạn hoặc kẹt task

            @Override
            public void run() {
                // Điều kiện hủy an toàn: Quá thời gian tối đa, thế giới null, hoặc vượt quá bán kính
                if (center.getWorld() == null || radius > maxRadius || safetyCounter++ > 30) {
                    this.cancel();
                    return;
                }

                // Tăng bán kính wave mỗi lần chạy
                // (bước tăng gấp đôi vì interval tick cũng tăng gấp đôi bên dưới,
                //  giữ nguyên tổng thời gian/tốc độ lan của wave, tránh 2 vòng
                //  liên tiếp chồng lên nhau trước khi vòng cũ kịp fade)
                radius += 1.0;
                if (radius > maxRadius) {
                    radius = maxRadius;
                }

                // ==========================================
                // WAVE PARTICLE
                // ==========================================

                // Giới hạn số điểm tối đa (Math.min) để tránh tràn hạt hoặc lag khi có lỗi
                int points = Math.min(200, Math.max(40, (int) (radius * 20)));

                for (int i = 0; i < points; i++) {

                    double angle = (2 * Math.PI * i) / points;

                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    Location particleLocation = center.clone().add(
                            x,
                            0.6,
                            z
                    );

                    if (particleLocation.getWorld() != center.getWorld()) continue;

                    // Màu cam-đỏ, khác với vòng vàng cố định của SOLAR_SEAL,
                    // để tránh 2 hiệu ứng bị nhầm thành 1 vòng lỗi khi dùng chung.
                    center.getWorld().spawnParticle(
                            Particle.DUST,
                            particleLocation,
                            1,
                            new Particle.DustOptions(
                                    Color.fromRGB(255, 90, 0),
                                    1.5f
                            )
                    );
                }

                // ==========================================
                // ENTITY BỊ WAVE CHẠM
                // ==========================================

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {

                    if (entity.isDead() || !entity.isValid()) continue;

                    // Không đánh người cast
                    if (entity.equals(attacker)) continue;

                    // Mỗi entity chỉ bị damage 1 lần trong đợt wave này
                    if (hitEntities.contains(entity.getUniqueId())) continue;

                    Location entityLocation = entity.getLocation();

                    if (Math.abs(entityLocation.getY() - center.getY()) > 2.0) {
                        continue;
                    }

                    double dx = entityLocation.getX() - center.getX();
                    double dz = entityLocation.getZ() - center.getZ();

                    double distance = Math.sqrt(
                            (dx * dx) + (dz * dz)
                    );

                    // Wave vừa chạm entity
                    if (distance <= radius) {

                        hitEntities.add(entity.getUniqueId());

                        // Đánh dấu damage đến từ ability để tránh vòng lặp vô hạn
                        entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                        // Gây sát thương
                        entity.damage(damage, attacker);

                        // Gỡ cờ "IS_ABILITY" sau 1 tick
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (entity.isValid()) {
                                    entity.removeMetadata("IS_ABILITY", Main.getInstance());
                                }
                            }
                        }.runTaskLater(Main.getInstance(), 1L);

                        // Particle hiệu ứng trúng đích
                        playHitEffect(entity);
                    }
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 2L);
    }

    /**
     * Vòng tròn bán kính 1 block.
     * Bắt đầu ở chân và nâng dần lên đầu.
     */
    private void playHitEffect(LivingEntity entity) {

        new BukkitRunnable() {

            double y = 0.1;

            @Override
            public void run() {

                if (entity.isDead() || !entity.isValid()) {
                    this.cancel();
                    return;
                }

                Location base = entity.getLocation();

                // ==========================================
                // VÒNG TRÒN 1 BLOCK
                // ==========================================

                double radius = 1.0;
                int points = 32;

                for (int i = 0; i < points; i++) {

                    double angle = (2 * Math.PI * i) / points;

                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    Location particleLocation = base.clone().add(
                            x,
                            y,
                            z
                    );

                    if (particleLocation.getWorld() != base.getWorld()) continue;

                    base.getWorld().spawnParticle(
                            Particle.DUST,
                            particleLocation,
                            1,
                            new Particle.DustOptions(
                                    Color.fromRGB(255, 90, 0),
                                    1.4f
                            )
                    );
                }

                // Nâng vòng lên
                y += 0.2;

                // Đến đầu thì kết thúc
                if (y >= entity.getHeight()) {
                    this.cancel();
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}