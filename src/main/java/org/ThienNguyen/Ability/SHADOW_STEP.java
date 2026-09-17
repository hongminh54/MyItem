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
import org.bukkit.util.Vector;

/**
 * SHADOW_STEP
 * Dịch chuyển attacker ra sau lưng mục tiêu rồi tung một đòn đánh bất ngờ, gây Mù + Yếu sức.
 */
public class SHADOW_STEP implements IAbility {

    @Override
    public String getName() {
        return "SHADOW_STEP";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;
        if (attacker == null || !attacker.isValid()) return;

        final int skillLevel = Math.max(1, level);

        final double damagePercent = 25.0 + (skillLevel - 1) * 8.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        Location origin = attacker.getLocation().clone();

        // Điểm đến: ngay sau lưng mục tiêu (ngược hướng nhìn của mục tiêu)
        Vector targetFacing = target.getLocation().getDirection().normalize();
        Location destination = target.getLocation().clone().add(targetFacing.clone().multiply(-1.5));
        destination.setY(target.getLocation().getY());

        // Cho attacker quay mặt lại phía mục tiêu sau khi dịch chuyển
        Vector facingBackToTarget = target.getLocation().toVector().subtract(destination.toVector());
        if (facingBackToTarget.lengthSquared() > 0.0001) {
            destination.setDirection(facingBackToTarget);
        }

        if (destination.getWorld() == null) return;

        // Hiệu ứng khói tại điểm xuất phát
        spawnSmoke(origin);

        attacker.teleport(destination);

        // Hiệu ứng khói tại điểm đến
        spawnSmoke(destination);

        target.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
        target.damage(damage, attacker);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (target.isValid()) target.removeMetadata("IS_ABILITY", Main.getInstance());
            }
        }.runTaskLater(Main.getInstance(), 1L);

        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, false, false, true));
    }

    private void spawnSmoke(Location location) {
        if (location.getWorld() == null) return;
        location.getWorld().spawnParticle(
                Particle.DUST,
                location.clone().add(0, 1, 0),
                20, 0.3, 0.5, 0.3, 0,
                new Particle.DustOptions(Color.fromRGB(60, 0, 90), 1.2f)
        );
    }
}
