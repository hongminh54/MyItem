package org.ThienNguyen.Listener.Passive.Editor;

import org.ThienNguyen.Listener.Passive.Editor.ActionSchema.Category;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.ThienNguyen.Listener.Passive.Editor.FieldKind.*;

/**
 * Registry schema cho toàn bộ 31 action của passive (khớp MechanicRegistry).
 * <ul>
 *   <li>Sát thương / hỗ trợ / di chuyển / thế giới / hiển thị: DAMAGE, EFFECT, BUFF_STAT, HEAL, FLAME, LAUNCH, EXPLODE,
 *       SUMMON_TNT, DROP_ITEM, COMMAND, MESSAGE, SOUND, PARTICLE_ANIMATION, PARTICLE_PROJECTILE, BREAK_AREA.</li>
 *   <li>Container / biến / đếm: DELAY, REPEAT, HIT_COUNTER, STACK_COUNTER, ADD_VALUE, CHECK_VALUE, TARGET_FILTER, REVIVE.</li>
 *   <li>Triệu hồi / trạng thái / tích hợp: SUMMON_VANILLA, SUMMON_MM, TITLE, STATUS, MYTHIC_SKILL, LIGHTNING.</li>
 *   <li>Đạn / vùng thuốc: PROJECTILE_SHOT, POTION_ZONE (đọc từ source mechanic).</li>
 * </ul>
 */
public final class ActionSchemaRegistry {

    private static final Map<String, ActionSchema> SCHEMAS = new LinkedHashMap<>();

    /** Danh sách stat hợp lệ của BUFF_STAT. */
    private static final String[] STATS = {
            "damage", "pve_damage", "pvp_damage", "all_damage", "bow_damage", "magic_damage", "true_damage",
            "death_damage", "critical_chance", "critical_damage", "critical_damage_reduction", "lifesteal",
            "penetration", "armor_pen", "accuracy", "damage_reduction", "armor", "pve_defense", "pvp_defense",
            "all_defense", "magic_defense", "dodge_rate", "block_rate", "thorns", "knockback_resistance",
            "max_mana", "mana_regen", "health_regen", "exp_bonus", "movement_speed", "health"
    };

    private ActionSchemaRegistry() {
    }

    // ------------------------------------------------------------------ API

    /** @return schema của type (không phân biệt hoa/thường) hoặc null nếu chưa có. */
    public static ActionSchema get(String type) {
        return type == null ? null : SCHEMAS.get(type.toUpperCase(Locale.ROOT));
    }

    public static boolean has(String type) {
        return get(type) != null;
    }

    public static Collection<ActionSchema> all() {
        return Collections.unmodifiableCollection(SCHEMAS.values());
    }

    public static List<ActionSchema> byCategory(Category category) {
        List<ActionSchema> out = new ArrayList<>();
        for (ActionSchema s : SCHEMAS.values()) {
            if (s.category() == category) out.add(s);
        }
        return out;
    }

    // ------------------------------------------------------------- factories

    private static FieldDef target(String descVN, String def, String... allowed) {
        return FieldDef.of("target", ENUM, def, "Mục tiêu", descVN)
                .withAllowed(allowed).withIcon(Material.ENDER_EYE);
    }

    private static FieldDef expr(String key, String def, String nameVN, String descVN) {
        return FieldDef.of(key, EXPRESSION, def, nameVN, descVN);
    }

    private static FieldDef dbl(String key, double def, String nameVN, String descVN) {
        return FieldDef.of(key, DOUBLE, def, nameVN, descVN);
    }

    private static FieldDef integer(String key, int def, String nameVN, String descVN) {
        return FieldDef.of(key, INT, def, nameVN, descVN).withRange(1.0, null);
    }

    private static FieldDef bool(String key, boolean def, String nameVN, String descVN) {
        return FieldDef.of(key, BOOLEAN, def, nameVN, descVN);
    }

    private static FieldDef enumOf(String key, String def, String nameVN, String descVN, String... allowed) {
        return FieldDef.of(key, ENUM, def, nameVN, descVN).withAllowed(allowed);
    }

    private static FieldDef damageType(String def) {
        return enumOf("damage-type", def, "Loại sát thương",
                "NORMAL = đi qua event sát thương của Bukkit nên bị giáp và hiệu ứng giảm bớt; TRUE = sát thương chuẩn, đặt thẳng máu và bỏ qua giáp.", "NORMAL", "TRUE")
                .withIcon(Material.IRON_CHESTPLATE);
    }

    private static FieldDef children(String key, String nameVN, String descVN) {
        return FieldDef.of(key, ACTION_LIST, null, nameVN, descVN);
    }

    private static void register(ActionSchema s) {
        SCHEMAS.put(s.type(), s);
    }

    // ---------------------------------------------------------------- schemas

