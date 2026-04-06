package me.earth.earthhack.impl.modules.client.clickgui;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.ColorSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.api.setting.settings.StringSetting;
import me.earth.earthhack.impl.gui.click.Click;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.client.SimpleData;
import net.minecraft.client.gui.GuiScreen;

import java.awt.*;

public class ClickGui extends Module
{
    public final Setting<Color> color =
            register(new ColorSetting("Color", new Color(0, 80, 255)));
    public final Setting<Color> backgroundColor =
            register(new ColorSetting("Background", new Color(20, 20, 20, 200)));
    public final Setting<Color> headerColor =
            register(new ColorSetting("Header", new Color(30, 30, 30, 240)));
    public final Setting<Color> outlineColor =
            register(new ColorSetting("Outline", new Color(10, 10, 10, 255)));
    public final Setting<Color> enabledTextColor =
            register(new ColorSetting("EnabledText", new Color(255, 255, 255, 255)));
    public final Setting<Color> disabledTextColor =
            register(new ColorSetting("DisabledText", new Color(150, 150, 150, 255)));
    public final Setting<Color> settingBackgroundColor =
            register(new ColorSetting("SettingBackground", new Color(25, 25, 25, 220)));
    public final Setting<Color> settingHeaderColor =
            register(new ColorSetting("SettingHeader", new Color(30, 30, 30, 240)));
    public final Setting<Color> hoverColor =
            register(new ColorSetting("Hover", new Color(50, 50, 50, 200)));
    public final Setting<Boolean> catEars =
            register(new BooleanSetting("CatEars", false));
    public final Setting<Boolean> blur =
            register(new BooleanSetting("Blur", false));
    public final Setting<Integer> blurAmount =
            register(new NumberSetting<>("Blur-Amount", 8, 1, 20));
    public final Setting<Integer> blurSize =
            register(new NumberSetting<>("Blur-Size", 3, 1, 20));
    public final Setting<String> open =
            register(new StringSetting("Open", "+"));
    public final Setting<String> close =
            register(new StringSetting("Close", "-"));
    public final Setting<Boolean> white =
            register(new BooleanSetting("White-Settings", true));
    public final Setting<Boolean> description =
        register(new BooleanSetting("Description", true));
    public final Setting<Boolean> showBind =
            register(new BooleanSetting("Show-Bind", true));
    public final Setting<Boolean> size =
            register(new BooleanSetting("Category-Size", true));
    public final Setting<Integer> descriptionWidth =
        register(new NumberSetting<>("Description-Width", 240, 100, 1000));
    public final Setting<Boolean> descNameValue =
        register(new BooleanSetting("Desc-NameValue", true));

    protected boolean fromEvent;
    protected GuiScreen screen;

    public ClickGui()
    {
        super("ClickGui", Category.Client);
        this.listeners.add(new ListenerScreen(this));
        this.setData(new SimpleData(this, "Beautiful ClickGui by oHare"));
    }

    public ClickGui(String name)
    {
        super(name, Category.Client);
        this.listeners.add(new ListenerScreen(this));
    }

    @Override
    protected void onEnable()
    {
        disableOtherGuis();
        Click.CLICK_GUI.set(this);
        screen = mc.currentScreen instanceof Click ? ((Click) mc.currentScreen).screen : mc.currentScreen;
        // dont save it since some modules add/del settings
        Click gui = newClick();
        gui.init();
        gui.onGuiOpened();
        mc.displayGuiScreen(gui);
    }

    protected void disableOtherGuis() {
        for (Module module : Managers.MODULES.getRegistered()) {
            if (module instanceof ClickGui && module != this) {
                module.disable();
            }
        }
    }

    protected Click newClick() {
        return new Click(screen);
    }

    @Override
    protected void onDisable()
    {
        if (!fromEvent)
        {
            mc.displayGuiScreen(screen);
        }

        fromEvent = false;
    }

}
