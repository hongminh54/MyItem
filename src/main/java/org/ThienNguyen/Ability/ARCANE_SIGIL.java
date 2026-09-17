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
 * ARCANE_SIGIL
 * Vẽ một đại pháp trận: 3 vòng tròn đồng tâm xoay ngược chiều nhau, bên trong là ngôi sao
 * 7 cánh nối bằng các đường thẳng. Khi pháp trận hoàn tất, cả trận sáng bùng và gây sát thương.
 */
public class ARCANE_SIGIL implements IAbility {

    private static final int STAR_POINTS = 7;
    private static final int STAR_SKIP = 3; // Nối mỗi đỉnh với đỉnh cách 3 -> sao 7 cánh

    @Override
    public String getName() {
        return "ARCANE_SIGIL";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 20% + 7%/level
        final double damagePercent = 20.0 + (skillLevel - 1) * 7.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double sigilRadius = 4.0 + (skillLevel * 0.2);
        final double sigilRadiusSquared = sigilRadius * sigilRadius;

        final Location center = target.getLocation().clone().add(0, 0.1, 0);

        final Color outerColor = Color.fromRGB(255, 200, 60);
        final Color midColor = Color.fromRGB(180, 120, 255);
        final Color innerColor = Color.fromRGB(100, 220, 255);

        center.getWorld().playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.2f, 0.7f);

        new BukkitRunnable() {

            int ticks = 0;
            final int drawTicks = 30;
            final int totalTicks = 38;

            @Override
            public void run() {

                if (center.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                if (ticks < drawTicks) {

                    double progress = (double) ticks / drawTicks;
                    double spin = ticks * 0.12;

                    // 3 vòng đồng tâm, vòng giữa xoay ngược chiều
                    drawRing(sigilRadius, spin, outerColor, progress, 1.4f);
                    drawRing(sigilRadius * 0.72, -spin * 1.5, midColor, progress, 1.2f);
                    drawRing(sigilRadius * 0.45, spin * 2.0, innerColor, progress, 1.1f);

                    // Ngôi sao 7 cánh bên trong, hiện dần
                    if (progress > 0.35) {
                        drawStar(sigilRadius * 0.72, -spin * 1.5, midColor,
                                (progress - 0.35) / 0.65);
                    }

                    // Các cột sáng dựng lên ở 7 đỉnh khi gần hoàn tất
                    if (progress > 0.7) {
                        double pillarHeight = 2.5 * ((progress - 0.7) / 0.3);
                        for (int i = 0; i < STAR_POINTS; i++) {
                            double a = (2 * Math.PI * i) / STAR_POINTS - spin * 1.5;
                            double px = Math.cos(a) * sigilRadius * 0.72;
                            double pz = Math.sin(a) * sigilRadius * 0.72;
                            for (double y = 0; y <= pillarHeight; y += 0.25) {
                                Location p = center.clone().add(px, y, pz);
                                if (p.getWorld() != center.getWorld()) continue;
                                p.getWorld().spawnParticle(
                                        Particle.DUST, p, 1,
                                        new Particle.DustOptions(outerColor, 1.1f)
                                );
                            }
                        }
                    }

                } else if (ticks == drawTicks) {
                    activate();
                } else {
                    // Pháp trận tan dần
                    double fade = 1.0 - ((double) (ticks - drawTicks) / (totalTicks - drawTicks));
                    drawRing(sigilRadius * fade, ticks * 0.3, outerColor, 1.0, 1.3f);
                }

                ticks++;
            }

            /** Vẽ một vòng tròn; progress điều khiển phần cung đã vẽ xong. */
            private void drawRing(double r, double rotation, Color color, double progress, float size) {
                int points = Math.max(30, (int) (r * 16));
                int drawn = Math.max(1, (int) (points * Math.min(1.0, progress * 1.4)));

                for (int i = 0; i < drawn; i++) {
                    double a = (2 * Math.PI * i) / points + rotation;
                    Location p = center.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(color, size)
                    );
                }
            }

            /** Vẽ ngôi sao 7 cánh bằng cách nối các đỉnh cách nhau STAR_SKIP bước. */
            private void drawStar(double r, double rotation, Color color, double progress) {
                for (int i = 0; i < STAR_POINTS; i++) {
                    int j = (i + STAR_SKIP) % STAR_POINTS;

                    double a1 = (2 * Math.PI * i) / STAR_POINTS + rotation;
                    double a2 = (2 * Math.PI * j) / STAR_POINTS + rotation;

                    double x1 = Math.cos(a1) * r, z1 = Math.sin(a1) * r;
                    double x2 = Math.cos(a2) * r, z2 = Math.sin(a2) * r;

                    double limit = Math.min(1.0, progress);
                    for (double t = 0; t <= limit; t += 0.06) {
                        Location p = center.clone().add(
                                x1 + (x2 - x1) * t, 0.02, z1 + (z2 - z1) * t);
                        if (p.getWorld() != center.getWorld()) continue;
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(color, 1.0f)
                        );
                    }
                }
            }

            private void activate() {
                center.getWorld().playSound(center, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.4f, 0.8f);
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 1.4f);

                // Bùng sáng: cột sáng dựng lên toàn trận
                for (int i = 0; i < 90; i++) {
                    double a = (2 * Math.PI * i) / 90;
                    double r = sigilRadius * (0.3 + Math.random() * 0.7);
                    for (double y = 0; y <= 4.5; y += 0.5) {
                        Location p = center.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                        if (p.getWorld() != center.getWorld()) continue;
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(
                                        y > 2.5 ? innerColor : outerColor, 1.3f)
                        );
                    }
                }
                center.getWorld().spawnParticle(Particle.FLASH, center.clone().add(0, 1, 0), 3, 0, 0, 0, 0);
                center.getWorld().spawnParticle(Particle.ENCHANT,
                        center.clone().add(0, 2, 0), 120, 2.0, 1.5, 2.0, 1.0);

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;

                    Location eLoc = entity.getLocation();
                    if (Math.abs(eLoc.getY() - center.getY()) > 3.0) continue;

                    double dx = eLoc.getX() - center.getX();
                    double dz = eLoc.getZ() - center.getZ();
                    if ((dx * dx) + (dz * dz) > sigilRadiusSquared) continue;

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
