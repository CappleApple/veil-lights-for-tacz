package com.cappleapple.veiltaczlights.content;

import com.cappleapple.veiltaczlights.VeilTaczLights;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModContent {
    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(VeilTaczLights.MOD_ID);
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(VeilTaczLights.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, VeilTaczLights.MOD_ID);

    public static final DeferredBlock<ArtificialBeamSourceBlock> ARTIFICIAL_BEAM_SOURCE =
            BLOCKS.registerBlock(
                    "artificial_beam_source",
                    ArtificialBeamSourceBlock::new,
                    BlockBehaviour.Properties.of()
                            .strength(3.0F)
                            .sound(SoundType.METAL)
            );
    public static final DeferredItem<BlockItem> ARTIFICIAL_BEAM_SOURCE_ITEM =
            ITEMS.registerSimpleBlockItem(ARTIFICIAL_BEAM_SOURCE);
    public static final Supplier<BlockEntityType<ArtificialBeamSourceBlockEntity>>
            ARTIFICIAL_BEAM_SOURCE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
                    "artificial_beam_source",
                    () -> BlockEntityType.Builder.of(
                            ArtificialBeamSourceBlockEntity::new,
                            ARTIFICIAL_BEAM_SOURCE.get()
                    ).build(null)
            );

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        modBus.addListener(ModContent::addCreativeTabContents);
    }

    private static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ARTIFICIAL_BEAM_SOURCE_ITEM.get());
        }
    }

    private ModContent() {
    }
}
