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
 * BLADE_DANCE
 * 4 lưỡi kiếm vô hình quay quanh mục tiêu theo 4 góc 90 độ, mỗi lưỡi lướt qua và chém
 * một nhát trong bán kính nhỏ, tạo thành chuỗi 4 đòn chém liên hoàn.
 */
public class BLADE_DANCE implements IAbility {

    private static final int HITS = 4;
    private static final double BASE_PERCENT = 20.0;
    private static final double PERCENT_PER_LEVEL = 14.0;

    @Override
    public String getName() {
        return "BLADE_DANCE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);
        final double damagePercent = BASE_PERCENT + (skillLevel - 1) * PERCENT_PER_LEVEL;
        final double hitDamage = baseDamage * (damagePercent / 100.0);

        final double radius = 1.8;
        final double radiusSquared = radius * radius;
        final double bladeOrbit = 1.6;

        final Location center = target.getLocation().clone().add(0, 1.0, 0);
        final Color bladeColor = Color.fromRGB(255, 245, 200);

        final int[] slashTicks = {4, 9, 14, 19};
        final int totalTicks = 22;

        center.getWorld().playSound(center, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.7f, 1.6f);

        new BukkitRunnable() {

            int ticks = 0;
            int slashIndex = 0;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                // Lưỡi kiếm quay liên tục quanh mục tiêu
                double spin = ticks * 0.6;
                for (int i = 0; i < 4; i++) {
                    double a = spin + (Math.PI / 2) * i;
                    Location bladeTip = center.clone().add(Math.cos(a) * bladeOrbit, 0, Math.sin(a) * bladeOrbit);
                    if (bladeTip.getWorld() != center.getWorld()) continue;
                    bladeTip.getWorld().spawnParticle(
                            Particle.DUST, bladeTip, 2,
                            new Particle.DustOptions(bladeColor, 1.1f)
                    );
                    bladeTip.getWorld().spawnParticle(Particle.CRIT, bladeTip, 1, 0.05, 0.05, 0.05, 0.01);
                }

                if (slashIndex < slashTicks.length && ticks == slashTicks[slashIndex]) {
                    slash(spin + (Math.PI / 2) * slashIndex);
                    slashIndex++;
                }

                ticks++;
            }

            private void slash(double angle) {
                center.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.3f);
                Location slashPoint = center.clone().add(Math.cos(angle) * bladeOrbit, 0, Math.sin(angle) * bladeOrbit);
                slashPoint.getWorld().spawnParticle(Particle.SWEEP_ATTACK, slashPoint, 3, 0.3, 0.2, 0.3, 0.0);
                for (double t = -0.5; t <= 0.5; t += 0.1) {
                    Location p = slashPoint.clone().add(
                            -Math.sin(angle) * t, 0, Math.cos(angle) * t);
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(bladeColor, 1.4f)
                    );
                }

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
