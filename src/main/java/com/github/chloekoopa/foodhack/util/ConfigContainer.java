package com.github.chloekoopa.foodhack.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * 把bukkit教程中写到的新配置文件教程封装了一下
 */
public class ConfigContainer {
    private JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;

    public ConfigContainer(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), fileName);
        this.reload();
    }

    public FileConfiguration get() {return this.config;}

    public void reload() {
        //创建默认配置文件
        if (!this.configFile.exists()) {
            this.plugin.saveResource(this.configFile.getName(), false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void save() {
        try {
            this.config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving config " + configFile.getName(), e);
        }
    }
}
