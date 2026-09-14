package cn.mythicland.mythiclogin.velocity.config;

import java.nio.file.Path;

public final class MessageConfig extends AbstractYamlConfiguration {

    public MessageConfig(Path dataDirectory, ClassLoader classLoader) {
        super(dataDirectory, classLoader, "messages.yml", "message.yml");
    }

    public void reload() {
        reloadYamlConfiguration();
    }
}
