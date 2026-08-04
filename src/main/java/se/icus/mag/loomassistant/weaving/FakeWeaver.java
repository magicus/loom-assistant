/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.weaving;

import java.util.List;
import net.minecraft.world.inventory.LoomMenu;
import se.icus.mag.loomassistant.types.recipe.BannerRecipe;

/**
 * Weaver for survival mode: drives the loom UI by simulating slot clicks via
 * AutoCraftStateMachine.
 */
public class FakeWeaver extends Weaver {
    private final AutoCraftStateMachine stateMachine;

    public FakeWeaver(LoomMenu menu) {
        this.stateMachine = new AutoCraftStateMachine(menu);
    }

    @Override
    public void weave(BannerRecipe banner) {
        stateMachine.start(banner);
    }

    @Override
    public boolean isActive() {
        return stateMachine.isActive();
    }

    @Override
    public void tick() {
        stateMachine.tick();
    }

    @Override
    public boolean canWeave(BannerRecipe banner) {
        return stateMachine.canCraft(banner);
    }

    @Override
    public List<String> getMissingMaterialDescriptions(BannerRecipe banner) {
        return stateMachine.getMissingMaterialDescriptions(banner);
    }
}
