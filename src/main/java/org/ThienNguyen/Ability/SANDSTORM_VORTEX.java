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

/**
 * SANDSTORM_VORTEX
 * Một cơn lốc cát hình thành tại vị trí mục tiêu, tồn tại 3.5 giây, hút và liên tục cào xé
 * bất kỳ entity nào đứng trong lốc, đồng thời gây Mù nhẹ. (Loại sát thương liên tục.)
 */
public class SANDSTORM_VORTEX implements IAbility {

    @Override
    public String getName() {
        return "SANDSTORM_VORTEX";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Tổng sát thương liên tục: 22% + 7%/level, chia đều 7 lần tick (10 tick/lần = 3.5s)
        final double totalPercent = 22.0 + (skillLevel - 1) * 7.0;
        final double tickDamage = baseDamage * (totalPercent / 7.0 / 100.0);

        final double radius = 2.8 + (skillLevel * 0.1);
        final double radiusSquared = radius * radius;
        final double vortexHeight = 4.0;

        final Location center = target.getLocation().clone();
        final Color sandColor = Color.fromRGB(210, 180, 110);

        center.getWorld().playSound(center, Sound.ENTITY_HUSK_AMBIENT, 1.0f, 0.6f);

        new BukkitRunnable() {

            int ticksLeft = 7; // 7 lần, mỗi 10 tick = 3.5 giây tồn tại
            int frame = 0;

            @Override
            public void run() {

                if (center.getWorld() == null || ticksLeft <= 0) {
                    this.cancel();
                    return;
                }

                double spin = frame * 0.35;

                // Vẽ lốc xoáy: nhiều vòng xếp chồng, bán kính hẹp dần lên đỉnh
                for (double y = 0; y <= vortexHeight; y += 0.3) {
                    double taper = 1.0 - (y / vortexHeight) * 0.5;
                    double r = radius * taper;
                    int points = Math.max(10, (int) (r * 10));

                    for (int i = 0; i < points; i++) {
                        double a = (2 * Math.PI * i) / points + spin + y * 0.8;
                        Location p = center.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                        if (p.getWorld() != center.getWorld()) continue;
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(sandColor, 1.1f)
                        );
                    }
                }

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;

                    Location eLoc = entity.getLocation();
                    if (Math.abs(eLoc.getY() - center.getY()) > vortexHeight) continue;
                    if (eLoc.distanceSquared(center) > radiusSquared) continue;

                    // Chỉ gây tick damage mỗi 10 frame (khớp với ticksLeft đếm ngoài)
                    if (frame % 10 == 0) {
                        entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                        entity.damage(tickDamage, attacker);
                        final LivingEntity ref = entity;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (ref.isValid()) ref.removeMetadata("IS_ABILITY", Main.getInstance());
                            }
                        }.runTaskLater(Main.getInstance(), 1L);

                        entity.addPotionEffect(new PotionEffect(
                                PotionEffectType.BLINDNESS, 15, 0, false, false, true));
                    }

                    // Hút nhẹ về tâm lốc mỗi frame
                    Vector toCenter = center.toVector().subtract(eLoc.toVector());
                    toCenter.setY(0);
                    if (toCenter.lengthSquared() > 0.05) {
                        Vector pull = toCenter.normalize().multiply(0.06);
                        Vector v = entity.getVelocity();
                        entity.setVelocity(new Vector(v.getX() + pull.getX(), v.getY(), v.getZ() + pull.getZ()));
                    }
                }

                frame++;
                if (frame % 10 == 0) ticksLeft--;
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
