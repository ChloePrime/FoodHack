package com.github.chloekoopa.foodhack;

import com.github.chloekoopa.foodhack.util.ConfigContainer;
import com.sun.istack.internal.Nullable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

/**
 * 插件主类
 * 这个插件允许服务器配置人员根据食物名字给食物添加特效
 * @author Chloe_koopa
 */
public class FoodHack extends JavaPlugin {
    public static final String PLUGIN_PREFIX = "§2[§aFoodHack§2]§r ";

    private EventListener listener = new EventListener(this);
    private ConfigContainer foodList;
    private Invocable script;
    private boolean scriptShouldLoad;

    private static final ScriptEngineManager ENGINES = new ScriptEngineManager();

    private static final String KEY_DEFAULT_REGEX = "default-regex";
    /**
     * 该字符串中{Name}表示道具名称
     */
    private String globalRegex = "";
    private static final String KEY_SCRIPT_LANG = "script-language";
    private static final String KEY_SCRIPT_FILE = "script-file";
    private List<Behavior> behaviors;

    @Override
    public void onEnable() {
        //设置命令执行器
        this.getCommand("food").setExecutor(new CommandFood(this));
        //注册事件监听器
        this.getServer().getPluginManager().registerEvents(listener, this);
        //重载数据
        this.reload();
    }

    public void reload() {
        //存取配置文件
        this.saveDefaultConfig();
        this.reloadConfig();
        //加载配置
        this.globalRegex = this.getConfig().getString(KEY_DEFAULT_REGEX, "{item}");
        //告知脚本需要加载
        if (getConfig().isString(KEY_SCRIPT_FILE) && getConfig().isString(KEY_SCRIPT_LANG)) {
            scriptShouldLoad = true;
        }

        //加载食物列表
        this.foodList = new ConfigContainer(this, "foods.yml");
        this.loadFoodList();
    }

    public boolean addBehavior(Behavior behavior) {
        if (behavior == null || behavior.getConfig() == null) { return false; }
        //删除同key的behavior
        Iterator<Behavior> iterator = behaviors.iterator();
        while (iterator.hasNext()) {
            Behavior element = iterator.next();
            if (element.getKey().equals(behavior.getKey())) {
                iterator.remove();
                break;
            }
        }
        behaviors.add(behavior);
        //保存配置文件
        String key = behavior.getKey();
        if(foodList.get().isConfigurationSection(key)) {
            foodList.get().set(key, null);
        }
        foodList.get().createSection(key, behavior.getConfig().getValues(true));
        foodList.save();
        return true;
    }

    @Nullable
    public Behavior getBehavior(String key) {
        for (Behavior behavior : behaviors) {
            if (behavior.getKey().equals(key)) {
                return behavior;
            }
        }
        return null;
    }

    public List<Behavior> getBehaviors() {return behaviors;}

    /**
     * 返回匹配name的第一个行为
     * @param name 寻求匹配的名称
     * @return 一个包含所有匹配名称的列表
     */
    public List<Behavior> getBehaviorByMatching(String name) {
        List<Behavior> result = new ArrayList<>();
        for (Behavior behavior : behaviors) {
            if (behavior.testName(name)) {
                result.add(behavior);
            }
        }
        return result;
    }

    public EventListener getListener() {return listener;}

    /**
     * 获取一个道具对应的全局正则表达式
     * @param itemName 道具名称
     * @return 道具对应的正则表达式
     */
    public String getRegExp(String itemName) {
        return this.globalRegex.replace("{item}", itemName);
    }

    public Invocable getScript() {
        loadScript();
        return this.script;
    }

    /**
     * 在脚本需要重新加载的时候加载脚本
     */
    public void loadScript()
    {
        if (scriptShouldLoad && script == null) {
            initScript();
            scriptShouldLoad = false;
        }
    }

    private void initScript() {
        String scriptFileName = this.getConfig().getString(KEY_SCRIPT_FILE);
        File scriptFile = new File(this.getDataFolder(), scriptFileName);
        //创建默认脚本文件
        if (!scriptFile.exists()) {
            InputStream defRes = this.getResource(scriptFileName);
            FileOutputStream fos = null;
            if (defRes == null) {
                this.script = null;
            } else {
                try {
                    fos = new FileOutputStream(scriptFile);
                    int dataLength;
                    byte[] data = new byte[1024];
                    while ((dataLength = defRes.read(data))> 0) {
                        fos.write(data, 0, dataLength);
                    }
                } catch (Exception e) {
                    this.script = null;
                    this.getLogger().log(Level.SEVERE,
                            "Cannot find script file with scripts enabled in config!", e);
                } finally {
                    try {
                        defRes.close();
                        if (fos != null) { fos.close(); }
                    } catch (IOException e) {
                        this.getLogger().log(Level.SEVERE, "Error closing script file!", e);
                    }
                }
            }
        }
        //读取脚本文件
        String scriptLanguage = this.getConfig().getString(KEY_SCRIPT_LANG);
        ScriptEngine engine = ENGINES.getEngineByName(scriptLanguage);
        try {
            InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(scriptFile), StandardCharsets.UTF_8);
            engine.eval(reader);
            this.script = (Invocable) engine;
        } catch (IOException e) {
            this.getLogger().log(Level.SEVERE, "Error loading script file!", e);
        } catch (ScriptException e) {
            this.getLogger().log(Level.SEVERE, "Invalid script!", e);
            this.script = null;
        }
    }

    private void loadFoodList() {
        FileConfiguration cfg = foodList.get();
        behaviors = new LinkedList<>();
        for (String key : cfg.getKeys(false)) {
            ConfigurationSection compound = cfg.getConfigurationSection(key);
            try {
                Behavior behavior = new Behavior(this, compound, key);
                this.behaviors.add(behavior);
            } catch (InvalidConfigurationException e) {
                this.getLogger().log(Level.WARNING, "Invalid hack " + key, e);
            }
        }
    }

    public void parseEvent(PlayerItemConsumeEvent event) {
        parseEvent(event, false);
    }

    /**
     * 解析事件，并根据debug的值决定是否记录结果
     * @param debug 是否返回解析结果
     * @return 表示这次解析被哪些behavior捕获，返回一个由这些行为的正则表达式字符串组成的列表
     */
    public List<String> parseEvent(PlayerItemConsumeEvent event, boolean debug) {
        List<String> successCount = debug ? new LinkedList<>() : null;
        this.behaviors.forEach(behavior -> {
            if(behavior.parse(event) && debug) {
                successCount.add("§e" + behavior.getKey() + " : §b" + behavior.getRegex());
            }
        });
        return successCount;
    }
}
