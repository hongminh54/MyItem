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
 * FROST_LANCE
 * Một mũi băng thạch dựng lên từ dưới đất xuyên qua mục tiêu, sau đó vỡ ra thành 4 đợt
 * sóng băng đồng tâm co giãn liên tiếp, mỗi đợt gây 1 lần sát thương trong bán kính nhỏ.
 */
public class FROST_LANCE implements IAbility {

    private static final int HITS = 4;
    private static final double BASE_PERCENT = 18.0;
    private static final double PERCENT_PER_LEVEL = 12.0;

    @Override
    public String getName() {
        return "FROST_LANCE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);
        final double damagePercent = BASE_PERCENT + (skillLevel - 1) * PERCENT_PER_LEVEL;
        final double hitDamage = baseDamage * (damagePercent / 100.0);

        final double radius = 2.2;
        final double radiusSquared = radius * radius;

        final Location center = target.getLocation().clone().add(0, 0.1, 0);
        final Color iceColor = Color.fromRGB(170, 225, 255);
        final Color coreColor = Color.fromRGB(220, 250, 255);

        // Tick khi mỗi đợt sóng bùng nổ và gây sát thương
        final int[] pulseTicks = {6, 12, 18, 24};
        final int totalTicks = 28;

        center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.6f);

        new BukkitRunnable() {

            int ticks = 0;
            int pulseIndex = 0;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                // Mũi băng dựng lên nhanh trong 6 tick đầu
                if (ticks < 6) {
                    double height = 3.0 * (ticks / 6.0);
                    for (double y = 0; y <= height; y += 0.3) {
                        Location p = center.clone().add(0, y, 0);
                        if (p.getWorld() != center.getWorld()) continue;
                        p.getWorld().spawnParticle(Particle.SNOWFLAKE, p, 2, 0.15, 0.05, 0.15, 0.01);
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(iceColor, 1.2f)
                        );
                    }
                }

                // Đợt sóng băng lan ra theo vòng tròn giữa các lần bùng nổ
                double waveProgress = (ticks % 6) / 6.0;
                double waveRadius = radius * waveProgress;
                int points = 26;
                for (int i = 0; i < points; i++) {
                    double a = (2 * Math.PI * i) / points;
                    Location p = center.clone().add(Math.cos(a) * waveRadius, 0, Math.sin(a) * waveRadius);
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(iceColor, 1.0f)
                    );
                }

                // Bùng nổ băng gây sát thương tại các mốc pulseTicks
                if (pulseIndex < pulseTicks.length && ticks == pulseTicks[pulseIndex]) {
                    burst();
                    pulseIndex++;
                }

                ticks++;
            }

            private void burst() {
                center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1.1f, 1.9f);
                for (int i = 0; i < 40; i++) {
                    double a = (2 * Math.PI * i) / 40;
                    Location p = center.clone().add(Math.cos(a) * radius * 0.6, 0.3, Math.sin(a) * radius * 0.6);
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(coreColor, 1.4f)
                    );
                }
                center.getWorld().spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 0.5, 0), 25, 0.6, 0.5, 0.6, 0.02);

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;

                    Location eLoc = entity.getLocation();
                    if (Math.abs(eLoc.getY() - center.getY()) > 2.5) continue;

                    double dx = eLoc.getX() - center.getX();
                    double dz = eLoc.getZ() - center.getZ();
                    if ((dx * dx) + (dz * dz) > radiusSquared) continue;

                    entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    entity.damage(hitDamage, attacker);
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
