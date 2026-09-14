package cn.mythicland.mythiclogin.util;

import java.util.logging.Level;
import java.util.logging.Logger;

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
        delegate.warning(prefix + message);
    }

    public void severe(String message) {
        delegate.severe(prefix + message);
    }

    public void severe(String message, Throwable throwable) {
        delegate.log(Level.SEVERE, prefix + message, throwable);
    }
}
