package org.ThienNguyen.Listener.Passive.Editor.Gui;

import org.ThienNguyen.Listener.Passive.Editor.FieldKind;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Menu chọn giá trị từ danh sách lớn (Material/Sound/Particle/PotionEffect): phân trang + tìm kiếm bằng chat
 * + nhập tay (phòng khi giá trị không có trong danh sách của phiên bản server).
 * {@code onPick} tự chịu trách nhiệm mở lại menu tiếp theo.
 */
public final class PickerMenu extends EditorMenu {

    public record Entry(String value, Material icon) {}

    private static final int PAGE_SIZE = 45;

    private final List<Entry> all;
    private final Consumer<String> onPick;
    private String filter = "";
    private int page;

    public PickerMenu(Player viewer, EditorMenu parent, String title, List<Entry> all, Consumer<String> onPick) {
        super(viewer, parent, title, 6);
        this.all = all;
        this.onPick = onPick;
    }

    /** Dựng picker theo kind; trả null nếu kind không phải loại chọn từ danh sách. */
    public static PickerMenu forKind(Player viewer, EditorMenu parent, FieldKind kind, Consumer<String> onPick) {
        List<Entry> list = new ArrayList<>();
        String title;
        switch (kind) {
            case MATERIAL -> {
                title = "&8Chọn vật phẩm";
                for (Material m : Material.values()) if (!m.isLegacy() && m.isItem() && !m.isAir()) list.add(new Entry(m.name(), m));
            }
            case SOUND -> {
                title = "&8Chọn âm thanh";
                for (Sound s : Sound.values()) list.add(new Entry(s.name(), Material.NOTE_BLOCK));
            }
            case PARTICLE -> {
                title = "&8Chọn hạt";
                for (Particle p : Particle.values()) list.add(new Entry(p.name(), Material.BLAZE_POWDER));
            }
            case POTION_EFFECT -> {
                title = "&8Chọn hiệu ứng thuốc";
                // getName() trả tên CŨ (INCREASE_DAMAGE, SLOW...) -> dùng key registry (STRENGTH, SLOWNESS...)
                for (PotionEffectType t : Registry.EFFECT) {
                    list.add(new Entry(t.getKey().getKey().toUpperCase(Locale.ROOT), Material.POTION));
                }
            }
            default -> { return null; }
        }
        list.sort((a, b) -> a.value().compareTo(b.value()));
        return new PickerMenu(viewer, parent, title, list, onPick);
    }

    @Override
    protected void build() {
        List<Entry> view = new ArrayList<>();
        String f = filter.toLowerCase(Locale.ROOT);
        for (Entry e : all) if (f.isEmpty() || e.value().toLowerCase(Locale.ROOT).contains(f)) view.add(e);

        int maxPage = Math.max(0, (view.size() - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, maxPage));
        int from = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && from + i < view.size(); i++) {
            Entry en = view.get(from + i);
            set(i, GuiUtil.item(en.icon(), "&f" + en.value(), "&aClick để chọn"), e -> onPick.accept(en.value()));
        }

        set(45, page > 0 ? GuiUtil.item(Material.ARROW, "&eTrang trước") : GuiUtil.filler(),
                page > 0 ? e -> { page--; refresh(); } : null);
        set(47, GuiUtil.item(Material.COMPASS, "&bTìm kiếm",
                "&7Bộ lọc: " + (filter.isEmpty() ? "&8(không)" : "&f" + filter),
                "&aClick: nhập từ khóa", "&aShift+Click: xóa bộ lọc"), e -> {
            if (e.isShiftClick()) { filter = ""; page = 0; refresh(); return; }
            ChatPrompt.ask(viewer, "Nhập từ khóa tìm kiếm (vd: zombie):", null,
                    v -> { filter = v; page = 0; open(); }, this::open);
        });
        set(48, GuiUtil.item(Material.WRITABLE_BOOK, "&eNhập tay", "&7Dùng khi tên không có trong danh sách."),
                e -> ChatPrompt.ask(viewer, "Nhập giá trị:", s -> s.isBlank() ? "Không được trống." : null,
                        onPick, this::open));
        set(49, GuiUtil.item(Material.BARRIER, "&cQuay lại"), e -> back());
        set(50, GuiUtil.item(Material.PAPER, "&7Trang " + (page + 1) + "/" + (maxPage + 1),
                "&7" + view.size() + " kết quả"));
        set(53, page < maxPage ? GuiUtil.item(Material.ARROW, "&eTrang sau") : GuiUtil.filler(),
                page < maxPage ? e -> { page++; refresh(); } : null);
        fillEmpty();
    }
}
