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

/**
 * STATIC_OVERLOAD
 * Một trường tĩnh điện bao phủ khu vực quanh mục tiêu trong 3 giây, liên tục phóng tia điện
 * ngẫu nhiên gây sát thương cho bất kỳ ai trong vùng ảnh hưởng. (Loại sát thương liên tục.)
 */
public class STATIC_OVERLOAD implements IAbility {

    @Override
    public String getName() {
        return "STATIC_OVERLOAD";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Tổng sát thương liên tục: 26% + 9%/level, chia đều 6 lần phóng điện
        final double totalPercent = 26.0 + (skillLevel - 1) * 9.0;
        final double tickDamage = baseDamage * (totalPercent / 6.0 / 100.0);

        final double radius = 3.2 + (skillLevel * 0.12);
        final double radiusSquared = radius * radius;

        final Location center = target.getLocation().clone();
        final Color electricBlue = Color.fromRGB(140, 210, 255);

        center.getWorld().playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.2f, 1.8f);

        new BukkitRunnable() {

            int ticksLeft = 6; // 6 lần, mỗi 10 tick = 3 giây tồn tại

            @Override
            public void run() {

                if (center.getWorld() == null || ticksLeft <= 0) {
                    this.cancel();
                    return;
                }

                // Vòng tĩnh điện lởn vởn tại biên
                int points = 26;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    double jitter = (Math.random() - 0.5) * 0.3;
                    Location p = center.clone().add(
                            Math.cos(angle) * (radius + jitter), 0.2, Math.sin(angle) * (radius + jitter));
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(electricBlue, 1.1f)
                    );
                }

                // Vài tia điện ngẫu nhiên bật lên trong vùng
                for (int i = 0; i < 3; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * radius;
                    Location base = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                    for (double y = 0; y <= 1.5; y += 0.25) {
                        Location p = base.clone().add(
                                (Math.random() - 0.5) * 0.2, y, (Math.random() - 0.5) * 0.2);
                        if (p.getWorld() != center.getWorld()) continue;
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f)
                        );
                    }
                }

                boolean anyHit = false;
                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (entity.getLocation().distanceSquared(center) > radiusSquared) continue;

                    anyHit = true;
                    entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    entity.damage(tickDamage, attacker);
                    final LivingEntity ref = entity;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (ref.isValid()) ref.removeMetadata("IS_ABILITY", Main.getInstance());
                        }
                    }.runTaskLater(Main.getInstance(), 1L);

                    entity.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            entity.getLocation().add(0, 1, 0), 12, 0.25, 0.4, 0.25, 0.2);
                }

                if (anyHit) {
                    center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.8f);
                }

                ticksLeft--;
            }

        }.runTaskTimer(Main.getInstance(), 0L, 10L);
    }
}
