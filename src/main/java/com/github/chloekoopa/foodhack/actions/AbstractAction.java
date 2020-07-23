package com.github.chloekoopa.foodhack.behaviors;

import com.google.common.collect.ImmutableList;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Chloe_koopa
 * @param <T> 接收的事件类型
 */
public abstract class AbstractAction<T extends Event>
        implements Action<T>
{
    protected List<String> info;
    protected AbstractAction(String info) {
        this.info = ImmutableList.of(info);
    }

    protected AbstractAction(List<String> info) {
        this.info = info;
    }

    @Override
    public List<String> getInfo() {
        return info;
    }
}
