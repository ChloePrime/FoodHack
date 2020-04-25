package com.github.chloekoopa.foodhack.util;

import com.sun.istack.internal.Nullable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;

/**
 * 记录一个title的相关信息
 */
public class TitleInfo {
    public static final String KEY_TITLE = "title";
    public static final String KEY_SUBTITLE = "subtitle";
    public static final String KEY_FADE_IN = "fade-in";
    public static final String KEY_STAY = "stay";
    public static final String KEY_FADE_OUT = "fade-out";

    private String title = "";
    private String subTitle = "";
    private int fadeIn = 0;
    private int stay = 0;
    private int fadeOut = 0;

    public TitleInfo(String title, String subTitle, int fadeIn, int stay, int fadeOut) {
        this.title = title;
        this.subTitle = subTitle;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
    }

    public TitleInfo(@Nullable ConfigurationSection cfg) {
        if (cfg == null) return;
        this.title = cfg.getString(KEY_TITLE, "");
        this.subTitle = cfg.getString(KEY_SUBTITLE, "");
        this.fadeIn = cfg.getInt(KEY_FADE_IN, 0);
        this.stay = cfg.getInt(KEY_STAY, 0);
        this.fadeOut = cfg.getInt(KEY_FADE_OUT, 0);
    }

    public TitleInfo(HashMap<String, Object> map) {
        this.title = (String) map.getOrDefault(KEY_TITLE, "");
        this.subTitle = (String) map.getOrDefault(KEY_SUBTITLE, "");
        this.fadeIn = (int) map.getOrDefault(KEY_FADE_IN, 0);
        this.stay = (int) map.getOrDefault(KEY_STAY, 0);
        this.fadeOut = (int) map.getOrDefault(KEY_FADE_OUT, 0);
    }

    @Override
    public String toString() {
        //下面几个引号是中文引号
        return '“' + title + "”, " +
                '“' + subTitle + "”, " +
                fadeIn + "->" + stay + "->" + fadeOut;
    }

    public void sendToPlayer(Player pl) {
        if (title.equals("") && subTitle.equals("")) return;
        pl.sendTitle(title, subTitle, fadeIn, stay, fadeOut);
    }
}
