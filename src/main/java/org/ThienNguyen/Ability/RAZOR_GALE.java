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
 * RAZOR_GALE
 * Phóng 3 lưỡi liềm gió hình trăng khuyết bay về phía trước theo hình quạt, mỗi lưỡi xuyên
 * qua nhiều mục tiêu trên đường bay. Sát thương 1 lần cho mỗi entity.
 */
public class RAZOR_GALE implements IAbility {

    private static final int BLADE_COUNT = 3;

    @Override
    public String getName() {
        return "RAZOR_GALE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (attacker == null || attacker.isDead() || attacker.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 23% + 7%/level, mỗi lưỡi gây riêng lẻ (không cộng dồn nếu trúng nhiều lưỡi)
        final double damagePercent = 23.0 + (skillLevel - 1) * 7.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double range = 10.0 + (skillLevel * 0.2);
        final double hitRadius = 1.1;
        final double spreadAngle = Math.toRadians(20);

        final Location start = attacker.getLocation().clone().add(0, 1.0, 0);
        Vector forward = start.getDirection().setY(0);
        if (forward.lengthSquared() < 0.0001) return;
        forward.normalize();
        final Vector baseDirection = forward;

        attacker.getWorld().playSound(start, Sound.ITEM_TRIDENT_THROW, 1.0f, 1.5f);

        final Vector[] directions = new Vector[BLADE_COUNT];
        for (int i = 0; i < BLADE_COUNT; i++) {
            double offset = (i - (BLADE_COUNT - 1) / 2.0) * spreadAngle;
            directions[i] = rotateY(baseDirection, offset);
        }

        final Set<UUID>[] hitPerBlade = new Set[BLADE_COUNT];
        for (int i = 0; i < BLADE_COUNT; i++) hitPerBlade[i] = new HashSet<>();

        new BukkitRunnable() {

            double travelled = 0.0;

            @Override
            public void run() {

                if (start.getWorld() == null || travelled >= range) {
                    this.cancel();
                    return;
                }

                double speed = 1.3;

                for (int b = 0; b < BLADE_COUNT; b++) {
                    Vector dir = directions[b];

                    for (double step = 0; step < speed; step += 0.3) {
                        double d = travelled + step;
                        if (d > range) break;

                        Location tip = start.clone().add(dir.clone().multiply(d));
                        if (tip.getWorld() != start.getWorld()) continue;

                        // Hình trăng khuyết: vẽ 2 cung cong nối nhau
                        for (int arc = 0; arc < 2; arc++) {
                            double curve = (arc == 0 ? 1 : -1) * 0.25;
                            Vector side = new Vector(-dir.getZ(), 0, dir.getX());
                            Location p = tip.clone().add(side.clone().multiply(curve));
                            p.getWorld().spawnParticle(
                                    Particle.DUST, p, 1,
                                    new Particle.DustOptions(Color.fromRGB(200, 255, 230), 1.2f)
                            );
                        }

                        checkHit(b, tip);
                    }
                }

                travelled += speed;
            }

            private void checkHit(int bladeIndex, Location tip) {
                for (LivingEntity entity : tip.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (hitPerBlade[bladeIndex].contains(entity.getUniqueId())) continue;

                    Location body = entity.getLocation().clone().add(0, entity.getHeight() / 2, 0);
                    if (body.distanceSquared(tip) > hitRadius * hitRadius) continue;

                    hitPerBlade[bladeIndex].add(entity.getUniqueId());

                    entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    entity.damage(damage, attacker);
                    final LivingEntity ref = entity;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (ref.isValid()) ref.removeMetadata("IS_ABILITY", Main.getInstance());
                        }
                    }.runTaskLater(Main.getInstance(), 1L);

                    entity.getWorld().spawnParticle(Particle.SWEEP_ATTACK, body, 1, 0, 0, 0, 0);
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    private Vector rotateY(Vector v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = v.getX() * cos - v.getZ() * sin;
        double z = v.getX() * sin + v.getZ() * cos;
        return new Vector(x, v.getY(), z);
    }
}
