package cn.mythicland.mythiclogin.common.premium;

import cn.mythicland.framework.security.SignedPayload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public final class PremiumProtocol {
    public static final String CHANNEL = "mythiclogin:auth";

    private PremiumProtocol() {
    }

    public static byte[] encode(PremiumMessage message, PremiumSettings settings) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeUTF(message.type().name());
            output.writeUTF(message.nonce().toString());
            output.writeUTF(message.playerName());
            output.writeUTF(message.gameUuid().toString());
            output.writeUTF(message.premiumUuid() == null ? "" : message.premiumUuid().toString());
            output.writeBoolean(message.showTitle());
            output.writeLong(message.registerDate());
            output.writeUTF(message.detail());
            return SignedPayload.encode(bytes.toByteArray(), settings.sharedSecret());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode premium message", exception);
        }
    }

    public static PremiumMessage decode(byte[] bytes, PremiumSettings settings) {
        byte[] payload = SignedPayload.decode(bytes, settings.sharedSecret(), 4096);
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
            PremiumMessage.Type type = PremiumMessage.Type.valueOf(input.readUTF());
            UUID nonce = UUID.fromString(input.readUTF());
            String name = input.readUTF();
            UUID gameUuid = UUID.fromString(input.readUTF());
            String premiumUuid = input.readUTF();
            PremiumMessage message = new PremiumMessage(
                    type, nonce, name, gameUuid,
                    premiumUuid.isEmpty() ? null : UUID.fromString(premiumUuid),
                    input.readBoolean(), input.readLong(), input.readUTF()
            );
            if (input.available() != 0) throw new IllegalArgumentException("Trailing premium message data");
            return message;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid premium message", exception);
        }
    }
}
