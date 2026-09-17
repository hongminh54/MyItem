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
 * CRIMSON_LOTUS
 * Một đóa sen đỏ thẫm nở bung quanh mục tiêu, từng lớp cánh hoa xòe ra và khép lại,
 * mỗi lần khép cánh gây một nhịp sát thương. Tổng cộng 4 nhịp liên tiếp trong phạm vi hẹp.
 */
public class CRIMSON_LOTUS implements IAbility {

    private static final int PETALS = 10;
    private static final int NUM_HITS = 4;
    private static final int HIT_INTERVAL = 6; // ticks giữa mỗi nhịp
    private static final int FIRST_HIT = 6;

    @Override
    public String getName() {
        return "CRIMSON_LOTUS";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 16% + 12%/level, chia đều cho 4 nhịp
        final double damagePercent = 16.0 + (skillLevel - 1) * 12.0;
        final double totalDamage = baseDamage * (damagePercent / 100.0);
        final double perHitDamage = totalDamage / NUM_HITS;

        final double radius = 1.9 + (skillLevel * 0.05);

        final Location center = target.getLocation().clone().add(0, 0.1, 0);

        final Color petalOuter = Color.fromRGB(220, 20, 60);
        final Color petalInner = Color.fromRGB(255, 105, 180);
        final Color coreColor = Color.fromRGB(255, 230, 240);

        center.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.6f);

        new BukkitRunnable() {

            int ticks = 0;
            int hitsDone = 0;
            final int totalTicks = FIRST_HIT + (NUM_HITS - 1) * HIT_INTERVAL + 8;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                // Chu kỳ nở - khép của từng nhịp
                int cyclePos = (ticks < FIRST_HIT) ? ticks : (ticks - FIRST_HIT) % HIT_INTERVAL;
                double cycleProgress = Math.min(1.0, (double) cyclePos / (HIT_INTERVAL - 1));
                double bloom = Math.sin(cycleProgress * Math.PI); // 0 -> 1 -> 0 (nở rồi khép)

                drawLotus(bloom);

                boolean isHitTick = (ticks == FIRST_HIT) ||
                        (ticks > FIRST_HIT && (ticks - FIRST_HIT) % HIT_INTERVAL == 0);

                if (isHitTick && hitsDone < NUM_HITS) {
                    pulseHit();
                    hitsDone++;
                }

                ticks++;
            }

            private void drawLotus(double bloom) {
                double spread = radius * bloom;

                for (int layer = 0; layer < 2; layer++) {
                    double layerRadius = spread * (layer == 0 ? 1.0 : 0.6);
                    Color color = layer == 0 ? petalOuter : petalInner;

                    for (int i = 0; i < PETALS; i++) {
                        double a = (2 * Math.PI * i) / PETALS;
                        for (double t = 0; t <= 1.0; t += 0.12) {
                            double petalR = layerRadius * Math.sin(t * Math.PI);
                            double px = Math.cos(a) * petalR * (0.4 + t * 0.6);
                            double pz = Math.sin(a) * petalR * (0.4 + t * 0.6);
                            double py = 0.05 + t * 0.25 * bloom;
                            Location p = center.clone().add(px, py, pz);
                            if (p.getWorld() != center.getWorld()) continue;
                            p.getWorld().spawnParticle(
                                    Particle.DUST, p, 1,
                                    new Particle.DustOptions(color, 1.0f)
                            );
                        }
                    }
                }

                // Nhụy hoa ở tâm
                Location coreP = center.clone().add(0, 0.15 + bloom * 0.1, 0);
                if (coreP.getWorld() == center.getWorld()) {
                    coreP.getWorld().spawnParticle(
                            Particle.DUST, coreP, 3, 0.08, 0.08, 0.08,
                            new Particle.DustOptions(coreColor, 0.9f)
                    );
                }
            }

            private void pulseHit() {
                center.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.3f);
                center.getWorld().spawnParticle(Particle.FLASH, center.clone().add(0, 0.3, 0), 1, 0, 0, 0, 0);

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
                            Particle.DUST, eLoc.clone().add(0, 1, 0), 10, 0.3, 0.4, 0.3,
                            new Particle.DustOptions(petalOuter, 1.1f)
                    );
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}