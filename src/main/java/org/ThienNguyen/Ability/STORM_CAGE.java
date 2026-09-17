package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * STORM_CAGE
 * 4 cột sét xoay quanh mục tiêu tạo thành lồng giam, phóng điện vào mục tiêu liên tục
 * trong 3 giây và giữ chân mục tiêu tại chỗ. (Loại sát thương liên tục, chỉ nhắm 1 mục tiêu.)
 */
public class STORM_CAGE implements IAbility {

    private static final int PILLAR_COUNT = 4;

    @Override
    public String getName() {
        return "STORM_CAGE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Tổng sát thương liên tục: 27% + 9%/level, chia đều 4 lần phóng điện
        final double totalPercent = 27.0 + (skillLevel - 1) * 9.0;
        final double pulseDamage = baseDamage * (totalPercent / 4.0 / 100.0);

        final double cageRadius = 1.8;
        final double pillarHeight = 3.0;

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.6f);

        new BukkitRunnable() {

            int ticks = 0;
            final int totalTicks = 60; // 3 giây
            int pulsesLeft = 4;

            @Override
            public void run() {

                if (target.isDead() || !target.isValid() || target.getWorld() == null || ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                Location center = target.getLocation();
                double spin = ticks * 0.25;

                // Giữ chân mục tiêu tại chỗ (không cho di chuyển ngang)
                target.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS, 6, 6, false, false, true));

                for (int i = 0; i < PILLAR_COUNT; i++) {
                    double angle = (2 * Math.PI * i) / PILLAR_COUNT + spin;
                    double px = Math.cos(angle) * cageRadius;
                    double pz = Math.sin(angle) * cageRadius;

                    for (double y = 0; y <= pillarHeight; y += 0.3) {
                        Location p = center.clone().add(px, y, pz);
                        if (p.getWorld() != center.getWorld()) continue;
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(Color.fromRGB(150, 190, 255), 1.2f)
                        );
                    }

                    // Tia điện nối cột này với mục tiêu ở giữa
                    Location pillarTop = center.clone().add(px, pillarHeight * 0.5, pz);
                    Location targetMid = center.clone().add(0, pillarHeight * 0.5, 0);
                    for (double t = 0; t <= 1.0; t += 0.15) {
                        Location arc = pillarTop.clone().add(
                                (targetMid.getX() - pillarTop.getX()) * t, 0,
                                (targetMid.getZ() - pillarTop.getZ()) * t);
                        if (arc.getWorld() != center.getWorld()) continue;
                        arc.getWorld().spawnParticle(
                                Particle.DUST, arc, 1,
                                new Particle.DustOptions(Color.fromRGB(220, 235, 255), 1.0f)
                        );
                    }
                }

                // Phóng điện định kỳ vào mục tiêu: mỗi 15 tick, 4 lần trong 60 tick
                if (ticks % 15 == 0 && pulsesLeft > 0) {
                    target.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    target.damage(pulseDamage, attacker);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (target.isValid()) target.removeMetadata("IS_ABILITY", Main.getInstance());
                        }
                    }.runTaskLater(Main.getInstance(), 1L);

                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            center.clone().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0.3);
                    center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.7f, 1.7f);
                    pulsesLeft--;
                }

                ticks++;
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
