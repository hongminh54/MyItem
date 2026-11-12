package org.ThienNguyen.Listener.Passive.Editor.Gui;

import org.ThienNguyen.Listener.Passive.Editor.PassiveDraft;
import org.ThienNguyen.Listener.Passive.Editor.PassiveFileStore;
import org.ThienNguyen.Listener.Passive.Editor.PassiveValidator;
import org.ThienNguyen.Listener.Passive.Trigger.PassiveTrigger;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Menu chỉnh sửa các thuộc tính cấp cao của một passive. Làm việc trên PassiveDraft, chỉ ghi file khi bấm Lưu. */
public final class PassiveEditMenu extends EditorMenu {

    private static final int UNDO_MAX = 30;

    private final PassiveDraft draft;
    private boolean dirty;
    /** Lịch sử Undo: các bản sao draft TRƯỚC mỗi thay đổi (mới nhất ở đầu). Xóa sau khi Lưu. */
    private final Deque<PassiveDraft> history = new ArrayDeque<>();
    /** Bản sao trạng thái hiện tại (sau thay đổi gần nhất) - sẽ thành bản Undo ở lần thay đổi kế tiếp. */
    private PassiveDraft baseline;
    /** Dirty mà không có bước nào để hoàn tác (passive mới / bản nhân bản chưa lưu). */
    private boolean forcedDirty;

    public PassiveEditMenu(Player viewer, EditorMenu parent, PassiveDraft draft) {
        super(viewer, parent, "&8Passive: " + GuiUtil.clip(draft.getId(), 24), 4);
        this.draft = draft;
        this.baseline = draft.copy();
    }

    public PassiveDraft draft() { return draft; }

    /**
     * Gọi SAU khi đã sửa draft (ActionTree.commit, ConditionMenu, menu này). Nếu nội dung đổi so với lần trước
     * thì đẩy bản trước đó vào lịch sử Undo.
     */
    public void markDirty() {
        if (!draft.raw().equals(baseline.raw())) {
            history.push(baseline);
            while (history.size() > UNDO_MAX) history.removeLast();
            baseline = draft.copy();
        } else if (history.isEmpty()) {
            forcedDirty = true;
        }
        dirty = true;
    }

    /** Có thay đổi chưa lưu? (EditorSessions dùng để cảnh báo khi đóng bằng ESC). */
    public boolean isDirty() { return dirty; }

    /** Mở menu và ghi nhớ làm bản nháp hiện tại của người chơi. */
    @Override
    public void open() {
        super.open();
        EditorSessions.track(this);
    }

