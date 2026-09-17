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
 * AURORA_VEIL
 * Một bức màn cực quang gợn sóng dựng lên trước mặt attacker rồi quét thẳng về phía trước,
 * đổi màu liên tục theo thời gian. Entity bị màn quét qua nhận sát thương 1 lần.
 */
public class AURORA_VEIL implements IAbility {

    @Override
    public String getName() {
        return "AURORA_VEIL";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (attacker == null || attacker.isDead() || attacker.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 22% + 8%/level
        final double damagePercent = 22.0 + (skillLevel - 1) * 8.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double veilWidth = 5.0 + (skillLevel * 0.15);
        final double veilHeight = 3.5;
        final double sweepDistance = 9.0;
        final double hitThickness = 1.0;

        final Location origin = attacker.getLocation().clone();
        // Hướng quét = hướng attacker đang nhìn (bỏ thành phần Y để màn đi ngang)
        final Vector forward = origin.getDirection().setY(0).normalize();
        if (forward.lengthSquared() < 0.0001) return;
        // Vector ngang vuông góc với hướng quét, dùng để trải bề rộng màn
        final Vector side = new Vector(-forward.getZ(), 0, forward.getX()).normalize();

        final Set<UUID> hitEntities = new HashSet<>();

        origin.getWorld().playSound(origin, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.8f);

        new BukkitRunnable() {

            int ticks = 0;
            final int totalTicks = 24;

            @Override
            public void run() {

                if (origin.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                double progress = (double) ticks / totalTicks;
                double travelled = sweepDistance * progress;

                // Tâm màn tại khoảng cách hiện tại
                Location veilCenter = origin.clone().add(forward.clone().multiply(travelled));

                // Vẽ màn: trải theo bề ngang, mỗi cột gợn sóng theo trục Y
                for (double w = -veilWidth / 2; w <= veilWidth / 2; w += 0.35) {

                    // Sóng dọc: biên độ phụ thuộc vị trí ngang và thời gian
                    double wave = Math.sin((w * 1.2) + (ticks * 0.45)) * 0.45;

                    for (double h = 0; h <= veilHeight; h += 0.3) {

                        // Màu cực quang chuyển dần theo chiều cao + thời gian
                        double hueShift = (h / veilHeight) + (ticks * 0.04) + (w * 0.05);
                        Color color = auroraColor(hueShift);

                        Location p = veilCenter.clone()
                                .add(side.clone().multiply(w))
                                .add(0, h + wave, 0);

                        if (p.getWorld() != origin.getWorld()) continue;

                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(color, 1.3f)
                        );
                    }
                }

                // Entity bị màn quét qua
                for (LivingEntity entity : origin.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (hitEntities.contains(entity.getUniqueId())) continue;

                    Location eLoc = entity.getLocation();
                    if (Math.abs(eLoc.getY() - origin.getY()) > 3.0) continue;

                    Vector toEntity = eLoc.toVector().subtract(origin.toVector());
                    toEntity.setY(0);

                    double along = toEntity.dot(forward);
                    double lateral = Math.abs(toEntity.dot(side));

                    // Nằm trong bề rộng màn và đúng lát cắt màn đang đi qua
                    if (lateral <= veilWidth / 2
                            && Math.abs(along - travelled) <= hitThickness) {

                        hitEntities.add(entity.getUniqueId());

                        entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                        entity.damage(damage, attacker);
                        final LivingEntity ref = entity;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (ref.isValid()) ref.removeMetadata("IS_ABILITY", Main.getInstance());
                            }
                        }.runTaskLater(Main.getInstance(), 1L);

                        entity.getWorld().spawnParticle(
                                Particle.END_ROD, eLoc.clone().add(0, 1, 0), 18, 0.3, 0.6, 0.3, 0.1);
                    }
                }

                ticks++;
            }

            /** Bảng màu cực quang: xanh lá -> xanh ngọc -> xanh dương -> tím -> hồng. */
            private Color auroraColor(double t) {
                double phase = (t % 1.0 + 1.0) % 1.0; // Chuẩn hóa về [0,1)
                int r, g, b;
                if (phase < 0.25) {
                    double k = phase / 0.25;
                    r = (int) (60 + k * 0);   g = (int) (255 - k * 55); b = (int) (120 + k * 135);
                } else if (phase < 0.5) {
                    double k = (phase - 0.25) / 0.25;
                    r = (int) (60 - k * 20);  g = (int) (200 - k * 120); b = (int) (255);
                } else if (phase < 0.75) {
                    double k = (phase - 0.5) / 0.25;
                    r = (int) (40 + k * 140); g = (int) (80 - k * 30);   b = (int) (255 - k * 20);
                } else {
                    double k = (phase - 0.75) / 0.25;
                    r = (int) (180 + k * 70); g = (int) (50 + k * 70);   b = (int) (235 - k * 75);
                }
                return Color.fromRGB(clamp(r), clamp(g), clamp(b));
            }

            private int clamp(int v) {
                return Math.max(0, Math.min(255, v));
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
