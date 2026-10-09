package com.commhub;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record ChatEntry(UUID sender, String senderName, UUID recipient, String message, long timestamp, boolean isPublic) {

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("sender", sender);
        tag.putString("senderName", senderName);
        if (recipient != null) tag.putUUID("recipient", recipient);
        tag.putString("message", message);
        tag.putLong("timestamp", timestamp);
        tag.putBoolean("isPublic", isPublic);
        return tag;
    }

    /** 从 NBT 还原；数据损坏时返回 null（调用方跳过） */
    public static ChatEntry fromNbt(CompoundTag tag) {
        try {
            if (!tag.hasUUID("sender")) return null;
            return new ChatEntry(
                    tag.getUUID("sender"),
                    tag.getString("senderName"),
                    tag.contains("recipient") ? tag.getUUID("recipient") : null,
                    tag.getString("message"),
                    tag.getLong("timestamp"),
                    tag.getBoolean("isPublic")
            );
        } catch (Exception e) {
            return null;
        }
    }
}