    static {
        register(new ActionSchema("DAMAGE", Category.DAMAGE, Material.IRON_SWORD,
                "Gây Sát Thương", "Gây một lượng sát thương lên mục tiêu. TRUE bỏ qua giáp (đặt thẳng máu), NORMAL đi qua hệ thống sát thương Bukkit nên bị giáp giảm. Hợp với hiệu ứng xử tử, trả đũa khi chết.",
                List.of(
                        target("Thực thể nhận sát thương. VICTIM = nạn nhân của sự kiện (mặc định), SELF = chủ passive (tự gây sát thương cho mình).", "VICTIM", "SELF", "VICTIM"),
                        expr("amount", "0", "Lượng sát thương", "Lượng sát thương gây ra. Dùng biểu thức để scale theo cấp (vd \"%player_level% * 3\") hoặc theo đòn vừa đánh ({damage}). Số rất lớn (99999) + TRUE = xử tử.")
                                .withIcon(Material.REDSTONE),
                        damageType("TRUE"))));

        register(new ActionSchema("EFFECT", Category.SUPPORT, Material.POTION,
                "Hiệu Ứng Thuốc", "Gán hiệu ứng thuốc vanilla (Speed, Strength, Poison...) lên mục tiêu. Muốn đổi chỉ số nội bộ của plugin như crit, lifesteal thì dùng Buff Chỉ Số.",
                List.of(
                        target("Thực thể nhận hiệu ứng thuốc. SELF = chủ passive (mặc định), VICTIM = nạn nhân của sự kiện.", "SELF", "SELF", "VICTIM"),
                        FieldDef.of("effect", POTION_EFFECT, null, "Loại hiệu ứng",
                                "Tên PotionEffectType: SPEED, STRENGTH, SLOWNESS, POISON, REGENERATION... Nhận cả tên cũ (INCREASE_DAMAGE, SLOW). Hiệu ứng sai tên sẽ bị bỏ qua và ghi cảnh báo ra console.").asRequired(),
                        expr("seconds", "5", "Thời gian (giây)", "Thời gian hiệu ứng tồn tại, tính bằng giây (mặc định 5).")
                                .withIcon(Material.CLOCK),
                        expr("level", "1", "Cấp độ", "Cấp hiệu ứng: 1 = cấp I, 2 = cấp II... Mặc định 1.")
                                .withIcon(Material.EXPERIENCE_BOTTLE),
                        bool("ambient", false, "Dạng beacon", "true = hạt mờ kiểu beacon. Mặc định tắt."),
                        bool("particles", true, "Hiện hạt", "Tắt (false) để ẩn hoàn toàn hạt của hiệu ứng, hợp với buff nhìn không rối mắt. Mặc định bật."))));

        register(new ActionSchema("BUFF_STAT", Category.SUPPORT, Material.NETHER_STAR,
                "Buff Chỉ Số", "Cộng/trừ tạm thời một chỉ số nội bộ của plugin, hết thời gian thì trả lại. Chỉ tác dụng lên người chơi. Muốn tăng tốc chạy thật sự hãy dùng Hiệu Ứng Thuốc (SPEED).",
                List.of(
                        target("Người được buff/nerf chỉ số. Phải là người chơi, nếu mục tiêu là mob thì không có tác dụng.", "SELF", "SELF", "VICTIM"),
                        enumOf("stat", null, "Chỉ số", "Chỉ số nội bộ cần buff: damage, pve_damage, critical_chance, lifesteal, armor, movement_speed, health... (31 loại). Bấm để chuyển qua danh sách.", STATS).asRequired()
                                .withIcon(Material.ENCHANTED_BOOK),
                        expr("amount", "0", "Lượng buff", "Lượng cộng thêm áp dụng ngay lập tức; số âm = nerf. Khi hết thời gian, đúng lượng này được trừ lại.")
                                .withIcon(Material.REDSTONE),
                        expr("duration-seconds", "5", "Thời gian (giây)", "Số giây trước khi buff bị gỡ và chỉ số về như cũ (mặc định 5).")
                                .withIcon(Material.CLOCK))));

        register(new ActionSchema("HEAL", Category.SUPPORT, Material.GOLDEN_APPLE,
                "Hồi Máu", "Hồi máu cố định hoặc theo % máu tối đa cho mục tiêu.",
                List.of(
                        target("Thực thể được hồi máu. SELF = chủ passive (mặc định), VICTIM = nạn nhân.", "SELF", "SELF", "VICTIM"),
                        expr("amount", "0", "Lượng hồi", "Lượng máu hồi cố định. Bị bỏ qua nếu 'Hồi theo %' lớn hơn 0.")
                                .withIcon(Material.APPLE),
                        expr("percent", "0", "Hồi theo %", "Hồi theo % máu tối đa (vd 10 = hồi 10%). Nếu > 0 sẽ được ưu tiên hơn 'Lượng hồi'.")
                                .withIcon(Material.GLISTERING_MELON_SLICE))));

        register(new ActionSchema("FLAME", Category.DAMAGE, Material.BLAZE_POWDER,
                "Thiêu Đốt", "Đốt mục tiêu: gây sát thương mỗi giây trong suốt thời gian cháy. Có thể ẩn ngọn lửa nhưng sát thương vẫn áp dụng.",
                List.of(
                        target("Thực thể bị đốt. VICTIM = nạn nhân (mặc định), SELF = chủ passive.", "VICTIM", "SELF", "VICTIM"),
                        expr("damage-per-second", "0", "Sát thương/giây", "Sát thương gây ra mỗi giây trong lúc mục tiêu đang cháy.")
                                .withIcon(Material.REDSTONE),
                        expr("duration-seconds", "5", "Thời gian (giây)", "Số giây mục tiêu bị đốt (mặc định 5).")
                                .withIcon(Material.CLOCK),
                        bool("visual-fire", true, "Hiện lửa", "false = ẩn ngọn lửa trên người mục tiêu nhưng sát thương theo giây vẫn được áp dụng. Mặc định hiện lửa."))));

        register(new ActionSchema("LAUNCH", Category.MOVEMENT, Material.FEATHER,
                "Hất Tung", "Hất/đẩy một thực thể theo vector tự do (tiến, ngang, lên) tính từ hướng nhìn của thực thể tham chiếu. Dùng để đẩy lùi, hất tung hoặc lao tới.",
                List.of(
                        target("Thực thể bị hất. Hướng đẩy được tính theo 'Thực thể tham chiếu', không phải hướng nhìn của mục tiêu.", "VICTIM", "SELF", "VICTIM"),
                        enumOf("reference", "SELF", "Thực thể tham chiếu", "Thực thể dùng để tính hướng đẩy: hướng 'tiến' là hướng nhìn của thực thể này. SELF = chủ passive (mặc định), VICTIM = nạn nhân.",
                                "SELF", "VICTIM").withIcon(Material.SPYGLASS),
                        expr("forward", "0", "Lực tiến", "Lực đẩy về phía trước theo hướng nhìn của thực thể tham chiếu. Số âm = đẩy lùi ra sau. Giá trị càng lớn càng xa.")
                                .withIcon(Material.ARROW),
                        expr("side", "0", "Lực ngang", "Lực đẩy theo chiều ngang, vuông góc với hướng nhìn của thực thể tham chiếu.")
                                .withIcon(Material.ARROW),
                        expr("up", "0", "Lực lên", "Lực hất lên trên. Giá trị càng lớn càng bay cao; kết hợp với 'Lực tiến' để tạo cú hất chéo.")
                                .withIcon(Material.FIREWORK_ROCKET),
                        bool("reset-velocity", true, "Ghi đè vận tốc", "true = ghi đè hoàn toàn vận tốc hiện tại bằng vector mới; false = cộng dồn vào vận tốc đang có (hợp khi muốn nhiều cú đẩy chồng nhau)."))));

        register(new ActionSchema("EXPLODE", Category.DAMAGE, Material.TNT,
                "Phát Nổ", "Tạo vụ nổ tại mục tiêu, gây sát thương cho sinh vật xung quanh. Sức nổ vanilla chỉ ảnh hưởng đẩy lùi/âm thanh, sát thương lấy từ ô Sát thương.",
                List.of(
                        target("Tâm vụ nổ: SELF, VICTIM hoặc BLOCK. BLOCK = block vừa phá, chỉ dùng được với trigger ON_BLOCK_BREAK.", "VICTIM", "SELF", "VICTIM", "BLOCK"),
                        expr("power", "4.0", "Sức nổ", "Sức nổ vanilla: chỉ ảnh hưởng đẩy lùi và âm thanh, KHÔNG quyết định sát thương (sát thương lấy từ ô 'Sát thương').")
                                .withIcon(Material.FIRE_CHARGE),
                        expr("radius", "4.0", "Bán kính", "Bán kính tìm sinh vật để gây sát thương quanh tâm vụ nổ.")
                                .withIcon(Material.COMPASS),
                        expr("amount", "0", "Sát thương", "Sát thương áp lên mỗi sinh vật nằm trong bán kính.")
                                .withIcon(Material.REDSTONE),
                        damageType("TRUE"),
                        bool("include-self", false, "Gồm bản thân", "true = chủ passive cũng nhận sát thương nếu đứng trong bán kính. Mặc định tắt (an toàn)."),
                        bool("break-blocks", false, "Phá block", "true = vụ nổ phá block địa hình (vẫn tôn trọng WorldGuard/claim). false = chỉ ảnh hưởng sinh vật."),
                        dbl("particle-scale", 1.0, "Cỡ hạt nổ", "Hệ số nhân kích thước/độ lan của hạt nổ; 1.0 = bình thường, lớn hơn = hoành tráng hơn."))));

        register(new ActionSchema("SUMMON_TNT", Category.DAMAGE, Material.TNT_MINECART,
                "Triệu Hồi TNT", "Tạo một hoặc nhiều TNT đã châm ngòi: rơi từ trên cao, bắn tung toé hoặc xuất hiện tại chỗ. Chủ passive là nguồn gây nổ nên kill-credit tính cho họ.",
                List.of(
                        target("Điểm tạo TNT: SELF/ACTOR = chủ passive, VICTIM = nạn nhân, BLOCK = block vừa phá (chỉ ON_BLOCK_BREAK).", "VICTIM", "SELF", "ACTOR", "VICTIM", "BLOCK"),
                        enumOf("mode", "FIXED", "Chế độ", "FALL = TNT rơi từ trên cao xuống; SHOOT = bắn tung toé ngẫu nhiên ra xung quanh; FIXED = xuất hiện ngay tại chỗ (mặc định).",
                                "FALL", "SHOOT", "FIXED").withIcon(Material.REPEATER),
                        expr("amount", "1", "Số lượng", "Số TNT đã châm ngòi được tạo ra mỗi lần kích hoạt.")
                                .withIcon(Material.CHEST),
                        expr("power", "4.0", "Sức nổ", "Sức nổ vanilla của TNT; chỉ có tác dụng khi bật 'Phá block'.")
                                .withIcon(Material.FIRE_CHARGE)
                                .visibleIf("destroy-blocks", "true"),
                        expr("fuse-ticks", "80", "Ngòi (tick)", "Số tick trước khi TNT nổ: 20 tick = 1 giây, mặc định 80 tick = 4 giây.")
                                .withIcon(Material.CLOCK),
                        bool("destroy-blocks", false, "Phá block", "true = vụ nổ phá địa hình; false = vẫn đẩy lùi và gây sát thương sinh vật nhưng giữ nguyên địa hình."),
                        expr("fall-height", "10.0", "Độ cao rơi", "(FALL) TNT xuất hiện cao hơn điểm gốc bao nhiêu block rồi rơi xuống.")
                                .withIcon(Material.ANVIL)
                                .visibleIf("mode", "FALL"),
                        expr("shoot-speed", "1.0", "Tốc độ bắn", "(SHOOT) Tốc độ phóng của mỗi TNT; càng lớn bay càng xa.")
                                .withIcon(Material.ARROW)
                                .visibleIf("mode", "SHOOT"),
                        expr("shoot-spread-angle", "45.0", "Góc tỏa tối đa", "(SHOOT) Góc phóng lên tối đa (độ), mỗi TNT chọn ngẫu nhiên trong khoảng này. Lớn hơn = tỏa rộng hơn.")
                                .withIcon(Material.BOW)
                                .visibleIf("mode", "SHOOT"),
                        dbl("radius", 1.0, "Bán kính rải", "(FIXED) Bán kính rải TNT quanh điểm gốc khi tạo nhiều hơn 1 quả.")
                                .withIcon(Material.COMPASS)
                                .visibleIf("mode", "FIXED"),
                        expr("damage", "6.0", "Sát thương", "Sát thương gây ra khi TNT phát nổ, áp dụng độc lập với việc có phá block hay không.")
                                .withIcon(Material.REDSTONE),
                        expr("damage-radius", "4.0", "Bán kính sát thương", "Bán kính quanh TNT dùng để tìm sinh vật chịu sát thương.")
                                .withIcon(Material.SPYGLASS),
                        damageType("TRUE"),
                        bool("include-self", false, "Gồm bản thân", "true = chủ passive cũng có thể bị TNT của chính mình gây sát thương."),
                        dbl("particle-scale", 1.5, "Cỡ hạt nổ", "Kích thước hạt của vụ nổ (mặc định 1.5)."))));

        register(new ActionSchema("DROP_ITEM", Category.WORLD, Material.GOLD_NUGGET,
                "Rơi Vật Phẩm", "Làm rơi vật phẩm tại vị trí mục tiêu. Số lượng nhận khoảng (\"1-3\") và biểu thức, hợp với rơi đồ khi hạ mob.",
                List.of(
                        target("Vật phẩm rơi tại vị trí của thực thể này. VICTIM = nạn nhân (mặc định), SELF = chủ passive.", "VICTIM", "SELF", "VICTIM"),
                        FieldDef.of("material", MATERIAL, "GOLD_NUGGET", "Vật phẩm", "Tên Material Bukkit của vật phẩm rơi (vd GOLD_NUGGET, DIAMOND). Mặc định GOLD_NUGGET."),
                        expr("amount", "1", "Số lượng", "Số lượng rơi. Nhận số cố định, khoảng \"1-3\" (random trong khoảng), biểu thức hoặc PAPI; khoảng cũng nhận biểu thức, vd \"1-%player_level%\".")
                                .withIcon(Material.CHEST))));

        register(new ActionSchema("COMMAND", Category.UTILITY, Material.COMMAND_BLOCK,
                "Chạy Lệnh", "Chạy một lệnh tùy ý khi passive kích hoạt. Dùng cho thứ chưa có action riêng: phát vật phẩm, kinh tế, plugin khác. Lệnh viết không có dấu '/'.",
                List.of(
                        target("Thực thể được thay vào {player} trong lệnh (mặc định SELF). {actor} luôn là chủ passive.", "SELF", "SELF", "VICTIM"),
                        enumOf("mode", "OP", "Chế độ chạy", "OP = console chạy lệnh với toàn quyền; PLAYER = mục tiêu tự chạy lệnh, bị giới hạn bởi quyền của họ. Mặc định OP.",
                                "OP", "PLAYER").withIcon(Material.REPEATER),
                        FieldDef.of("command", STRING, null, "Lệnh",
                                "Lệnh cần chạy, KHÔNG có dấu '/'. Placeholder: {player} = tên mục tiêu, {actor} = tên chủ passive, {damage} = sát thương (1 chữ số thập phân).")
                                .asRequired())));

        register(new ActionSchema("MESSAGE", Category.UTILITY, Material.PAPER,
                "Gửi Tin Nhắn", "Gửi tin nhắn riêng vào chat của người chơi, hỗ trợ mã màu & và các placeholder tên/sát thương.",
                List.of(
                        target("Người nhận tin nhắn. Phải là người chơi, nếu không tin sẽ không được gửi.", "SELF", "SELF", "VICTIM"),
                        FieldDef.of("message", STRING, null, "Nội dung",
                                "Nội dung chat, hỗ trợ mã màu & và {actor_name}, {victim_name}, {damage}. Luôn dùng khóa 'message' (không phải 'text').")
                                .asRequired())));

        register(new ActionSchema("SOUND", Category.VISUAL, Material.NOTE_BLOCK,
                "Phát Âm Thanh", "Phát một âm thanh Bukkit tại vị trí mục tiêu.",
                List.of(
                        target("Âm thanh được phát tại vị trí của thực thể này.", "SELF", "SELF", "VICTIM"),
                        FieldDef.of("sound", SOUND, null, "Âm thanh", "Tên Sound của Bukkit (vd ENTITY_PLAYER_LEVELUP, ENTITY_GENERIC_EXPLODE).").asRequired(),
                        dbl("volume", 1.0, "Âm lượng", "Âm lượng phát ra (mặc định 1.0).").withRange(0.0, null),
                        dbl("pitch", 1.0, "Cao độ", "Cao độ từ 0.5 đến 2.0: thấp = trầm, cao = thanh. Mặc định 1.0.").withRange(0.5, 2.0))));

        register(new ActionSchema("PARTICLE_ANIMATION", Category.VISUAL, Material.FIREWORK_STAR,
                "Hoạt Ảnh Hạt", "Vẽ vòng tròn hoặc hình cầu hạt quay quanh mục tiêu trong một khoảng thời gian. Có thể dịch tâm trước/sau, trái/phải và bám theo hướng nhìn của mục tiêu.",
                List.of(
                        target("Thực thể mà hiệu ứng hạt xoay quanh. SELF = chủ passive, VICTIM = nạn nhân.", "SELF", "SELF", "VICTIM"),
                        FieldDef.of("particle", PARTICLE, "FLAME", "Loại hạt", "Loại hạt Bukkit dùng cho hiệu ứng (mặc định FLAME). DUST và BLOCK/FALLING_DUST có thêm tùy chọn màu/block."),
                        enumOf("shape", "CIRCLE", "Hình dạng", "CIRCLE = vòng phẳng nằm ngang; SPHERE = hình cầu 3D.",
                                "CIRCLE", "SPHERE").withIcon(Material.HEART_OF_THE_SEA),
                        expr("radius", "1.0", "Bán kính", "Bán kính của vòng hoặc hình cầu.")
                                .withIcon(Material.COMPASS),
                        expr("height", "1.0", "Độ cao", "Độ cao tâm hiệu ứng tính từ chân mục tiêu (trục Y).")
                                .withIcon(Material.LADDER),
                        expr("forward-offset", "0.0", "Lệch trước/sau", "Dịch tâm ra trước (+) hoặc ra sau (-) theo hướng nhìn của mục tiêu. Được tính lại mỗi tick nên hiệu ứng bám theo khi mục tiêu quay người.")
                                .withIcon(Material.ARROW),
                        expr("side-offset", "0.0", "Lệch trái/phải", "Dịch tâm sang phải (+) hoặc trái (-) theo hướng nhìn của mục tiêu. Cũng được tính lại mỗi tick.")
                                .withIcon(Material.ARROW),
                        expr("points-per-tick", "12", "Điểm mỗi tick", "Số điểm vẽ mỗi lần cập nhật; nhiều = vòng/cầu dày hơn nhưng tốn tài nguyên hơn.")
                                .withIcon(Material.GLOWSTONE_DUST),
                        expr("particle-per-point", "1", "Hạt mỗi điểm", "Số hạt sinh ra tại mỗi điểm.")
                                .withIcon(Material.GLOWSTONE_DUST),
                        expr("duration-seconds", "3", "Thời gian (giây)", "Tổng thời gian hiệu ứng kéo dài (mặc định 3 giây).")
                                .withIcon(Material.CLOCK),
                        integer("update-interval-ticks", 4, "Chu kỳ cập nhật (tick)", "Số tick giữa hai lần vẽ. Càng thấp càng mượt nhưng càng tốn tài nguyên.")
                                .withIcon(Material.REPEATER),
                        bool("rotate", true, "Xoay", "true = hiệu ứng xoay liên tục; false = đứng yên."),
                        FieldDef.of("dust-color", STRING, "#FF5500", "Màu bụi", "Mã màu hex #RRGGBB, chỉ dùng khi loại hạt là DUST.")
                                .withIcon(Material.RED_DYE)
                                .visibleIf("particle", "DUST"),
                        FieldDef.of("block-material", MATERIAL, "OAK_LEAVES", "Block hạt", "Material của block dùng làm hạt khi loại hạt là BLOCK hoặc FALLING_DUST.")
                                .visibleIf("particle", "BLOCK", "FALLING_DUST"))));

        register(new ActionSchema("PARTICLE_PROJECTILE", Category.DAMAGE, Material.FIRE_CHARGE,
                "Đạn Hạt", "Bắn một đạn hạt về phía nạn nhân; khi trúng gây sát thương và tạo hiệu ứng va chạm (vòng, cầu, nổ tung, lốc xoáy). Có thể bay xoắn ốc và vẽ vệt chém.",
                List.of(
                        FieldDef.of("particle", PARTICLE, "FLAME", "Loại hạt", "Loại hạt bay dọc đường đạn (mặc định FLAME)."),
                        expr("particle-per-step", "1", "Hạt mỗi bước", "Số hạt sinh ra mỗi bước bay; nhiều = vệt đạn đậm hơn.")
                                .withIcon(Material.GLOWSTONE_DUST),
                        expr("speed", "1.0", "Tốc độ", "Tốc độ bay của đạn, đơn vị block/tick.")
                                .withIcon(Material.ARROW),
                        expr("amount", "0", "Sát thương", "Sát thương gây ra khi đạn trúng mục tiêu. Cũng là mặc định cho 'Sát thương va chạm'.")
                                .withIcon(Material.REDSTONE),
                        damageType("NORMAL"),
                        expr("hit-radius", "1.0", "Bán kính va chạm", "Bán kính hitbox của đạn; lớn hơn = dễ trúng hơn.")
                                .withIcon(Material.SPYGLASS),
                        bool("hit-actor-self", false, "Trúng người bắn", "true = đạn có thể trúng chính người bắn. Mặc định tắt."),
                        bool("immediate-impact", false, "Nổ ngay", "true = bỏ qua giai đoạn bay, va chạm xảy ra ngay tại vị trí mục tiêu."),
                        FieldDef.of("dust-color", STRING, "#FFAA00", "Màu bụi", "Mã màu hex #RRGGBB, chỉ dùng cho hạt DUST.")
                                .withIcon(Material.RED_DYE)
                                .visibleIf("particle", "DUST"),
                        FieldDef.of("block-material", MATERIAL, "MAGMA_BLOCK", "Block hạt", "Material của block dùng làm hạt khi loại hạt là BLOCK hoặc FALLING_DUST.")
                                .visibleIf("particle", "BLOCK", "FALLING_DUST"),

                        enumOf("flight-shape", "NONE", "Hình dạng đường bay", "NONE = bay thẳng; TORNADO = các vòng hạt xoắn ốc quanh đạn khi bay.",
                                "NONE", "TORNADO").withIcon(Material.HEART_OF_THE_SEA),
                        expr("flight-radius", "0.45", "Bán kính xoắn", "Bán kính vòng xoáy quanh đường bay.")
                                .withIcon(Material.COMPASS)
                                .visibleIf("flight-shape", "TORNADO"),
                        expr("flight-rotation-speed", "0.55", "Tốc độ xoay", "Tốc độ xoay của vòng xoáy; lớn hơn = xoắn nhanh hơn.")
                                .withIcon(Material.REPEATER)
                                .visibleIf("flight-shape", "TORNADO"),
                        integer("flight-rings", 2, "Số vòng", "Số vòng xoáy chồng nhau (tối thiểu 1).")
                                .visibleIf("flight-shape", "TORNADO"),
                        integer("flight-points-per-ring", 6, "Điểm mỗi vòng", "Số điểm hạt trên mỗi vòng xoáy (tối thiểu 3).")
                                .visibleIf("flight-shape", "TORNADO"),
                        bool("flight-grow", true, "Phình to dần", "true = vòng xoáy lớn dần từ nhỏ tới kích thước tối đa khi đạn bay.")
                                .visibleIf("flight-shape", "TORNADO"),
                        expr("flight-grow-steps", "20", "Số bước phình", "Số bước để vòng xoáy đạt kích thước tối đa.")
                                .withIcon(Material.CLOCK)
                                .visibleIf("flight-shape", "TORNADO"),
                        bool("sweep-attack", false, "Hiệu ứng quét", "Bật hiệu ứng vệt chém SWEEP_ATTACK dọc theo đường bay."),
                        integer("sweep-attack-interval", 4, "Chu kỳ quét (tick)", "Số tick giữa hai lần tạo vệt chém (tối thiểu 1).")
                                .withIcon(Material.REPEATER)
                                .visibleIf("sweep-attack", "true"),

                        enumOf("impact-shape", "NONE", "Hình dạng va chạm", "Chỉ là hình ảnh khi trúng: NONE = không hiện, CIRCLE = vòng nở ra, SPHERE = cầu nở ra, BURST = tia bắn tỏa như nổ, TORNADO = cột xoáy bay lên. Vùng sát thương luôn là hình cầu.",
                                "NONE", "CIRCLE", "SPHERE", "BURST", "TORNADO").withIcon(Material.HEART_OF_THE_SEA),
                        FieldDef.of("impact-particle", PARTICLE, null, "Hạt va chạm", "Loại hạt của hiệu ứng va chạm. Bỏ trống = dùng cùng loại hạt với đường bay.")
                                .withIcon(Material.BLAZE_POWDER),
                        expr("impact-radius", "3.0", "Bán kính va chạm", "Bán kính vùng sát thương khi trúng. Luôn là hình cầu, bất kể hình dạng hiển thị.")
                                .withIcon(Material.COMPASS),
                        expr("impact-duration-ticks", "15", "Thời gian va chạm (tick)", "Thời gian vùng va chạm tồn tại (tick). Mỗi thực thể chỉ nhận sát thương va chạm tối đa 1 lần mỗi 3 tick.")
                                .withIcon(Material.CLOCK),
                        expr("impact-points", "30", "Số điểm va chạm", "Số điểm hạt dùng để vẽ hiệu ứng va chạm; nhiều = dày hơn nhưng nặng hơn.")
                                .withIcon(Material.GLOWSTONE_DUST),
                        expr("impact-particle-per-point", "2", "Hạt mỗi điểm va chạm", "Số hạt sinh ra tại mỗi điểm của hiệu ứng va chạm.")
                                .withIcon(Material.GLOWSTONE_DUST),
                        enumOf("impact-damage-type", null, "Loại sát thương va chạm", "Loại sát thương của vùng va chạm. Bỏ trống = dùng cùng loại với sát thương đạn.",
                                "NORMAL", "TRUE").withIcon(Material.IRON_CHESTPLATE),
                        expr("impact-damage", null, "Sát thương va chạm", "Sát thương mỗi nhịp của vùng va chạm, mặc định bằng 'Sát thương'. Giá trị này suy giảm dần theo thời gian tồn tại của vùng.")
                                .withIcon(Material.REDSTONE))));

        register(new ActionSchema("BREAK_AREA", Category.WORLD, Material.DIAMOND_PICKAXE,
                "Đào Vùng", "Đào thêm các block quanh block vừa phá (ví dụ 3×3). Chỉ dùng với trigger ON_BLOCK_BREAK; tôn trọng WorldGuard/claim và Fortune/Silk Touch.",
                List.of(
                        expr("size", "3", "Kích thước", "Cạnh vùng đào: 3 = 3×3, 5 = 5×5. Nên dùng số lẻ để vùng đào cân đối quanh block vừa phá.")
                                .withIcon(Material.IRON_PICKAXE),
                        expr("depth", "1", "Độ sâu", "Số block đào thêm theo trục Y (độ sâu của vùng đào).")
                                .withIcon(Material.LADDER),
                        bool("use-tool-drops", true, "Drop theo công cụ", "true = drop tính theo công cụ đang cầm (Fortune/Silk Touch); false = drop mặc định của block."),
                        FieldDef.of("excluded-materials", STRING, "BEDROCK,BARRIER,...", "Block loại trừ",
                                "Danh sách Material không bao giờ bị đào, ngăn cách bằng dấu phẩy (vd BEDROCK,BARRIER). Vùng được bảo vệ bởi WorldGuard/claim cũng bị bỏ qua.")
                                .withIcon(Material.BEDROCK))));

        // ================================================================ phần 2 ================================================================


        // ---- Container / luồng điều khiển ----

        register(new ActionSchema("DELAY", Category.UTILITY, Material.CLOCK,
                "Trì Hoãn", "Chờ N giây rồi mới chạy danh sách hành động con. Các action ngay sau DELAY vẫn chạy luôn, không bị chờ.",
                List.of(
                        expr("seconds", "1", "Thời gian chờ (giây)", "Số giây chờ rồi mới chạy hành động con (mặc định 1).")
                                .withIcon(Material.CLOCK),
                        children("children", "Hành động con", "Danh sách hành động chạy sau khi chờ. DELAY tự quản lý hành động con của nó; đừng đặt DELAY như action thường bên trong DELAY/REPEAT khác."))));

        register(new ActionSchema("REPEAT", Category.UTILITY, Material.REPEATER,
                "Lặp Lại", "Lặp cả danh sách hành động con nhiều lần, mỗi lần cách nhau một khoảng thời gian. Hợp với sát thương/độc theo nhịp.",
                List.of(
                        expr("times", "1", "Số lần lặp", "Tổng số lần lặp danh sách hành động con (mặc định 1).")
                                .withIcon(Material.COMPARATOR),
                        expr("interval-seconds", "1", "Khoảng cách (giây)", "Khoảng cách giữa hai lần lặp, tính bằng giây (mặc định 1).")
                                .withIcon(Material.CLOCK),
                        children("children", "Hành động con", "Toàn bộ danh sách này được chạy lại ở mỗi lần lặp."))));

        register(new ActionSchema("HIT_COUNTER", Category.UTILITY, Material.COMPARATOR,
                "Đếm Đòn Đánh", "Đếm số lần passive kích hoạt; chỉ khi đủ ngưỡng mới chạy hành động con (vd: mỗi 5 đòn đánh thì nổ một lần).",
                List.of(
                        expr("every", "5", "Ngưỡng kích hoạt", "Số lần kích hoạt cần có để chạy hành động con (mặc định 5).")
                                .withIcon(Material.COMPARATOR),
                        expr("reset-after", null, "Reset sau", "Số lần tối đa trước khi bộ đếm tự reset về 0. Bỏ trống = bằng 'Ngưỡng kích hoạt'.")
                                .withIcon(Material.BARRIER),
                        children("children", "Hành động con", "Danh sách hành động chạy mỗi khi bộ đếm đạt ngưỡng."))));

        register(new ActionSchema("STACK_COUNTER", Category.UTILITY, Material.NETHER_STAR,
                "Cộng Dồn Stack", "Cộng dồn stack theo từng người chơi; chạm đúng mốc thì chạy hành động tương ứng. Stack tự tan nếu đứng yên quá lâu, và reset khi chết/thoát game.",
                List.of(
                        expr("max-stacks", "5", "Stack tối đa", "Số stack tối đa có thể cộng dồn (mặc định 5).")
                                .withIcon(Material.NETHER_STAR),
                        expr("decay-seconds", "5", "Thời gian tan (giây)", "Số giây không được kích hoạt lại trước khi stack tan về 0. Mỗi lần kích hoạt làm mới đồng hồ này.")
                                .withIcon(Material.CLOCK),
                        bool("trigger-at-max", false, "Kích hoạt ở mức tối đa",
                                "true = kích hoạt khi đã đủ stack tối đa sẽ chạy lại mốc cuối (gia hạn thời gian); false = stack bị reset về 0 (kiểu 'quá tải')."),
                        bool("on-quit", true, "Reset khi thoát", "true = tự reset stack về 0 ngay khi người chơi thoát game (mặc định bật)."),
                        bool("on-death", true, "Reset khi chết", "true = tự reset stack về 0 ngay khi người chơi chết (mặc định bật)."),
                        children("on-empty", "Khi stack về 0", "Danh sách hành động chạy mỗi khi stack tan về 0 do hết thời gian (vd báo 'stack đã tan')."),
                        // TODO: milestones là Map<String, List<Map>> (khóa là số stack), không phải một danh sách.
                        // Hiện FieldKind chưa có kind cho map nên tạm dùng ACTION_LIST; GUI phải xử lý riêng key "milestones".
                        children("milestones", "Các mốc stack", "Bảng 'số stack' → danh sách hành động. Hành động chạy khi stack đạt ĐÚNG mốc đó (vd mốc 3 cho buff, mốc 5 cho nổ)."))));

        register(new ActionSchema("ADD_VALUE", Category.UTILITY, Material.EXPERIENCE_BOTTLE,
                "Cộng Giá Trị", "Cộng một số vào giá trị có khóa lưu trên mục tiêu, gọn hơn STACK_COUNTER khi không cần mốc. Mỗi lần cộng sẽ làm mới đồng hồ hết hạn. Đọc lại bằng Kiểm Tra Giá Trị.",
                List.of(
                        target("Thực thể lưu giá trị (SELF/ACTOR = chủ passive, VICTIM = nạn nhân). Phải khớp với mục tiêu của Kiểm Tra Giá Trị tương ứng.", "VICTIM", "SELF", "ACTOR", "VICTIM"),
                        FieldDef.of("key", STRING, null, "Khóa", "Định danh giá trị lưu. Mọi passive dùng cùng khóa trên cùng thực thể sẽ đọc/ghi chung một giá trị.")
                                .asRequired().withIcon(Material.NAME_TAG),
                        expr("amount", "1", "Lượng cộng", "Lượng cộng thêm vào giá trị hiện có (tạo mới từ 0 nếu chưa có); số âm = trừ đi.")
                                .withIcon(Material.REDSTONE),
                        expr("duration", "0", "Thời hạn (giây)", "Số giây trước khi giá trị hết hạn và bị xóa; 0 = không tự hết hạn. Mỗi lần cộng sẽ cộng dồn VÀ làm mới đồng hồ, không reset số đếm.")
                                .withIcon(Material.CLOCK))));

        register(new ActionSchema("CHECK_VALUE", Category.UTILITY, Material.SPYGLASS,
                "Kiểm Tra Giá Trị", "Đọc giá trị do Cộng Giá Trị lưu, so với ngưỡng; đạt thì chạy hành động con. Nếu khóa chưa có hoặc đã hết hạn thì không làm gì.",
                List.of(
                        target("Thực thể chứa giá trị cần đọc. Phải khớp với mục tiêu đã dùng ở Cộng Giá Trị.", "VICTIM", "SELF", "ACTOR", "VICTIM"),
                        FieldDef.of("key", STRING, null, "Khóa", "Định danh giá trị cần đọc; phải khớp với khóa đã dùng ở Cộng Giá Trị.")
                                .asRequired().withIcon(Material.NAME_TAG),
                        FieldDef.of("operator", ENUM, ">=", "Phép so sánh", "Phép so sánh giá trị đã lưu với ngưỡng: >=, >, <=, <, ==, !=. Mặc định >=.")
                                .withAllowed(">=", ">", "<=", "<", "==", "!=").withIcon(Material.COMPARATOR),
                        expr("value", "1", "Ngưỡng", "Ngưỡng để so sánh với giá trị đã lưu (mặc định 1).")
                                .withIcon(Material.GOLD_NUGGET),
                        bool("consume", false, "Tiêu hao", "true = xóa hẳn giá trị đã lưu khi kiểm tra đạt (kiểu 'tiêu stack để tung chiêu'); false = giữ lại và tiếp tục cộng dồn."),
                        children("actions", "Hành động con", "Danh sách hành động chạy khi phép so sánh đạt. Khóa chưa có hoặc đã hết hạn thì không chạy gì."))));

        register(new ActionSchema("TARGET_FILTER", Category.UTILITY, Material.HOPPER,
                "Lọc Mục Tiêu", "Chỉ chạy hành động con khi mục tiêu thuộc loại cho phép: người chơi, mob vanilla hay mob MythicMobs. Nếu thiếu danh sách loại hoặc hành động con thì sẽ không làm gì.",
                List.of(
                        target("Thực thể cần kiểm tra loại (người chơi, mob vanilla hay mob MythicMobs).", "VICTIM", "SELF", "VICTIM"),
                        FieldDef.of("types", STRING_LIST, null, "Loại cho phép",
                                "Các loại được phép: PLAYER; ALL = mọi mob vanilla; ALL_MM = mọi mob MythicMobs; MM_<ID> = một mob MythicMobs cụ thể (vd MM_SKELETON_KING); hoặc tên EntityType (ZOMBIE, ENDERMAN...). Thứ tự kiểm tra: người chơi → MythicMobs → vanilla.")
                                .asRequired().withIcon(Material.BOOK),
                        children("actions", "Hành động con", "Danh sách hành động chỉ chạy khi mục tiêu khớp một trong các loại cho phép.").asRequired())));

        register(new ActionSchema("REVIVE", Category.SUPPORT, Material.TOTEM_OF_UNDYING,
                "Hồi Sinh", "Bảo vệ người chơi khỏi đòn chí mạng: sát thương bị hủy, người chơi được cứu và chuỗi hành động con chạy ngay. Bảo vệ bị tiêu hao khi kích hoạt lần đầu.",
                List.of(
                        target("Người được bảo vệ khỏi đòn chí mạng. Chỉ áp dụng cho người chơi, mob bị bỏ qua.", "SELF", "SELF", "VICTIM"),
                        expr("duration-seconds", "10", "Thời gian bảo vệ (giây)", "Thời gian bảo vệ tồn tại. 0 hoặc bỏ trống = bảo vệ vĩnh viễn cho tới khi kích hoạt (không tạo đồng hồ đếm). Kích hoạt lại khi đang có bảo vệ chỉ làm mới đồng hồ.")
                                .withIcon(Material.CLOCK),
                        expr("revive-health-percent", "50", "% máu hồi sinh", "% máu tối đa hồi lại ngay lúc được cứu (mặc định 50). 0 hoặc bỏ trống = giữ nguyên máu hiện tại.")
                                .withIcon(Material.GOLDEN_APPLE),
                        children("actions", "Hành động con", "Danh sách hành động chạy ngay khoảnh khắc người chơi được cứu (vd hồi máu, bất tử ngắn, gửi tin)."))));

        // ---- Triệu hồi ----

        register(new ActionSchema("SUMMON_VANILLA", Category.WORLD, Material.ZOMBIE_SPAWN_EGG,
                "Triệu Hồi Mob Vanilla", "Triệu hồi một mob vanilla tại vị trí mục tiêu, có thể chỉnh máu, sát thương, tốc độ và chạy hành động khi mob chết.",
                List.of(
                        // Chưa có FieldKind.ENTITY_TYPE nên dùng STRING (tên EntityType).
                        FieldDef.of("mob", STRING, null, "Loại mob", "Tên EntityType của mob vanilla (vd ZOMBIE, SKELETON).")
                                .asRequired().withIcon(Material.ZOMBIE_HEAD),
                        expr("health", null, "Máu", "Máu của mob triệu hồi. Bỏ trống = máu mặc định của loại thực thể.")
                                .withIcon(Material.RED_DYE),
                        expr("damage", null, "Sát thương", "Sát thương tấn công của mob. Bỏ trống = mặc định của loại thực thể.")
                                .withIcon(Material.IRON_SWORD),
                        FieldDef.of("speed", DOUBLE, 1.0, "Tốc độ", "Hệ số nhân tốc độ di chuyển (1.0 = bình thường).")
                                .withIcon(Material.FEATHER),
                        target("Mob xuất hiện tại vị trí của thực thể này. VICTIM = nạn nhân (mặc định), SELF = chủ passive.", "VICTIM", "SELF", "VICTIM"),
                        children("on-death", "Khi mob chết", "Danh sách hành động chạy khi mob được triệu hồi này chết."))));

        register(new ActionSchema("SUMMON_MM", Category.WORLD, Material.DRAGON_EGG,
                "Triệu Hồi Mob MythicMobs", "Triệu hồi mob MythicMobs với cấp độ/chỉ số ghi đè. Cần plugin MythicMobs; ID phân biệt hoa/thường.",
                List.of(
                        FieldDef.of("mob", STRING, null, "ID mob", "ID MythicMob, phân biệt hoa/thường, phải khớp chính xác với cấu hình MythicMobs.")
                                .asRequired().withIcon(Material.DRAGON_HEAD),
                        expr("level", "1", "Cấp độ", "Cấp độ của mob (mặc định 1). Có thể scale theo cấp người chơi, vd \"%player_level% / 10\".")
                                .withIcon(Material.EXPERIENCE_BOTTLE),
                        expr("health", null, "Máu", "Ghi đè máu tối đa của mob. Bỏ trống = máu theo cấu hình mob.")
                                .withIcon(Material.RED_DYE),
                        expr("damage", null, "Sát thương", "Ghi đè sát thương của mob. Bỏ trống = sát thương theo cấu hình mob.")
                                .withIcon(Material.IRON_SWORD),
                        FieldDef.of("speed", DOUBLE, 1.0, "Tốc độ", "Hệ số nhân tốc độ (2 = 200%, 1 = bình thường).")
                                .withIcon(Material.FEATHER),
                        target("Mob xuất hiện tại vị trí của thực thể này. VICTIM = nạn nhân (mặc định), SELF = chủ passive.", "VICTIM", "SELF", "VICTIM"),
                        children("on-death", "Khi mob chết", "Danh sách hành động chạy khi mob MythicMobs được triệu hồi này chết."))));

        // ---- Hiển thị / trạng thái / tích hợp ----

        register(new ActionSchema("TITLE", Category.VISUAL, Material.OAK_SIGN,
                "Hiện Tiêu Đề", "Gửi Title/Subtitle lên màn hình người chơi. Để trống phần nào thì phần đó bị ẩn.",
                List.of(
                        target("Người nhận Title/Subtitle (mặc định VICTIM). Placeholder PAPI trong chữ được tính theo người nhận.", "VICTIM", "SELF", "VICTIM"),
                        FieldDef.of("title", STRING, null, "Tiêu đề", "Dòng tiêu đề lớn; hỗ trợ mã màu & và PAPI của người nhận. Để trống = ẩn phần này.")
                                .withIcon(Material.NAME_TAG),
                        FieldDef.of("subtitle", STRING, null, "Phụ đề", "Dòng phụ đề nhỏ bên dưới; hỗ trợ mã màu & và PAPI của người nhận. Để trống = ẩn phần này.")
                                .withIcon(Material.PAPER),
                        expr("fade-in", "10", "Hiện dần (tick)", "Số tick hiện dần (20 tick = 1 giây). Tính theo chủ passive, không phải người nhận.")
                                .withIcon(Material.CLOCK),
                        expr("stay", "70", "Giữ nguyên (tick)", "Số tick giữ nguyên trên màn hình (mặc định 70 ≈ 3,5 giây). Tính theo chủ passive.")
                                .withIcon(Material.CLOCK),
                        expr("fade-out", "20", "Mờ dần (tick)", "Số tick mờ dần trước khi biến mất. Tính theo chủ passive.")
                                .withIcon(Material.CLOCK))));

        register(new ActionSchema("STATUS", Category.SUPPORT, Material.SHIELD,
                "Trạng Thái Đặc Biệt", "Áp một trạng thái đặc biệt tạm thời: choáng, trói chân, tước vũ khí hoặc bất tử. Áp lại sẽ tự dọn tác vụ cũ, không cộng dồn.",
                List.of(
                        target("Thực thể bị áp trạng thái. VICTIM = nạn nhân (mặc định), SELF = chủ passive (hợp với INVINCIBLE).", "VICTIM", "SELF", "VICTIM"),
                        FieldDef.of("status", ENUM, null, "Trạng thái", "STUN = khóa hoàn toàn: không di chuyển, không xoay đầu, không gây sát thương. ROOT = chỉ khóa di chuyển, vẫn xoay và đánh được. DISARM = không gây được sát thương. INVINCIBLE = chặn mọi sát thương nhận vào.")
                                .withAllowed("STUN", "ROOT", "DISARM", "INVINCIBLE").asRequired()
                                .withIcon(Material.SHIELD),
                        expr("duration", "3", "Thời gian (giây)", "Số giây trạng thái kéo dài (mặc định 3). Áp lại khi đang có trạng thái sẽ dọn tác vụ cũ rồi tính lại.")
                                .withIcon(Material.CLOCK))));

        register(new ActionSchema("MYTHIC_SKILL", Category.DAMAGE, Material.BLAZE_ROD,
                "Kỹ Năng MythicMobs", "Tung trực tiếp một skill MythicMobs khi passive kích hoạt. Cần MythicMobs; nếu thiếu plugin thì chỉ ghi cảnh báo một lần và bỏ qua, không gây lỗi.",
                List.of(
                        target("Thực thể mà skill được tung từ/lên. VICTIM = nạn nhân (mặc định), SELF = chủ passive.", "VICTIM", "SELF", "VICTIM"),
                        FieldDef.of("skill", STRING, null, "Tên skill", "Tên skill khớp với ID trong MythicMobs/Skills/. Cần MythicMobs; thiếu plugin thì mechanic bỏ qua an toàn.")
                                .asRequired().withIcon(Material.ENCHANTED_BOOK),
                        expr("damage-multiplier", "1.0", "Hệ số sát thương", "Hệ số truyền qua tham số 'power' của MythicMobs. CHỈ có tác dụng nếu mechanic trong skill bật power: true.")
                                .withIcon(Material.REDSTONE))));

        register(new ActionSchema("LIGHTNING", Category.DAMAGE, Material.LIGHTNING_ROD,
                "Sét Đánh", "Giáng sét lên mục tiêu, có thể kèm sát thương thêm, gây cháy, hoặc chỉ phát hình ảnh/âm thanh.",
                List.of(
                        target("Thực thể bị sét đánh. VICTIM = nạn nhân (mặc định), ACTOR = chủ passive.", "VICTIM", "ACTOR", "VICTIM"),
                        expr("damage", "0.0", "Sát thương thêm", "Sát thương thêm lên mục tiêu ngoài cú sét. Có thể scale theo cấp, vd \"%player_level% * 0.5 + 4\".")
                                .withIcon(Material.REDSTONE),
                        bool("causes-burning", false, "Gây cháy", "true = cú sét làm mục tiêu bốc cháy. false = lửa sẽ bị dập ngay sau khi sét đánh để không cháy ngoài ý muốn."),
                        FieldDef.of("fire-ticks", INT, 100, "Thời gian cháy (tick)", "Thời gian cháy (tick) nếu bật 'Gây cháy'; 100 tick = 5 giây.")
                                .withRange(0.0, null).withIcon(Material.FLINT_AND_STEEL)
                                .visibleIf("causes-burning", "true"),
                        bool("visual-only", false, "Chỉ hiệu ứng", "true = chỉ phát hình ảnh và âm thanh sét, không gây sát thương hay lửa. false = dùng tia sét vanilla thật."))));

        // ---- Đọc từ source Mechanics/ProjectileShotMechanic & PotionZoneMechanic ----

        register(new ActionSchema("PROJECTILE_SHOT", Category.DAMAGE, Material.ARROW,
                "Bắn Đạn", "Bắn ra một hoặc nhiều đạn (tuyết, trứng, tên, đầu lâu Wither, cầu lửa...) xòe hình quạt quanh hướng nhìn. Sát thương tùy chỉnh được áp dụng khi đạn trúng sinh vật.",
                List.of(
                        enumOf("projectile", "SNOWBALL", "Loại đạn", "Loại đạn: SNOWBALL, EGG, ARROW, SPECTRAL_ARROW, WITHER_SKULL, SMALL_FIREBALL, FIREBALL, DRAGON_FIREBALL, SHULKER_BULLET, LLAMA_SPIT, ENDER_PEARL, TRIDENT.",
                                "SNOWBALL", "EGG", "ARROW", "SPECTRAL_ARROW", "WITHER_SKULL", "SMALL_FIREBALL",
                                "FIREBALL", "DRAGON_FIREBALL", "SHULKER_BULLET", "LLAMA_SPIT", "ENDER_PEARL", "TRIDENT")
                                .withIcon(Material.SNOWBALL),
                        expr("damage", "0", "Sát thương", "Sát thương áp khi đạn trúng sinh vật. 0 = giữ sát thương mặc định của loại đạn.")
                                .withIcon(Material.REDSTONE),
                        expr("amount", "1", "Số lượng", "Số đạn bắn ra mỗi lần kích hoạt, xòe đều quanh hướng nhìn.")
                                .withIcon(Material.ARROW),
                        expr("spread-angle", "10", "Góc lệch (độ)", "Góc lệch ngang (độ) giữa hai đạn liên tiếp; cả chùm được căn giữa theo hướng nhìn. Chỉ có tác dụng khi bắn từ 2 đạn trở lên.")
                                .withIcon(Material.COMPASS),
                        expr("speed", "1.5", "Tốc độ", "Tốc độ bắn của đạn (mặc định 1.5).")
                                .withIcon(Material.SUGAR),
                        bool("gravity", true, "Trọng lực", "true = đạn rơi xuống theo trọng lực; false = bay thẳng."))));

        register(new ActionSchema("POTION_ZONE", Category.SUPPORT, Material.SPLASH_POTION,
                "Vùng Thuốc", "Ném một bình thuốc; khi chạm đất tạo vùng hiệu ứng tồn tại một lúc, áp các hiệu ứng thuốc lên ai đứng trong vùng. Vị trí rơi được chốt lúc kích hoạt nên vẫn chạy dù nạn nhân đã chết.",
                List.of(
                        target("Điểm bình thuốc rơi xuống: VICTIM = vị trí nạn nhân (mặc định), ACTOR = vị trí chủ passive. Vị trí được chốt lúc kích hoạt.", "VICTIM", "VICTIM", "ACTOR"),
                        expr("speed", "0.6", "Tốc độ ném", "Tốc độ ném bình thuốc (mặc định 0.6); bình được ném theo đường vòng cung tới điểm đích.")
                                .withIcon(Material.SUGAR),
                        expr("radius", "3", "Bán kính", "Bán kính vùng hiệu ứng khi bình chạm đất (mặc định 3, tối thiểu 0.5).")
                                .withIcon(Material.ENDER_PEARL),
                        expr("duration-seconds", "8", "Thời gian vùng (giây)", "Số giây vùng hiệu ứng tồn tại trên mặt đất (mặc định 8).")
                                .withIcon(Material.CLOCK),
                        integer("reapplication-delay-ticks", 20, "Chu kỳ áp lại (tick)", "Số tick giữa hai lần áp hiệu ứng lên cùng một thực thể đứng trong vùng (mặc định 20 = 1 giây)."),
                        FieldDef.of("color", STRING, null, "Màu hạt", "Màu hạt của vùng, dạng \"R,G,B\" mỗi số 0-255 (vd 255,0,0 = đỏ). Tùy chọn.")
                                .withIcon(Material.RED_DYE),
                        FieldDef.of("particle", PARTICLE, null, "Loại hạt", "Loại hạt của vùng hiệu ứng (vd ENTITY_EFFECT). Tùy chọn."),
                        FieldDef.of("effects", EFFECT_LIST, null, "Danh sách hiệu ứng", "Danh sách hiệu ứng thuốc áp lên mọi thực thể trong vùng. Mỗi mục gồm loại, cấp (amplifier, 0 = cấp I), thời gian giây, ambient, particles, icon.")
                                .asRequired())));
    }
}
