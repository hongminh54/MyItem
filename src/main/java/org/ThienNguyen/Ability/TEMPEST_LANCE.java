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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * TEMPEST_LANCE
 * Phóng một ngọn giáo sét xoắn ốc bay thẳng về phía trước, để lại vệt xoắn kép và tia điện
 * nhánh. Xuyên qua nhiều mục tiêu, mỗi entity chỉ trúng 1 lần.
 */
public class TEMPEST_LANCE implements IAbility {

    @Override
    public String getName() {
        return "TEMPEST_LANCE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {

        if (attacker == null || attacker.isDead() || attacker.getWorld() == null) return;

        final int skillLevel = Math.max(1, level);

        // Damage: 32% + 12%/level
        final double damagePercent = 32.0 + (skillLevel - 1) * 12.0;
        final double damage = baseDamage * (damagePercent / 100.0);

        final double maxRange = 14.0 + (skillLevel * 0.3);
        final double hitRadius = 1.4;
        final double speedPerTick = 1.6;

        final Location start = attacker.getEyeLocation().clone();
        final Vector direction = start.getDirection().normalize();
        // Vector vuông góc dùng dựng xoắn ốc quanh trục bay
        final Vector perpA = pickPerpendicular(direction);
        final Vector perpB = direction.clone().crossProduct(perpA).normalize();

        final Set<UUID> hitEntities = new HashSet<>();

        start.getWorld().playSound(start, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.8f);

        new BukkitRunnable() {

            double travelled = 0.0;
            int ticks = 0;

            @Override
            public void run() {

                // Chặn cứng: hết tầm, world null, hoặc quá 40 tick thì dừng
                if (start.getWorld() == null || travelled >= maxRange || ticks++ > 40) {
                    this.cancel();
                    return;
                }

                // Vẽ từng đoạn nhỏ trong quãng bay của tick này cho mượt
                for (double step = 0; step < speedPerTick; step += 0.25) {

                    double d = travelled + step;
                    if (d > maxRange) break;

                    Location tip = start.clone().add(direction.clone().multiply(d));
                    if (tip.getWorld() != start.getWorld()) continue;

                    // Xoắn ốc kép quanh trục
                    double spin = d * 2.4;
                    for (int strand = 0; strand < 2; strand++) {
                        double angle = spin + (strand * Math.PI);
                        double r = 0.45;
                        Vector offset = perpA.clone().multiply(Math.cos(angle) * r)
                                .add(perpB.clone().multiply(Math.sin(angle) * r));
                        Location p = tip.clone().add(offset);
                        p.getWorld().spawnParticle(
                                Particle.DUST, p, 1,
                                new Particle.DustOptions(Color.fromRGB(120, 200, 255), 1.2f)
                        );
                    }

                    // Lõi giáo sáng trắng
                    tip.getWorld().spawnParticle(
                            Particle.DUST, tip, 1,
                            new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.5f)
                    );

                    // Tia điện nhánh ngẫu nhiên bắn ra hai bên
                    if (Math.random() < 0.15) {
                        double branchAngle = Math.random() * 2 * Math.PI;
                        double branchLen = 0.5 + Math.random() * 0.8;
                        for (double bd = 0.2; bd <= branchLen; bd += 0.2) {
                            Vector boff = perpA.clone().multiply(Math.cos(branchAngle) * bd)
                                    .add(perpB.clone().multiply(Math.sin(branchAngle) * bd));
                            Location bp = tip.clone().add(boff);
                            bp.getWorld().spawnParticle(
                                    Particle.DUST, bp, 1,
                                    new Particle.DustOptions(Color.fromRGB(200, 230, 255), 0.9f)
                            );
                        }
                    }

                    checkHit(tip);
                }

                travelled += speedPerTick;
            }

            private void checkHit(Location tip) {
                for (LivingEntity entity : tip.getWorld().getLivingEntities()) {
                    if (entity.isDead() || !entity.isValid()) continue;
                    if (entity.equals(attacker)) continue;
                    if (hitEntities.contains(entity.getUniqueId())) continue;

                    // So với tâm thân entity thay vì chân, cho va chạm tự nhiên hơn
                    Location body = entity.getLocation().clone().add(0, entity.getHeight() / 2, 0);
                    if (body.distanceSquared(tip) > hitRadius * hitRadius) continue;

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

                    entity.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, body, 30, 0.4, 0.5, 0.4, 0.3);
                    entity.getWorld().playSound(body, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.7f, 1.6f);
                }
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    /** Chọn một vector vuông góc ổn định với hướng bay (tránh suy biến khi nhìn thẳng lên/xuống). */
    private Vector pickPerpendicular(Vector dir) {
        Vector reference = Math.abs(dir.getY()) > 0.9
                ? new Vector(1, 0, 0)
                : new Vector(0, 1, 0);
        return dir.clone().crossProduct(reference).normalize();
    }
}
