@file:OptIn(dev.kikugie.stonecutter.StonecutterExperimentalAPI::class)

plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT" apply false
    id("org.jetbrains.kotlin.jvm") version "2.3.0" apply false
}

stonecutter active file(".sc_active_version")