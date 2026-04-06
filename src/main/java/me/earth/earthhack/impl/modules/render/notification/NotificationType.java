package me.earth.earthhack.impl.modules.render.notification;

/**
 * Notification categories, each with its own color scheme.
 * Colors inspired by the reference screenshot.
 */
public enum NotificationType {
    INFO    ("Info",    0xCC3DA8DB, 0xCC2E8BAE),  // blue/cyan
    SUCCESS ("Success", 0xCC4CAF50, 0xCC388E3C),  // green
    WARNING ("Warning", 0xCCFF9800, 0xCCE68900),  // orange
    ERROR   ("Error",   0xCCE85D6C, 0xCCCC4455);  // red/pink

    private final String title;
    private final int backgroundColor;
    private final int accentColor;

    NotificationType(String title, int backgroundColor, int accentColor) {
        this.title = title;
        this.backgroundColor = backgroundColor;
        this.accentColor = accentColor;
    }

    public String getTitle() {
        return title;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public int getAccentColor() {
        return accentColor;
    }
}
