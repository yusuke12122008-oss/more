package me.earth.earthhack.impl.core.mixins.entity;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.api.cache.SettingCache;
import me.earth.earthhack.api.event.bus.instance.Bus;
import me.earth.earthhack.api.event.events.Stage;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.api.util.interfaces.Globals;
import me.earth.earthhack.impl.core.ducks.entity.IEntity;
import me.earth.earthhack.impl.core.ducks.entity.IEntityNoInterp;
import me.earth.earthhack.impl.event.events.misc.ReachEvent;
import me.earth.earthhack.impl.event.events.movement.MoveEvent;
import me.earth.earthhack.impl.event.events.movement.OnGroundEvent;
import me.earth.earthhack.impl.event.events.movement.StepEvent;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.client.management.Management;

import me.earth.earthhack.impl.util.math.StopWatch;
import me.earth.earthhack.impl.util.minecraft.entity.EntityType;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(Entity.class)
public abstract class MixinEntity implements IEntity, Globals
{


    private static final SettingCache
        <Integer, NumberSetting<Integer>, Management> DEATH_TIME =
        Caches.getSetting(Management.class, Setting.class, "DeathTime", 500);

    @Shadow
    public double posX;
    @Shadow
    public double posY;
    @Shadow
    public double posZ;
    @Shadow
    public double motionX;
    @Shadow
    public double motionY;
    @Shadow
    public double motionZ;
    @Shadow
    public float rotationYaw;
    @Shadow
    public float rotationPitch;
    @Shadow
    public boolean onGround;
    @Shadow
    public World world;
    @Shadow
    public double prevPosX;
    @Shadow
    public double prevPosY;
    @Shadow
    public double prevPosZ;
    @Shadow
    public double lastTickPosX;
    @Shadow
    public double lastTickPosY;
    @Shadow
    public double lastTickPosZ;
    @Shadow
    protected EntityDataManager dataManager;
    @Shadow
    public float stepHeight;
    @Shadow
    public boolean isDead;
    @Shadow
    public float width;
    @Shadow
    public float prevRotationYaw;
    @Shadow
    public float prevRotationPitch;
    @Shadow
    public float height;

    @Unique
    private long oldServerX;
    @Unique
    private long oldServerY;
    @Unique
    private long oldServerZ;

    private final StopWatch pseudoWatch = new StopWatch();
    private MoveEvent moveEvent;

    private Supplier<EntityType> type;
    private boolean pseudoDead;
    private long stamp;
    private boolean dummy;

    @Shadow
    public abstract AxisAlignedBB getEntityBoundingBox();
    @Shadow
    public abstract boolean isSneaking();
    @Shadow
    protected abstract boolean getFlag(int flag);
    @Shadow
    public abstract boolean equals(Object p_equals_1_);
    @Shadow
    protected abstract void setRotation(float yaw, float pitch);
    @Shadow
    public abstract boolean isRiding();

    @Shadow
    public boolean noClip;

    @Shadow
    public abstract void move(MoverType type, double x, double y,
                                      double z);
    @Shadow
    public abstract String getName();

    @Override
    @Accessor(value = "isInWeb")
    public abstract boolean inWeb();

    @Override
    public EntityType getType()
    {
        return type.get();
    }

    @Override
    public long getDeathTime()
    {
        // TODO!!!
        return 0;
    }

    @Override
    public void setOldServerPos(long x, long y, long z)
    {
        this.oldServerX = x;
        this.oldServerY = y;
        this.oldServerZ = z;
    }

    @Override
    public long getOldServerPosX()
    {
        return oldServerX;
    }

    @Override
    public long getOldServerPosY()
    {
        return oldServerY;
    }

    @Override
    public long getOldServerPosZ()
    {
        return oldServerZ;
    }

    @Override
    public boolean isPseudoDead()
    {
        if (pseudoDead
                && !isDead
                && pseudoWatch.passed(DEATH_TIME.getValue()))
        {
            pseudoDead = false;
        }

        return pseudoDead;
    }

    @Override
    public void setPseudoDead(boolean pseudoDead)
    {
        this.pseudoDead = pseudoDead;
        if (pseudoDead)
        {
            pseudoWatch.reset();
        }
    }

    @Override
    public StopWatch getPseudoTime()
    {
        return pseudoWatch;
    }

    @Override
    public long getTimeStamp()
    {
        return stamp;
    }

    @Override
    public boolean isDummy()
    {
        return dummy;
    }

    @Override
    public void setDummy(boolean dummy)
    {
        this.dummy = dummy;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void ctrHook(CallbackInfo info)
    {
        this.type = EntityType.getEntityType(Entity.class.cast(this));
        this.stamp = System.currentTimeMillis();
    }



    @Inject(
        method = "move",
        at = @At("HEAD"),
        cancellable = true)
    public void moveEntityHook_Head(MoverType type,
                                    double x,
                                    double y,
                                    double z,
                                    CallbackInfo ci)
    {
        //noinspection ConstantConditions
        if (EntityPlayerSP.class.isInstance(this))
        {
            this.moveEvent = new MoveEvent(type, x, y, z, this.isSneaking());
            Bus.EVENT_BUS.post(this.moveEvent);
            if (moveEvent.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @ModifyVariable(
        method = "move",
        at = @At(
            value = "HEAD"),
        ordinal = 0)
    private double setX(double x)
    {
        return this.moveEvent != null ? this.moveEvent.getX() : x;
    }

    @ModifyVariable(
        method = "move",
        at = @At("HEAD"),
        ordinal = 1)
    private double setY(double y)
    {
        return this.moveEvent != null ? this.moveEvent.getY() : y;
    }

    @ModifyVariable(
        method = "move",
        at = @At("HEAD"),
        ordinal = 2)
    private double setZ(double z)
    {
        return this.moveEvent != null ? this.moveEvent.getZ() : z;
    }

    @Redirect(
        method = "move",
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/entity/Entity.isSneaking()Z",
            ordinal = 0))
    public boolean isSneakingHook(Entity entity)
    {
        return this.moveEvent != null
                    ? this.moveEvent.isSneaking()
                    : entity.isSneaking();
    }



    @Inject(method = "setPositionAndRotation", at = @At("RETURN"))
    public void setPositionAndRotationHook(double x,
                                            double y,
                                            double z,
                                            float yaw,
                                            float pitch,
                                            CallbackInfo ci)
    {
        if (this instanceof IEntityNoInterp)
        {
            ((IEntityNoInterp) this).setNoInterpX(x);
            ((IEntityNoInterp) this).setNoInterpY(y);
            ((IEntityNoInterp) this).setNoInterpZ(z);
        }
    }



    @Inject(
        method = "move",
        at = @At("RETURN"))
    public void moveEntityHook_Return(MoverType type,
                                      double x,
                                      double y,
                                      double z,
                                      CallbackInfo info)
    {
        this.moveEvent = null;
    }

    @Inject(
        method = "getCollisionBorderSize",
        at = @At("RETURN"),
        cancellable = true)
    public void getCollisionBorderSizeHook(CallbackInfoReturnable<Float> info)
    {
        ReachEvent event = new ReachEvent(0.0f, info.getReturnValue());
        Bus.EVENT_BUS.post(event);

        if (event.isCancelled())
        {
            info.setReturnValue(event.getHitBox());
        }
    }

    @Redirect(
        method = "applyEntityCollision",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;addVelocity(DDD)V"))
    public void addVelocityHook(Entity entity, double x, double y, double z)
    {
        if (entity != null && !entity.equals(mc.player))
        {
            entity.addVelocity(x, y, z);
        }
    }



}
