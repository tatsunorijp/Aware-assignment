package com.example.awarechat_android

import java.nio.file.Files
import java.nio.file.Path

object TestFixtures {
    fun protocol(name: String): String {
        val fixtureDirectory = generateSequence(Path.of(System.getProperty("user.dir"))) { it.parent }
            .map { it.resolve("fixtures/protocol") }
            .firstOrNull(Files::isDirectory)
            ?: error("Could not locate fixtures/protocol from the test working directory")
        return Files.readString(fixtureDirectory.resolve(name))
    }
}
