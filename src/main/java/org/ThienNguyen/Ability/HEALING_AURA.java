package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * HEALING_AURA
 * Tỏa vùng hồi máu quanh attacker, hồi máu định kỳ cho các Player trong bán kính (kể cả bản thân).
 */
public class HEALING_AURA implements IAbility {

    @Override
    public String getName() {
        return "HEALING_AURA";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        // Buff tự thân/đồng đội, tâm hiệu ứng theo vị trí attacker
        if (attacker == null || attacker.isDead() || attacker.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        final double radius = 5.0 + (skillLevel * 0.25);
        final double radiusSquared = radius * radius;
        final double healPercent = 12.0 + (skillLevel - 1) * 5.0;
        final double healAmount = baseDamage * (healPercent / 100.0);

        new BukkitRunnable() {

            int pulsesLeft = 5; // 5 lần, mỗi 20 tick = 5 giây tổng

            @Override
            public void run() {

                if (attacker.isDead() || !attacker.isValid() || pulsesLeft <= 0) {
                    this.cancel();
                    return;
                }

                Location center = attacker.getLocation();
                if (center.getWorld() == null) {
                    this.cancel();
                    return;
                }

                int points = 24;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location p = center.clone().add(x, 0.1, z);
                    p.getWorld().spawnParticle(
                            Particle.DUST,
                            p,
                            1,
                            new Particle.DustOptions(Color.fromRGB(80, 255, 120), 1.3f)
                    );
                }

                for (org.bukkit.entity.Entity nearby : center.getWorld().getNearbyEntities(
                        center, radius, 3.0, radius)) {

                    if (!(nearby instanceof Player player)) continue;
                    if (player.isDead() || !player.isValid()) continue;
                    if (center.distanceSquared(player.getLocation()) > radiusSquared) continue;

                    double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null
                            ? player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()
                            : 20.0;

                    double newHealth = Math.min(maxHealth, player.getHealth() + healAmount);
                    player.setHealth(newHealth);

                    player.getWorld().spawnParticle(
                            Particle.DUST,
                            player.getLocation().add(0, 1, 0),
                            6, 0.3, 0.5, 0.3, 0,
                            new Particle.DustOptions(Color.fromRGB(80, 255, 120), 1.0f)
                    );
                }

                pulsesLeft--;
            }

        }.runTaskTimer(Main.getInstance(), 0L, 20L);
    }
}
