package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * THORN_ARMOR
 * Buff tự thân: người cast nhận Kháng sát thương, đồng thời tỏa gai gây sát thương định kỳ
 * lên kẻ địch trong bán kính xung quanh mình.
 */
public class THORN_ARMOR implements IAbility {

    private static final String METADATA_TASK = "THORN_ARMOR_TASK";

    @Override
    public String getName() {
        return "THORN_ARMOR";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        // Đây là buff tự thân, tâm hiệu ứng luôn theo vị trí attacker (không phải target)
        if (attacker == null || attacker.isDead() || attacker.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        final double radius = 4.0 + (skillLevel * 0.2);
        final double radiusSquared = radius * radius;
        final double pulsePercent = 12.0 + (skillLevel - 1) * 6.0;
        final double pulseDamage = baseDamage * (pulsePercent / 100.0);
        final int resistanceAmplifier = Math.min(2, skillLevel / 4);

        // Hủy task THORN_ARMOR cũ nếu attacker recast, tránh 2 task cùng chạy
        // (2 task cộng dồn sẽ gây pulse gấp đôi và không bao giờ dừng đúng lúc)
        if (attacker.hasMetadata(METADATA_TASK)) {
            Object old = attacker.getMetadata(METADATA_TASK).get(0).value();
            if (old instanceof BukkitRunnable oldTask) {
                oldTask.cancel();
            }
        }

        attacker.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE, 100, resistanceAmplifier, false, false, true));

        BukkitRunnable task = new BukkitRunnable() {

            int pulsesLeft = 5; // 5 lần, mỗi 20 tick = 5 giây tổng

            @Override
            public void run() {

                if (attacker.isDead() || !attacker.isValid() || pulsesLeft <= 0) {
                    cleanup();
                    this.cancel();
                    return;
                }

                Location center = attacker.getLocation();
                if (center.getWorld() == null) {
                    cleanup();
                    this.cancel();
                    return;
                }

                // Vòng gai hiển thị quanh attacker
                int points = 32;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location p = center.clone().add(x, 0.1, z);
                    p.getWorld().spawnParticle(
                            Particle.DUST,
                            p,
                            1,
                            new Particle.DustOptions(Color.fromRGB(120, 60, 20), 1.2f)
                    );
                }

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;

                    if (entity.getLocation().distanceSquared(center) <= radiusSquared) {
                        entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                        entity.damage(pulseDamage, attacker);
                        final LivingEntity ref = entity;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (ref.isValid()) ref.removeMetadata("IS_ABILITY", Main.getInstance());
                            }
                        }.runTaskLater(Main.getInstance(), 1L);
                    }
                }

                pulsesLeft--;
            }

            private void cleanup() {
                if (attacker.isValid()) attacker.removeMetadata(METADATA_TASK, Main.getInstance());
            }
        };

        attacker.setMetadata(METADATA_TASK, new FixedMetadataValue(Main.getInstance(), task));
        task.runTaskTimer(Main.getInstance(), 0L, 20L);
    }
}
