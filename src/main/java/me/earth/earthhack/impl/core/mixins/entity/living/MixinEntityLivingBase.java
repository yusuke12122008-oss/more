package me.earth.earthhack.impl.core.mixins.entity.living;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.api.event.bus.instance.Bus;
import me.earth.earthhack.impl.core.ducks.entity.IEntityLivingBase;
import me.earth.earthhack.impl.core.ducks.entity.IEntityNoInterp;
import me.earth.earthhack.impl.core.ducks.entity.IEntityRemoteAttack;
import me.earth.earthhack.impl.core.mixins.entity.MixinEntity;
import me.earth.earthhack.impl.event.events.misc.DeathEvent;
import me.earth.earthhack.impl.event.events.movement.LiquidJumpEvent;
import me.earth.earthhack.impl.modules.Caches;

import me.earth.earthhack.impl.util.minecraft.ICachedDamage;
import me.earth.earthhack.impl.util.minecraft.MotionTracker;
import me.earth.earthhack.impl.util.thread.EnchantmentUtil;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends MixinEntity
        implements IEntityLivingBase, IEntityNoInterp,
                        ICachedDamage, IEntityRemoteAttack
{


    @Shadow
    @Final
    private static DataParameter<Float> HEALTH;
    @Shadow
    public float moveStrafing;
    @Shadow
    public float moveForward;
    @Shadow
    protected int activeItemStackUseCount;
    @Shadow
    protected ItemStack activeItemStack;

    protected double noInterpX;
    protected double noInterpY;
    protected double noInterpZ;
    protected int noInterpPositionIncrements;
    protected float noInterpPrevSwing;
    protected float noInterpSwingAmount;
    protected float noInterpSwing;
    protected float lowestDura = Float.MAX_VALUE;
    protected boolean noInterping = true;

    protected int armorValue = Integer.MAX_VALUE;
    protected float armorToughness = Float.MAX_VALUE;
    protected int explosionModifier = Integer.MAX_VALUE;

    @Shadow
    public abstract IAttributeInstance getEntityAttribute(IAttribute attribute);

    @Shadow
    public abstract int getTotalArmorValue();

    @Shadow
    public abstract Iterable<ItemStack> getArmorInventoryList();

    @Shadow
    public abstract boolean isServerWorld();

    @Override
    @Invoker(value = "getArmSwingAnimationEnd")
    public abstract int armSwingAnimationEnd();

    @Override
    @Accessor(value = "ticksSinceLastSwing")
    public abstract int getTicksSinceLastSwing();

    @Override
    @Accessor(value = "activeItemStackUseCount")
    public abstract int getActiveItemStackUseCount();

    @Override
    @Accessor(value = "ticksSinceLastSwing")
    public abstract void setTicksSinceLastSwing(int ticks);

    @Override
    @Accessor(value = "activeItemStackUseCount")
    public abstract void setActiveItemStackUseCount(int count);

    @Override
    public boolean getElytraFlag()
    {
        return this.getFlag(7);
    }

    @Override
    public double getNoInterpX()
    {
        return isNoInterping() ? noInterpX : posX;
    }

    @Override
    public double getNoInterpY()
    {
        return isNoInterping() ? noInterpY: posY;
    }

    @Override
    public double getNoInterpZ()
    {
        return isNoInterping() ? noInterpZ : posZ;
    }

    @Override
    public void setNoInterpX(double x)
    {
        this.noInterpX = x;
    }

    @Override
    public void setNoInterpY(double y)
    {
        this.noInterpY = y;
    }

    @Override
    public void setNoInterpZ(double z)
    {
        this.noInterpZ = z;
    }

    @Override
    public int getPosIncrements()
    {
        return noInterpPositionIncrements;
    }

    @Override
    public void setPosIncrements(int posIncrements)
    {
        this.noInterpPositionIncrements = posIncrements;
    }

    @Override
    public float getNoInterpSwingAmount()
    {
        return noInterpSwingAmount;
    }

    @Override
    public float getNoInterpSwing()
    {
        return noInterpSwing;
    }

    @Override
    public float getNoInterpPrevSwing()
    {
        return noInterpPrevSwing;
    }

    @Override
    public void setNoInterpSwingAmount(float noInterpSwingAmount)
    {
        this.noInterpSwingAmount = noInterpSwingAmount;
    }

    @Override
    public void setNoInterpSwing(float noInterpSwing)
    {
        this.noInterpSwing = noInterpSwing;
    }

    @Override
    public void setNoInterpPrevSwing(float noInterpPrevSwing)
    {
        this.noInterpPrevSwing = noInterpPrevSwing;
    }

    @Override
    public boolean isNoInterping()
    {
        EntityPlayerSP player = mc.player;
        return !this.isRiding()
                && noInterping
                && (player == null || !player.isRiding());
    }

    @Override
    public void setNoInterping(boolean noInterping)
    {
        this.noInterping = noInterping;
    }

    @Override
    public void setLowestDura(float lowest)
    {
        this.lowestDura = lowest;
    }

    @Override
    public float getLowestDurability()
    {
        return lowestDura;
    }

    @Override
    public int getArmorValue()
    {
        return shouldCache()
                ? armorValue
                : this.getTotalArmorValue();
    }

    @Override
    public float getArmorToughness()
    {
        return shouldCache()
                ? armorToughness
                : (float) this
                    .getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS)
                    .getAttributeValue();
    }

    @Override
    public int getExplosionModifier(DamageSource source)
    {
        return shouldCache()
                ? explosionModifier
                : EnchantmentUtil.getEnchantmentModifierDamage(
                        this.getArmorInventoryList(), source);
    }

    @Redirect(
        method = "attackEntityFrom",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/World;isRemote:Z"))
    public boolean isRemoteHook(World world)
    {
        if (world.isRemote)
        {
            return !shouldRemoteAttack();
        }

        return false;
    }

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "knockBack", at = @At("HEAD"), cancellable = true)
    private void knockBackHook(Entity entityIn, float strength, double xRatio,
                               double zRatio, CallbackInfo ci)
    {
        if (EntityOtherPlayerMP.class.isInstance(this))
        {
            ci.cancel();
        }
    }



    @Redirect(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;isServerWorld()Z"
            )
    )
    public boolean travelHook(EntityLivingBase entityLivingBase)
    {
        return isServerWorld() || entityLivingBase instanceof MotionTracker;
    }

}