    @Override
    protected void build() {
        boolean isNew = draft.getSourceFile() == null;

        set(4, GuiUtil.item(Material.BOOK, "&bPassive hoạt động thế nào?",
                "&71. Sự kiện xảy ra (trigger).",
                "&72. Quét 6 ô trang bị: mũ, giáp, quần,",
                "&7   giày, tay chính, tay phụ.",
                "&73. Kiểm tra điều kiện.",
                "&74. Roll chance.",
                "&75. Kiểm tra cooldown.",
                "&76. Chạy lần lượt các action."));

        set(10, GuiUtil.item(Material.NAME_TAG, "&eID",
                "&7Hiện tại: " + GuiUtil.show(draft.getId()),
                "&8Duy nhất, không dấu cách, phải khớp ID", "&8ghi trên vật phẩm (passive_ids).",
                "", isNew ? "&aClick để đổi" : "&aClick để ĐỔI TÊN (đổi cả tên file)",
                isNew ? "" : "&8Cần lưu hết thay đổi trước. Giữ file .bak."),
                e -> { if (isNew) promptId(); else promptRename(); });

        set(11, GuiUtil.item(Material.OAK_SIGN, "&eTên hiển thị",
                "&7Hiện tại: " + GuiUtil.show(draft.getDisplayName()),
                "&8Hỗ trợ mã màu &", "", "&aClick để đổi"),
                e -> prompt("Nhập tên hiển thị (hỗ trợ &màu):", null, draft::setDisplayName));

        List<String> trigLore = new java.util.ArrayList<>();
        trigLore.add("&7Hiện tại: &f" + draft.getTrigger());
        trigLore.addAll(GuiUtil.wrap(triggerDesc(String.valueOf(draft.getTrigger())), 38, "&7"));
        trigLore.add("");
        trigLore.add("&aChuột trái: kế tiếp");
        trigLore.add("&aChuột phải: trước đó");
        set(12, GuiUtil.item(Material.TRIPWIRE_HOOK, "&eTrigger", trigLore),
                e -> { cycleTrigger(e.isRightClick() ? -1 : 1); refresh(); });

        set(13, GuiUtil.item(Material.RABBIT_FOOT, "&eChance (%)",
                "&7Hiện tại: " + GuiUtil.show(draft.getChance()),
                "&8Xác suất kích hoạt, số 0-100 hoặc biểu thức.",
                "&8Biểu thức tự giới hạn trong 0-100", "&8(vd %player_level% * 2).",
                "&8Roll sau điều kiện, trước cooldown.", "", "&aClick để đổi"),
                e -> prompt("Nhập chance (số hoặc biểu thức, vd 25 hoặc %player_level% * 2):",
                        s -> s.isBlank() ? "Không được trống." : null, draft::setChance));

        set(14, GuiUtil.item(Material.CLOCK, "&eCooldown (giây)",
                "&7Hiện tại: " + GuiUtil.show(draft.getCooldown()),
                "&8Hồi chiêu theo từng người chơi và từng passive.",
                "&80 = không có. Chỉ bắt đầu đếm SAU KHI", "&8passive chạy xong mọi action.",
                "", "&aClick để đổi"),
                e -> prompt("Nhập cooldown (số nguyên giây >= 0):",
                        s -> s.matches("\\d{1,9}") ? null : "Phải là số nguyên >= 0.", draft::setCooldown));

        set(15, GuiUtil.item(Material.COMPARATOR, "&eĐiều kiện",
                "&7Loại mục tiêu: &f" + draft.getTargetType(),
                "&7Phải chí mạng: &f" + draft.isMustBeCrit(),
                "&7HP mục tiêu dưới: " + GuiUtil.show(draft.getTargetHpPercentBelow()),
                "&7Biểu thức: &f" + draft.getConditionExpressions().size(),
                "&8Mọi điều kiện phải đạt; sai 1 cái là", "&8bỏ qua passive trong lần kích hoạt đó.",
                "", "&aClick để mở"),
                e -> new ConditionMenu(viewer, this).open());

        set(16, GuiUtil.item(Material.COMMAND_BLOCK, "&eActions",
                "&7Số action: &f" + draft.getActions().size(),
                "&8Chạy lần lượt từ trên xuống dưới sau khi", "&8qua điều kiện, chance và cooldown.",
                "", "&aClick để mở"),
                e -> {
                    ActionTree tree = new ActionTree(this);
                    new ActionListMenu(viewer, this, tree, tree.root(), "&8Actions", null).open();
                });

        set(27, GuiUtil.item(Material.ARROW, dirty ? "&cQuay lại (bỏ thay đổi)" : "&eQuay lại",
                dirty ? List.of("&7Có thay đổi chưa lưu!", "&7Shift+Click để bỏ và quay lại.") : List.of()),
                e -> {
                    if (dirty && !e.isShiftClick()) {
                        viewer.sendMessage(GuiUtil.color("&cCó thay đổi chưa lưu. Bấm Lưu hoặc Shift+Click để bỏ."));
                        return;
                    }
                    EditorSessions.clear(viewer.getUniqueId()); // bỏ bản nháp
                    back();
                });

        List<String> held = HeldItemPassive.idsOnHeld(viewer);
        set(28, GuiUtil.item(Material.ITEM_FRAME, "&bGán vào vật phẩm đang cầm",
                "&7Tay chính: " + (HeldItemPassive.holdingItem(viewer) ? "&fcó vật phẩm" : "&8(trống)"),
                "&7Passive trên vật phẩm: " + (held.isEmpty() ? "&8(không)" : "&f" + GuiUtil.clip(String.join(", ", held), 34)),
                "", isNew ? "&8Hãy Lưu passive trước khi gán." : "&aTrái: gán &f" + draft.getId(),
                isNew ? "" : "&cPhải: gỡ &f" + draft.getId()),
                e -> onHeldItem(isNew, e.isRightClick()));

        set(29, GuiUtil.item(Material.RECOVERY_COMPASS,
                history.isEmpty() ? "&8Hoàn tác" : "&eHoàn tác &7(" + history.size() + ")",
                "&7Quay về trạng thái trước thay đổi gần nhất.", "&8Xóa lịch sử sau khi Lưu."),
                e -> undo());

        set(30, GuiUtil.item(Material.SPYGLASS, "&bKiểm tra lỗi", "&7In kết quả vào chat."), e -> {
            report(PassiveValidator.validate(draft));
            EditorSessions.quietClose(viewer); // đóng để đọc chat, không cần cảnh báo ESC
            viewer.closeInventory();
        });

        set(31, GuiUtil.item(Material.LIME_DYE, dirty ? "&aLưu &c*" : "&aLưu",
                "&7Ghi ra file và reload passive.",
                "&8Giữ comment đầu file & key cấp 1.",
                "&8Comment trong actions sẽ mất.",
                "&8File cũ sao lưu thành .bak."), e -> save());

        set(33, GuiUtil.item(Material.PAPER, "&bNhân bản", "&7Tạo bản sao với ID mới (chưa lưu)."), e ->
                ChatPrompt.ask(viewer, "Nhập ID cho bản sao:", s -> {
                    if (!PassiveFileStore.ID_PATTERN.matcher(s).matches()) return "ID không hợp lệ.";
                    if (PassiveFileStore.findById(s, null) != null) return "ID đã tồn tại.";
                    return null;
                }, id -> {
                    PassiveDraft c = draft.copy();
                    c.setId(id);
                    PassiveEditMenu m = new PassiveEditMenu(viewer, parent, c);
                    m.markDirty();
                    m.open();
                }, this::open));

        if (!isNew) {
            set(34, GuiUtil.item(Material.TNT, "&cXóa passive", "&7File được giữ lại dạng .bak."), e ->
                    new ConfirmMenu(viewer, this, "Xóa passive " + draft.getId() + "?", () -> {
                        String err = PassiveFileStore.delete(draft);
                        if (err == null) EditorSessions.clear(viewer.getUniqueId());
                        viewer.sendMessage(GuiUtil.color(err == null ? "&aĐã xóa passive." : "&c" + err));
                        if (parent != null) parent.open(); else viewer.closeInventory();
                    }).open());
        }

        set(32, GuiUtil.item(Material.MAP, "&bXem YAML", "&7In YAML sẽ ghi ra chat.", "&8(chưa gồm comment)"),
                e -> YamlPreview.send(viewer, this, "passive " + draft.getId(), draft.raw()));

        fillEmpty();
    }

