/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.config.clothconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.minecraft.client.gui.screens.Screen;

/**
 * Adds one or more action buttons to the config UI row for the annotated field.
 * */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigButtons {
    ButtonAction[] value();

    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface ButtonAction {
        Class<? extends Screen> screenClass();

        String buttonLabelKey();
    }
}
