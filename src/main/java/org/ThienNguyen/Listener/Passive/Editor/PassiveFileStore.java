package org.ThienNguyen.Listener.Passive.Editor;

import org.ThienNguyen.Listener.Passive.PassiveManager;
import org.ThienNguyen.Main;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Đọc / ghi file passive trong {@code plugins/<plugin>/Listener/Passives}.
 * Nằm trong package Editor vì cần {@link PassiveDraft#setSourceFile}.
 * Lưu: nạp lại file gốc (parseComments) rồi chỉ set các key cấp 1 -> giữ comment đầu file, comment trên key cấp 1
 * và comment cuối dòng. Comment BÊN TRONG danh sách actions/condition bị mất (Bukkit API không hỗ trợ). Luôn tạo .bak.
 */
public final class PassiveFileStore {

    public static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9_\\-]{1,48}");

    private PassiveFileStore() {}

    public static File folder() {
        return new File(Main.getInstance().getDataFolder(), "Listener/Passives");
    }

    /** Các file .yml, sắp xếp theo tên. */
    public static List<File> listFiles() {
        File[] fs = folder().listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (fs == null) return List.of();
        List<File> out = new ArrayList<>(Arrays.asList(fs));
        out.sort(Comparator.comparing(f -> f.getName().toLowerCase(Locale.ROOT)));
        return out;
    }

    public static PassiveDraft load(File f) {
        return PassiveDraft.fromYaml(YamlConfiguration.loadConfiguration(f), f);
    }

    /** File đang dùng id này (khác {@code except}), hoặc null. */
    public static File findById(String id, File except) {
        for (File f : listFiles()) {
            if (except != null && f.getAbsoluteFile().equals(except.getAbsoluteFile())) continue;
            String fid = YamlConfiguration.loadConfiguration(f).getString("id", "");
            if (fid.equals(id)) return f;
        }
        return null;
    }

    /**
     * Ghi draft ra file (tạo mới {@code <id>.yml} nếu chưa gắn file) rồi nạp lại ĐÚNG passive đó.
     * @return null nếu thành công, ngược lại thông báo lỗi.
     */
    public static String save(PassiveDraft draft) {
        String id = draft.getId();
        if (!ID_PATTERN.matcher(id).matches()) return "ID chỉ gồm chữ/số/_/- (tối đa 48 ký tự).";

        File target = draft.getSourceFile();
        if (findById(id, target) != null) return "ID \"" + id + "\" đã được file khác dùng.";
        if (target == null) {
            target = new File(folder(), id + ".yml");
            if (target.exists()) return "Đã tồn tại file " + target.getName() + ".";
        }

        String err = writeFile(draft, target);
        if (err != null) return err;
        draft.setSourceFile(target);
        reloadOne(target, id);
        return null;
    }

    /** Tạo .bak rồi ghi draft vào target (giữ comment cấp 1). @return null nếu OK. */
    private static String writeFile(PassiveDraft draft, File target) {
        try {
            folder().mkdirs();
            if (target.exists()) {
                Files.copy(target.toPath(), new File(target.getPath() + ".bak").toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            YamlConfiguration cfg = target.exists() ? YamlConfiguration.loadConfiguration(target) : new YamlConfiguration();
            Map<String, Object> raw = draft.raw();
            for (String k : new ArrayList<>(cfg.getKeys(false))) { // key đã bị xóa khỏi draft
                if (!raw.containsKey(k)) cfg.set(k, null);
            }
            for (Map.Entry<String, Object> e : raw.entrySet()) cfg.set(e.getKey(), e.getValue());
            cfg.save(target);
            return null;
        } catch (IOException ex) {
            return "Không ghi được file: " + ex.getMessage();
        }
    }

    /**
     * Đổi ID của passive đã có file: đổi tên file thành {@code <newId>.yml}, ghi lại (tạo {@code <newId>.yml.bak}
     * chứa nội dung trước khi đổi) và nạp lại. Gọi khi draft KHÔNG có thay đổi chưa lưu và đã qua validate.
     * Lỗi giữa chừng -> hoàn lại tên file/ID cũ.
     * @return null nếu thành công, ngược lại thông báo lỗi.
     */
    public static String rename(PassiveDraft draft, String newId) {
        File old = draft.getSourceFile();
        if (old == null || !old.exists()) return "Passive chưa có file.";
        if (newId == null || !ID_PATTERN.matcher(newId).matches()) return "ID chỉ gồm chữ/số/_/- (tối đa 48 ký tự).";
        String oldId = draft.getId();
        if (newId.equals(oldId)) return "ID không đổi.";
        if (findById(newId, old) != null) return "ID \"" + newId + "\" đã được file khác dùng.";

        File dest = new File(folder(), newId + ".yml");
        boolean sameFile = dest.getAbsoluteFile().equals(old.getAbsoluteFile());
        if (!sameFile && dest.exists()) return "Đã tồn tại file " + dest.getName() + ".";

        try {
            if (!sameFile) Files.move(old.toPath(), dest.toPath());
        } catch (IOException ex) {
            return "Không đổi tên được file: " + ex.getMessage();
        }
        draft.setSourceFile(dest);
        draft.setId(newId);
        String err = writeFile(draft, dest);
        if (err != null) { // hoàn tác
            try { if (!sameFile) Files.move(dest.toPath(), old.toPath()); } catch (IOException ignored) {}
            draft.setSourceFile(old);
            draft.setId(oldId);
            return err;
        }
        reloadOne(dest, oldId);
        return null;
    }

    /** Xóa file (giữ lại .bak) rồi gỡ passive khỏi registry. */
    public static String delete(PassiveDraft draft) {
        File f = draft.getSourceFile();
        if (f == null || !f.exists()) return "Passive chưa có file.";
        try {
            Files.move(f.toPath(), new File(f.getPath() + ".bak").toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            return "Không xóa được: " + ex.getMessage();
        }
        PassiveManager pm = PassiveManager.getInstance();
        if (pm != null) pm.unloadOne(draft.getId());
        return null;
    }

    /**
     * Nạp lại 1 passive; nếu PassiveManager từ chối (file lỗi) thì quay về loadAll() cho khớp với file trên đĩa
     * (state mechanic bị reset, giống lúc khởi động server).
     */
    private static void reloadOne(File file, String oldId) {
        PassiveManager pm = PassiveManager.getInstance();
        if (pm == null) return;
        if (!pm.reloadOne(file, oldId)) pm.loadAll();
    }

    /** Reload toàn bộ passive (state nội bộ của mechanic như HIT_COUNTER sẽ reset). */
    public static void reload() {
        PassiveManager pm = PassiveManager.getInstance();
        if (pm != null) pm.loadAll();
    }
}