    private void save() {
        List<PassiveValidator.Issue> issues = PassiveValidator.validate(draft);
        if (PassiveValidator.hasErrors(issues)) {
            report(issues);
            viewer.sendMessage(GuiUtil.color("&cKhông lưu: còn lỗi."));
            EditorSessions.quietClose(viewer);
            viewer.closeInventory();
            return;
        }
        String err = PassiveFileStore.save(draft);
        if (err != null) {
            viewer.sendMessage(GuiUtil.color("&c" + err));
            return;
        }
        dirty = false;
        forcedDirty = false;
        history.clear();
        baseline = draft.copy();
        if (!issues.isEmpty()) report(issues);
        viewer.sendMessage(GuiUtil.color("&aĐã lưu &f" + draft.getSourceFile().getName() + "&a và reload passive."));
        refresh();
    }

    private void report(List<PassiveValidator.Issue> issues) {
        if (issues.isEmpty()) {
            viewer.sendMessage(GuiUtil.color("&aKhông có vấn đề nào."));
            return;
        }
        for (PassiveValidator.Issue i : issues) viewer.sendMessage(i.toString());
    }

    private static String triggerDesc(String trigger) {
        return switch (trigger.toUpperCase(java.util.Locale.ROOT)) {
            case "ON_HIT" -> "Kích hoạt khi chủ passive gây sát thương cho một thực thể. Nạn nhân = thực thể bị đánh.";
            case "ON_TAKE_DAMAGE" -> "Kích hoạt khi chủ passive nhận sát thương từ bất kỳ nguồn nào.";
            case "ON_KILL" -> "Kích hoạt khi chủ passive tung đòn kết liễu một thực thể.";
            case "HP_THRESHOLD" -> "Kích hoạt khi máu nạn nhân tụt dưới ngưỡng %. Cần listener riêng, hãy hỏi admin server trước khi dùng.";
            case "ON_DEATH" -> "Kích hoạt khi chủ passive chết (mọi nguyên nhân). Nạn nhân = kẻ đã giết họ, có thể trống nếu chết do môi trường. Dùng Mục tiêu VICTIM để trả đũa.";
            case "ON_BLOCK_BREAK" -> "Kích hoạt khi chủ passive phá block. Không có nạn nhân nên điều kiện chí mạng hoặc {victim_hp} luôn thất bại. Không chạy nếu BlockBreakEvent bị hủy (WorldGuard/claim).";
            default -> "";
        };
    }

