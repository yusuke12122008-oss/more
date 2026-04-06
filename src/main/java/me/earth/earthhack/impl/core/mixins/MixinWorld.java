package me.earth.earthhack.impl.core.mixins;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.api.event.bus.instance.Bus;
import me.earth.earthhack.impl.core.ducks.IWorld;
import me.earth.earthhack.impl.event.events.misc.UpdateEntitiesEvent;
import me.earth.earthhack.impl.event.events.movement.WaterPushEvent;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.Caches;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.IntHashMap;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class MixinWorld implements IWorld
{


    @Shadow
    @Final
    public boolean isRemote;

    @Override
    @Invoker(value = "isChunkLoaded")
    public abstract boolean isChunkLoaded(int x, int z, boolean allowEmpty);

    @Redirect(
            method = "handleMaterialAcceleration",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;isPushedByWater()Z"))
    public boolean isPushedByWaterHook(Entity entity)
    {
        WaterPushEvent event = new WaterPushEvent(entity);
        Bus.EVENT_BUS.post(event);

        return entity.isPushedByWater() && !event.isCancelled();
    }



    @Redirect(
            method = "getEntityByID",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/IntHashMap;lookup(I)Ljava/lang/Object;"))
    private Object getEntityByIDHook(IntHashMap<Entity> intHashMap, int id)
    {
        Entity entity = intHashMap.lookup(id);
        if (entity == null)
        {
            entity = Managers.SET_DEAD.getEntity(id);
            if (entity != null)
            {
                entity.lastTickPosX = entity.posX;
                entity.lastTickPosY = entity.posY;
                entity.lastTickPosZ = entity.posZ;

                entity.prevPosX = entity.posX;
                entity.prevPosY = entity.posY;
                entity.prevPosZ = entity.posZ;
            }
        }

        return entity;
    }

    @Inject(
            method = "updateEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V",
                    ordinal = 2
            )
    )
    public void updateEntitiesHook(CallbackInfo ci)
    {
        if (isRemote)
        {
            UpdateEntitiesEvent event = new UpdateEntitiesEvent();
            Bus.EVENT_BUS.post(event);
        }
    }

    // We could inject this in the chunks as well, but that would affect render
    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    public void getBlockStateHook(BlockPos pos,
                                   CallbackInfoReturnable<IBlockState> cir)
    {
    }

    @Redirect(
            method = "mayPlace",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;checkNoEntityCollision(Lnet/minecraft/util/math/AxisAlignedBB;Lnet/minecraft/entity/Entity;)Z"))
    public boolean checkNoEntityCollisionHook(World world,
                                               AxisAlignedBB bb,
                                               Entity entityIn)
    {
        return world.checkNoEntityCollision(bb, entityIn);
    }

}
