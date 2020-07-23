package com.github.chloekoopa.foodhack.behaviors;

import org.bukkit.event.Event;

import java.util.List;

/**
 * @author Chloe_koopa
 * @param <T> 接收的事件类型
 */
public interface Action<T extends Event>
{
    /**
     * 获取简介
     * @return 这个行为的简介
     */
    List<String> getInfo();

    /**
     * 应用效果
     * @param event 输入的事件
     * @return 结果是否被改变
     */
    boolean parse(T event);
}
