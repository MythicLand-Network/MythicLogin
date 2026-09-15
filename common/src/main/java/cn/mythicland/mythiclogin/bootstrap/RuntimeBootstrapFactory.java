package cn.mythicland.mythiclogin.bootstrap;

import cn.mythicland.framework.runtime.RuntimeBootstrap;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class RuntimeBootstrapFactory {
    private RuntimeBootstrapFactory() {
    }

    public static RuntimeBootstrap create(
            Object plugin,
            String mainLoaderName,
            Path dataDirectory,
            Set<String> activePlatforms,
            Consumer<String> infoLogger,
            Consumer<String> warningLogger
    ) {
        return RuntimeBootstrap.builder(plugin, mainLoaderName)
                .runtimeName("MythicLogin")
                .libraries(
                        dataDirectory,
                        "META-INF/mythiclogin-libraries.properties",
                        activePlatforms
                )
                .loggers(infoLogger, warningLogger)
                .childFirstPackages(List.of(
                        "cn.mythicland.mythiclogin.",
                        "org.reflections.",
                        "javassist.",
                        "com.zaxxer.hikari.",
                        "org.flywaydb.",
                        "org.postgresql.",
                        "org.sqlite.",
                        "org.yaml.snakeyaml.",
                        "com.resend.",
                        "okhttp3.",
                        "okio.",
                        "kotlin.",
                        "org.jetbrains.",
                        "org.json.",
                        "com.fasterxml.jackson.",
                        "tools.jackson."
                ))
                .parentFirstClasses(List.of(
                        "cn.mythicland.mythiclogin.MythicLogin",
                        "cn.mythicland.mythiclogin.velocity.MythicLoginVelocity"
                ))
                .parentFirstPackages(List.of("cn.mythicland.mythiclogin.bootstrap."))
                .build();
    }
}
