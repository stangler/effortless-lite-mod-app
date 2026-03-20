package com.example.effortlesslite.client;

import com.example.effortlesslite.EffortlessLite;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * キーバインドの定義と登録
 *
 * デフォルトキー:
 *  [G]      : ビルドモード切り替え
 *  [H]      : ミラー軸切り替え
 *  [Z]      : Undo
 *  [Y]      : Redo
 *  [Escape] : ビルドキャンセル
 */
@EventBusSubscriber(modid = EffortlessLite.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public class ModKeyBindings {

    public static final String CATEGORY = "key.categories.effortlesslite";

    /** ビルドモードの循環切り替え */
    public static final KeyMapping MODE_TOGGLE = new KeyMapping(
            "key.effortlesslite.mode_toggle",
            GLFW.GLFW_KEY_G,
            CATEGORY
    );

    /** ミラー軸の循環切り替え */
    public static final KeyMapping MIRROR_TOGGLE = new KeyMapping(
            "key.effortlesslite.mirror_toggle",
            GLFW.GLFW_KEY_H,
            CATEGORY
    );

    /** Undo (直前の配置を取り消す) */
    public static final KeyMapping UNDO = new KeyMapping(
            "key.effortlesslite.undo",
            GLFW.GLFW_KEY_Z,
            CATEGORY
    );

    /** Redo (取り消した配置をやり直す) */
    public static final KeyMapping REDO = new KeyMapping(
            "key.effortlesslite.redo",
            GLFW.GLFW_KEY_Y,
            CATEGORY
    );

    /** 始点をリセットしてビルドをキャンセルする */
    public static final KeyMapping CANCEL = new KeyMapping(
            "key.effortlesslite.cancel",
            GLFW.GLFW_KEY_ESCAPE,
            CATEGORY
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(MODE_TOGGLE);
        event.register(MIRROR_TOGGLE);
        event.register(UNDO);
        event.register(REDO);
        event.register(CANCEL);
    }
}
