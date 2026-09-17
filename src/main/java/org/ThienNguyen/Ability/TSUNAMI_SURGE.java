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
 * TSUNAMI_SURGE
 * Một bức tường sóng thần khổng lồ dâng lên và quét thẳng về phía trước attacker trên một
 * diện rộng, cuốn phăng mọi thứ trên đường đi. Sát thương 1 lần.
 */
public class TSUNAMI_SURGE implements IAbility {

    @Override
    public String getName() {
        return "TSUNAMI_SURGE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (attacker == null || attacker.isDead() || attacker.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 28% + 9%/level
        final double damagePercent = 28.0 + (skillLevel - 1) * 9.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double waveWidth = 8.0 + (skillLevel * 0.2);
        final double sweepDistance = 12.0;
        final double waveHeight = 4.5;
        final double hitThickness = 1.4;

        final Location origin = attacker.getLocation().clone();
        Vector forward = origin.getDirection().setY(0);
        if (forward.lengthSquared() < 0.0001) return;
        forward.normalize();
        final Vector direction = forward;
        final Vector side = new Vector(-direction.getZ(), 0, direction.getX());

        final Set<UUID> hitEntities = new HashSet<>();
        final Color deepBlue = Color.fromRGB(30, 90, 170);
        final Color foam = Color.fromRGB(230, 245, 255);

        origin.getWorld().playSound(origin, Sound.ENTITY_GENERIC_SWIM, 1.4f, 0.4f);

        new BukkitRunnable() {

            int ticks = 0;
            final int totalTicks = 28;

            @Override
            public void run() {

                if (origin.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                double progress = (double) ticks / totalTicks;
                double travelled = sweepDistance * progress;
                // Đợt sóng vươn cao dần rồi hạ xuống khi gần cuối hành trình
                double heightFactor = Math.sin(Math.min(1.0, progress * 1.3) * Math.PI);
                double currentHeight = waveHeight * heightFactor;

                Location waveCenter = origin.clone().add(direction.clone().multiply(travelled));

                for (double w = -waveWidth / 2; w <= waveWidth / 2; w += 0.4) {
                    double edgeFade = 1.0 - Math.abs(w) / (waveWidth / 2) * 0.3;

                    for (double h = 0; h <= currentHeight; h += 0.35) {
                        // Sóng cong về phía trước ở phần đỉnh
                        double curl = (h / Math.max(0.1, currentHeight)) * 0.8;

                        Location p = waveCenter.clone()
                                .add(side.clone().multiply(w))
                                .add(direction.clone().multiply(curl))
                                .add(0, h, 0);
                        if (p.getWorld() != origin.getWorld()) continue;

                        Color color = (h / Math.max(0.1, currentHeight)) > 0.75 ? foam : deepBlue;
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(color, (float) (1.3 * edgeFade))
                        );
                    }
                }

                if (ticks % 3 == 0) {
                    waveCenter.getWorld().spawnParticle(Particle.SPLASH, waveCenter.clone().add(0, currentHeight * 0.4, 0),
                            10, waveWidth * 0.3, currentHeight * 0.2, 1.0, 0.05);
                }

                checkHit(travelled, direction);
                ticks++;
            }

            private void checkHit(double travelled, Vector dir) {
                for (LivingEntity entity : origin.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (hitEntities.contains(entity.getUniqueId())) continue;

                    Location eLoc = entity.getLocation();
                    if (Math.abs(eLoc.getY() - origin.getY()) > 3.5) continue;

                    Vector toEntity = eLoc.toVector().subtract(origin.toVector());
                    toEntity.setY(0);

                    double along = toEntity.dot(dir);
                    double lateral = Math.abs(toEntity.dot(side));

                    if (lateral <= waveWidth / 2 && Math.abs(along - travelled) <= hitThickness) {
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

                        Vector push = dir.clone().multiply(1.1);
                        push.setY(0.35);
                        entity.setVelocity(push);

                        entity.getWorld().spawnParticle(Particle.SPLASH,
                                eLoc.clone().add(0, 1, 0), 25, 0.3, 0.6, 0.3, 0.1);
                        entity.getWorld().playSound(eLoc, Sound.ENTITY_GENERIC_SPLASH, 0.8f, 0.9f);
                    }
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}