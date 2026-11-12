package org.ThienNguyen.Listener.Passive.Editor.Gui;

import org.ThienNguyen.Listener.Passive.Editor.PassiveDraft;
import org.ThienNguyen.Listener.Passive.Editor.PassiveFileStore;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Menu gốc: danh sách file passive (phân trang, tìm kiếm theo id / tên / trigger) + nút tạo mới. */
public final class PassiveListMenu extends EditorMenu {

    private static final int PAGE_SIZE = 45;

    /** Một dòng trong danh sách: file + draft đã đọc (đọc 1 lần mỗi lần mở menu). */
    private record Row(File file, PassiveDraft draft) {}

    private int page;
    private String filter = "";
    private List<Row> rows; // null = cần đọc lại từ đĩa

    public PassiveListMenu(Player viewer) {
        super(viewer, null, "&8Passive Editor", 6);
    }

    /** Mỗi lần mở (kể cả quay lại từ menu con) đọc lại file để thấy thay đổi; refresh() thì dùng lại cache. */
    @Override
    public void open() {
        rows = null;
        super.open();
    }

    private List<Row> loadRows() {
        List<Row> out = new ArrayList<>();
        for (File f : PassiveFileStore.listFiles()) out.add(new Row(f, PassiveFileStore.load(f)));
        return out;
    }

    private boolean matches(Row r, String q) {
        if (q.isEmpty()) return true;
        PassiveDraft d = r.draft();
        String name = ChatColor.stripColor(GuiUtil.color(d.getDisplayName()));
        return contains(d.getId(), q) || contains(name, q) || contains(d.getTrigger(), q);
    }

    private static boolean contains(String s, String q) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(q);
    }

    @Override
    protected void build() {
        if (rows == null) rows = loadRows();
        String q = filter.trim().toLowerCase(Locale.ROOT);
        List<Row> view = new ArrayList<>();
        for (Row r : rows) if (matches(r, q)) view.add(r);

        int maxPage = Math.max(0, (view.size() - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, maxPage));

        int from = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && from + i < view.size(); i++) {
            Row r = view.get(from + i);
            PassiveDraft d = r.draft();
            String id = d.getId().isEmpty() ? "&c(thiếu id)" : d.getId();
            set(i, GuiUtil.item(Material.ENCHANTED_BOOK, "&e" + id,
                    "&7Tên: " + GuiUtil.show(d.getDisplayName()),
                    "&7Trigger: &f" + d.getTrigger(),
                    "&7Số action: &f" + d.getActions().size(),
                    "&8File: " + r.file().getName(),
                    "",
                    "&aClick để chỉnh sửa"),
                    e -> { if (blockedByDraft()) return; new PassiveEditMenu(viewer, this, d).open(); });
        }

        set(45, page > 0 ? GuiUtil.item(Material.ARROW, "&eTrang trước") : GuiUtil.filler(),
                page > 0 ? e -> { page--; refresh(); } : null);
        set(47, GuiUtil.item(Material.COMPASS, "&bTìm kiếm",
                "&7Bộ lọc: " + (filter.isEmpty() ? "&8(không)" : "&f" + GuiUtil.clip(filter, 30)),
                "&8Theo ID, tên hiển thị, trigger.", "",
                "&aClick: nhập từ khóa", "&aShift+Click: xóa bộ lọc"), e -> {
            if (e.isShiftClick()) { filter = ""; page = 0; refresh(); return; }
            ChatPrompt.ask(viewer, "Nhập từ khóa tìm (ID / tên / trigger, vd: on_hit):", null,
                    v -> { filter = v; page = 0; open(); }, this::open);
        });
        set(49, GuiUtil.item(Material.EMERALD, "&a+ Tạo passive mới", "&7Nhập ID trong chat."), e -> createNew());
        set(50, GuiUtil.item(Material.PAPER, "&7Trang " + (page + 1) + "/" + (maxPage + 1),
                "&7" + (q.isEmpty() ? view.size() + " passive" : view.size() + "/" + rows.size() + " passive")));
        set(53, page < maxPage ? GuiUtil.item(Material.ARROW, "&eTrang sau") : GuiUtil.filler(),
                page < maxPage ? e -> { page++; refresh(); } : null);
        set(48, GuiUtil.item(Material.BARRIER, "&cĐóng"), e -> viewer.closeInventory());

        PassiveEditMenu pending = EditorSessions.pending(viewer.getUniqueId());
        if (pending != null) {
            set(46, GuiUtil.item(Material.WRITABLE_BOOK, "&6Tiếp tục bản nháp",
                    "&7Passive: &f" + GuiUtil.clip(pending.draft().getId(), 30),
                    "&7Có thay đổi chưa lưu.", "",
                    "&aClick: mở lại", "&cShift+Click: bỏ bản nháp"), e -> {
                if (e.isShiftClick()) { EditorSessions.clear(viewer.getUniqueId()); refresh(); }
                else pending.open();
            });
        }
        fillEmpty();
    }

    /** Còn bản nháp chưa lưu -> không cho mở/tạo passive khác (tránh ghi đè mất thay đổi). */
    private boolean blockedByDraft() {
        PassiveEditMenu p = EditorSessions.pending(viewer.getUniqueId());
        if (p == null) return false;
        viewer.sendMessage(GuiUtil.color("&cCòn bản nháp chưa lưu (&f" + p.draft().getId()
                + "&c). Hãy tiếp tục hoặc bỏ nó trước (nút &6Tiếp tục bản nháp&c)."));
        return true;
    }

    private void createNew() {
        if (blockedByDraft()) return;
        ChatPrompt.ask(viewer, "Nhập ID cho passive mới (chữ/số/_/-):",
                s -> {
                    if (!PassiveFileStore.ID_PATTERN.matcher(s).matches()) return "ID không hợp lệ.";
                    if (PassiveFileStore.findById(s, null) != null) return "ID đã tồn tại.";
                    return null;
                },
                id -> {
                    PassiveEditMenu m = new PassiveEditMenu(viewer, this, PassiveDraft.blank(id));
                    m.markDirty(); // passive mới luôn coi là chưa lưu
                    m.open();
                },
                this::open);
    }
}
