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

/**
 * GRAVITY_CRUSH
 * Một trường trọng lực hình thành phía trên mục tiêu, nén dần xuống rồi giáng một cú ép
 * thẳng đứng cực mạnh xuống đất, gây sát thương AoE quanh điểm va chạm. Sát thương 1 lần.
 */
public class GRAVITY_CRUSH implements IAbility {

    @Override
    public String getName() {
        return "GRAVITY_CRUSH";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 30% + 10%/level
        final double damagePercent = 30.0 + (skillLevel - 1) * 10.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double crushRadius = 3.8 + (skillLevel * 0.15);
        final double crushRadiusSquared = crushRadius * crushRadius;
        final double fieldHeight = 6.0;

        final Location center = target.getLocation().clone();
        final Color darkPurple = Color.fromRGB(70, 20, 100);
        final Color voidColor = Color.fromRGB(20, 5, 30);

        center.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 0.4f);

        // Kìm chân mục tiêu tại chỗ trong lúc trường trọng lực nén xuống
        target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SLOWNESS, 24, 6, false, false, true));

        new BukkitRunnable() {

            int ticks = 0;
            final int compressTicks = 20;
            final int totalTicks = 24;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                if (ticks < compressTicks) {

                    double progress = (double) ticks / compressTicks;
                    double sphereY = fieldHeight * (1.0 - progress) + 1.0;
                    double sphereSize = 1.2 - progress * 0.5;

                    // Quả cầu trọng lực hạ dần xuống đầu mục tiêu
                    int points = 40;
                    double golden = Math.PI * (3 - Math.sqrt(5));
                    for (int i = 0; i < points; i++) {
                        double y = 1 - (i / (double) (points - 1)) * 2;
                        double r = Math.sqrt(Math.max(0, 1 - y * y));
                        double theta = golden * i + progress * 10;

                        Location p = center.clone().add(
                                x(theta, r) * sphereSize,
                                sphereY + y * sphereSize,
                                z(theta, r) * sphereSize);
                        if (p.getWorld() != center.getWorld()) continue;

                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(darkPurple, 1.3f)
                        );
                    }

                    // Vệt hút xuống nối quả cầu với mặt đất
                    for (double y = 0; y <= sphereY; y += 0.4) {
                        Location p = center.clone().add(0, y, 0);
                        if (p.getWorld() != center.getWorld()) continue;
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(voidColor, 0.9f)
                        );
                    }

                } else if (ticks == compressTicks) {
                    slam();
                } else {
                    center.getWorld().spawnParticle(
                            Particle.DUST, center, 10, crushRadius * 0.4, 0.2, crushRadius * 0.4, 0,
                            new Particle.DustOptions(darkPurple, 1.0f)
                    );
                }

                ticks++;
            }

            private double x(double theta, double r) { return Math.cos(theta) * r; }
            private double z(double theta, double r) { return Math.sin(theta) * r; }

            private void slam() {
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.3f, 0.4f);
                center.getWorld().spawnParticle(Particle.FLASH, center.clone().add(0, 0.3, 0), 2, 0, 0, 0, 0);

                int points = 70;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    Location p = center.clone().add(
                            Math.cos(angle) * crushRadius, 0.1, Math.sin(angle) * crushRadius);
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(darkPurple, 1.4f)
                    );
                }
                center.getWorld().spawnParticle(Particle.EXPLOSION, center, 3, 0.5, 0.3, 0.5, 0);

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (entity.getLocation().distanceSquared(center) > crushRadiusSquared) continue;

                    entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    entity.damage(damage, attacker);
                    final LivingEntity ref = entity;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (ref.isValid()) ref.removeMetadata("IS_ABILITY", Main.getInstance());
                        }
                    }.runTaskLater(Main.getInstance(), 1L);

                    Vector v = entity.getVelocity();
                    entity.setVelocity(new Vector(v.getX(), -0.3, v.getZ()));
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
