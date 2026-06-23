package com.example.client.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ConfigCategory {

    public final String name;
    public final List<ToggleEntry> entries = new ArrayList<>();

    public ConfigCategory(String name) {
        this.name = name;
    }

    public ConfigCategory toggle(String label, BooleanSupplier getter, Consumer<Boolean> setter) {
        entries.add(new ToggleEntry(label, getter, setter));
        return this;
    }

    public static class ToggleEntry {
        public final String label;
        public final BooleanSupplier getter;
        public final Consumer<Boolean> setter;

        public ToggleEntry(String label, BooleanSupplier getter, Consumer<Boolean> setter) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
        }
    }
}