package me.earth.earthhack.impl.modules.render.notification;

import me.earth.earthhack.api.event.bus.EventListener;
import me.earth.earthhack.api.event.bus.instance.Bus;
import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.event.events.client.PostInitEvent;
import me.earth.earthhack.impl.managers.Managers;

/**
 * Visual popup notification module (Candy Plus style).
 * Supports colored backgrounds per notification type:
 *   ERROR (red), WARNING (orange), SUCCESS (green), INFO (blue).
 */
public class Notification extends Module {

    protected final Setting<Integer> time =
            register(new NumberSetting<>("Time", 3, 1, 10));
    protected final Setting<Boolean> toggle =
            register(new BooleanSetting("Toggle", true));
    protected final Setting<Boolean> totemPop =
            register(new BooleanSetting("TotemPop", true));
    protected final Setting<Boolean> death =
            register(new BooleanSetting("Death", true));

    private final NotificationRenderer renderer;
    private static Notification INSTANCE;

    public Notification() {
        super("Notification", Category.Render);
        INSTANCE = this;
        this.renderer = new NotificationRenderer(this);
        this.listeners.add(new ListenerRender2D(this));
        this.listeners.add(new ListenerPacketReceive(this));
        this.setData(new NotificationData(this));

        Bus.EVENT_BUS.register(
            new EventListener<PostInitEvent>(PostInitEvent.class) {
                @Override
                public void invoke(PostInitEvent event) {
                    registerToggleListeners();
                }
            }
        );
    }

    private void registerToggleListeners() {
        for (Module module : Managers.MODULES.getRegistered()) {
            if (module == this) continue;
            Setting<Boolean> enabled =
                    module.getSetting("Enabled", BooleanSetting.class);
            if (enabled == null) continue;

            enabled.addObserver(e -> {
                if (!e.isCancelled()
                        && isEnabled()
                        && toggle.getValue()) {
                    String name = module.getDisplayName();
                    if (e.getValue()) {
                        renderer.show(NotificationType.SUCCESS,
                                name + " has been enabled");
                    } else {
                        renderer.show(NotificationType.INFO,
                                name + " has been disabled");
                    }
                }
            });
        }
    }

    public NotificationRenderer getRenderer() {
        return renderer;
    }

    public static Notification getInstance() {
        return INSTANCE;
    }

    /* --- Static convenience methods for other modules --- */

    /** Show an INFO notification from anywhere. */
    public static void info(String message) {
        post(NotificationType.INFO, message);
    }

    /** Show a SUCCESS notification from anywhere. */
    public static void success(String message) {
        post(NotificationType.SUCCESS, message);
    }

    /** Show a WARNING notification from anywhere. */
    public static void warn(String message) {
        post(NotificationType.WARNING, message);
    }

    /** Show an ERROR notification from anywhere. */
    public static void error(String message) {
        post(NotificationType.ERROR, message);
    }

    /** Generic post. */
    public static void post(NotificationType type, String message) {
        if (INSTANCE != null && INSTANCE.isEnabled()) {
            INSTANCE.renderer.show(type, message);
        }
    }

    @Override
    protected void onDisable() {
        renderer.clear();
    }
}
