package com.github.chloekoopa.foodhack.behaviors;

import com.google.common.collect.ImmutableList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.potion.PotionEffect;

import java.util.Collections;
import java.util.List;

/**
 * @author Chloe_koopa
 */
public class ActionPotionEffect extends AbstractAction<PlayerEvent>
{
    public ActionPotionEffect(List<PotionEffect> effects) {
        super(genInfo(effects));
        this.effects = (effects == null) ? Collections.emptyList() : effects;
    }

    @Override
    public boolean parse(PlayerEvent event) {
        event.getPlayer().addPotionEffects(this.effects);
        return (effects.size() > 0);
    }

    protected static List<String> genInfo(List<PotionEffect> effects) {
        if (effects == null) { return Collections.emptyList(); }

        ImmutableList.Builder<String> info = ImmutableList.builder();
        info.add("Effects :");
        for (PotionEffect effect : effects) {
            info.add("  §eType : §b" + effect.getType().toString().toLowerCase());
            info.add("  §eDuration : §b" + effect.getDuration() + "tick" +
                    (effect.getDuration() > 1 ? "s" : ""));
            info.add("  §eEffect Level : §b" + effect.getAmplifier());
            info.add("  §eHas particle : §b" + effect.hasParticles());
            info.add("");
        }
        return info.build();
    }

    private final List<PotionEffect> effects;
}
