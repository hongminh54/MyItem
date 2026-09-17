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
 * MIRROR_FRACTURE
 * Sáu tấm gương bạc dựng lên bao quanh mục tiêu, phản chiếu ánh sáng qua lại giữa các tấm,
 * rồi vỡ tan thành các mảnh gương xoáy bắn ra ngoài. Sát thương 1 lần khi mảnh gương văng ra.
 */
public class MIRROR_FRACTURE implements IAbility {

    private static final int PANEL_COUNT = 6;

    @Override
    public String getName() {
        return "MIRROR_FRACTURE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 29% + 10%/level
        final double damagePercent = 29.0 + (skillLevel - 1) * 10.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double panelRadius = 2.6;
        final double shardRange = 6.0 + (skillLevel * 0.25);
        final double shardRangeSquared = shardRange * shardRange;

        final Location center = target.getLocation().clone().add(0, 0.1, 0);
        final Set<UUID> hitEntities = new HashSet<>();

        final Color silver = Color.fromRGB(225, 235, 245);
        final Color paleBlue = Color.fromRGB(160, 200, 235);

        center.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.2f);

        new BukkitRunnable() {

            int ticks = 0;
            final int raiseTicks = 14;                     // Gương dựng lên
            final int reflectTicks = 12;                   // Phản chiếu qua lại
            final int breakAt = raiseTicks + reflectTicks; // Vỡ
            final int shardTicks = 12;                     // Mảnh văng ra
            final int totalTicks = breakAt + shardTicks;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                if (ticks < raiseTicks) {
                    // GIAI ĐOẠN 1: 6 tấm gương trồi lên
                    double progress = (double) ticks / raiseTicks;
                    drawPanels(progress * 2.6, silver, progress);

                } else if (ticks < breakAt) {
                    // GIAI ĐOẠN 2: gương đứng yên, tia sáng bật qua lại giữa các tấm
                    drawPanels(2.6, silver, 1.0);

                    int reflectTick = ticks - raiseTicks;
                    // Mỗi tick nối 1 cặp tấm khác nhau bằng tia sáng
                    int from = reflectTick % PANEL_COUNT;
                    int to = (from + 2 + (reflectTick / PANEL_COUNT)) % PANEL_COUNT;
                    if (from != to) {
                        drawBeam(from, to, paleBlue);
                    }

                } else if (ticks == breakAt) {
                    center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.8f);
                    center.getWorld().spawnParticle(Particle.FLASH, center.clone().add(0, 1.3, 0),
                            2, 0, 0, 0, 0);

                } else {
                    // GIAI ĐOẠN 3: mảnh gương xoáy văng ra ngoài
                    int shardTick = ticks - breakAt;
                    double progress = (double) shardTick / shardTicks;
                    double dist = shardRange * progress;

                    int shards = 48;
                    for (int i = 0; i < shards; i++) {
                        // Mỗi mảnh vừa bay ra vừa xoay
                        double a = (2 * Math.PI * i) / shards + progress * 2.5;
                        double y = 0.4 + Math.sin(i * 1.7) * 1.4 * (1.0 - progress * 0.4);

                        Location p = center.clone().add(
                                Math.cos(a) * dist, y, Math.sin(a) * dist);
                        if (p.getWorld() != center.getWorld()) continue;

                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(i % 2 == 0 ? silver : paleBlue, 1.3f)
                        );
                    }

                    checkShardHit(dist);
                }

                ticks++;
            }

            /** Vẽ 6 tấm gương phẳng đứng quanh tâm. */
            private void drawPanels(double height, Color color, double alphaProgress) {
                for (int panel = 0; panel < PANEL_COUNT; panel++) {
                    double a = (2 * Math.PI * panel) / PANEL_COUNT;
                    double px = Math.cos(a) * panelRadius;
                    double pz = Math.sin(a) * panelRadius;

                    // Hướng mặt phẳng tấm gương: vuông góc với bán kính
                    double tangentX = -Math.sin(a);
                    double tangentZ = Math.cos(a);
                    double panelWidth = 1.5;

                    for (double w = -panelWidth / 2; w <= panelWidth / 2; w += 0.25) {
                        for (double y = 0; y <= height; y += 0.3) {
                            Location p = center.clone().add(
                                    px + tangentX * w, y, pz + tangentZ * w);
                            if (p.getWorld() != center.getWorld()) continue;

                            // Viền tấm sáng hơn phần giữa
                            boolean edge = Math.abs(w) > panelWidth / 2 - 0.26
                                    || y < 0.3 || y > height - 0.3;
                            p.getWorld().spawnParticle(
                                    Particle.DUST, p, 1,
                                    new Particle.DustOptions(color, edge ? 1.4f : 1.0f)
                            );
                        }
                    }
                }
            }

            /** Tia sáng nối giữa 2 tấm gương. */
            private void drawBeam(int from, int to, Color color) {
                double a1 = (2 * Math.PI * from) / PANEL_COUNT;
                double a2 = (2 * Math.PI * to) / PANEL_COUNT;

                double x1 = Math.cos(a1) * panelRadius, z1 = Math.sin(a1) * panelRadius;
                double x2 = Math.cos(a2) * panelRadius, z2 = Math.sin(a2) * panelRadius;

                for (double t = 0; t <= 1.0; t += 0.05) {
                    Location p = center.clone().add(
                            x1 + (x2 - x1) * t, 1.3, z1 + (z2 - z1) * t);
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(color, 1.2f)
                    );
                }
            }

            private void checkShardHit(double currentDist) {
                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (hitEntities.contains(entity.getUniqueId())) continue;

                    Location eLoc = entity.getLocation();
                    if (Math.abs(eLoc.getY() - center.getY()) > 3.0) continue;

                    double dx = eLoc.getX() - center.getX();
                    double dz = eLoc.getZ() - center.getZ();
                    double distSquared = (dx * dx) + (dz * dz);

                    if (distSquared > shardRangeSquared) continue;

                    // Trúng khi vành mảnh gương vừa quét qua vị trí entity
                    double dist = Math.sqrt(distSquared);
                    if (Math.abs(dist - currentDist) <= 1.2) {

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
                                Particle.DUST, eLoc.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0,
                                new Particle.DustOptions(silver, 1.2f)
                        );
                        entity.getWorld().playSound(eLoc, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.5f);
                    }
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
