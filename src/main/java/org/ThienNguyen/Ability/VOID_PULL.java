package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * VOID_PULL
 * Tạo hố hút, kéo entity trong bán kính về tâm trong 1.5 giây rồi gây sát thương nhỏ khi kéo xong.
 */
public class VOID_PULL implements IAbility {

    private static final double MAX_PULL_SPEED = 0.35; // Chặn tốc độ kéo, tránh văng entity quá xa/lỗi vật lý

    @Override
    public String getName() {
        return "VOID_PULL";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 15% + 6%/level (dù mục đích chính là kiểm soát đội hình)
        final double damagePercent = 15.0 + (skillLevel - 1) * 6.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double radius = 5.0 + (skillLevel * 0.3);
        final double radiusSquared = radius * radius;
        final double coreRadius = 1.5;

        final Location center = target.getLocation().clone();
        final Set<UUID> pulledEntities = new HashSet<>();

        new BukkitRunnable() {

            int ticks = 0;
            final int durationTicks = 30; // 1.5 giây

            @Override
            public void run() {

                // An toàn tuyệt đối: quá thời lượng hoặc world null thì dừng ngay
                if (center.getWorld() == null || ticks >= durationTicks) {
                    finish();
                    this.cancel();
                    return;
                }

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;

                    double distSquared = entity.getLocation().distanceSquared(center);
                    if (distSquared > radiusSquared) continue;

                    pulledEntities.add(entity.getUniqueId());

                    Vector toCenter = center.toVector().subtract(entity.getLocation().toVector());
                    double distance = toCenter.length();
                    if (distance < 0.1) continue;

                    Vector pull = toCenter.normalize().multiply(Math.min(MAX_PULL_SPEED, distance / 6.0));
                    // Giữ nguyên phần Y hiện tại một chút để tránh kéo entity xuyên sàn
                    Vector velocity = entity.getVelocity();
                    entity.setVelocity(new Vector(pull.getX(), velocity.getY() * 0.5, pull.getZ()));
                }

                // Vòng xoáy hạt hút vào tâm
                double swirlRadius = Math.max(0.5, radius * (1.0 - (double) ticks / durationTicks));
                int points = 24;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points + (ticks * 0.3);
                    double x = Math.cos(angle) * swirlRadius;
                    double z = Math.sin(angle) * swirlRadius;
                    Location p = center.clone().add(x, 0.2, z);
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(
                            Particle.DUST,
                            p,
                            1,
                            new Particle.DustOptions(Color.fromRGB(140, 0, 200), 1.3f)
                    );
                }

                ticks++;
            }

            private void finish() {
                double coreRadiusSquared = coreRadius * coreRadius;

                for (UUID id : pulledEntities) {
                    if (center.getWorld() == null) continue;
                    LivingEntity entity = (LivingEntity) center.getWorld().getEntities().stream()
                            .filter(e -> e.getUniqueId().equals(id))
                            .findFirst().orElse(null);

                    if (entity == null || entity.isDead() || !entity.isValid()) continue;
                    if (entity.getLocation().distanceSquared(center) > coreRadiusSquared) continue;

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
