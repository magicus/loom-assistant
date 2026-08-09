/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.ConfigSerializer;
import me.shedaniel.autoconfig.util.Utils;

public class NestedPathConfigSerializer<T extends ConfigData> implements ConfigSerializer<T> {
    private final Config definition;
    private final Class<T> configClass;
    private final Gson gson;

    public NestedPathConfigSerializer(Config definition, Class<T> configClass) {
        this(definition, configClass, new GsonBuilder().setPrettyPrinting().create());
    }

    NestedPathConfigSerializer(Config definition, Class<T> configClass, Gson gson) {
        this.definition = definition;
        this.configClass = configClass;
        this.gson = gson;
    }

    protected Path getConfigPath() {
        return Utils.getConfigFolder().resolve(definition.name()).resolve("config.json");
    }

    @Override
    public void serialize(T config) throws ConfigSerializer.SerializationException {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            try (var writer = Files.newBufferedWriter(path)) {
                gson.toJson(config, writer);
            }
        } catch (IOException e) {
            throw new ConfigSerializer.SerializationException(e);
        }
    }

    @Override
    public T deserialize() throws ConfigSerializer.SerializationException {
        Path path = getConfigPath();
        if (!Files.exists(path)) {
            return createDefault();
        }

        try (var reader = Files.newBufferedReader(path)) {
            return gson.fromJson(reader, configClass);
        } catch (IOException | JsonParseException e) {
            throw new ConfigSerializer.SerializationException(e);
        }
    }

    @Override
    public T createDefault() {
        return Utils.constructUnsafely(configClass);
    }
}