    private void cycleTrigger(int dir) {
        PassiveTrigger[] all = PassiveTrigger.values();
        int idx = 0;
        for (int i = 0; i < all.length; i++) if (all[i].name().equalsIgnoreCase(draft.getTrigger())) idx = i;
        draft.setTrigger(all[Math.floorMod(idx + dir, all.length)]);
        markDirty();
    }

    private void onHeldItem(boolean isNew, boolean remove) {
        if (isNew) {
            viewer.sendMessage(GuiUtil.color("&cHãy Lưu passive trước khi gán vào vật phẩm."));
            return;
        }
        HeldItemPassive.Result r = remove ? HeldItemPassive.remove(viewer, draft.getId())
                : HeldItemPassive.assign(viewer, draft.getId());
        viewer.sendMessage(GuiUtil.color(r.message()));
        refresh();
    }

    /** Hoàn tác thay đổi gần nhất (chỉ trong lúc đang sửa; lịch sử xóa sau khi Lưu). */
    private void undo() {
        if (history.isEmpty()) {
            viewer.sendMessage(GuiUtil.color("&eKhông còn gì để hoàn tác."));
            return;
        }
        PassiveDraft prev = history.pop();
        draft.restoreFrom(prev);
        baseline = draft.copy();
        dirty = !history.isEmpty() || forcedDirty;
        refresh();
    }

    /** Đổi ID passive đã có file: đổi tên file, giữ .bak. Cần không còn thay đổi chưa lưu. */
    private void promptRename() {
        if (dirty) {
            viewer.sendMessage(GuiUtil.color("&cHãy Lưu hoặc bỏ thay đổi trước khi đổi ID."));
            return;
        }
        File cur = draft.getSourceFile();
        ChatPrompt.ask(viewer, "Nhập ID mới (chữ/số/_/-). File sẽ đổi tên thành <id>.yml:", s -> {
            if (!PassiveFileStore.ID_PATTERN.matcher(s).matches()) return "ID không hợp lệ.";
            if (s.equals(draft.getId())) return "ID không đổi.";
            if (PassiveFileStore.findById(s, cur) != null) return "ID đã tồn tại.";
            return null;
        }, newId -> {
            String err = PassiveFileStore.rename(draft, newId);
            if (err != null) {
                viewer.sendMessage(GuiUtil.color("&c" + err));
                open();
                return;
            }
            viewer.sendMessage(GuiUtil.color("&aĐã đổi ID thành &f" + newId + "&a (file &f"
                    + draft.getSourceFile().getName() + "&a, bản .bak được giữ)."));
            viewer.sendMessage(GuiUtil.color("&eLưu ý: vật phẩm đã gán ID cũ cần gán lại ID mới."));
            new PassiveEditMenu(viewer, parent, draft).open(); // dựng lại để tiêu đề theo ID mới
        }, this::open);
    }

    private void promptId() {
        prompt("Nhập ID mới (chữ/số/_/-):", s -> {
            if (!PassiveFileStore.ID_PATTERN.matcher(s).matches()) return "ID không hợp lệ.";
            if (PassiveFileStore.findById(s, null) != null) return "ID đã tồn tại.";
            return null;
        }, draft::setId);
    }

    /** Hỏi chat -> áp giá trị vào draft -> đánh dấu dirty -> mở lại menu. */
    private void prompt(String hint, java.util.function.Function<String, String> validator,
                        java.util.function.Consumer<String> apply) {
        ChatPrompt.ask(viewer, hint, validator, v -> { apply.accept(v); markDirty(); open(); }, this::open);
    }
}
