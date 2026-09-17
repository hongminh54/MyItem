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
import org.bukkit.util.Vector;

/**
 * SUPERNOVA_CORE
 * Đòn mạnh nhất gói này: một ngôi sao co sập vào tâm (hạt bị hút vào), nén tới hạn rồi nổ
 * siêu tân tinh với nhiều lớp vỏ sóng xung kích lan ra. Sát thương nổ 1 lần, giảm dần theo
 * khoảng cách tới tâm.
 */
public class SUPERNOVA_CORE implements IAbility {

    @Override
    public String getName() {
        return "SUPERNOVA_CORE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 40% + 15%/level (giảm còn 50% ở rìa vụ nổ)
        final double damagePercent = 40.0 + (skillLevel - 1) * 15.0;
        final double maxDamage = baseDamage * (damagePercent / 100.0);

        final double blastRadius = 6.0 + (skillLevel * 0.25);
        final double blastRadiusSquared = blastRadius * blastRadius;

        final Location center = target.getLocation().clone().add(0, 1.2, 0);

        center.getWorld().playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.2f, 0.5f);

        new BukkitRunnable() {

            int ticks = 0;
            final int implodeTicks = 24;   // Co sập
            final int holdTicks = 4;       // Nén tới hạn
            final int explodeAt = implodeTicks + holdTicks;
            final int shellTicks = 12;     // Sóng xung kích lan ra
            final int totalTicks = explodeAt + shellTicks;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                if (ticks < implodeTicks) {

                    // GIAI ĐOẠN 1: hạt bị hút vào tâm theo quỹ đạo xoắn
                    double progress = (double) ticks / implodeTicks;
                    double shellRadius = blastRadius * (1.0 - progress * 0.9);
                    double spin = progress * 8.0;

                    int points = 70;
                    double golden = Math.PI * (3 - Math.sqrt(5));
                    for (int i = 0; i < points; i++) {
                        double y = 1 - (i / (double) (points - 1)) * 2;
                        double r = Math.sqrt(Math.max(0, 1 - y * y));
                        double theta = golden * i + spin;

                        Location p = center.clone().add(
                                Math.cos(theta) * r * shellRadius,
                                y * shellRadius * 0.85,
                                Math.sin(theta) * r * shellRadius);
                        if (p.getWorld() != center.getWorld()) continue;

                        // Càng co càng chuyển từ cam sang trắng xanh
                        Color color = progress < 0.5
                                ? Color.fromRGB(255, (int) (120 + progress * 200), 40)
                                : Color.fromRGB(
                                        (int) (255 - (progress - 0.5) * 100),
                                        (int) (220 + (progress - 0.5) * 70),
                                        (int) (100 + (progress - 0.5) * 310));

                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(color, 1.3f)
                        );
                    }

                    // Lõi sáng lên dần
                    center.getWorld().spawnParticle(Particle.END_ROD, center,
                            1 + (int) (progress * 10), 0.1, 0.1, 0.1, 0.02);

                } else if (ticks < explodeAt) {

                    // GIAI ĐOẠN 2: nén tới hạn, lõi rung và sáng trắng
                    center.getWorld().spawnParticle(Particle.FLASH, center, 1, 0, 0, 0, 0);
                    center.getWorld().spawnParticle(
                            Particle.DUST, center, 30, 0.35, 0.35, 0.35, 0,
                            new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.8f)
                    );

                } else if (ticks == explodeAt) {
                    detonate();

                } else {
                    // GIAI ĐOẠN 3: các lớp vỏ sóng xung kích lan ra
                    int shellTick = ticks - explodeAt;
                    double progress = (double) shellTick / shellTicks;

                    // 3 lớp vỏ lệch pha nhau
                    for (int shell = 0; shell < 3; shell++) {
                        double shellProgress = progress - (shell * 0.12);
                        if (shellProgress <= 0) continue;

                        double r = blastRadius * shellProgress;
                        int points = Math.min(120, Math.max(40, (int) (r * 16)));

                        Color color = switch (shell) {
                            case 0 -> Color.fromRGB(255, 255, 220);
                            case 1 -> Color.fromRGB(255, 170, 60);
                            default -> Color.fromRGB(255, 80, 30);
                        };

                        for (int i = 0; i < points; i++) {
                            double a = (2 * Math.PI * i) / points;
                            // Vỏ hơi dẹt theo trục Y
                            Location p = center.clone().add(
                                    Math.cos(a) * r,
                                    Math.sin(a * 2) * 0.3,
                                    Math.sin(a) * r);
                            if (p.getWorld() != center.getWorld()) continue;
                            p.getWorld().spawnParticle(
                                    Particle.DUST, p, 1,
                                    new Particle.DustOptions(color, 1.4f)
                            );
                        }
                    }
                }

                ticks++;
            }

            private void detonate() {
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.4f);
                center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.2f, 0.7f);

                center.getWorld().spawnParticle(Particle.FLASH, center, 5, 0.2, 0.2, 0.2, 0);
                center.getWorld().spawnParticle(Particle.EXPLOSION, center, 6, 1.0, 1.0, 1.0, 0);
                center.getWorld().spawnParticle(Particle.END_ROD, center, 120, 0.5, 0.5, 0.5, 0.9);
                center.getWorld().spawnParticle(Particle.FLAME, center, 90, 1.0, 1.0, 1.0, 0.5);

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;

                    double distSquared = entity.getLocation().distanceSquared(center);
                    if (distSquared > blastRadiusSquared) continue;

                    // Sát thương giảm dần: 100% ở tâm -> 50% ở rìa
                    double falloff = 1.0 - (Math.sqrt(distSquared) / blastRadius) * 0.5;
                    double finalDamage = maxDamage * falloff;

                    entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    entity.damage(finalDamage, attacker);
                    final LivingEntity ref = entity;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (ref.isValid()) ref.removeMetadata("IS_ABILITY", Main.getInstance());
                        }
                    }.runTaskLater(Main.getInstance(), 1L);

                    // Hất văng ra xa tâm, tốc độ có giới hạn
                    Vector away = entity.getLocation().toVector().subtract(center.toVector());
                    if (away.lengthSquared() < 0.01) away = new Vector(1, 0, 0);
                    away.setY(0);
                    Vector knock = away.normalize().multiply(0.7);
                    knock.setY(0.45);
                    entity.setVelocity(knock);
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
