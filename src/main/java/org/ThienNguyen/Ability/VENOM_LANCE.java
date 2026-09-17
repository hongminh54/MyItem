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
 * VENOM_LANCE
 * Một mũi lao độc bay thẳng về phía trước, cắn 1 nhát tức thời rồi tiêm độc gây sát thương
 * liên tục trong 3 giây. (Loại sát thương liên tục.)
 */
public class VENOM_LANCE implements IAbility {

    private static final String METADATA_TASK = "VENOM_LANCE_TASK";
    private static final String METADATA_IS_POISONED = "VENOM_LANCE_ACTIVE";

    @Override
    public String getName() {
        return "VENOM_LANCE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (attacker == null || attacker.isDead() || attacker.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Tổng sát thương (cắn + độc): 24% + 8%/level, chia 30% cắn tức thời / 70% độc
        final double totalPercent = 24.0 + (skillLevel - 1) * 8.0;
        final double biteDamage = baseDamage * (totalPercent * 0.30 / 100.0);
        final double dotTickDamage = baseDamage * ((totalPercent * 0.70) / 3.0 / 100.0);

        final double maxRange = 12.0 + (skillLevel * 0.25);
        final double hitRadius = 1.3;

        final Location start = attacker.getEyeLocation().clone();
        final Vector direction = start.getDirection().normalize();

        final Set<UUID> hitEntities = new HashSet<>();

        start.getWorld().playSound(start, Sound.ENTITY_SPIDER_HURT, 0.9f, 0.6f);

        new BukkitRunnable() {

            double travelled = 0.0;

            @Override
            public void run() {

                if (start.getWorld() == null || travelled >= maxRange) {
                    this.cancel();
                    return;
                }

                double speed = 1.4;
                for (double step = 0; step < speed; step += 0.25) {
                    double d = travelled + step;
                    if (d > maxRange) break;

                    Location tip = start.clone().add(direction.clone().multiply(d));
                    if (tip.getWorld() != start.getWorld()) continue;

                    tip.getWorld().spawnParticle(
                            Particle.DUST, tip, 2, 0.05, 0.05, 0.05, 0,
                            new Particle.DustOptions(Color.fromRGB(90, 200, 60), 1.3f)
                    );

                    checkHit(tip);
                }

                travelled += speed;
            }

            private void checkHit(Location tip) {
                for (LivingEntity entity : tip.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (hitEntities.contains(entity.getUniqueId())) continue;

                    Location body = entity.getLocation().clone().add(0, entity.getHeight() / 2, 0);
                    if (body.distanceSquared(tip) > hitRadius * hitRadius) continue;

                    hitEntities.add(entity.getUniqueId());

                    entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    entity.damage(biteDamage, attacker);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (entity.isValid()) entity.removeMetadata("IS_ABILITY", Main.getInstance());
                        }
                    }.runTaskLater(Main.getInstance(), 1L);

                    entity.getWorld().spawnParticle(
                            Particle.DUST, body, 16, 0.25, 0.35, 0.25, 0,
                            new Particle.DustOptions(Color.fromRGB(60, 160, 40), 1.1f)
                    );

                    applyPoison(attacker, entity, dotTickDamage);
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    private void applyPoison(Player attacker, LivingEntity target, double dotTickDamage) {
        // Hủy task độc cũ nếu có, tránh cộng dồn nhiều task độc vô hạn trên cùng entity
        if (target.hasMetadata(METADATA_IS_POISONED)) {
            if (target.hasMetadata(METADATA_TASK)) {
                Object old = target.getMetadata(METADATA_TASK).get(0).value();
                if (old instanceof BukkitRunnable oldTask) {
                    oldTask.cancel();
                }
            }
        } else {
            target.setMetadata(METADATA_IS_POISONED, new FixedMetadataValue(Main.getInstance(), true));
        }
        startPoisonTask(attacker, target, dotTickDamage);
    }

    private void startPoisonTask(Player attacker, LivingEntity target, double dotTickDamage) {

        BukkitRunnable poisonTask = new BukkitRunnable() {
            int count = 0;
            final int maxCount = 3;

            @Override
            public void run() {
                if (target == null || target.isDead() || !target.isValid() || count >= maxCount) {
                    cleanup(target);
                    this.cancel();
                    return;
                }

                target.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                target.damage(dotTickDamage, attacker);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (target.isValid()) target.removeMetadata("IS_ABILITY", Main.getInstance());
                    }
                }.runTaskLater(Main.getInstance(), 1L);

                target.getWorld().spawnParticle(
                        Particle.DUST, target.getLocation().add(0, 1, 0), 10, 0.2, 0.3, 0.2, 0,
                        new Particle.DustOptions(Color.fromRGB(60, 160, 40), 1.0f)
                );

                count++;
            }
        };

        target.setMetadata(METADATA_TASK, new FixedMetadataValue(Main.getInstance(), poisonTask));
        poisonTask.runTaskTimer(Main.getInstance(), 20L, 20L);
    }

    private void cleanup(LivingEntity target) {
        if (target != null) {
            target.removeMetadata(METADATA_TASK, Main.getInstance());
            target.removeMetadata(METADATA_IS_POISONED, Main.getInstance());
        }
    }
}
