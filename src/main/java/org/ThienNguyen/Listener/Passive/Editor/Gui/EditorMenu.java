package org.ThienNguyen.Listener.Passive.Editor.Gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Menu nền của editor. Mỗi menu = 1 InventoryHolder; click được {@link EditorGuiListener} chuyển về
 * {@link #handleClick}. Menu con giữ tham chiếu {@code parent} để quay lại (rebuild khi mở lại).
 */
public abstract class EditorMenu implements InventoryHolder {

    @FunctionalInterface
    public interface ClickAction {
        void run(InventoryClickEvent e);
    }

    protected final Player viewer;
    protected final EditorMenu parent;
    private final int rows;
    private final String title;
    private Inventory inv;
    private final Map<Integer, ClickAction> actions = new HashMap<>();

    protected EditorMenu(Player viewer, EditorMenu parent, String title, int rows) {
        this.viewer = viewer;
        this.parent = parent;
        this.title = GuiUtil.color(title);
        this.rows = Math.max(1, Math.min(6, rows));
    }

    /** Vẽ nội dung: gọi {@link #set} / {@link #fillEmpty}. Được gọi mỗi lần open/refresh. */
    protected abstract void build();

    @Override
    public Inventory getInventory() {
        if (inv == null) inv = Bukkit.createInventory(this, rows * 9, title);
        return inv;
    }

    public Player viewer() { return viewer; }

    /** Dựng lại và mở cho người xem. */
    public void open() {
        redraw();
        viewer.openInventory(getInventory());
        // Phải gọi SAU openInventory: để EditorSessions phân biệt "chuyển menu" với "ESC".
        EditorSessions.markOpened(this);
    }

    /** Dựng lại nội dung khi menu đang mở (sau khi đổi giá trị). */
    public void refresh() {
        redraw();
    }

    /** Quay về menu cha (rebuild để thấy thay đổi); không có cha thì đóng. */
    public void back() {
        if (parent != null) parent.open();
        else viewer.closeInventory();
    }

    private void redraw() {
        Inventory i = getInventory();
        i.clear();
        actions.clear();
        build();
    }

    protected void set(int slot, ItemStack item, ClickAction action) {
        if (slot < 0 || slot >= getInventory().getSize()) return;
        getInventory().setItem(slot, item);
        if (action != null) actions.put(slot, action);
    }

    protected void set(int slot, ItemStack item) {
        set(slot, item, null);
    }

    protected void fillEmpty() {
        Inventory i = getInventory();
        for (int s = 0; s < i.getSize(); s++) {
            if (i.getItem(s) == null) i.setItem(s, GuiUtil.filler());
        }
    }

    protected int lastRowSlot(int col) {
        return (rows - 1) * 9 + col;
    }

    /** Gọi từ listener. Mặc định chỉ chạy action của slot trong inventory trên. */
    void handleClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null || e.getClickedInventory() != getInventory()) return;
        ClickAction a = actions.get(e.getSlot());
        if (a != null) a.run(e);
    }
}
