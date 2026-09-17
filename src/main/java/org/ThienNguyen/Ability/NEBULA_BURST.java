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
 * NEBULA_BURST
 * Một tinh vân xoáy 3 nhánh hình thành quanh mục tiêu, cuộn vào tâm rồi nổ tung thành
 * quả cầu sao. Sát thương nổ 1 lần.
 */
public class NEBULA_BURST implements IAbility {

    private static final int ARMS = 3;
    private static final Color[] NEBULA_COLORS = {
            Color.fromRGB(255, 80, 200),
            Color.fromRGB(120, 90, 255),
            Color.fromRGB(80, 200, 255)
    };

    @Override
    public String getName() {
        return "NEBULA_BURST";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 25% + 8%/level
        final double damagePercent = 25.0 + (skillLevel - 1) * 8.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double blastRadius = 4.5 + (skillLevel * 0.2);
        final double blastRadiusSquared = blastRadius * blastRadius;

        final Location center = target.getLocation().clone().add(0, 1.2, 0);

        center.getWorld().playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.6f);

        new BukkitRunnable() {

            int ticks = 0;
            final int swirlTicks = 26;
            final int totalTicks = 34;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                if (ticks < swirlTicks) {

                    double progress = (double) ticks / swirlTicks;
                    // Tinh vân cuộn vào tâm, xoay nhanh dần
                    double maxArm = blastRadius * (1.0 - progress * 0.85);
                    double spin = progress * progress * 10.0;

                    for (int arm = 0; arm < ARMS; arm++) {
                        double armOffset = (2 * Math.PI * arm) / ARMS;

                        for (double d = 0.3; d <= maxArm; d += 0.3) {
                            // Nhánh xoắn: góc tăng theo khoảng cách
                            double angle = armOffset + spin + (d * 0.9);
                            double x = Math.cos(angle) * d;
                            double z = Math.sin(angle) * d;
                            // Nhánh hơi uốn theo trục Y tạo cảm giác đĩa nghiêng
                            double y = Math.sin(d * 0.7 + spin) * 0.35;

                            Location p = center.clone().add(x, y, z);
                            if (p.getWorld() != center.getWorld()) continue;

                            p.getWorld().spawnParticle(
                                    Particle.DUST, p, 1,
                                    new Particle.DustOptions(NEBULA_COLORS[arm % NEBULA_COLORS.length], 1.3f)
                            );
                        }
                    }

                    // Lõi sáng ở tâm, to dần trước khi nổ
                    center.getWorld().spawnParticle(Particle.END_ROD, center,
                            2 + (int) (progress * 6), 0.15, 0.15, 0.15, 0.01);

                } else if (ticks == swirlTicks) {
                    detonate();
                } else {
                    // Tàn dư tinh vân trôi ra
                    center.getWorld().spawnParticle(
                            Particle.DUST, center, 12, blastRadius * 0.6, 0.8, blastRadius * 0.6, 0,
                            new Particle.DustOptions(NEBULA_COLORS[ticks % NEBULA_COLORS.length], 1.1f)
                    );
                }

                ticks++;
            }

            private void detonate() {
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.5f);
                center.getWorld().playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 0.8f);

                // Quả cầu sao: phân bố đều trên mặt cầu bằng phương pháp Fibonacci
                int points = 220;
                double golden = Math.PI * (3 - Math.sqrt(5));
                for (int i = 0; i < points; i++) {
                    double y = 1 - (i / (double) (points - 1)) * 2;
                    double r = Math.sqrt(1 - y * y);
                    double theta = golden * i;

                    double x = Math.cos(theta) * r;
                    double z = Math.sin(theta) * r;

                    Location p = center.clone().add(
                            x * blastRadius, y * blastRadius * 0.8, z * blastRadius);
                    if (p.getWorld() != center.getWorld()) continue;

                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(NEBULA_COLORS[i % NEBULA_COLORS.length], 1.4f)
                    );
                }

                center.getWorld().spawnParticle(Particle.END_ROD, center, 80, 0.6, 0.6, 0.6, 0.55);
                center.getWorld().spawnParticle(Particle.FLASH, center, 2, 0, 0, 0, 0);

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
