package com.github.chloekoopa.foodhack.behaviors;

import com.github.chloekoopa.foodhack.FoodHack;
import org.bukkit.event.player.PlayerEvent;

/**
 * 修改饥饿值
 * 覆盖食物原本的饥饿值
 * @author Chloe_koopa
 */
public class ActionChangeHunger extends AbstractAction<PlayerEvent> {
    private final int level;
    private final FoodHack plugin;

    public ActionChangeHunger(FoodHack plugin, int level) {
        super("Food level override : §b" + level);
        this.plugin = plugin;
        this.level = level;
    }

    @Override
    public boolean parse(PlayerEvent event) {
        //手动增加饥饿值
        event.getPlayer().setFoodLevel(event.getPlayer().getFoodLevel() + this.level);
        //添加一个取消计划
        this.plugin.getListener().putDisablingPlan(event.getPlayer());
        return true;
    }
}
