package cn.mythicland.mythiclogin.bootstrap;

import cn.mythicland.framework.runtime.FrameworkRuntimeClassLoading;
import cn.mythicland.framework.runtime.RuntimeBootstrap;

import java.nio.file.Path;
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
            Consumer<String> warningLogger,
            FrameworkRuntimeClassLoading.ClassLoadingPreset classLoadingPreset
    ) {
        return RuntimeBootstrap.builder(plugin, mainLoaderName)
                .runtimeName("MythicLogin")
                .libraries(
                        dataDirectory,
                        "META-INF/mythiclogin-libraries.properties",
                        activePlatforms
                )
                .loggers(infoLogger, warningLogger)
                .classLoading(classLoadingPreset)
                .build();
    }
}
