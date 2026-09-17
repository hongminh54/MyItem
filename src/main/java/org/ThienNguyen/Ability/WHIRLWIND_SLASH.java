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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * WHIRLWIND_SLASH
 * Attacker xoay tròn 360 độ với lưỡi kiếm sáng vẽ thành một vòng xoáy hoàn chỉnh quanh mình,
 * chém trúng mọi kẻ địch trong tầm. Sát thương 1 lần, tâm luôn là attacker.
 */
public class WHIRLWIND_SLASH implements IAbility {

    @Override
    public String getName() {
        return "WHIRLWIND_SLASH";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (attacker == null || attacker.isDead() || attacker.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 25% + 8%/level
        final double damagePercent = 25.0 + (skillLevel - 1) * 8.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double slashRadius = 3.0 + (skillLevel * 0.12);
        final double slashRadiusSquared = slashRadius * slashRadius;

        final Set<UUID> hitEntities = new HashSet<>();

        attacker.getWorld().playSound(attacker.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.2f);

        new BukkitRunnable() {

            int ticks = 0;
            final int totalTicks = 10; // 2 vòng quay đầy trong nửa giây

            @Override
            public void run() {

                if (attacker.isDead() || !attacker.isValid() || attacker.getWorld() == null
                        || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                Location center = attacker.getLocation();
                double spin = (ticks * (2 * Math.PI * 2)) / totalTicks;

                // Vệt lưỡi kiếm quét theo cung hiện tại (chỉ vẽ đoạn vừa quét qua để có cảm giác "chém")
                double arcSpan = Math.PI * 0.6;
                int arcPoints = 24;
                for (int i = 0; i <= arcPoints; i++) {
                    double angle = spin - (arcSpan * i / arcPoints);
                    Location p = center.clone().add(
                            Math.cos(angle) * slashRadius, 1.0, Math.sin(angle) * slashRadius);
                    if (p.getWorld() != center.getWorld()) continue;

                    float alpha = (float) (1.5 - (1.5 * i / arcPoints));
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(Color.fromRGB(210, 230, 255), alpha)
                    );
                }
                center.getWorld().spawnParticle(Particle.SWEEP_ATTACK,
                        center.clone().add(Math.cos(spin) * slashRadius * 0.6, 1.0, Math.sin(spin) * slashRadius * 0.6),
                        1, 0, 0, 0, 0);

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (hitEntities.contains(entity.getUniqueId())) continue;
                    if (entity.getLocation().distanceSquared(center) > slashRadiusSquared) continue;

                    hitEntities.add(entity.getUniqueId());

                    entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    entity.damage(damage, attacker);
                    final LivingEntity ref = entity;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (ref.isValid()) ref.removeMetadata("IS_ABILITY", Main.getInstance());
                        }
                    }.runTaskLater(Main.getInstance(), 1L);
                }

                ticks++;
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
