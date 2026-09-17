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
 * SPECTRAL_HOWL
 * Attacker rú lên một tiếng hú ma quái, phát ra một hình nón sóng âm ma quái quét về phía
 * trước, đẩy lùi và gây Mù + chậm cho kẻ địch trúng đòn. Sát thương 1 lần.
 */
public class SPECTRAL_HOWL implements IAbility {

    @Override
    public String getName() {
        return "SPECTRAL_HOWL";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (attacker == null || attacker.isDead() || attacker.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 26% + 9%/level
        final double damagePercent = 26.0 + (skillLevel - 1) * 9.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double coneRange = 7.0 + (skillLevel * 0.2);
        final double coneHalfAngle = Math.toRadians(40); // Nón rộng 80 độ

        final Location origin = attacker.getEyeLocation().clone();
        Vector forward = origin.getDirection().setY(0);
        if (forward.lengthSquared() < 0.0001) return;
        forward.normalize();
        final Vector direction = forward;

        final Set<UUID> hitEntities = new HashSet<>();

        origin.getWorld().playSound(origin, Sound.ENTITY_VEX_AMBIENT, 1.4f, 0.5f);
        origin.getWorld().playSound(origin, Sound.ENTITY_PHANTOM_AMBIENT, 1.2f, 0.6f);

        new BukkitRunnable() {

            int ticks = 0;
            final int totalTicks = 10;

            @Override
            public void run() {

                if (origin.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                double progress = (double) (ticks + 1) / totalTicks;
                double currentRange = coneRange * progress;

                // Vẽ hình nón bằng các cung tròn tăng dần bán kính
                for (double r = 0.5; r <= currentRange; r += 0.5) {
                    double arcAngle = coneHalfAngle * 2;
                    int arcPoints = Math.max(6, (int) (r * 4));

                    for (int i = 0; i <= arcPoints; i++) {
                        double offsetAngle = -coneHalfAngle + (arcAngle * i / arcPoints);
                        Vector rotated = rotateY(direction, offsetAngle);
                        Location p = origin.clone().add(rotated.multiply(r));
                        if (p.getWorld() != origin.getWorld()) continue;

                        // Màu ghost xanh lục nhạt, mờ dần ra rìa nón
                        float alpha = (float) (1.4 - (r / currentRange) * 0.5);
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(Color.fromRGB(140, 255, 200), alpha)
                        );
                    }
                }

                checkHit(currentRange);

                ticks++;
            }

            private void checkHit(double currentRange) {
                for (LivingEntity entity : origin.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (hitEntities.contains(entity.getUniqueId())) continue;

                    Location eLoc = entity.getLocation();
                    if (Math.abs(eLoc.getY() - origin.getY()) > 2.5) continue;

                    Vector toEntity = eLoc.toVector().subtract(origin.toVector());
                    toEntity.setY(0);
                    double dist = toEntity.length();
                    if (dist > currentRange || dist < 0.1) continue;

                    double angleTo = toEntity.normalize().angle(direction);
                    if (angleTo > coneHalfAngle) continue;

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

                    entity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false, true));
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false, true));

                    Vector push = toEntity.clone().normalize().multiply(0.6);
                    push.setY(0.25);
                    entity.setVelocity(push);

                    entity.getWorld().spawnParticle(
                            Particle.DUST, eLoc.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0,
                            new Particle.DustOptions(Color.fromRGB(140, 255, 200), 1.2f)
                    );
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    /** Xoay vector quanh trục Y một góc radians. */
    private Vector rotateY(Vector v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = v.getX() * cos - v.getZ() * sin;
        double z = v.getX() * sin + v.getZ() * cos;
        return new Vector(x, v.getY(), z);
    }
}
