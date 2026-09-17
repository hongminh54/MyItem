package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * POISON_CLOUD
 * Thả đám mây độc tại vị trí mục tiêu, tồn tại 4 giây. Entity đứng trong mây sẽ bị nhiễm độc
 * (3 tick sát thương trải dài 3 giây). Nếu đứng lại trong mây, độc được làm mới thay vì cộng dồn
 * (tái sử dụng cơ chế hủy-task-cũ của Bleed để tránh nhiều task độc chạy chồng nhau vô hạn).
 */
public class POISON_CLOUD implements IAbility {

    private static final String METADATA_TASK = "POISON_TASK";
    private static final String METADATA_IS_POISONED = "IS_POISONED";

    @Override
    public String getName() {
        return "POISON_CLOUD";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        final double radius = 3.0 + (skillLevel * 0.15);
        final double radiusSquared = radius * radius;
        // 6%/tick x 3 tick = 18% tổng base, +2%/tick/level (tổng +6%/level)
        final double tickPercent = 6.0 + (skillLevel - 1) * 2.0;
        final double damagePerTick = baseDamage * (tickPercent / 100.0);

        final Location center = target.getLocation().clone();

        new BukkitRunnable() {

            int sweepsLeft = 8; // 8 lần quét, mỗi 10 tick = 4 giây tồn tại

            @Override
            public void run() {

                if (center.getWorld() == null || sweepsLeft <= 0) {
                    this.cancel();
                    return;
                }

                int points = 30;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    double dist = radius * Math.sqrt(Math.random());
                    double x = Math.cos(angle) * dist;
                    double z = Math.sin(angle) * dist;
                    Location p = center.clone().add(x, 0.2, z);
                    p.getWorld().spawnParticle(
                            Particle.DUST,
                            p,
                            1,
                            new Particle.DustOptions(Color.fromRGB(60, 180, 40), 1.2f)
                    );
                }

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;

                    if (entity.getLocation().distanceSquared(center) <= radiusSquared) {
                        applyPoison(attacker, entity, damagePerTick);
                    }
                }

                sweepsLeft--;
            }

        }.runTaskTimer(Main.getInstance(), 0L, 10L);
    }

    private void applyPoison(Player attacker, LivingEntity target, double damagePerTick) {

        // Nếu đang nhiễm độc, hủy task cũ trước khi bắt đầu task mới (làm mới thời gian độc)
        // để tránh nhiều task độc cộng dồn chạy song song vô hạn trên cùng 1 entity.
        if (target.hasMetadata(METADATA_IS_POISONED)) {
            if (target.hasMetadata(METADATA_TASK)) {
                Object old = target.getMetadata(METADATA_TASK).get(0).value();
                if (old instanceof BukkitRunnable oldTask) {
                    oldTask.cancel();
                }
            }
            startPoisonTask(attacker, target, damagePerTick);
            return;
        }

        target.setMetadata(METADATA_IS_POISONED, new FixedMetadataValue(Main.getInstance(), true));
        startPoisonTask(attacker, target, damagePerTick);
    }

    private void startPoisonTask(Player attacker, LivingEntity target, double damagePerTick) {
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
                target.damage(damagePerTick, attacker);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (target.isValid()) target.removeMetadata("IS_ABILITY", Main.getInstance());
                    }
                }.runTaskLater(Main.getInstance(), 1L);

                target.getWorld().spawnParticle(
                        Particle.DUST,
                        target.getLocation().add(0, 1, 0),
                        8, 0.2, 0.3, 0.2, 0,
                        new Particle.DustOptions(Color.fromRGB(60, 180, 40), 1.0f)
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
