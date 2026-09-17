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
 * TIDAL_CRESCENT
 * Một lưỡi trăng khuyết bằng nước dâng lên rồi khép vòng quanh mục tiêu như thủy triều,
 * vỗ vào 3 nhịp liên tiếp trước khi tan thành bọt nước.
 */
public class TIDAL_CRESCENT implements IAbility {

    private static final int NUM_HITS = 3;
    private static final int HIT_INTERVAL = 6;

    @Override
    public String getName() {
        return "TIDAL_CRESCENT";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 22% + 15%/level, chia đều cho 3 nhịp
        final double damagePercent = 22.0 + (skillLevel - 1) * 15.0;
        final double totalDamage = baseDamage * (damagePercent / 100.0);
        final double perHitDamage = totalDamage / NUM_HITS;

        final double radius = 2.1 + (skillLevel * 0.04);

        final Location center = target.getLocation().clone().add(0, 0.1, 0);

        final Color deepBlue = Color.fromRGB(20, 100, 200);
        final Color foam = Color.fromRGB(200, 240, 255);

        center.getWorld().playSound(center, Sound.ITEM_BUCKET_EMPTY, 1.0f, 0.8f);

        new BukkitRunnable() {

            int ticks = 0;
            int hitsDone = 0;
            final int totalTicks = (NUM_HITS - 1) * HIT_INTERVAL + 10;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                double swirl = ticks * 0.25;
                drawCrescent(swirl);

                boolean isHitTick = (ticks % HIT_INTERVAL == HIT_INTERVAL - 1) && hitsDone < NUM_HITS;
                if (isHitTick) {
                    pulseHit();
                    hitsDone++;
                }

                ticks++;
            }

            private void drawCrescent(double rotation) {
                // Lưỡi trăng khuyết: cung 240 độ quét quanh, tạo cảm giác thủy triều cuộn
                int points = 40;
                for (int i = 0; i < points; i++) {
                    double frac = (double) i / points;
                    double a = rotation + frac * (Math.PI * 1.33);
                    double wave = Math.sin(ticks * 0.5 + frac * 6) * 0.15;
                    double r = radius + wave;

                    double px = Math.cos(a) * r;
                    double pz = Math.sin(a) * r;
                    double py = 0.1 + Math.abs(Math.sin(frac * Math.PI)) * 0.6;

                    Location p = center.clone().add(px, py, pz);
                    if (p.getWorld() != center.getWorld()) continue;

                    Color c = frac < 0.5 ? deepBlue : foam;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(c, 1.2f)
                    );
                    if (i % 4 == 0) {
                        p.getWorld().spawnParticle(Particle.DRIPPING_WATER, p, 1, 0.05, 0.05, 0.05, 0.0);
                    }
                }
            }

            private void pulseHit() {
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_SPLASH, 1.1f, 1.0f);

                double rSq = radius * radius;
                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;

                    Location eLoc = entity.getLocation();
                    if (Math.abs(eLoc.getY() - center.getY()) > 3.0) continue;

                    double dx = eLoc.getX() - center.getX();
                    double dz = eLoc.getZ() - center.getZ();
                    if ((dx * dx) + (dz * dz) > rSq) continue;

                    entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    entity.damage(perHitDamage, attacker);
                    final LivingEntity ref = entity;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (ref.isValid()) ref.removeMetadata("IS_ABILITY", Main.getInstance());
                        }
                    }.runTaskLater(Main.getInstance(), 1L);

                    entity.getWorld().spawnParticle(
                            Particle.BUBBLE_POP, eLoc.clone().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.02);
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}