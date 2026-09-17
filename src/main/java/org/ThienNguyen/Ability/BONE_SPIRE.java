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
 * BONE_SPIRE
 * Một cột xương nhọn xoắn ốc trồi thẳng lên từ dưới chân mục tiêu, đâm xuyên và hất tung
 * lên không, kèm trói chân ngắn khi rơi xuống. Sát thương 1 lần.
 */
public class BONE_SPIRE implements IAbility {

    @Override
    public String getName() {
        return "BONE_SPIRE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 25% + 8%/level
        final double damagePercent = 25.0 + (skillLevel - 1) * 8.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double spireHeight = 3.5 + (skillLevel * 0.1);
        final double hitRadius = 1.2;

        final Location center = target.getLocation().clone();
        final Color boneColor = Color.fromRGB(230, 220, 190);

        center.getWorld().playSound(center, Sound.BLOCK_BONE_BLOCK_BREAK, 1.2f, 0.6f);

        new BukkitRunnable() {

            int ticks = 0;
            final int growTicks = 8;
            final int totalTicks = 16;
            boolean hit = false;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                if (ticks <= growTicks) {
                    double progress = (double) ticks / growTicks;
                    double currentHeight = spireHeight * progress;

                    // Cột xương xoắn: 2 chuỗi xương xoắn quanh trục
                    for (int strand = 0; strand < 2; strand++) {
                        for (double y = 0; y <= currentHeight; y += 0.3) {
                            double angle = (y * 2.2) + (strand * Math.PI);
                            double r = 0.35 * (1.0 - (y / spireHeight) * 0.5);
                            Location p = center.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r);
                            if (p.getWorld() != center.getWorld()) continue;
                            p.getWorld().spawnParticle(
                                    Particle.DUST, p, 1,
                                    new Particle.DustOptions(boneColor, 1.2f)
                            );
                        }
                    }

                    // Đâm trúng mục tiêu ngay khi cột chạm tới người
                    if (!hit && currentHeight >= 1.0) {
                        hit = true;
                        strike();
                    }

                } else {
                    // Bụi xương rơi sau cú đâm
                    center.getWorld().spawnParticle(
                            Particle.DUST, center.clone().add(0, 1.5, 0), 6, 0.5, 0.6, 0.5, 0,
                            new Particle.DustOptions(boneColor, 1.0f)
                    );
                }

                ticks++;
            }

            private void strike() {
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.6f);
                center.getWorld().spawnParticle(Particle.CRIT, center.clone().add(0, 1, 0),
                        24, 0.3, 0.6, 0.3, 0.3);

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (entity.getLocation().distanceSquared(center) > hitRadius * hitRadius) continue;

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
                    entity.setVelocity(new Vector(v.getX(), 0.7, v.getZ()));

                    // Trói chân ngắn khi tiếp đất trở lại
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (entity.isValid() && !entity.isDead()) {
                                entity.addPotionEffect(new PotionEffect(
                                        PotionEffectType.SLOWNESS, 30, 5, false, false, true));
                            }
                        }
                    }.runTaskLater(Main.getInstance(), 12L);
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
