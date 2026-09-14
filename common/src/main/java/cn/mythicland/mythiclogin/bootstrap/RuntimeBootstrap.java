package cn.mythicland.mythiclogin.bootstrap;

import cn.mythicland.framework.runtime.RuntimeLibraryLoader;
import cn.mythicland.framework.runtime.SelectiveChildFirstClassLoader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class RuntimeBootstrap {
    private final Object plugin;
    private final String mainLoaderName;
    private final Class<?> pluginType;
    private final PathConfiguration pathConfiguration;
    private SelectiveChildFirstClassLoader classLoader;
    private Object mainLoader;
    private Method loadMethod;
    private Method enableMethod;
    private Method reloadMethod;
    private Method unloadMethod;

    public RuntimeBootstrap(
            Object plugin,
            String mainLoaderName,
            Class<?> pluginType,
            java.nio.file.Path dataDirectory,
            Set<String> activePlatforms,
            Consumer<String> infoLogger,
            Consumer<String> warningLogger
    ) {
        this.plugin = plugin;
        this.mainLoaderName = mainLoaderName;
        this.pluginType = pluginType;
        this.pathConfiguration = new PathConfiguration(
                dataDirectory,
                activePlatforms,
                infoLogger,
                warningLogger
        );
    }

    public void load() {
        try {
            List<URL> urls = new ArrayList<>();
            urls.add(this.pluginJarUrl());
            urls.addAll(new RuntimeLibraryLoader(
                    this.pathConfiguration.dataDirectory(),
                    this.pluginType.getClassLoader(),
                    "META-INF/mythiclogin-libraries.properties",
                    this.pathConfiguration.infoLogger(),
                    this.pathConfiguration.warningLogger()
            ).resolveLibraries(this.pathConfiguration.activePlatforms()));
            this.classLoader = new SelectiveChildFirstClassLoader(
                    urls.toArray(URL[]::new),
                    this.pluginType.getClassLoader(),
                    List.of(
                            "cn.mythicland.mythiclogin.",
                            "cn.mythicland.framework.",
                            "org.reflections.",
                            "javassist.",
                            "com.zaxxer.hikari.",
                            "org.flywaydb.",
                            "org.postgresql.",
                            "org.sqlite.",
                            "org.yaml.snakeyaml.",
                            "com.fasterxml.jackson.",
                            "tools.jackson."
                    ),
                    List.of(
                            "cn.mythicland.mythiclogin.MythicLogin",
                            "cn.mythicland.mythiclogin.velocity.MythicLoginVelocity"
                    ),
                    List.of("cn.mythicland.mythiclogin.bootstrap.")
            );
            Class<?> type = Class.forName(this.mainLoaderName, true, this.classLoader);
            this.mainLoader = type.getConstructor(this.pluginType).newInstance(this.plugin);
            this.loadMethod = type.getMethod("load");
            this.enableMethod = type.getMethod("enable");
            this.reloadMethod = type.getMethod("reload");
            this.unloadMethod = type.getMethod("unload");
            this.invoke(this.loadMethod);
        } catch (ReflectiveOperationException exception) {
            this.closeAfterFailure(exception);
            throw new IllegalStateException("Failed to load MythicLogin runtime", exception);
        }
    }

    public void enable() {
        this.invoke(this.enableMethod);
    }

    public void reload() {
        this.invoke(this.reloadMethod);
    }

    public void disable() {
        try {
            if (this.mainLoader != null) this.invoke(this.unloadMethod);
        } finally {
            this.mainLoader = null;
            this.loadMethod = null;
            this.enableMethod = null;
            this.reloadMethod = null;
            this.unloadMethod = null;
            if (this.classLoader == null) return;
            try {
                this.classLoader.close();
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to close MythicLogin runtime classloader", exception);
            } finally {
                this.classLoader = null;
            }
        }
    }

    private URL pluginJarUrl() {
        CodeSource codeSource = this.pluginType.getProtectionDomain().getCodeSource();
        if (codeSource == null || codeSource.getLocation() == null) {
            throw new IllegalStateException("Unable to resolve MythicLogin plugin JAR");
        }
        return codeSource.getLocation();
    }

    private void invoke(Method method) {
        if (this.mainLoader == null || method == null) {
            throw new IllegalStateException("MythicLogin runtime is not loaded");
        }
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.classLoader);
            method.invoke(this.mainLoader);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot access MythicLogin runtime lifecycle", exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) throw runtimeException;
            throw new IllegalStateException("MythicLogin runtime lifecycle failed", cause);
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private void closeAfterFailure(Exception failure) {
        if (this.classLoader == null) return;
        try {
            this.classLoader.close();
        } catch (IOException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
        this.classLoader = null;
    }

    private record PathConfiguration(
            java.nio.file.Path dataDirectory,
            Set<String> activePlatforms,
            Consumer<String> infoLogger,
            Consumer<String> warningLogger
    ) {
        private PathConfiguration {
            activePlatforms = Set.copyOf(activePlatforms);
        }
    }
}
