package com.github.chloekoopa.foodhack.behaviors;

import org.bukkit.attribute.Attribute;
import org.bukkit.event.player.PlayerEvent;

/**
 * 改变HP
 * >0回血
 * <0造成伤害
 * @author Chloe_koopa
 */
public class ActionHealthChange extends AbstractAction<PlayerEvent>
{
    private static final double EPSILON  = 2e-8;
    protected ActionHealthChange(double amount) {
        super(String.format("Health alteration : §b%.2f", amount));
        this.amount = amount;
        this.belowEps = (amount <= EPSILON) && (amount >= -EPSILON);
    }

    @Override
    public boolean parse(PlayerEvent event) {
        if (this.belowEps) {
            return false;
        }
        if (this.amount > 0) {
            //回血超过上限会报错。。。绝了
            event.getPlayer().setHealth(Math.min(event.getPlayer().getHealth() + this.amount,
                    event.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
        } else if (this.amount < 0) {
            event.getPlayer().damage(-this.amount);
        }
        return true;
    }

    private final double amount;
    /**
     * 输入的数量是否低于下限值（2e-8）
     * 如果是的话那么这个组件不会生效
     */
    private boolean belowEps;
}
