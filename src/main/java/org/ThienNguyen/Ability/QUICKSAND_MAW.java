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
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/**
 * QUICKSAND_MAW
 * Mặt đất quanh mục tiêu hóa thành cát lún, kéo mục tiêu chìm dần xuống và liên tục gây
 * sát thương trong 3 giây. Ai khác bước vào vùng cũng bị ảnh hưởng. (Loại sát thương liên tục.)
 */
public class QUICKSAND_MAW implements IAbility {

    @Override
    public String getName() {
        return "QUICKSAND_MAW";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (target == null || target.isDead() || target.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Tổng sát thương liên tục: 21% + 7%/level, chia đều 6 lần tick (10 tick/lần = 3 giây)
        final double totalPercent = 21.0 + (skillLevel - 1) * 7.0;
        final double tickDamage = baseDamage * (totalPercent / 6.0 / 100.0);

        final double radius = 2.5 + (skillLevel * 0.1);
        final double radiusSquared = radius * radius;

        final Location center = target.getLocation().clone();
        final Color sandDark = Color.fromRGB(140, 110, 70);
        final Color sandLight = Color.fromRGB(190, 160, 110);

        center.getWorld().playSound(center, Sound.BLOCK_SAND_BREAK, 1.0f, 0.6f);

        new BukkitRunnable() {

            int ticksLeft = 6; // 6 lần, mỗi 10 tick = 3 giây tồn tại
            double sinkDepth = 0.0;

            @Override
            public void run() {

                if (center.getWorld() == null || ticksLeft <= 0) {
                    this.cancel();
                    return;
                }

                // Phủ full hạt kín toàn bộ hình tròn (cả viền và bên trong diện tích)
                int innerParticles = 35;
                ThreadLocalRandom random = ThreadLocalRandom.current();

                for (int i = 0; i < innerParticles; i++) {
                    // Dùng phân bố ngẫu nhiên đều trong hình tròn (Polar coordinate offset)
                    double r = radius * Math.sqrt(random.nextDouble());
                    double theta = random.nextDouble() * 2 * Math.PI;

                    double offsetX = r * Math.cos(theta);
                    double offsetZ = r * Math.sin(theta);

                    Location p = center.clone().add(offsetX, 0.05, offsetZ);
                    if (p.getWorld() != center.getWorld()) continue;

                    // Đan xen màu đậm nhạt tạo hiệu ứng cát lún chân thực
                    Color chosenColor = random.nextBoolean() ? sandDark : sandLight;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(chosenColor, 1.1f)
                    );
                }

                // Viền ngoài sắc nét hơn để định hình vùng lún
                int borderPoints = 30;
                for (int i = 0; i < borderPoints; i++) {
                    double angle = (2 * Math.PI * i) / borderPoints;
                    Location p = center.clone().add(Math.cos(angle) * radius, 0.05, Math.sin(angle) * radius);
                    if (p.getWorld() != center.getWorld()) continue;
                    p.getWorld().spawnParticle(
                            Particle.DUST, p, 1,
                            new Particle.DustOptions(sandDark, 1.3f)
                    );
                }

                sinkDepth = Math.min(0.5, sinkDepth + 0.08);

                for (LivingEntity entity : center.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (entity.getLocation().distanceSquared(center) > radiusSquared) continue;

                    entity.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    entity.damage(tickDamage, attacker);
                    final LivingEntity ref = entity;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (ref.isValid()) ref.removeMetadata("IS_ABILITY", Main.getInstance());
                        }
                    }.runTaskLater(Main.getInstance(), 1L);

                    // Kéo nhẹ xuống dưới (cảm giác đang bị lún), giới hạn để tránh kéo xuyên sàn quá sâu
                    Vector v = entity.getVelocity();
                    entity.setVelocity(new Vector(v.getX() * 0.6, -0.12, v.getZ() * 0.6));

                    entity.addPotionEffect(new PotionEffect(
                            PotionEffectType.SLOWNESS, 15, 2, false, false, true));

                    entity.getWorld().spawnParticle(
                            Particle.DUST, entity.getLocation(), 6, 0.2, 0.05, 0.2, 0,
                            new Particle.DustOptions(sandDark, 1.0f)
                    );
                }

                ticksLeft--;
            }

        }.runTaskTimer(Main.getInstance(), 0L, 10L);
    }
}