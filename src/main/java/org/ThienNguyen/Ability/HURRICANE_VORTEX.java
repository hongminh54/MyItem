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
 * HURRICANE_VORTEX
 * Một cơn bão xoáy hình thành tại mục tiêu, tồn tại 3.5 giây, hút entity vào tâm và liên tục
 * quật ngã bằng gió giật. (Loại sát thương liên tục.)
 */
public class HURRICANE_VORTEX implements IAbility {

    @Override
    public String getName() {
        return "HURRICANE_VORTEX";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Tổng sát thương liên tục: 24% + 8%/level, chia đều 7 lần tick
        final double totalPercent = 24.0 + (skillLevel - 1) * 8.0;
        final double tickDamage = baseDamage * (totalPercent / 7.0 / 100.0);

        final double radius = 4.0 + (skillLevel * 0.15);
        final double radiusSquared = radius * radius;
        final double vortexHeight = 5.0;

        final Location center = target.getLocation().clone();
        final Color stormGray = Color.fromRGB(140, 150, 160);
        final Color stormDark = Color.fromRGB(70, 80, 95);

        center.getWorld().playSound(center, Sound.ITEM_ELYTRA_FLYING, 1.2f, 0.5f);
        center.getWorld().playSound(center, Sound.WEATHER_RAIN, 1.4f, 0.7f);

        new BukkitRunnable() {

            int ticksLeft = 7; // 7 lần, mỗi 10 tick = 3.5 giây tồn tại
            int frame = 0;

            @Override
            public void run() {

                if (center.getWorld() == null || ticksLeft <= 0) {
                    this.cancel();
                    return;
                }

                double spin = frame * 0.4;

                for (double y = 0; y <= vortexHeight; y += 0.3) {
                    double taper = 1.0 - (y / vortexHeight) * 0.6;
                    double r = radius * taper;
                    int points = Math.max(12, (int) (r * 9));

                    for (int i = 0; i < points; i++) {
                        double a = (2 * Math.PI * i) / points + spin + y * 0.5;
                        Location p = center.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                        if (p.getWorld() != center.getWorld()) continue;
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(y > vortexHeight * 0.6 ? stormDark : stormGray, 1.1f)
                        );
                    }
                }

                if (frame % 3 == 0) {
                    center.getWorld().spawnParticle(Particle.CLOUD, center.clone().add(0, vortexHeight * 0.7, 0),
                            6, radius * 0.4, 0.4, radius * 0.4, 0.02);
                }

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;

                    Location eLoc = entity.getLocation();
                    if (Math.abs(eLoc.getY() - center.getY()) > vortexHeight) continue;
                    if (eLoc.distanceSquared(center) > radiusSquared) continue;

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

                        entity.getWorld().spawnParticle(Particle.CLOUD,
                                eLoc.clone().add(0, 1, 0), 10, 0.3, 0.4, 0.3, 0.05);
                    }

                    // Hút vào tâm + đẩy nhẹ lên trên (gió xoáy cuốn lên), tốc độ giới hạn để an toàn
                    Vector toCenter = center.toVector().subtract(eLoc.toVector());
                    toCenter.setY(0);
                    if (toCenter.lengthSquared() > 0.05) {
                        Vector pull = toCenter.normalize().multiply(Math.min(0.12, toCenter.length() / 8.0));
                        Vector v = entity.getVelocity();
                        entity.setVelocity(new Vector(
                                v.getX() + pull.getX(),
                                Math.min(0.15, v.getY() + 0.02),
                                v.getZ() + pull.getZ()));
                    }
                }

                frame++;
                if (frame % 10 == 0) ticksLeft--;
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}