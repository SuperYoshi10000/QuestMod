package local.ytk.questmod.client;

import local.ytk.questmod.client.gui.QuestScreen;
import local.ytk.questmod.quests.QuestPayload;
import local.ytk.questmod.quests.QuestStatus;
import local.ytk.questmod.quests.QuestSubmitPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class QuestModClient implements ClientModInitializer {
    private static List<QuestStatus> quests;
    KeyBinding openQuestScreenKeyBinding;
    
    @Override
    public void onInitializeClient() {
        openQuestScreenKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.questmod.open_quest_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Y,
                "category.questmod.keybindings"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openQuestScreenKeyBinding.wasPressed()) {
                if (!(client.currentScreen instanceof QuestScreen)) {
                    // Request the quest list. This will open the screen once the quest list is received.
                    requestQuests();
                } else {
                    client.currentScreen.close();
                }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(QuestPayload.PAYLOAD_ID, QuestModClient::receivePayload);
    }
    
    private static void receivePayload(QuestPayload payload, ClientPlayNetworking.Context context) {
        quests = payload.quests();
        // Quests will only be sent when opening the quest screen, so immediately open it
        context.client().setScreen(new QuestScreen(quests));
    }
    
    public static void requestQuests() {
        ClientPlayNetworking.send(QuestPayload.EMPTY);
    }
    public static void claimQuest(int id) {
        ClientPlayNetworking.send(new QuestSubmitPayload(id));
    }
}
