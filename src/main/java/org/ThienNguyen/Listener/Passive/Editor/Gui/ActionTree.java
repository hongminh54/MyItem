package org.ThienNguyen.Listener.Passive.Editor.Gui;

import org.ThienNguyen.Listener.Passive.Editor.ActionNode;

import java.util.List;

/**
 * Cây action đang sửa của một passive. Dựng 1 lần từ draft; mọi danh sách con là danh sách SỐNG nằm trong cây này,
 * nên sau mỗi thay đổi (ở bất kỳ cấp nào) chỉ cần {@link #commit()} để ghi ngược vào draft.
 */
public final class ActionTree {

    private final PassiveEditMenu owner;
    private final List<ActionNode> root;

    public ActionTree(PassiveEditMenu owner) {
        this.owner = owner;
        this.root = owner.draft().getActions();
    }

    public List<ActionNode> root() { return root; }

    /** Ghi cây vào draft và đánh dấu chưa lưu. */
    public void commit() {
        owner.draft().setActions(root);
        owner.markDirty();
    }
}
