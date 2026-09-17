package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * CHAIN_LIGHTNING
 * Sét đánh mục tiêu đầu tiên rồi nảy sang các mục tiêu gần đó, sát thương giảm dần mỗi lần nảy.
 */
public class CHAIN_LIGHTNING implements IAbility {

    private static final double JUMP_RANGE = 6.0;
    private static final double FALLOFF = 0.75;

    @Override
    public String getName() {
        return "CHAIN_LIGHTNING";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 28% + 8%/level cho lần đánh đầu tiên
        final double initialPercent = 28.0 + (skillLevel - 1) * 8.0;
        final double initialDamage = baseDamage * (initialPercent / 100.0);

        // Số lần nảy tối đa, tối đa 6 lần để tránh lan quá xa
        final int maxJumps = Math.min(6, 2 + skillLevel / 2);

        final Set<UUID> hitEntities = new HashSet<>();

        new BukkitRunnable() {

            Location currentPoint = target.getLocation().clone();
            LivingEntity currentTarget = target;
            double currentDamage = initialDamage;
            int jumpsDone = 0;

            @Override
            public void run() {

                // An toàn: hết lượt nảy hoặc world null thì dừng ngay, tránh treo task
                if (currentTarget == null || jumpsDone >= maxJumps
                        || currentPoint.getWorld() == null) {
                    this.cancel();
                    return;
                }

                if (currentTarget.isDead() || !currentTarget.isValid()) {
                    this.cancel();
                    return;
                }

                // Đánh mục tiêu hiện tại
                hitEntities.add(currentTarget.getUniqueId());
                currentTarget.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                currentTarget.damage(currentDamage, attacker);
                final LivingEntity hitRef = currentTarget;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (hitRef.isValid()) hitRef.removeMetadata("IS_ABILITY", Main.getInstance());
                    }
                }.runTaskLater(Main.getInstance(), 1L);

                drawBolt(currentPoint, currentTarget.getLocation().clone().add(0, 1, 0));

                jumpsDone++;

                // Tìm mục tiêu tiếp theo trong tầm, chưa từng bị đánh
                LivingEntity next = findNextTarget(currentTarget.getLocation(), attacker);

                if (next == null) {
                    this.cancel();
                    return;
                }

                currentPoint = currentTarget.getLocation().clone().add(0, 1, 0);
                currentTarget = next;
                currentDamage *= FALLOFF;
            }

            private LivingEntity findNextTarget(Location from, Player attacker) {
                LivingEntity best = null;
                double bestDistSquared = JUMP_RANGE * JUMP_RANGE;

                for (LivingEntity entity : from.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (hitEntities.contains(entity.getUniqueId())) continue;

                    double distSquared = entity.getLocation().distanceSquared(from);
                    if (distSquared <= bestDistSquared) {
                        bestDistSquared = distSquared;
                        best = entity;
                    }
                }
                return best;
            }

            private void drawBolt(Location from, Location to) {
                if (from.getWorld() == null || to.getWorld() == null) return;
                int steps = 12;
                for (int i = 0; i <= steps; i++) {
                    double t = (double) i / steps;
                    Location point = from.clone().add(
                            (to.getX() - from.getX()) * t,
                            (to.getY() - from.getY()) * t,
                            (to.getZ() - from.getZ()) * t
                    );
                    point.getWorld().spawnParticle(
                            Particle.DUST,
                            point,
                            1,
                            new Particle.DustOptions(Color.fromRGB(230, 230, 255), 1.3f)
                    );
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 3L);
    }
}
