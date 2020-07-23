package com.github.chloekoopa.foodhack.behaviors;

import com.sun.istack.internal.Nullable;
import org.bukkit.event.player.PlayerEvent;

/**
 * 给玩家发送消息
 * @author Chloe_koopa
 */
public class ActionMessage extends AbstractAction<PlayerEvent>
{
    protected ActionMessage(@Nullable String msg) {
        super("Message when eaten : §b" + msg);
        this.msg = msg;
    }

    @Override
    public boolean parse(PlayerEvent event) {
        if (msg != null) { event.getPlayer().sendMessage(msg); }
        return msg == null;
    }

    private final String msg;
}
