package com.github.chloekoopa.foodhack.actions;

import com.github.chloekoopa.foodhack.FoodHack;
import org.bukkit.event.player.PlayerEvent;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;

public class ActionScript extends AbstractAction<PlayerEvent>
{
    public static final String PERM_DEBUG = "foodhack.debug";

    public ActionScript(FoodHack plugin, String func) {
        super("Runs method §b" + func);
        this.plugin = plugin;
        this.func = func;
        this.theBigScript = script;
        this.hasScript = (script == null);
    }
    @Override
    public boolean parse(PlayerEvent event) {
        //执行脚本
        if ( (!this.errored) && (plugin.getScript() != null) ) {
            try {
                plugin.getScript().invokeFunction(this.func, event);
                return true;
            } catch (NoSuchMethodException e) {
                this.hasScript = false;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Script " + this.func + " get errored...", e);
                //把stacktrace发送给想看的人
                if (event.getPlayer().hasPermission(PERM_DEBUG)) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    event.getPlayer().sendMessage("§c[Error] " + sw.toString());
                }
            }
        }
        return false;
    }

    private final FoodHack plugin;
    private final String func;
    private final Invocable theBigScript;
    boolean hasScript;
    boolean errored;
}
