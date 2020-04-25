package com.github.chloekoopa.foodhack;

import javafx.scene.layout.Priority;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.HashMap;

/**
 * 监听吃东西和饥饿变化事件
 * @author Chloe_koopa
 */
public class EventListener implements Listener {
    private FoodHack plugin;
    /**
     * 记录每一个饥饿值取消计划对应的时间，只有同一tick内的更改计划才会被执行
     */
    private HashMap<HumanEntity, Long> hungerDisabler = new HashMap<>();

    public EventListener(FoodHack plugin) {
        this.plugin = plugin;
    }

    /**
     * 添加一个取消原版进食结果的计划
     * 被这个计划记录的玩家，当前tick内的下一次饱食度变化会被取消
     * 以达到替换原版食物饱食度的功能
     * @param player 要取消原版进食计划的玩家
     */
    public void putDisablingPlan(HumanEntity player) {
        hungerDisabler.put(player, player.getWorld().getFullTime());
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        plugin.parseEvent(event);
    }

    @EventHandler
    public void onEat(FoodLevelChangeEvent event) {
        //只对饱食度变化>0的事件起效
        if (event.getFoodLevel() > 0) {
            HumanEntity player = event.getEntity();
            if (hungerDisabler.containsKey(player)) {
                //取消计划必须在设定的同意刻内才会取消
                if (hungerDisabler.get(player) == player.getWorld().getFullTime()) {
                    event.setCancelled(true);
                }
                hungerDisabler.remove(player);
            }
        }
    }

    /**
     * 为防止潜在的第一次吃食物加载脚本导致卡顿的问题，
     * 在世界加载时确保脚本被读取
     */
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        plugin.loadScript();
    }
}
