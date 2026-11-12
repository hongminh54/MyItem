package org.ThienNguyen.Listener.Passive.Editor.Gui;

import org.ThienNguyen.Listener.Passive.Editor.ActionNode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Chỉnh {@code milestones} (STACK_COUNTER): bảng "số stack" -> danh sách action. Q = xóa mốc. */
public final class MilestonesMenu extends EditorMenu {

    private static final String KEY = "milestones";

    private final ActionTree tree;
    private final ActionNode node;
    private final DeleteMode del = new DeleteMode(false);

    public MilestonesMenu(Player viewer, EditorMenu parent, ActionTree tree, ActionNode node) {
        super(viewer, parent, "&8Các mốc stack", 6);
        this.tree = tree;
        this.node = node;
    }

    @Override
    public void back() {
        if (node.keyedChildren(KEY).isEmpty()) {
            node.removeKeyedChildren(KEY);
            tree.commit();
        }
        super.back();
    }

    private void confirmDelete(String k) {
        new ConfirmMenu(viewer, this, "Xóa mốc " + k + "?", () -> {
            node.keyedChildren(KEY).remove(k);
            tree.commit();
            open();
        }).open();
    }

    @Override
    protected void build() {
        LinkedHashMap<String, List<ActionNode>> ms = node.keyedChildren(KEY);
        List<String> keys = new ArrayList<>(ms.keySet());
        keys.sort((a, b) -> {
            boolean na = a.matches("\\d+"), nb = b.matches("\\d+");
            if (na && nb) return Long.compare(Long.parseLong(a), Long.parseLong(b));
            return a.compareTo(b);
        });

        for (int i = 0; i < keys.size() && i < 45; i++) {
            String k = keys.get(i);
            List<ActionNode> l = ms.get(k);
            set(i, GuiUtil.item(Material.CLOCK, "&eMốc &f" + k,
                    "&7" + l.size() + " action", "", "&aTrái: sửa danh sách", "&cQ / chuột giữa: xóa mốc"), e -> {
                if (DeleteMode.isAltDelete(e.getClick()) || (del.isDelete() && e.getClick() == ClickType.LEFT)) {
                    confirmDelete(k);
                    return;
                }
                switch (e.getClick()) {
                    case LEFT -> new ActionListMenu(viewer, this, tree, l, "&8Mốc " + k, null).open();
                    case DROP, CONTROL_DROP -> { ms.remove(k); tree.commit(); refresh(); }
                    default -> { }
                }
            });
        }

        set(47, del.button(), e -> { del.cycle(); refresh(); });
        set(48, GuiUtil.item(Material.ARROW, "&eQuay lại"), e -> back());
        set(49, GuiUtil.item(Material.EMERALD, "&a+ Thêm mốc", "&7Nhập số stack trong chat."), e ->
                ChatPrompt.ask(viewer, "Nhập số stack của mốc (số nguyên dương):",
                        s -> s.matches("[1-9]\\d{0,8}") ? null : "Phải là số nguyên dương.",
                        v -> {
                            ms.putIfAbsent(v, new ArrayList<>());
                            tree.commit();
                            open();
                        }, this::open));
        fillEmpty();
    }
}
