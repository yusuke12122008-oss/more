package me.earth.earthhack.impl.commands;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.api.cache.SettingCache;
import me.earth.earthhack.api.command.Command;
import me.earth.earthhack.api.command.PossibleInputs;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.util.TextUtil;
import me.earth.earthhack.api.util.interfaces.Globals;
import me.earth.earthhack.impl.commands.util.CommandDescriptions;
import me.earth.earthhack.impl.managers.thread.scheduler.Scheduler;
import me.earth.earthhack.impl.modules.Caches;

import me.earth.earthhack.impl.util.text.ChatUtil;
import me.earth.earthhack.impl.util.text.TextColor;
import net.minecraft.item.ItemShulkerBox;
import net.minecraft.item.ItemStack;

public class PeekCommand extends Command implements Globals
{


    public PeekCommand()
    {
        super(new String[][]{{"peek"}, {"player"}});
        CommandDescriptions.register(this, "Type peek to view the shulker" +
                " you are currently holding. Specify a player name to view" +
                " that players last held shulker" +
                " (The Tooltips module needs to be enabled for this).");
    }

    @Override
    public void execute(String[] args)
    {
        ChatUtil.sendMessage(TextColor.RED + "ToolTips module is missing!");
    }

    @Override
    public PossibleInputs getPossibleInputs(String[] args)
    {
        return PossibleInputs.empty();
    }



}
