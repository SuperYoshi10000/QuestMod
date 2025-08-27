package local.ytk.questmod;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class QuestMod implements ModInitializer {
    public static final String MOD_ID = "questmod";
    
    
    
    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
    
    @Override
    public void onInitialize() {
    }
}
