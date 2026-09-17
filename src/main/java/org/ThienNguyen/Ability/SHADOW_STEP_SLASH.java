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

/**
 * SHADOW_STEP_SLASH
 * 4 bóng ảo của attacker chớp hiện quanh mục tiêu ở các góc khác nhau, mỗi bóng ảo
 * lướt tới chém một nhát trong bán kính nhỏ rồi tan biến thành khói đen.
 */
public class SHADOW_STEP_SLASH implements IAbility {

    private static final int HITS = 4;
    private static final double BASE_PERCENT = 24.0;
    private static final double PERCENT_PER_LEVEL = 17.0;

    @Override
    public String getName() {
        return "SHADOW_STEP_SLASH";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);
        final double damagePercent = BASE_PERCENT + (skillLevel - 1) * PERCENT_PER_LEVEL;
        final double hitDamage = baseDamage * (damagePercent / 100.0);

        final double radius = 1.9;
        final double radiusSquared = radius * radius;
        final double appearDistance = 1.7;

        final Location center = target.getLocation().clone().add(0, 1.0, 0);
        final Color shadowColor = Color.fromRGB(40, 20, 60);
        final Color slashColor = Color.fromRGB(190, 160, 255);

        final int[] slashTicks = {4, 9, 14, 19};
        final int totalTicks = 22;

        center.getWorld().playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.9f);

        new BukkitRunnable() {

            int ticks = 0;
            int slashIndex = 0;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                if (slashIndex < slashTicks.length && ticks == slashTicks[slashIndex]) {
                    // Góc xuất hiện thay đổi ngẫu nhiên mỗi lần để tạo cảm giác "chớp dịch chuyển"
                    double a = Math.random() * 2 * Math.PI;
                    appearAndSlash(a);
                    slashIndex++;
                }

                ticks++;
            }

            private void appearAndSlash(double angle) {
                Location appearPoint = center.clone().add(
                        Math.cos(angle) * appearDistance, 0, Math.sin(angle) * appearDistance);

                center.getWorld().playSound(appearPoint, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.8f, 1.5f);

                // Khói bóng ảo hiện ra
                for (int i = 0; i < 10; i++) {
                    Location p = appearPoint.clone().add(
                            (Math.random() - 0.5) * 0.6, Math.random() * 1.6, (Math.random() - 0.5) * 0.6);
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(shadowColor, 1.2f)
                    );
                }
                appearPoint.getWorld().spawnParticle(Particle.SQUID_INK, appearPoint.clone().add(0, 0.8, 0), 8, 0.3, 0.5, 0.3, 0.01);

                // Vệt chém hướng vào tâm mục tiêu
                for (double t = 0; t <= 1.0; t += 0.1) {
                    Location p = appearPoint.clone().add(
                            (center.getX() - appearPoint.getX()) * t,
                            0,
                            (center.getZ() - appearPoint.getZ()) * t);
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(slashColor, 1.3f)
                    );
                }
                appearPoint.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center, 2, 0.2, 0.2, 0.2, 0.0);

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;

                    Location eLoc = entity.getLocation();
                    if (Math.abs(eLoc.getY() - center.getY()) > 2.0) continue;

                    double dx = eLoc.getX() - center.getX();
                    double dz = eLoc.getZ() - center.getZ();
                    if ((dx * dx) + (dz * dz) > radiusSquared) continue;

                    entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    entity.damage(hitDamage, attacker);
                    final LivingEntity ref = entity;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (ref.isValid()) ref.removeMetadata("IS_ABILITY", Main.getInstance());
                        }
                    }.runTaskLater(Main.getInstance(), 1L);
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
