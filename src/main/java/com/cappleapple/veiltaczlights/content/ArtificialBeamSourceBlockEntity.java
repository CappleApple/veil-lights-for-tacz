package com.cappleapple.veiltaczlights.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ArtificialBeamSourceBlockEntity extends BlockEntity {
    public ArtificialBeamSourceBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.ARTIFICIAL_BEAM_SOURCE_BLOCK_ENTITY.get(), pos, state);
    }
}
