package local.ytk.questmod.client;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class QuestModDataGenerator implements DataGeneratorEntrypoint {
    
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        
    }
    
    public static class QuestDataProvider implements DataProvider {
        @Override
        public CompletableFuture<?> run(DataWriter writer) {
            List<CompletableFuture<?>> futures = new ArrayList<>();
            
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        }
        
        @Override
        public String getName() {
            return "Quest Mod Data";
        }
    }
}
