package cn.mythicland.mythiclogin.velocity.util;

import org.slf4j.Logger;

public final class PluginLogger {

    private final Logger delegate;
    private final String prefix;

    public PluginLogger(Logger delegate, String prefix) {
        this.delegate = delegate;
        this.prefix = "[" + prefix + "] ";
    }

    public Logger getDelegate() {
        return delegate;
    }

    public void info(String message) {
        delegate.info(prefix + message);
    }

    public void warning(String message) {
        delegate.warn(prefix + message);
    }

    public void severe(String message, Throwable throwable) {
        delegate.error(prefix + message, throwable);
    }
}
