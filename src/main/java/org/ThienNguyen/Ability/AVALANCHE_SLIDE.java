package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * AVALANCHE_SLIDE
 * Một bức tường tuyết và đá lăn cuộn về phía trước, ngày càng lớn dần khi lăn đi, cuốn phăng
 * và vùi lấp mọi thứ trên đường đi. Sát thương 1 lần kèm làm chậm nặng.
 */
public class AVALANCHE_SLIDE implements IAbility {

    @Override
    public String getName() {
        return "AVALANCHE_SLIDE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (attacker == null || attacker.isDead() || attacker.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 27% + 9%/level
        final double damagePercent = 27.0 + (skillLevel - 1) * 9.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double baseWidth = 4.0 + (skillLevel * 0.15);
        final double sweepDistance = 11.0;
        final double hitThickness = 1.5;

        final Location origin = attacker.getLocation().clone();
        Vector forward = origin.getDirection().setY(0);
        if (forward.lengthSquared() < 0.0001) return;
        forward.normalize();
        final Vector direction = forward;
        final Vector side = new Vector(-direction.getZ(), 0, direction.getX());

        final Set<UUID> hitEntities = new HashSet<>();
        final Color snowWhite = Color.fromRGB(240, 245, 250);
        final Color rockGray = Color.fromRGB(120, 115, 110);

        origin.getWorld().playSound(origin, Sound.BLOCK_SNOW_BREAK, 1.2f, 0.5f);

        new BukkitRunnable() {

            int ticks = 0;
            final int totalTicks = 26;

            @Override
            public void run() {

                if (origin.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                double progress = (double) ticks / totalTicks;
                double travelled = sweepDistance * progress;
                // Càng lăn xa, tường tuyết càng phình to (giống avalanche thật)
                double currentWidth = baseWidth * (0.6 + progress * 0.7);
                double currentHeight = 2.0 + progress * 2.0;

                Location wallCenter = origin.clone().add(direction.clone().multiply(travelled));

                for (double w = -currentWidth / 2; w <= currentWidth / 2; w += 0.4) {
                    for (double h = 0; h <= currentHeight; h += 0.35) {
                        // Vón cục cuộn tròn ở phần đáy tường (bánh xe tuyết lăn)
                        double rollOffset = Math.sin((w * 1.5) + (ticks * 0.5)) * 0.2;

                        Location p = wallCenter.clone()
                                .add(side.clone().multiply(w))
                                .add(0, h + rollOffset, 0);
                        if (p.getWorld() != origin.getWorld()) continue;

                        Color color = Math.random() < 0.75 ? snowWhite : rockGray;
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(color, 1.3f)
                        );
                    }
                }

                if (ticks % 3 == 0) {
                    wallCenter.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            wallCenter.clone().add(0, currentHeight * 0.5, 0),
                            10, currentWidth * 0.3, currentHeight * 0.3, 1.0, 0.02);
                }

                checkHit(travelled);
                ticks++;
            }

            private void checkHit(double travelled) {
                for (LivingEntity entity : origin.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (hitEntities.contains(entity.getUniqueId())) continue;

                    Location eLoc = entity.getLocation();
                    if (Math.abs(eLoc.getY() - origin.getY()) > 3.0) continue;

                    Vector toEntity = eLoc.toVector().subtract(origin.toVector());
                    toEntity.setY(0);

                    double progress = travelled / sweepDistance;
                    double currentWidth = baseWidth * (0.6 + progress * 0.7);

                    double along = toEntity.dot(direction);
                    double lateral = Math.abs(toEntity.dot(side));

                    if (lateral <= currentWidth / 2 && Math.abs(along - travelled) <= hitThickness) {
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

                        entity.addPotionEffect(new PotionEffect(
                                PotionEffectType.SLOWNESS, 50, 4, false, false, true));

                        Vector push = direction.clone().multiply(0.9);
                        push.setY(0.25);
                        entity.setVelocity(push);

                        entity.getWorld().spawnParticle(Particle.SNOWFLAKE,
                                eLoc.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.02);
                    }
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}