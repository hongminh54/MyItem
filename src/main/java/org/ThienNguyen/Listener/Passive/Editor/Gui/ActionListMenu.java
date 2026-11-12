package org.ThienNguyen.Listener.Passive.Editor.Gui;

import org.ThienNguyen.Listener.Passive.Editor.ActionNode;
import org.ThienNguyen.Listener.Passive.Editor.ActionSchema;
import org.ThienNguyen.Listener.Passive.Editor.ActionSchemaRegistry;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Danh sách action của một cấp (root hoặc con của một node). {@code list} là danh sách SỐNG trong {@link ActionTree}.
 * Điều khiển: Trái = sửa · Phải = nhân bản · Shift+Trái/Phải = lên/xuống · Q = xóa.
 */
public final class ActionListMenu extends EditorMenu {

    private static final int PAGE_SIZE = 45;

    private final ActionTree tree;
    private final List<ActionNode> list;
    private final Runnable onBack;
    private int page;
    private final DeleteMode del = new DeleteMode(true);

    /** @param onBack chạy ngay trước khi quay lại (vd dọn danh sách rỗng); có thể null. */
    public ActionListMenu(Player viewer, EditorMenu parent, ActionTree tree, List<ActionNode> list,
                          String title, Runnable onBack) {
        super(viewer, parent, title, 6);
        this.tree = tree;
        this.list = list;
        this.onBack = onBack;
    }

    @Override
    public void back() {
        if (onBack != null) onBack.run();
        super.back();
    }

    @Override
    protected void build() {
        int maxPage = Math.max(0, (list.size() - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, maxPage));
        int from = page * PAGE_SIZE;

        for (int i = 0; i < PAGE_SIZE && from + i < list.size(); i++) {
            int idx = from + i;
            ActionNode n = list.get(idx);
            ActionSchema s = ActionSchemaRegistry.get(n.getType());
            Material icon = s == null ? Material.BARRIER : s.icon();
            String name = s == null ? "&c" + n.getType() + " &8(không rõ)" : "&e" + s.nameVN() + " &8(" + s.type() + ")";
            set(i, GuiUtil.item(icon, "&f" + (idx + 1) + ". " + name, lore(n, s)), e -> onClick(e, idx));
        }

        set(45, page > 0 ? GuiUtil.item(Material.ARROW, "&eTrang trước") : GuiUtil.filler(),
                page > 0 ? e -> { page--; refresh(); } : null);
        set(46, del.button(), e -> { del.cycle(); refresh(); });
        set(47, pasteButton(), e -> paste(e.isShiftClick()));
        set(48, GuiUtil.item(Material.ARROW, "&eQuay lại"), e -> back());
        set(49, GuiUtil.item(Material.EMERALD, "&a+ Thêm action"), e -> addAction());
        set(50, GuiUtil.item(Material.PAPER, "&7Trang " + (page + 1) + "/" + (maxPage + 1),
                "&7" + list.size() + " action"));
        set(53, page < maxPage ? GuiUtil.item(Material.ARROW, "&eTrang sau") : GuiUtil.filler(),
                page < maxPage ? e -> { page++; refresh(); } : null);
        fillEmpty();
    }

    private List<String> lore(ActionNode n, ActionSchema s) {
        List<String> l = new ArrayList<>();
        int shown = 0;
        for (Map.Entry<String, Object> f : n.fields().entrySet()) {
            if (shown++ >= 4) { l.add("&8..."); break; }
            l.add("&7" + f.getKey() + ": " + GuiUtil.show(f.getValue()));
        }
        for (Map.Entry<String, List<ActionNode>> c : n.allChildren().entrySet())
            l.add("&b" + c.getKey() + ": &f" + c.getValue().size() + " action");
        for (Map.Entry<String, java.util.LinkedHashMap<String, List<ActionNode>>> k : n.allKeyedChildren().entrySet())
            l.add("&b" + k.getKey() + ": &f" + k.getValue().size() + " mốc");
        l.add("");
        l.add("&aTrái: sửa  &aPhải: nhân bản");
        l.add("&aShift+Trái/Phải: lên/xuống");
        l.add("&cQ / chuột giữa: xóa");
        l.add("&8Nút dưới: chế độ xóa/chép, dán.");
        return l;
    }

