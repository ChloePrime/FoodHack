package com.github.chloekoopa.foodhack;

import com.github.chloekoopa.foodhack.util.TitleInfo;
import com.google.gson.*;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.script.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class Behavior {

    private static final String IS_REGEX_HEADER = "regex:";
    private static final double EPSILON  = 2e-8;

    /**
     * 对应的config文件
     * 仅在反序列化后存在，普通的构造方法中这个值将为null
     */
    private ConfigurationSection config;
    /**
     * 行为的识别名
     * 等于config的key
     */
    private String key;
    private FoodHack plugin;
    private Pattern regex;
    private int foodLevel;
    private boolean shouldChangeFoodLevel;
    private double health;
    private String message;
    private PotionEffect[] effects;
    private TitleInfo title;
    /**
     * 脚本文件中的函数方法名称
     */
    private String script;
    private boolean hasScript = true;

    private static final String PERM_DEBUG = "foodhack.debug";


    //键的名称们
    private static final String KEY_FOOD_NAME = "name";
    private static final String KEY_FOOD_LEVEL = "hunger";
    private static final String KEY_FOOD_MESSAGE = "message";
    private static final String KEY_FOOD_HEALTH = "health";
    private static final String KEY_FOOD_EFFECT = "effect";
    private static class EffectKeys {
        public static final String TYPE = "type";
        public static final String LEVEL = "level";
        public static final String DURATION = "duration";
        public static final String PARTICLE = "particle";
    }
    private static final String KEY_FOOD_TITLE = "title";
    private static final String KEY_FOOD_SCRIPT = "script";

    /**
     * 从config块中加载行为
     * @param plugin 生成这个行为的插件
     * @param compound config组
     * @param defaultScript 默认的脚本函数名
     * @throws InvalidConfigurationException 如果这一组的name不符合要求（不是字符串类型）则报错
     */
    public Behavior(FoodHack plugin, ConfigurationSection compound, String defaultScript)
            throws InvalidConfigurationException {
        //加载物品名称，如果一个组的物品名称不正常，则抛出异常，这一个key将作废
        if (!compound.isString(KEY_FOOD_NAME)) {
            throw new InvalidConfigurationException("Config section has no name for this food hack!");
        }
        this.key = compound.getName();
        this.plugin = plugin;
        this.regex = getRegExp(compound.getString(KEY_FOOD_NAME));
        //加载饥饿值修改
        this.foodLevel = 0;
        this.shouldChangeFoodLevel = false;
        if (compound.isInt(KEY_FOOD_LEVEL)) {
            foodLevel = compound.getInt(KEY_FOOD_LEVEL);
            this.shouldChangeFoodLevel = true;
        }
        //加载提示信息
        this.message = compound.getString(KEY_FOOD_MESSAGE, null);
        //加载HP异动值
        this.health = compound.getInt(KEY_FOOD_HEALTH, 0);
        //加载药水效果
        this.effects = null;
        if (compound.isList(KEY_FOOD_EFFECT)) {
            Object[] effectList = compound.getList(KEY_FOOD_EFFECT).toArray();
            if (effectList.length > 0 && effectList[0] instanceof HashMap) {
                this.effects = new PotionEffect[effectList.length];
                for (int i = 0; i < effectList.length; i++) {
                    plugin.getLogger().log(Level.INFO, "the map is " + effectList[i].toString());
                    //一个效果的配置文件（未计算的效果）
                    HashMap rawEffect = (HashMap) effectList[i];
                    //检测这个效果是否有效
                    if (rawEffect.containsKey(Behavior.EffectKeys.TYPE) && rawEffect.containsKey(Behavior.EffectKeys.LEVEL) &&
                            rawEffect.containsKey(Behavior.EffectKeys.DURATION)) {
                        boolean hasParticle = (boolean) rawEffect.getOrDefault(Behavior.EffectKeys.PARTICLE, false);
                        PotionEffect effect = new PotionEffect(
                                PotionEffectType.getByName((String) rawEffect.get(Behavior.EffectKeys.TYPE)),
                                (int) rawEffect.get(Behavior.EffectKeys.DURATION),
                                (int) rawEffect.get(Behavior.EffectKeys.LEVEL),
                                false,
                                hasParticle
                        );
                        this.effects[i] = effect;
                    } else {
                        plugin.getLogger().log(Level.WARNING, "Invalid effect for food hack " + defaultScript +
                                " effect " + i);
                        this.effects[i] = null;
                    }
                }
            }
        }
        //加载标题
        this.title = compound.isConfigurationSection(KEY_FOOD_TITLE) ?
                new TitleInfo(compound.getConfigurationSection(KEY_FOOD_TITLE)) : null;
        //加载脚本
        if (compound.isString(KEY_FOOD_SCRIPT)) {
            this.script = compound.getString(KEY_FOOD_SCRIPT);
        } else {
            this.script = compound.getBoolean(KEY_FOOD_SCRIPT, false) ? defaultScript : null;
        }
    }

    /**
     * @deprecated
     */
    public Behavior(@NotNull FoodHack plugin, @NotNull String name, int targetLevel, boolean shouldChange,
                    @Nullable String message, @Nullable PotionEffect[] effect, @Nullable TitleInfo title,
                    @Nullable String script) {
        this.plugin = plugin;
        //处理物品名匹配，如果物品名以regex:开头，则说明物品名是一个独立的正则表达式，否则将物品名填入全局正则表达式中
        this.regex = getRegExp(name);

        this.foodLevel = targetLevel;
        this.shouldChangeFoodLevel = shouldChange;

        this.message = message;
        this.effects = effect;
        this.title = title;
        this.script = script;
    }

    public String getKey() {
        return key;
    }

    /**
     * 获取正则表达式
     * @param configStr config中设置的name字段
     * @return 如果以regex:开头，则返回regex:后的独立的正则表达式，否则将输入字符串应用到全局正则表达式
     */
    private Pattern getRegExp(String configStr) {
        String regExp = configStr.startsWith(IS_REGEX_HEADER) ? configStr.substring(IS_REGEX_HEADER.length())
                :this.plugin.getRegExp(configStr);
        return Pattern.compile(regExp);
    }

    public String getRegex() {
        return this.regex.toString();
    }

    public List<String> getInfo() {
        List<String> info = new ArrayList<>();
        info.add("Key : §b" + key);
        info.add("Matcher regex : §b" + regex.toString());
        if (shouldChangeFoodLevel) info.add("Food level override : §b" + foodLevel);
        if (health < -EPSILON || health > EPSILON) {
            info.add(String.format("Health alteration : §b%.2f", health));
        }
        if (message != null) info.add("Message when eaten : §b" + message);
        if (effects != null && effects.length > 0) {
            info.add("Effects :");
            for (PotionEffect effect : this.effects) {
                info.add("  §eType : §b" + effect.getType().toString().toLowerCase());
                info.add("  §eDuration : §b" + effect.getDuration() + "tick" +
                        (effect.getDuration() > 1 ? "s" : ""));
                info.add("  §eEffect Level : §b" + effect.getAmplifier());
                info.add("  §eHas particle : §b" + effect.hasParticles());
                info.add("");
            }
        }
        if (title != null) {
            info.add("Title : §b" + title.toString());
        }
        if (hasScript && script != null) {
            info.add("Runs method §b" + script + "§e from the script file");
        }
        return info;
    }
    /**
     * 处理一个道具消耗事件
     * @return 这次消耗是否被behavior处理
     */
    public boolean parse(PlayerItemConsumeEvent event) {
        if (!event.isCancelled()) {
            //如果物品没有自定义名称，则物品名会使用bukkit注册名，而不是原版的中文名
            String itemName = event.getItem().hasItemMeta() && event.getItem().getItemMeta().hasDisplayName() ?
                    event.getItem().getItemMeta().getDisplayName() : event.getItem().getType().toString();
            //检查物品名是否匹配
            if (!testName(itemName)) return false;
            //放送调试信息
            if (event.getPlayer().hasPermission(PERM_DEBUG)) {
                this.tellDebugMsg(event.getPlayer());
            }
            boolean edited = false;
            //改变饥饿值
            //如果这个修改内容会改变原本的饥饿值，则需要取消原版事件
            if (this.shouldChangeFoodLevel) {
                edited = true;
                //手动增加饥饿值
                event.getPlayer().setFoodLevel(event.getPlayer().getFoodLevel() + this.foodLevel);
                //添加一个取消计划
                this.plugin.getListener().putDisablingPlan(event.getPlayer());
            }
            //发送消息
            if (this.message != null) {
                edited = true;
                event.getPlayer().sendMessage(this.message);
            }
            //HP异动
            if (this.health > EPSILON) {
                //回血超过上限会报错。。。绝了
                event.getPlayer().setHealth(Math.min(event.getPlayer().getHealth() + this.health,
                        event.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
            } else if (this.health < -EPSILON) {
                event.getPlayer().damage(-this.health);
            }
            //添加药水效果
            if (this.effects != null && this.effects.length > 0) {
                edited = true;
                for (PotionEffect effect : this.effects) {
                    event.getPlayer().addPotionEffect(effect);
                }
            }
            //发送标题
            if (this.title != null) {
                title.sendToPlayer(event.getPlayer());
            }
            //执行脚本
            if (this.hasScript && this.script != null && plugin.getScript() != null) {
                try {
                    plugin.getScript().invokeFunction(this.script, event);
                } catch (ScriptException e) {
                    plugin.getLogger().log(Level.WARNING, "Script " + this.script + " get errored...", e);
                    //把stacktrace发送给想看的人
                    if (event.getPlayer().hasPermission(PERM_DEBUG)) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        event.getPlayer().sendMessage("§c[Error] " + sw.toString());
                    }
                } catch (NoSuchMethodException e) {
                    this.hasScript = false;
                } finally {
                    edited = true;
                }
            }
            //返回结果
            return edited;
        }
        return false;
    }

    /**
     * 测试一个名称是否会被该行为对象匹配
     * @param name 用于测试的名字
     * @return 如果这个名字的食物服用后将会被这个行为解析，则返回true
     */
    public boolean testName(String name) {
        return this.regex.matcher(name).matches();
    }

    private void tellDebugMsg(Player pl) {
        pl.sendMessage("Tweaking event with effect " + (effects == null ? "null" : Arrays.toString(effects)));
        pl.sendMessage("Tweaking event with script " + (script == null ? "null" : script));
    }

    /////////////////////////////////////////////////////
    //
    //             以下内容服务于伟大的扎克
    //
    /////////////////////////////////////////////////////

    //构造带有特殊反序列化器的gson对象
    private static final Deserializer deserializer = new Deserializer();
    private static final Gson gson = (new GsonBuilder().registerTypeAdapter(
            MemoryConfiguration.class, deserializer
    )).create();

    public static Behavior fromJson(FoodHack plugin, String key, String json) {
        plugin.getLogger().log(Level.INFO,"The Json passed is " + json);
        ConfigurationSection config = gson.fromJson(json, MemoryConfiguration.class);
        Behavior behavior;
        try {
            plugin.getLogger().log(Level.INFO, "Converted Config is " + config.getValues(true).toString());
            behavior = new Behavior(plugin, config, key);
            //反序列化的config没有名字，需要手动设置key
            behavior.key = key;
            //只有反序列化后的Behavior才有config，该config用于保存到config文件
            behavior.setParentConfig(config);
            return behavior;
        } catch (InvalidConfigurationException e) {
            plugin.getLogger().log(Level.SEVERE, "Error parsing Json " + key + "!" , e);
            return null;
        }
    }

    public ConfigurationSection getConfig() {return config;}

    /**
     * 仅在反序列化时调用
     */
    private void setParentConfig(ConfigurationSection config) {this.config = config;}

    private static class Deserializer implements JsonDeserializer<MemoryConfiguration> {
        /**
         * 这个方法不会设置这个对象对应的插件
         * 请在构造后手动设置
         */
        @Override
        public MemoryConfiguration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            //将json解析为yaml块
            if (!json.isJsonObject() || json.getAsJsonObject().entrySet().isEmpty()) {
                throw new JsonParseException("Input json should be an object!");
            }
            JsonObject thisJson = json.getAsJsonObject();
            MemoryConfiguration config = new MemoryConfiguration();
            //json -> yaml
            TransformString(thisJson, config, KEY_FOOD_NAME);
            TransformInt(thisJson, config, KEY_FOOD_LEVEL);
            TransformString(thisJson, config, KEY_FOOD_MESSAGE);
            TransformDouble(thisJson, config, KEY_FOOD_HEALTH);
            //读取药水效果
            JsonArray jsonEffect = thisJson.getAsJsonArray(KEY_FOOD_EFFECT);
            if (jsonEffect != null) {
                List<Map<?, ?>> effects = new ArrayList<>(jsonEffect.size());
                jsonEffect.forEach(e -> {
                    JsonObject element = e.getAsJsonObject();
                    ConfigurationSection effect = new MemoryConfiguration();

                    TransformString(element, effect, EffectKeys.TYPE);
                    TransformInt(element, effect, EffectKeys.LEVEL);
                    TransformInt(element, effect, EffectKeys.DURATION);
                    TransformBoolean(element, effect, EffectKeys.PARTICLE);

                    effects.add(effect.getValues(false));
                });
                config.set(KEY_FOOD_EFFECT, effects);
            }
            //读取标题
            JsonObject titleJson = thisJson.getAsJsonObject(KEY_FOOD_TITLE);
            if (titleJson != null) {
                ConfigurationSection title = config.createSection(KEY_FOOD_TITLE);
                TransformString(titleJson, title, TitleInfo.KEY_TITLE);
                TransformString(titleJson, title, TitleInfo.KEY_SUBTITLE);
                TransformInt(titleJson, title, TitleInfo.KEY_FADE_IN);
                TransformInt(titleJson, title, TitleInfo.KEY_STAY);
                TransformInt(titleJson, title, TitleInfo.KEY_FADE_OUT);
            }
            //读取脚本
            if (thisJson.has(KEY_FOOD_SCRIPT)) {
                JsonPrimitive script = thisJson.getAsJsonPrimitive(KEY_FOOD_SCRIPT);
                config.set(KEY_FOOD_SCRIPT, script.isBoolean() ? script.getAsBoolean() : script.getAsString());
            }
            return config;
        }

        private static void TransformBoolean(JsonObject json, ConfigurationSection yaml, String key) {
            yaml.set(key, json.has(key) ? json.get(key).getAsBoolean() : null);
        }

        private static void TransformDouble(JsonObject json, ConfigurationSection yaml, String key) {
            yaml.set(key, json.has(key) ? json.get(key).getAsDouble() : null);
        }

        private static void TransformInt(JsonObject json, ConfigurationSection yaml, String key) {
            yaml.set(key, json.has(key) ? json.get(key).getAsInt() : null);
        }

        private static void TransformString(JsonObject json, ConfigurationSection yaml, String key) {
            yaml.set(key, json.has(key) ? json.get(key).getAsString() : null);
        }
    }
}
