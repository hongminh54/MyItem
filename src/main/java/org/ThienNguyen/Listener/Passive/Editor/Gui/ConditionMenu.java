package org.ThienNguyen.Listener.Passive.Editor.Gui;

import org.ThienNguyen.Listener.Passive.Editor.PassiveDraft;
import org.ThienNguyen.Listener.Passive.TargetType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Chỉnh khối {@code condition:} của passive. Parent bắt buộc là {@link PassiveEditMenu}. */
public final class ConditionMenu extends EditorMenu {

    private final PassiveEditMenu owner;
    private final PassiveDraft draft;

    public ConditionMenu(Player viewer, PassiveEditMenu owner) {
        super(viewer, owner, "&8Điều kiện", 3);
        this.owner = owner;
        this.draft = owner.draft();
    }

    @Override
    protected void build() {
        List<String> targetLore = new ArrayList<>();
        targetLore.add("&7Hiện tại: &f" + draft.getTargetType());
        targetLore.addAll(GuiUtil.wrap(targetTypeDesc(String.valueOf(draft.getTargetType())), 38, "&7"));
        targetLore.add("");
        targetLore.add("&aChuột trái: kế tiếp");
        targetLore.add("&aChuột phải: trước đó");
        set(10, GuiUtil.item(Material.PLAYER_HEAD, "&eLoại mục tiêu", targetLore),
                e -> { cycleTarget(e.isRightClick() ? -1 : 1); changed(); });

        set(11, GuiUtil.item(draft.isMustBeCrit() ? Material.LIME_DYE : Material.GRAY_DYE, "&ePhải chí mạng",
                "&7Hiện tại: &f" + draft.isMustBeCrit(),
                "&8true = chỉ kích hoạt khi đòn đánh là chí mạng.",
                "&8Không có tác dụng với ON_BLOCK_BREAK.", "", "&aClick để bật/tắt"),
                e -> { draft.setMustBeCrit(!draft.isMustBeCrit()); changed(); });

        boolean hpOff = "-1".equals(draft.getTargetHpPercentBelow());
        set(12, GuiUtil.item(Material.REDSTONE, "&eHP mục tiêu dưới (%)",
                "&7Hiện tại: " + (hpOff ? "&8(tắt)" : "&f" + draft.getTargetHpPercentBelow()),
                "&8Chỉ kích hoạt nếu máu nạn nhân đang thấp hơn",
                "&8% này so với máu tối đa (vd 5 = dưới 5%).",
                "&8Nhận số hoặc biểu thức.", "",
                "&aClick: nhập giá trị", "&aShift+Click: tắt"),
                e -> {
                    if (e.isShiftClick()) { draft.setTargetHpPercentBelow(null); changed(); return; }
                    ChatPrompt.ask(viewer, "Nhập ngưỡng HP% (vd 5 hoặc %player_level% * 0.5):",
                            s -> s.isBlank() ? "Không được trống." : null,
                            v -> { draft.setTargetHpPercentBelow(v); owner.markDirty(); open(); },
                            this::open);
                });

        List<String> exprs = draft.getConditionExpressions();
        List<String> lore = new ArrayList<>();
        if (exprs.isEmpty()) lore.add("&8(chưa có)");
        for (int i = 0; i < exprs.size(); i++) lore.add("&7" + (i + 1) + ". &f" + GuiUtil.clip(exprs.get(i), 40));
        lore.add("");
        lore.add("&aClick: thêm biểu thức");
        lore.add("&aChuột phải: xóa cái cuối");
        lore.add("&aShift+Phải: xóa hết");
        lore.add("");
        lore.add("&7Mỗi dòng đúng 1 phép so sánh:");
        lore.add("&8>=  <=  ==  !=  >  <");
        lore.add("&8Không dùng AND/OR trong 1 dòng, hãy");
        lore.add("&8thêm dòng mới (các dòng được AND).");
        lore.add("");
        lore.add("&7Placeholder dùng được:");
        lore.addAll(GuiUtil.wrap("{damage} {actor_level} {actor_hp} {actor_max_hp} {actor_missing_hp} "
                + "{victim_hp} {victim_max_hp} {victim_missing_hp} {victim_hp_percent} và %papi% bất kỳ.", 38, "&8"));
        set(14, GuiUtil.item(Material.WRITABLE_BOOK, "&eBiểu thức tùy ý", lore), e -> {
            if (e.isRightClick()) {
                List<String> cur = new ArrayList<>(exprs);
                if (e.isShiftClick()) cur.clear();
                else if (!cur.isEmpty()) cur.remove(cur.size() - 1);
                draft.setConditionExpressions(cur);
                changed();
                return;
            }
            ChatPrompt.ask(viewer, "Nhập biểu thức có toán tử so sánh (vd {damage} > 5.0):",
                    s -> s.matches(".*(>=|<=|==|!=|>|<).*") ? null : "Thiếu toán tử (>=, <=, ==, !=, >, <).",
                    v -> {
                        List<String> cur = new ArrayList<>(draft.getConditionExpressions());
                        cur.add(v);
                        draft.setConditionExpressions(cur);
                        owner.markDirty();
                        open();
                    }, this::open);
        });

        set(22, GuiUtil.item(Material.ARROW, "&eQuay lại"), e -> back());
        fillEmpty();
    }

    private static String targetTypeDesc(String type) {
        return switch (type.toUpperCase(java.util.Locale.ROOT)) {
            case "BOTH" -> "Bất kỳ thực thể nào, người chơi hay mob (mặc định).";
            case "PLAYER" -> "Chỉ khi nạn nhân là người chơi khác.";
            case "MOB" -> "Chỉ khi nạn nhân là mob (không phải người chơi).";
            case "SELF" -> "Chỉ khi chủ passive tự gây sát thương cho chính mình.";
            default -> "";
        };
    }

    private void cycleTarget(int dir) {
        TargetType[] all = TargetType.values();
        int idx = 0;
        for (int i = 0; i < all.length; i++) if (all[i].name().equalsIgnoreCase(draft.getTargetType())) idx = i;
        TargetType next = all[Math.floorMod(idx + dir, all.length)];
        // BOTH là mặc định -> xóa key cho YAML gọn
        draft.setTargetType(next == TargetType.BOTH ? null : next.name());
    }

    private void changed() {
        owner.markDirty();
        refresh();
    }
}
