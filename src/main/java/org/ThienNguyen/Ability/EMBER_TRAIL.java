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

import java.util.ArrayList;
import java.util.List;

/**
 * EMBER_TRAIL
 * Rải một vệt lửa thẳng về phía trước attacker, cháy âm ỉ trong 3 giây. Bất kỳ entity nào
 * đứng trong vệt lửa khi bị quét sẽ bị bén lửa. (Loại sát thương liên tục.)
 */
public class EMBER_TRAIL implements IAbility {

    @Override
    public String getName() {
        return "EMBER_TRAIL";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (attacker == null || attacker.isDead() || attacker.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Tổng sát thương liên tục: 21% + 7%/level, chia đều 4 lần quét
        final double totalPercent = 21.0 + (skillLevel - 1) * 7.0;
        final double tickDamage = baseDamage * (totalPercent / 4.0 / 100.0);

        final double trailLength = 8.0 + (skillLevel * 0.2);
        final double trailWidth = 1.4;

        final Location origin = attacker.getLocation().clone();
        Vector forward = origin.getDirection().setY(0);
        if (forward.lengthSquared() < 0.0001) return;
        forward.normalize();
        final Vector direction = forward;

        // Danh sách điểm dọc theo vệt lửa
        final List<Location> trailPoints = new ArrayList<>();
        for (double d = 0.5; d <= trailLength; d += 0.6) {
            trailPoints.add(origin.clone().add(direction.clone().multiply(d)));
        }

        origin.getWorld().playSound(origin, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.7f);

        new BukkitRunnable() {

            int sweepsLeft = 4; // 4 lần quét, mỗi 15 tick (~0.75s) = 3 giây tồn tại

            @Override
            public void run() {

                if (origin.getWorld() == null || sweepsLeft <= 0) {
                    this.cancel();
                    return;
                }

                for (Location point : trailPoints) {
                    if (point.getWorld() == null) continue;

                    point.getWorld().spawnParticle(Particle.FLAME, point.clone().add(0, 0.15, 0),
                            3, 0.15, 0.1, 0.15, 0.01);
                    point.getWorld().spawnParticle(
                            Particle.DUST, point.clone().add(0, 0.1, 0), 1, 0.1, 0.05, 0.1, 0,
                            new Particle.DustOptions(Color.fromRGB(255, 120, 30), 1.1f)
                    );

                    for (LivingEntity entity : point.getWorld().getLivingEntities()) {
                        if (entity.isDead() || !entity.isValid()) continue;
                        if (entity.equals(attacker)) continue;

                        Location eLoc = entity.getLocation();
                        if (Math.abs(eLoc.getY() - point.getY()) > 1.5) continue;
                        if (eLoc.distanceSquared(point) > trailWidth * trailWidth) continue;

                        entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                        entity.damage(tickDamage, attacker);
                        final LivingEntity ref = entity;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (ref.isValid()) ref.removeMetadata("IS_ABILITY", Main.getInstance());
                            }
                        }.runTaskLater(Main.getInstance(), 1L);

                        entity.setFireTicks(Math.max(entity.getFireTicks(), 15));
                        entity.getWorld().spawnParticle(Particle.FLAME,
                                eLoc.clone().add(0, 1, 0), 12, 0.25, 0.4, 0.25, 0.02);
                    }
                }

                sweepsLeft--;
            }

        }.runTaskTimer(Main.getInstance(), 0L, 15L);
    }
}
