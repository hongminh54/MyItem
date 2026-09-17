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
 * HOLY_LANCE
 * Bầu trời phía trên mục tiêu mở ra, một mũi giáo ánh sáng thần thánh lớn giáng thẳng xuống
 * xuyên qua mục tiêu và nổ tung thành vòng hào quang. Sát thương 1 lần, mạnh nhất pack.
 */
public class HOLY_LANCE implements IAbility {

    @Override
    public String getName() {
        return "HOLY_LANCE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 34% + 13%/level
        final double damagePercent = 34.0 + (skillLevel - 1) * 13.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double aoeRadius = 3.2 + (skillLevel * 0.15);
        final double aoeRadiusSquared = aoeRadius * aoeRadius;
        final double lanceStartHeight = 10.0;

        final Location center = target.getLocation().clone();
        final Color gold = Color.fromRGB(255, 235, 160);
        final Color white = Color.fromRGB(255, 255, 255);

        center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);

        new BukkitRunnable() {

            int ticks = 0;
            final int descendTicks = 8; // Giáng xuống rất nhanh, uy lực bất ngờ
            final int totalTicks = 16;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                if (ticks <= descendTicks) {

                    double progress = (double) ticks / descendTicks;
                    double lanceTipY = center.getY() + lanceStartHeight * (1.0 - progress);
                    double lanceLength = 5.0;

                    // Thân giáo: hình trụ thon với các cạnh phát sáng
                    for (int ring = 0; ring < 3; ring++) {
                        double ringY = lanceTipY + ring * 1.6;
                        double r = 0.4 - ring * 0.08;
                        int points = 10;
                        for (int i = 0; i < points; i++) {
                            double a = (2 * Math.PI * i) / points + ticks * 0.4;
                            Location p = center.clone().add(Math.cos(a) * r, ringY, Math.sin(a) * r);
                            if (p.getWorld() != center.getWorld()) continue;
                            p.getWorld().spawnParticle(
                                    Particle.DUST, p, 1,
                                    new Particle.DustOptions(ring == 0 ? white : gold, 1.3f)
                            );
                        }
                    }

                    // Vệt sáng phía sau mũi giáo
                    for (double y = lanceTipY; y <= lanceTipY + lanceLength; y += 0.4) {
                        Location p = center.clone().add(0, y, 0);
                        if (p.getWorld() != center.getWorld()) continue;
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(gold, 0.9f)
                        );
                    }

                    if (ticks == descendTicks) {
                        impact();
                    }

                } else {
                    center.getWorld().spawnParticle(
                            Particle.DUST, center.clone().add(0, 1, 0), 8, aoeRadius * 0.4, 0.6, aoeRadius * 0.4, 0,
                            new Particle.DustOptions(gold, 1.0f)
                    );
                }

                ticks++;
            }

            private void impact() {
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.5f);
                center.getWorld().playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.6f);

                center.getWorld().spawnParticle(Particle.FLASH, center.clone().add(0, 0.5, 0), 3, 0, 0, 0, 0);

                int points = 80;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    Location p = center.clone().add(
                            Math.cos(angle) * aoeRadius, 0.1, Math.sin(angle) * aoeRadius);
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(gold, 1.4f)
                    );
                }
                center.getWorld().spawnParticle(Particle.END_ROD, center, 60, 0.4, 0.6, 0.4, 0.4);

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (entity.getLocation().distanceSquared(center) > aoeRadiusSquared) continue;

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
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
