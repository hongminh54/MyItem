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

/**
 * CORRUPTED_GROUND
 * Phủ một vùng đất bị hủ hóa tại vị trí mục tiêu, tồn tại 4 giây. Entity đứng trong vùng
 * bị bào mòn máu liên tục và dính Yếu sức. (Loại sát thương liên tục.)
 */
public class CORRUPTED_GROUND implements IAbility {

    @Override
    public String getName() {
        return "CORRUPTED_GROUND";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Tổng sát thương liên tục: 25% + 8%/level, chia đều 4 lần tick (1 giây/lần)
        final double totalPercent = 25.0 + (skillLevel - 1) * 8.0;
        final double tickDamage = baseDamage * (totalPercent / 4.0 / 100.0);

        final double radius = 3.5 + (skillLevel * 0.15);
        final double radiusSquared = radius * radius;

        final Location center = target.getLocation().clone();

        center.getWorld().playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.6f);

        new BukkitRunnable() {

            int ticksLeft = 4; // 4 lần, mỗi 20 tick = 4 giây tồn tại

            @Override
            public void run() {

                if (center.getWorld() == null || ticksLeft <= 0) {
                    this.cancel();
                    return;
                }

                // Vòng ranh giới vùng hủ hóa
                int points = 30;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location p = center.clone().add(x, 0.1, z);
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(Color.fromRGB(80, 20, 90), 1.2f)
                    );
                }
                // Sương độc bốc lên bên trong vùng
                center.getWorld().spawnParticle(
                        Particle.DUST, center, 20, radius * 0.6, 0.3, radius * 0.6, 0,
                        new Particle.DustOptions(Color.fromRGB(50, 10, 60), 0.9f)
                );

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (entity.getLocation().distanceSquared(center) > radiusSquared) continue;

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
                            PotionEffectType.WEAKNESS, 30, 0, false, false, true));

                    entity.getWorld().spawnParticle(
                            Particle.DUST, entity.getLocation().add(0, 1, 0), 8, 0.2, 0.3, 0.2, 0,
                            new Particle.DustOptions(Color.fromRGB(80, 20, 90), 1.0f)
                    );
                }

                ticksLeft--;
            }

        }.runTaskTimer(Main.getInstance(), 0L, 20L);
    }
}
