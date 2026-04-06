package me.earth.earthhack.impl.core.mixins.block;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.core.ducks.block.IBlock;
import me.earth.earthhack.impl.event.events.misc.CollisionEvent;
import me.earth.earthhack.impl.modules.Caches;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.List;

@Mixin(Block.class)
public abstract class MixinBlock implements IBlock
{

    private static final Minecraft MC = Minecraft.getMinecraft();

    private final String[] harvestToolNonForge = new String[16];
    private final int[] harvestLevelNonForge   = new int[16];

    @Shadow
    @Final
    protected Material material;

    @Shadow
    protected static void addCollisionBoxToList(BlockPos pos,
                                                AxisAlignedBB entityBox,
                                                List<AxisAlignedBB> cBoxes,
                                                AxisAlignedBB blockBox)
    {
        throw new IllegalStateException(
                "MixinBlock.addCollisionBoxToList has not been shadowed");
    }

    @Shadow
    public abstract BlockStateContainer getBlockState();

    @Shadow
    public abstract int getMetaFromState(IBlockState state);

    @Unique // TODO: test and use everywhere
    @Override
    public void setHarvestLevelNonForge(String toolClass, int level)
    {
        for (IBlockState state : getBlockState().getValidStates())
        {
            int idx = this.getMetaFromState(state);
            this.harvestToolNonForge[idx]  = toolClass;
            this.harvestLevelNonForge[idx] = level;
        }
    }

    @Unique
    @Override
    public String getHarvestToolNonForge(IBlockState state)
    {
        return harvestToolNonForge[getMetaFromState(state)];
    }

    @Unique
    @Override
    public int getHarvestLevelNonForge(IBlockState state)
    {
        return harvestLevelNonForge[getMetaFromState(state)];
    }

    @Inject(
        method = "<init>(Lnet/minecraft/block/material/Material;Lnet/minecraft/block/material/MapColor;)V",
        at = @At("RETURN"))
    public void ctrHook(Material blockMaterialIn,
                         MapColor blockMapColorIn,
                         CallbackInfo ci)
    {
        Arrays.fill(harvestLevelNonForge, -1);
    }









}
