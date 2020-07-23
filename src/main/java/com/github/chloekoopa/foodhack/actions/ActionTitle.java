package com.github.chloekoopa.foodhack.behaviors;

import com.github.chloekoopa.foodhack.util.TitleInfo;
import org.bukkit.event.player.PlayerEvent;

import java.util.List;

public class ActionTitle extends AbstractAction<PlayerEvent> {
    protected ActionTitle(TitleInfo title) {
        super("Title : §b" + title.toString() + title);
    }

    @Override
    public boolean parse(PlayerEvent event) {
        return false;
    }
}
