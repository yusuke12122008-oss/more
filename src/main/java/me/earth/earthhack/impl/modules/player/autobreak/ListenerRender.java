package me.earth.earthhack.impl.modules.player.autobreak;

import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.render.Interpolation;
import me.earth.earthhack.impl.util.render.RenderUtil;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;

public class ListenerRender extends ModuleListener<AutoBreak, Render3DEvent> {
    public ListenerRender(AutoBreak module) {
        super(module, Render3DEvent.class);
    }

    @Override
    public void invoke(Render3DEvent event) {
        BlockPos renderPos = module.mining != null ? module.mining : module.stopPos;
        if (renderPos == null || mc.world.getBlockState(renderPos).getBlock() == Blocks.AIR) {
            return;
        }

        AxisAlignedBB renderBox = mc.world.getBlockState(renderPos).getSelectedBoundingBox(mc.world, renderPos);
        double maxDmg = Math.max(0.0, Math.min(module.maxDamage, 1.0));
        double shrinkFactor = MathHelper.clamp(maxDmg / 2.0, 0.0, 0.5);
        renderBox = renderBox.shrink(shrinkFactor);

        AxisAlignedBB interpolatedBox = Interpolation.interpolateAxis(renderBox);

        Color fillColorInterp = module.fillColor.getValue();
        Color lineColorInterp = module.lineColor.getValue();
        if (module.maxDamage > 0.7f) {
            float progress = MathHelper.clamp((module.maxDamage - 0.7f) / 0.3f, 0.0f, 1.0f);
            fillColorInterp = new Color(
                (int) (module.fillColor2.getValue().getRed() * progress + fillColorInterp.getRed() * (1 - progress)),
                (int) (module.fillColor2.getValue().getGreen() * progress + fillColorInterp.getGreen() * (1 - progress)),
                (int) (module.fillColor2.getValue().getBlue() * progress + fillColorInterp.getBlue() * (1 - progress)),
                (int) (module.fillColor2.getValue().getAlpha() * progress + fillColorInterp.getAlpha() * (1 - progress))
            );
            lineColorInterp = new Color(
                (int) (module.lineColor2.getValue().getRed() * progress + lineColorInterp.getRed() * (1 - progress)),
                (int) (module.lineColor2.getValue().getGreen() * progress + lineColorInterp.getGreen() * (1 - progress)),
                (int) (module.lineColor2.getValue().getBlue() * progress + lineColorInterp.getBlue() * (1 - progress)),
                (int) (module.lineColor2.getValue().getAlpha() * progress + lineColorInterp.getAlpha() * (1 - progress))
            );
        }

        RenderUtil.startRender();
        RenderUtil.drawBox(interpolatedBox, fillColorInterp);
        RenderUtil.drawOutline(interpolatedBox, 1.5f, lineColorInterp);
        RenderUtil.endRender();
    }
}
