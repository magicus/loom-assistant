/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant;

import static org.junit.jupiter.api.Assertions.assertFalse;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DummyTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        BuiltInRegistries.bootStrap();
    }

    @Test
    void canCreateStoneItemStackAndPrintItsIdentifier() {
        if (!Items.STONE.builtInRegistryHolder().areComponentsBound()) {
            Items.STONE.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }

        ItemStack stack = new ItemStack(Items.STONE);
        String identifier = stack.getItem().getDescriptionId();

        System.out.println("Stone item identifier: " + identifier);

        assertFalse(identifier.isBlank());
    }
}
