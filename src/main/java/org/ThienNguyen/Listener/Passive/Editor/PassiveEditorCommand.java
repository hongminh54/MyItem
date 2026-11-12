package org.ThienNguyen.Listener.Passive.Editor;

import org.ThienNguyen.Listener.Passive.Editor.Gui.EditorSessions;
import org.ThienNguyen.Listener.Passive.Editor.Gui.PassiveEditMenu;
import org.ThienNguyen.Listener.Passive.Editor.Gui.PassiveListMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * /passiveeditor — mở GUI editor. Quyền: passive.editor (op mặc định).
 * /passiveeditor discard — bỏ bản nháp chưa lưu.
 */
public final class PassiveEditorCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = List.of("discard");

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Chỉ người chơi mới dùng được lệnh này.");
            return true;
        }
        if (!p.hasPermission("passive.editor")) {
            p.sendMessage("§cBạn không có quyền.");
            return true;
        }
        if (args.length > 0) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "discard" -> {
                    boolean had = EditorSessions.pending(p.getUniqueId()) != null;
                    EditorSessions.clear(p.getUniqueId());
                    p.sendMessage(had ? "§aĐã bỏ bản nháp." : "§eKhông có bản nháp nào chưa lưu.");
                    return true;
                }
                default -> {
                    p.sendMessage("§eDùng: /" + label + " [discard]");
                    return true;
                }
            }
        }
        new PassiveListMenu(p).open();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length != 1 || !sender.hasPermission("passive.editor")) return List.of();
        String pre = args[0].toLowerCase(Locale.ROOT);
        return SUBS.stream().filter(s -> s.startsWith(pre)).toList();
    }
}
