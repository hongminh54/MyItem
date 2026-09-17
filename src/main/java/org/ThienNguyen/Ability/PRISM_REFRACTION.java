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
 * PRISM_REFRACTION
 * Một cột sáng trắng dựng lên tại mục tiêu rồi tách thành 7 tia cầu vồng bắn ra mọi hướng.
 * Sát thương tính 1 lần cho entity nằm trên đường các tia.
 */
public class PRISM_REFRACTION implements IAbility {

    // 7 màu cầu vồng cho 7 tia
    private static final Color[] SPECTRUM = {
            Color.fromRGB(255, 40, 40),
            Color.fromRGB(255, 150, 30),
            Color.fromRGB(255, 240, 40),
            Color.fromRGB(60, 230, 80),
            Color.fromRGB(50, 150, 255),
            Color.fromRGB(90, 70, 230),
            Color.fromRGB(190, 70, 230)
    };

    @Override
    public String getName() {
        return "PRISM_REFRACTION";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 24% + 9%/level
        final double damagePercent = 24.0 + (skillLevel - 1) * 9.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double beamLength = 7.0 + (skillLevel * 0.2);
        final double hitWidth = 1.2;

        final Location center = target.getLocation().clone().add(0, 0.2, 0);
        final Set<UUID> hitEntities = new HashSet<>();

        center.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);

        new BukkitRunnable() {

            int ticks = 0;
            final int chargeTicks = 10;   // Dựng cột sáng
            final int fireTicks = 14;     // Tia bung ra
            final int totalTicks = chargeTicks + fireTicks;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                if (ticks < chargeTicks) {
                    // GIAI ĐOẠN 1: cột sáng trắng dựng lên
                    double height = 3.5 * ((double) ticks / chargeTicks);
                    for (double y = 0; y <= height; y += 0.25) {
                        double wobble = Math.sin((y * 4) + ticks * 0.6) * 0.12;
                        Location p = center.clone().add(wobble, y, wobble);
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.3f)
                        );
                    }
                } else {
                    // GIAI ĐOẠN 2: 7 tia cầu vồng bung ra
                    int fireTick = ticks - chargeTicks;
                    double progress = (double) (fireTick + 1) / fireTicks;
                    double currentLength = beamLength * progress;

                    for (int b = 0; b < SPECTRUM.length; b++) {
                        double angle = (2 * Math.PI * b) / SPECTRUM.length;
                        double dirX = Math.cos(angle);
                        double dirZ = Math.sin(angle);

                        for (double d = 0; d <= currentLength; d += 0.4) {
                            // Tia hơi nâng lên dần theo khoảng cách
                            double y = 0.4 + (d * 0.06);
                            Location p = center.clone().add(dirX * d, y, dirZ * d);
                            if (p.getWorld() != center.getWorld()) continue;
                            p.getWorld().spawnParticle(
                                    Particle.DUST, p, 1,
                                    new Particle.DustOptions(SPECTRUM[b], 1.4f)
                            );
                        }

                        // Kiểm tra entity nằm gần trục tia này
                        checkBeamHit(dirX, dirZ, currentLength);
                    }
                }

                ticks++;
            }

            private void checkBeamHit(double dirX, double dirZ, double length) {
                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (hitEntities.contains(entity.getUniqueId())) continue;

                    Location eLoc = entity.getLocation();
                    if (Math.abs(eLoc.getY() - center.getY()) > 2.5) continue;

                    double ex = eLoc.getX() - center.getX();
                    double ez = eLoc.getZ() - center.getZ();

                    // Chiếu entity lên trục tia
                    double along = (ex * dirX) + (ez * dirZ);
                    if (along < 0 || along > length) continue;

                    // Khoảng cách vuông góc tới trục tia
                    double perpX = ex - (dirX * along);
                    double perpZ = ez - (dirZ * along);
                    double perpDist = Math.sqrt((perpX * perpX) + (perpZ * perpZ));

                    if (perpDist <= hitWidth) {
                        hitEntities.add(entity.getUniqueId());
                        dealDamage(entity, attacker, damage);
                    }
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    private void dealDamage(LivingEntity entity, Player attacker, double damage) {
        entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
        entity.damage(damage, attacker);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (entity.isValid()) entity.removeMetadata("IS_ABILITY", Main.getInstance());
            }
        }.runTaskLater(Main.getInstance(), 1L);
    }
}
