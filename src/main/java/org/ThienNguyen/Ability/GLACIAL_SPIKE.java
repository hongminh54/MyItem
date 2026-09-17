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
 * GLACIAL_SPIKE
 * Các cột băng nhọn lần lượt trồi lên từ mặt đất theo vòng xoắn quanh mục tiêu, kết thúc bằng
 * một cột băng khổng lồ ở chính giữa. Entity bị cột băng đâm trúng nhận sát thương 1 lần
 * và bị hất nhẹ lên.
 */
public class GLACIAL_SPIKE implements IAbility {

    private static final int SPIKE_COUNT = 10;

    @Override
    public String getName() {
        return "GLACIAL_SPIKE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 27% + 10%/level
        final double damagePercent = 27.0 + (skillLevel - 1) * 10.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double spreadRadius = 3.5 + (skillLevel * 0.15);
        final double spikeHitRadius = 1.3;

        final Location center = target.getLocation().clone();
        final Set<UUID> hitEntities = new HashSet<>();

        final Color iceColor = Color.fromRGB(150, 230, 255);
        final Color deepIce = Color.fromRGB(70, 140, 220);

        center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 0.9f, 1.6f);

        new BukkitRunnable() {

            int ticks = 0;
            final int spikeInterval = 2;                                  // Mỗi 2 tick một cột
            final int spikePhase = SPIKE_COUNT * spikeInterval;           // Giai đoạn cột nhỏ
            final int finalePhase = spikePhase + 14;                      // Cột lớn ở giữa

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= finalePhase) {
                    this.cancel();
                    return;
                }

                if (ticks < spikePhase) {

                    // Trồi cột mới theo nhịp
                    if (ticks % spikeInterval == 0) {
                        int index = ticks / spikeInterval;
                        // Xếp theo xoắn ốc: góc vàng cho phân bố đẹp mắt
                        double angle = index * 2.39996;
                        double dist = spreadRadius * Math.sqrt((double) (index + 1) / SPIKE_COUNT);

                        double sx = Math.cos(angle) * dist;
                        double sz = Math.sin(angle) * dist;
                        Location spikeBase = center.clone().add(sx, 0, sz);

                        growSpike(spikeBase, 2.2, iceColor, deepIce);
                        checkSpikeHit(spikeBase, 2.2);

                        spikeBase.getWorld().playSound(spikeBase, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.9f);
                    }

                } else if (ticks == spikePhase) {
                    // Cột băng lớn ở tâm
                    center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1.4f, 0.6f);
                    growSpike(center.clone(), 5.0, Color.fromRGB(210, 245, 255), deepIce);
                    checkSpikeHit(center.clone(), 5.0);

                    center.getWorld().spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 2, 0),
                            60, 1.2, 1.5, 1.2, 0.05);

                } else {
                    // Băng vụn rơi lả tả sau khi cột lớn dựng xong
                    center.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            center.clone().add(0, 3.5, 0), 8, 1.8, 0.8, 1.8, 0.02);
                    center.getWorld().spawnParticle(
                            Particle.DUST, center.clone().add(0, 1.5, 0), 6, 1.5, 1.2, 1.5, 0,
                            new Particle.DustOptions(iceColor, 1.0f)
                    );
                }

                ticks++;
            }

            /** Vẽ một cột băng nhọn: các vòng tròn nhỏ dần từ chân lên đỉnh. */
            private void growSpike(Location base, double height, Color tip, Color bottom) {
                for (double y = 0; y <= height; y += 0.25) {
                    double taper = 1.0 - (y / height);
                    double r = 0.55 * taper;
                    int points = Math.max(4, (int) (r * 14));

                    for (int i = 0; i < points; i++) {
                        double a = (2 * Math.PI * i) / points;
                        Location p = base.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                        if (p.getWorld() != base.getWorld()) continue;

                        // Chân cột xanh đậm, đỉnh cột sáng
                        Color color = (y / height) > 0.55 ? tip : bottom;
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(color, 1.2f)
                        );
                    }
                }
            }

            private void checkSpikeHit(Location spikeBase, double spikeHeight) {
                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (hitEntities.contains(entity.getUniqueId())) continue;

                    Location eLoc = entity.getLocation();
                    if (Math.abs(eLoc.getY() - spikeBase.getY()) > spikeHeight) continue;

                    double dx = eLoc.getX() - spikeBase.getX();
                    double dz = eLoc.getZ() - spikeBase.getZ();

                    if ((dx * dx) + (dz * dz) <= spikeHitRadius * spikeHitRadius) {
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

                        // Hất nhẹ lên như bị cột băng đội lên
                        Vector v = entity.getVelocity();
                        entity.setVelocity(new Vector(v.getX(), Math.min(0.45, v.getY() + 0.4), v.getZ()));

                        entity.getWorld().spawnParticle(Particle.SNOWFLAKE,
                                eLoc.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
                    }
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
