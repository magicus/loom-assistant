/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.weaving;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.LoomMenu;
import se.icus.mag.loomassistant.types.recipe.BannerRecipe;

/** Encapsulates how a banner recipe is woven; hides the creative/survival distinction. */
public abstract class Weaver {
    /** Weave the given banner recipe. */
    public abstract void weave(BannerRecipe banner);

    /** Returns true if a weave operation is currently in progress. */
    public abstract boolean isActive();

    /** Called once per game tick while a weave may be in progress. */
    public abstract void tick();

    /** Returns true if the banner can be woven with the current inventory. */
    public abstract boolean canWeave(BannerRecipe banner);

    /** Returns human-readable descriptions of any missing materials. Empty if nothing is missing. */
    public abstract List<String> getMissingMaterialDescriptions(BannerRecipe banner);

    /** Returns the appropriate Weaver for the current game context. */
    public static Weaver getWeaver(LoomMenu menu) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null && mc.player.hasInfiniteMaterials()) {
            return new CreativeWeaver();
        }
        return new FakeWeaver(menu);
    }
}
