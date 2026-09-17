package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
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
 * EARTHQUAKE_SLAM
 * Attacker đập đất tạo chấn động quanh mình, gây sát thương, hất văng và làm choáng
 * (Slowness rất cao trong thời gian ngắn) mọi kẻ địch trong bán kính.
 */
public class EARTHQUAKE_SLAM implements IAbility {

    private static final double MAX_KNOCKBACK_SPEED = 1.0; // Chặn tốc độ hất văng để tránh văng entity quá xa

    @Override
    public String getName() {
        return "EARTHQUAKE_SLAM";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        // Đòn AoE tự thân, tâm là vị trí attacker
        if (attacker == null || attacker.isDead() || attacker.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        final double radius = 4.0 + (skillLevel * 0.3);
        final double radiusSquared = radius * radius;
        final double damagePercent = 28.0 + (skillLevel - 1) * 8.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final Location center = attacker.getLocation().clone();
        if (center.getWorld() == null) return;

        center.getWorld().playSound(center, Sound.ENTITY_RAVAGER_ATTACK, 1.0f, 0.7f);

        org.bukkit.block.data.BlockData blockData = Material.STONE.createBlockData();

        int points = 60;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location p = center.clone().add(x, 0.1, z);
            if (p.getWorld() == null) continue;

            p.getWorld().spawnParticle(Particle.BLOCK, p, 6, 0.15, 0.1, 0.15, 0.0, blockData);
            p.getWorld().spawnParticle(
                    Particle.DUST,
                    p,
                    1,
                    new Particle.DustOptions(Color.fromRGB(150, 120, 80), 1.4f)
            );
        }

        for (LivingEntity entity : center.getWorld().getLivingEntities()) {
            if (entity.isDead() || !entity.isValid()) continue;
            if (entity.equals(attacker)) continue;

            double distSquared = entity.getLocation().distanceSquared(center);
            if (distSquared > radiusSquared) continue;

            entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
            entity.damage(damage, attacker);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (entity.isValid()) entity.removeMetadata("IS_ABILITY", Main.getInstance());
                }
            }.runTaskLater(Main.getInstance(), 1L);

            // Hất văng ra xa tâm, tốc độ giới hạn để tránh bug vật lý
            Vector away = entity.getLocation().toVector().subtract(center.toVector());
            if (away.lengthSquared() < 0.01) away = new Vector(1, 0, 0);
            away.setY(0);
            Vector knockback = away.normalize().multiply(Math.min(MAX_KNOCKBACK_SPEED, 0.6));
            knockback.setY(0.5);
            entity.setVelocity(knockback);

            // Choáng ngắn
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 6, false, false, true));
        }
    }
}
