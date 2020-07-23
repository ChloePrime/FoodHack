package com.github.chloekoopa.foodhack.behaviors;

import com.google.common.collect.ImmutableList;
import org.bukkit.event.Event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 空响应，啥都不做
 */
public class ActionDoNothing implements Action<Event> {
    private final List<String> desc;
    private static Map<String, ActionDoNothing> cache = new HashMap<>(30);

    protected ActionDoNothing(String desc)
    {
        this.desc = ImmutableList.of(desc);
    }

    public static ActionDoNothing of(String desc)
    {
        return new ActionDoNothing(desc);
    }

    @Override
    public List<String> getInfo() {
        return desc;
    }

    @Override
    public boolean parse(Event event) {
        return true;
    }
}