    private void onClick(InventoryClickEvent e, int idx) {
        ActionNode n = list.get(idx);
        // Creative không gửi sự kiện cho phím Q -> thêm chuột giữa và chế độ xóa (có hộp xác nhận)
        if (DeleteMode.isAltDelete(e.getClick()) || (del.isDelete() && e.getClick() == ClickType.LEFT)) {
            confirmDelete(idx);
            return;
        }
        if (del.isCopy() && e.getClick() == ClickType.LEFT) {
            int size = ActionClipboard.add(viewer.getUniqueId(), n);
            viewer.sendMessage(GuiUtil.color(size < 0 ? "&cClipboard đầy (45 action)."
                    : "&aĐã chép &f" + n.getType() + "&a (clipboard: &f" + size + "&a)."));
            refresh();
            return;
        }
        switch (e.getClick()) {
            case LEFT -> {
                if (ActionSchemaRegistry.get(n.getType()) == null) {
                    viewer.sendMessage(GuiUtil.color("&cType lạ, chỉ có thể xóa hoặc di chuyển trong GUI."));
                    return;
                }
                new ActionEditMenu(viewer, this, tree, n).open();
            }
            case RIGHT -> { list.add(idx + 1, n.deepCopy()); changed(); }
            case SHIFT_LEFT -> { if (idx > 0) { swap(idx, idx - 1); changed(); } }
            case SHIFT_RIGHT -> { if (idx < list.size() - 1) { swap(idx, idx + 1); changed(); } }
            case DROP, CONTROL_DROP -> { list.remove(idx); changed(); }
            default -> { }
        }
    }

    private ItemStack pasteButton() {
        UUID id = viewer.getUniqueId();
        int n = ActionClipboard.size(id);
        if (n == 0) {
            return GuiUtil.item(Material.CHEST, "&8Clipboard trống",
                    "&7Bật chế độ sao chép rồi click", "&7action để chép vào đây.");
        }
        List<String> lore = new ArrayList<>();
        List<String> types = ActionClipboard.types(id);
        for (int i = 0; i < types.size() && i < 6; i++) lore.add("&7" + (i + 1) + ". &f" + types.get(i));
        if (types.size() > 6) lore.add("&8... +" + (types.size() - 6));
        lore.add("");
        lore.add("&aClick: dán vào cuối danh sách");
        lore.add("&cShift+Click: xóa clipboard");
        return GuiUtil.item(Material.ENDER_CHEST, "&bDán " + n + " action", lore);
    }

    /** Dán bản sao các action trong clipboard vào cuối danh sách; clearOnly = xóa clipboard. */
    private void paste(boolean clearOnly) {
        UUID id = viewer.getUniqueId();
        if (clearOnly) { ActionClipboard.clear(id); refresh(); return; }
        List<ActionNode> copies = ActionClipboard.copies(id);
        if (copies.isEmpty()) return;
        list.addAll(copies);
        viewer.sendMessage(GuiUtil.color("&aĐã dán &f" + copies.size() + "&a action."));
        changed();
    }

    private void confirmDelete(int idx) {
        ActionNode n = list.get(idx);
        new ConfirmMenu(viewer, this, "Xóa action " + (idx + 1) + " (" + n.getType() + ")?", () -> {
            if (idx < list.size() && list.get(idx) == n) { list.remove(idx); tree.commit(); }
            open();
        }).open();
    }

    private void swap(int a, int b) {
        ActionNode t = list.get(a);
        list.set(a, list.get(b));
        list.set(b, t);
    }

    private void changed() {
        tree.commit();
        refresh();
    }

    private void addAction() {
        new ActionTypeMenu(viewer, this, type -> {
            ActionNode n = new ActionNode(type);
            list.add(n);
            tree.commit();
            new ActionEditMenu(viewer, this, tree, n).open();
        }).open();
    }
}
