package org.ThienNguyen.Ability;
import org.ThienNguyen.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Bukkit;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
public class AbilityManager {
    private static final Map<String, IAbility> abilities = new HashMap<>();
    static {
        registerDefaultAbilities();
    }
    /**
     * Đăng ký các kỹ năng Java thuần (Hard-coded).
     * Tách riêng để có thể gọi lại khi Reload plugin.
     */
    public static void registerDefaultAbilities() {
        register(new Lightning());
        register(new Poison());
        register(new Wither());
        register(new Hunger());
        register(new Tired());
        register(new Slow());
        register(new Confuse());
        register(new Weak());
        register(new Blind());
        register(new AirShock());
        register(new Curse());
        register(new Bubble());
        register(new Bleed());
        register(new FireVortex());
        register(new Freeze());
        register(new Disarm());
        register(new Explode());
        register(new FlamePulse());
        register(new Angel());
        register(new ShadowDevour());
        register(new SonicWave());
        register(new FireRain());
        register(new StarRitual());
        register(new FireTripleShot());
        register(new StarFall());
        register(new SunStrikeAOE());
        register(new BlackHole());
        register(new ShadowWave());
        register(new FairyChain());
        register(new LilacBloomBomb());
        register(new LeafStorm());
        register(new SpiritWolf());
        register(new PlagueSpread());
        register(new WindTornado());
        register(new FireOrb());
        register(new PlasmaOrb());
        register(new ElectricBlade());
        register(new FirstStrike());
        register(new TNTStuck());
        register(new BadLuck());
        register(new BubbleDeflector());
        register(new DarkFlame());
        register(new DarkImpact());
        register(new Roots());
        register(new Vampirism());
        register(new VenomSpread());
        register(new SOLAR_SEAL());
        register(new RADIANT_WAVE());
        register(new FROST_NOVA());
        register(new CHAIN_LIGHTNING());
        register(new VOID_PULL());
        register(new THORN_ARMOR());
        register(new METEOR_STRIKE());
        register(new HEALING_AURA());
        register(new SHADOW_STEP());
        register(new POISON_CLOUD());
        register(new EARTHQUAKE_SLAM());
        register(new CELESTIAL_JUDGMENT());
        register(new PRISM_REFRACTION());
        register(new CRYSTAL_SHATTER());
        register(new AURORA_VEIL());
        register(new MAGMA_FISSURE());
        register(new PHANTOM_BLADES());
        register(new TEMPEST_LANCE());
        register(new GLACIAL_SPIKE());
        register(new NEBULA_BURST());
        //
        register(new VENOM_LANCE());
        register(new EMBER_TRAIL());
        register(new CORRUPTED_GROUND());
        register(new STORM_CAGE());
        register(new SPECTRAL_HOWL());
        register(new RUNIC_BLAST());
        register(new STELLAR_COLLAPSE());
        register(new BONE_SPIRE());
        register(new TIDAL_CRUSH());
        register(new INFERNO_RING());
        // ability khác
        register(new FROST_LANCE());
        register(new VOID_RIFT());
        register(new BLADE_DANCE());
        register(new INFERNO_PULSE());
        register(new THUNDER_CASCADE());
        register(new CRIMSON_THORNS());
        register(new SHADOW_STEP_SLASH());
        register(new CELESTIAL_JUDGMENT());
        register(new TOXIC_BLOOM());
        register(new GALE_FANG());
        register(new EMBER_SPIRAL());
        register(new TIDAL_CRESCENT());
/// //
        register(new GRAVITY_CRUSH());
        register(new CHRONO_FRACTURE());
        register(new OBSIDIAN_LANCE());
        register(new PLASMA_CASCADE());
        register(new WHIRLWIND_SLASH());
        register(new RAZOR_GALE());
        register(new HOLY_LANCE());
        register(new SANDSTORM_VORTEX());
        register(new STATIC_OVERLOAD());
        register(new QUICKSAND_MAW());
        // enviroment
        register(new TSUNAMI_SURGE());
        register(new VOLCANIC_ERUPTION());
        register(new AVALANCHE_SLIDE());
        register(new WILDFIRE_SPREAD());
        register(new HURRICANE_VORTEX());

    }
    /**
     * Hàm quét thư mục Skript/Ability và nạp code động từ file .yml
     */
    public static void loadExternalAbilities() {
        File folder = new File(Main.getInstance().getDataFolder(), "Skript/Ability");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        int count = 0;
        for (File file : files) {
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                String abilityName = file.getName().replace(".yml", "").toUpperCase();
                String scriptCode = config.getString("code");
                if (scriptCode != null && !scriptCode.isEmpty()) {
                    register(new ScriptAbility(abilityName, scriptCode));
                    count++;
                }
            } catch (Exception e) {
                Bukkit.getLogger().severe("[MyItem] Khong the nap ability tu file: " + file.getName());
            }
        }
    }
    public static void register(IAbility ability) {
        if (ability != null) {
            abilities.put(ability.getName().toUpperCase(), ability);
        }
    }
    public static IAbility getAbility(String name) {
        if (name == null) return null;
        return abilities.get(name.toUpperCase());
    }
    /**
     * Xóa sạch danh sách và nạp lại từ đầu (Dùng khi Reload plugin)
     */
    public static void clearAbilities() {
        abilities.clear();
        registerDefaultAbilities();
    }
}