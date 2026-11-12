package org.ThienNguyen.Listener.Passive.Editor.Gui;

import org.ThienNguyen.Listener.Passive.Editor.ActionNode;
import org.ThienNguyen.Listener.Passive.Editor.FieldDef;
import org.ThienNguyen.Listener.Passive.Editor.FieldKind;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chỉnh field {@link FieldKind#EFFECT_LIST} (POTION_ZONE.effects): danh sách map
 * {type, amplifier, duration-seconds, ambient, particles, icon}. Trái = sửa · Q = xóa.
 */
public final class EffectListMenu extends EditorMenu {

    private final ActionTree tree;
    private final ActionNode node;
    private final String key;
    private final DeleteMode del = new DeleteMode(false);

    public EffectListMenu(Player viewer, EditorMenu parent, ActionTree tree, ActionNode node, FieldDef field) {
        super(viewer, parent, "&8" + field.nameVN(), 5);
        this.tree = tree;
        this.node = node;
        this.key = field.key();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> entries() {
        List<Map<String, Object>> out = new ArrayList<>();
        if (node.getField(key) instanceof List<?> l) {
            for (Object o : l) if (o instanceof Map<?, ?> m) out.add((Map<String, Object>) m);
        }
        return out;
    }

    private void store(List<Map<String, Object>> list) {
        node.setField(key, list.isEmpty() ? null : new ArrayList<Object>(list)); // rỗng -> xóa key
        tree.commit();
    }

    @Override
    protected void build() {
        List<Map<String, Object>> list = entries();
        for (int i = 0; i < list.size() && i < 36; i++) {
            Map<String, Object> en = list.get(i);
            int idx = i;
            set(i, GuiUtil.item(Material.POTION, "&e" + GuiUtil.show(en.get("type")),
                    "&7Cấp (amplifier): &f" + en.getOrDefault("amplifier", "0"),
                    "&7Thời gian (giây): &f" + en.getOrDefault("duration-seconds", "4"),
                    "&7Ambient/Hạt/Icon: &f" + en.getOrDefault("ambient", false) + "/"
                            + en.getOrDefault("particles", true) + "/" + en.getOrDefault("icon", true),
                    "", "&aTrái: sửa", "&cQ / chuột giữa: xóa"), e -> {
                if (DeleteMode.isAltDelete(e.getClick()) || (del.isDelete() && e.getClick() == ClickType.LEFT)) {
                    confirmDelete(idx, en);
                    return;
                }
                switch (e.getClick()) {
                    case LEFT -> new Entry(viewer, this, tree, en).open();
                    case DROP, CONTROL_DROP -> { list.remove(idx); store(list); refresh(); }
                    default -> { }
                }
            });
        }
        set(39, del.button(), e -> { del.cycle(); refresh(); });
        set(40, GuiUtil.item(Material.ARROW, "&eQuay lại"), e -> back());
        set(41, GuiUtil.item(Material.EMERALD, "&a+ Thêm hiệu ứng"), e -> {
            PickerMenu p = PickerMenu.forKind(viewer, this, FieldKind.POTION_EFFECT, type -> {
                List<Map<String, Object>> cur = entries();
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("type", type.toUpperCase());
                cur.add(m);
                store(cur);
                open();
            });
            if (p != null) p.open();
        });
        fillEmpty();
    }

    private void confirmDelete(int idx, Map<String, Object> en) {
        new ConfirmMenu(viewer, this, "Xóa hiệu ứng " + (idx + 1) + "?", () -> {
            List<Map<String, Object>> cur = entries();
            if (idx < cur.size() && cur.get(idx) == en) { cur.remove(idx); store(cur); }
            open();
        }).open();
    }

    /** Sửa một hiệu ứng. Map được sửa trực tiếp (là phần tử sống trong field), chỉ cần commit. */
    private static final class Entry extends EditorMenu {
        private final ActionTree tree;
        private final Map<String, Object> en;

        Entry(Player viewer, EditorMenu parent, ActionTree tree, Map<String, Object> en) {
            super(viewer, parent, "&8Sửa hiệu ứng", 3);
            this.tree = tree;
            this.en = en;
        }

        @Override
        protected void build() {
            set(10, GuiUtil.item(Material.POTION, "&eLoại hiệu ứng", "&7Hiện tại: " + GuiUtil.show(en.get("type")),
                    "&aClick để chọn"), e -> {
                PickerMenu p = PickerMenu.forKind(viewer, this, FieldKind.POTION_EFFECT, t -> {
                    en.put("type", t.toUpperCase());
                    tree.commit();
                    open();
                });
                if (p != null) p.open();
            });
            exprItem(11, "amplifier", "0", "Cấp (0 = cấp I)", Material.REDSTONE);
            exprItem(12, "duration-seconds", "4", "Thời gian (giây)", Material.CLOCK);
            boolItem(14, "ambient", false, "Ambient");
            boolItem(15, "particles", true, "Hiện hạt");
            boolItem(16, "icon", true, "Hiện icon");
            set(22, GuiUtil.item(Material.ARROW, "&eQuay lại"), e -> back());
            fillEmpty();
        }

        private void exprItem(int slot, String k, String def, String name, Material icon) {
            set(slot, GuiUtil.item(icon, "&e" + name, "&7Hiện tại: " + GuiUtil.show(en.getOrDefault(k, def)),
                    "&aClick: nhập (số/biểu thức)", "&cShift+Click: về mặc định (" + def + ")"), e -> {
                if (e.isShiftClick()) { en.remove(k); tree.commit(); refresh(); return; }
                ChatPrompt.ask(viewer, "Nhập " + name + " (số hoặc biểu thức):",
                        s -> s.isBlank() ? "Không được trống." : null,
                        v -> { en.put(k, ExprValue.of(v)); tree.commit(); open(); }, this::open);
            });
        }

        private void boolItem(int slot, String k, boolean def, String name) {
            boolean cur = Boolean.parseBoolean(String.valueOf(en.getOrDefault(k, def)));
            set(slot, GuiUtil.item(cur ? Material.LIME_DYE : Material.GRAY_DYE, "&e" + name,
                    "&7Hiện tại: &f" + cur, "&aClick để bật/tắt"), e -> {
                en.put(k, !cur);
                tree.commit();
                refresh();
            });
        }
    }
}
