package com.github.chloekoopa.foodhack;

import com.google.gson.JsonSyntaxException;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class CommandFood implements CommandExecutor {
    private FoodHack plugin;
    //权限相关
    private static final String PERM_CREATE = "foodhack.create";
    private static final String PERM_LIST = "foodhack.list";
    private static final String PERM_QUERY = "foodhack.query";
    private static final String PERM_RELOAD = "foodhack.reload";
    private static final String PERM_TRY = "foodhack.try";
    private static final String NO_PERM_MSG = FoodHack.PLUGIN_PREFIX +
            "§cYou don't have permission to use this command!";
    public CommandFood (FoodHack plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            switch (args[0]) {
                case "create":
                    return this.onCreate(sender, args);
                case "help":
                    return this.onHelp(sender);
                case "list":
                    return this.onGetList(sender);
                case "query":
                    return onQuery(sender, args);
                case "reload":
                    return this.onReload(sender);
                case "try":
                    return this.onTry(sender, args);
                default:
                    break;
            }
        }
        return false;
    }

    private boolean onCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_CREATE)) {
            sender.sendMessage(NO_PERM_MSG);
            return true;
        }
        if (args.length < 3) return false;
        String id = args[1];
        StringBuilder jsonText = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            jsonText.append(args[i]);
        }
        Behavior behavior;
        try {
            behavior = Behavior.fromJson(plugin, id, jsonText.toString());
        } catch (JsonSyntaxException e) {
            sender.sendMessage(FoodHack.PLUGIN_PREFIX + "§cInvalid Json! " + e.getMessage());
            return true;
        }
        boolean added = plugin.addBehavior(behavior);
        if (added) {
            sender.sendMessage(FoodHack.PLUGIN_PREFIX + "§aCreated item from json, hoping it won't get errors...");
            optionallyPlaySound(sender);
        }
        return added;
    }

    private boolean onHelp(CommandSender sender) {
        List<String> helpMsg = new ArrayList<>();
        if (sender.hasPermission(PERM_CREATE)) helpMsg.add(
                FoodHack.PLUGIN_PREFIX + "§e/food create <key> <(json)> :§b creates a new food hack from a json"
        );
        if (sender.hasPermission(PERM_LIST)) helpMsg.add(
                FoodHack.PLUGIN_PREFIX + "§e/food list :§b get all available hacks' keys"
        );
        if (sender.hasPermission(PERM_QUERY)) helpMsg.add(
                FoodHack.PLUGIN_PREFIX + "§e/food query <key> :§b get the hack's info with the given key"
        );
        if (sender.hasPermission(PERM_RELOAD)) helpMsg.add(
                FoodHack.PLUGIN_PREFIX + "§e/food reload :§b reloads hack lists"
        );
        if (sender.hasPermission(PERM_TRY)) helpMsg.add(
                FoodHack.PLUGIN_PREFIX + "§e/food try <name> :§b tries a hack effect for the specific named food"
        );
        if (helpMsg.isEmpty()) {
            sender.sendMessage("§cCurrently there's no available sub command within your permission!");
        } else {
            String[] msg = new String[helpMsg.size()];
            sender.sendMessage(helpMsg.toArray(msg));
        }
        return true;
    }

    private boolean onGetList(CommandSender sender) {
        if (!sender.hasPermission(PERM_LIST)) {
            sender.sendMessage(NO_PERM_MSG);
            return true;
        }
        if (plugin.getBehaviors().size() == 0) {
            sender.sendMessage(FoodHack.PLUGIN_PREFIX + "There's no hack present!");
        } else {
            sender.sendMessage(FoodHack.PLUGIN_PREFIX + "§aHack List :");
            plugin.getBehaviors().forEach(behavior ->
                    sender.sendMessage(FoodHack.PLUGIN_PREFIX + "§e  - §b" + behavior.getKey())
            );
        }
        return true;
    }

    private boolean onQuery(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_QUERY)) {
            sender.sendMessage(NO_PERM_MSG);
            return true;
        }
        if (args.length < 2) return false;
        Behavior behavior = plugin.getBehavior(args[1]);
        if (behavior != null) {
            List<String> info = behavior.getInfo();
            String[] message = new String[info.size()];
            for (int i = 0; i < message.length; i++) {
                message[i] = FoodHack.PLUGIN_PREFIX + "§e" + info.get(i);
            }
            sender.sendMessage(message);
        } else {
            sender.sendMessage(FoodHack.PLUGIN_PREFIX + "Unable to find hack with key ");
        }
        return true;
    }

    private boolean onReload(CommandSender sender) {
        if (sender.hasPermission(PERM_RELOAD)) {
            try {
                plugin.reload();
                sender.sendMessage(FoodHack.PLUGIN_PREFIX + "§aReload succeeded!");
                optionallyPlaySound(sender);
            } catch (Exception e) {
                String errorMsg = "Error reloading " + plugin.getName();
                if (!(sender instanceof ConsoleCommandSender)) {
                    sender.sendMessage(FoodHack.PLUGIN_PREFIX + "§a" + errorMsg);
                }
                plugin.getLogger().log(Level.SEVERE, errorMsg, e);
            }
        } else {
            sender.sendMessage(NO_PERM_MSG);
        }
        return true;
    }

    private boolean onTry(CommandSender sender, String[] args) {
        //不是玩家执行的情况
        //该命令只能由玩家发出
        if (!(sender instanceof Player)) {
            sender.sendMessage(FoodHack.PLUGIN_PREFIX + "§cThis command can only be used by player!");
            return false;
        }
        //执行者没有权限的情况
        if (!sender.hasPermission(PERM_TRY)) {
            sender.sendMessage(NO_PERM_MSG);
            return true;
        }
        //参数过少的情况
        if (args.length < 2) {
            sender.sendMessage(FoodHack.PLUGIN_PREFIX + "Too few args!");
            return false;
        }
        //生成一个命名的石头道具
        ItemStack dummy = new ItemStack(Material.STONE);
        ItemMeta dummyMeta = dummy.getItemMeta();
        dummyMeta.setDisplayName(args[1].replace("&", "§"));
        dummy.setItemMeta(dummyMeta);

        //解析事件并记录结果
        List<String> result = plugin.parseEvent(
                new PlayerItemConsumeEvent((Player) sender, dummy), true);

        StringBuilder msg = (new StringBuilder(FoodHack.PLUGIN_PREFIX)).append("§aEat result is modified by\n");
        result.forEach(str -> msg.append(FoodHack.PLUGIN_PREFIX).append("<§b").append(str).append("§a>\n"));

        sender.sendMessage(msg.toString());
        optionallyPlaySound(sender);
        return true;
    }

    /**
     * 如果命令发送者是个玩家则发送经验球音效
     */
    private void optionallyPlaySound(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = ((Player) sender);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }
}
