package org.ThienNguyen.Listener.Passive.Editor.Gui;

import org.ThienNguyen.Listener.Passive.Editor.ActionNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Clipboard action theo người chơi (dùng chung giữa các passive). Luôn lưu BẢN SAO sâu; mất khi thoát server. */
final class ActionClipboard {

    private static final int MAX = 45;
    private static final Map<UUID, List<ActionNode>> CLIP = new ConcurrentHashMap<>();

    private ActionClipboard() {}

    /** @return số action trong clipboard sau khi thêm, hoặc -1 nếu đã đầy. */
    static int add(UUID id, ActionNode n) {
        List<ActionNode> l = CLIP.computeIfAbsent(id, k -> new ArrayList<>());
        if (l.size() >= MAX) return -1;
        l.add(n.deepCopy());
        return l.size();
    }

    static int size(UUID id) {
        List<ActionNode> l = CLIP.get(id);
        return l == null ? 0 : l.size();
    }

    static List<String> types(UUID id) {
        List<String> out = new ArrayList<>();
        for (ActionNode n : CLIP.getOrDefault(id, List.of())) out.add(String.valueOf(n.getType()));
        return out;
    }

    /** Bản sao mới (mỗi lần dán là một bản độc lập). */
    static List<ActionNode> copies(UUID id) {
        List<ActionNode> out = new ArrayList<>();
        for (ActionNode n : CLIP.getOrDefault(id, List.of())) out.add(n.deepCopy());
        return out;
    }

    static void clear(UUID id) {
        CLIP.remove(id);
    }
}
