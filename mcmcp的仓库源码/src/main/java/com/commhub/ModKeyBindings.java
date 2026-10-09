package com.commhub;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ModKeyBindings {
    public static final String CATEGORY = "key.categories.commhub";
    public static final KeyMapping OPEN_HUB = new KeyMapping("key.commhub.open", GLFW.GLFW_KEY_K, CATEGORY);

    private ModKeyBindings() {
    }
}
