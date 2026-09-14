package cn.mythicland.mythiclogin.bootstrap;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

final class RuntimeClassLoader extends URLClassLoader {
    private static final List<String> CHILD_FIRST_PACKAGES = List.of(
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
    );

    static {
        registerAsParallelCapable();
    }

    RuntimeClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null && shouldUseChildFirst(name)) {
                try {
                    loaded = findClass(name);
                } catch (ClassNotFoundException ignored) {
                }
            }
            if (loaded == null) loaded = super.loadClass(name, false);
            if (resolve) resolveClass(loaded);
            return loaded;
        }
    }

    private static boolean shouldUseChildFirst(String className) {
        if (className.equals("cn.mythicland.mythiclogin.MythicLogin")) return false;
        if (className.equals("cn.mythicland.mythiclogin.velocity.MythicLoginVelocity")) return false;
        if (className.startsWith("cn.mythicland.mythiclogin.bootstrap.")) return false;
        for (String packageName : CHILD_FIRST_PACKAGES) {
            if (className.startsWith(packageName)) return true;
        }
        return false;
    }
}
