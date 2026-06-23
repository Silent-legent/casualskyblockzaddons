package com.example.client.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ConfigCategory {

    //   FIELDS (Category name and its contained list of toggle options)
    public final String name;
    public final List<ToggleEntry> entries = new ArrayList<>();

    //   CONSTRUCTOR
    public ConfigCategory(String name) {
        this.name = name;
    }

    //   BUILDER METHODS (Used to chain settings onto a category easily)
    public ConfigCategory toggle(String label, BooleanSupplier getter, Consumer<Boolean> setter) {
        entries.add(new ToggleEntry(label, getter, setter));
        return this;
    }

    //   SUBCLASS: TOGGLE ENTRY (Data holder for an individual config option)
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