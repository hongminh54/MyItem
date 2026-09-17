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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * MAGMA_FISSURE
 * Nứt đất: 5 vết nứt zigzag chạy lan ra từ mục tiêu, phát sáng đỏ cam dần rồi phun trào
 * dung nham dọc theo vết nứt. Sát thương 1 lần khi vết nứt chạm entity.
 */
public class MAGMA_FISSURE implements IAbility {

    private static final int FISSURE_COUNT = 5;

    @Override
    public String getName() {
        return "MAGMA_FISSURE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 28% + 11%/level
        final double damagePercent = 28.0 + (skillLevel - 1) * 11.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double fissureLength = 8.0 + (skillLevel * 0.25);
        final double hitWidth = 1.3;

        final Location center = target.getLocation().clone();
        final Set<UUID> hitEntities = new HashSet<>();

        // Hướng gốc của mỗi vết nứt, lệch ngẫu nhiên một chút cho tự nhiên
        final double[] baseAngles = new double[FISSURE_COUNT];
        for (int i = 0; i < FISSURE_COUNT; i++) {
            baseAngles[i] = (2 * Math.PI * i) / FISSURE_COUNT + (Math.random() - 0.5) * 0.4;
        }

        center.getWorld().playSound(center, Sound.BLOCK_STONE_BREAK, 1.2f, 0.5f);

        new BukkitRunnable() {

            int ticks = 0;
            final int growTicks = 12;   // Nứt lan ra
            final int eruptTicks = 10;  // Phun trào
            final int totalTicks = growTicks + eruptTicks;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                boolean erupting = ticks >= growTicks;
                double grownLength = erupting
                        ? fissureLength
                        : fissureLength * ((double) (ticks + 1) / growTicks);

                for (int f = 0; f < FISSURE_COUNT; f++) {

                    double angle = baseAngles[f];

                    // Vết nứt zigzag: lệch trái phải theo khoảng cách
                    for (double d = 0.5; d <= grownLength; d += 0.35) {

                        double zig = Math.sin(d * 2.2 + f) * 0.35;
                        double dirX = Math.cos(angle);
                        double dirZ = Math.sin(angle);
                        // Lệch vuông góc để tạo zigzag
                        double px = center.getX() + dirX * d - dirZ * zig;
                        double pz = center.getZ() + dirZ * d + dirX * zig;

                        Location p = new Location(center.getWorld(), px, center.getY() + 0.08, pz);

                        // Càng gần tâm càng sáng (vàng), càng xa càng đỏ sẫm
                        double heat = 1.0 - (d / fissureLength);
                        Color crackColor = Color.fromRGB(
                                255,
                                (int) (40 + heat * 160),
                                0
                        );

                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(crackColor, erupting ? 1.5f : 1.2f)
                        );

                        // Giai đoạn phun trào: lửa và dung nham bắn lên dọc vết nứt
                        if (erupting && Math.random() < 0.18) {
                            p.getWorld().spawnParticle(Particle.FLAME, p.clone().add(0, 0.3, 0),
                                    2, 0.08, 0.25, 0.08, 0.02);
                            p.getWorld().spawnParticle(Particle.LAVA, p, 1, 0.1, 0.1, 0.1, 0);
                        }

                        // Kiểm tra entity đứng trên đoạn nứt này
                        checkHit(px, pz);
                    }
                }

                if (ticks == growTicks) {
                    center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
                }

                ticks++;
            }

            private void checkHit(double px, double pz) {
                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (hitEntities.contains(entity.getUniqueId())) continue;

                    Location eLoc = entity.getLocation();
                    if (Math.abs(eLoc.getY() - center.getY()) > 2.5) continue;

                    double dx = eLoc.getX() - px;
                    double dz = eLoc.getZ() - pz;

                    if ((dx * dx) + (dz * dz) <= hitWidth * hitWidth) {
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

                        entity.getWorld().spawnParticle(Particle.LAVA,
                                eLoc.clone().add(0, 0.5, 0), 12, 0.3, 0.4, 0.3, 0);
                    }
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
