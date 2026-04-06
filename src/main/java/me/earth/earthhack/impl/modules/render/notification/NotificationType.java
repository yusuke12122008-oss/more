package me.earth.earthhack.impl.modules.render.notification;

/**
 * Notification categories with CandyPlus-inspired color scheme.
 *
 * CandyPlus uses:
 *   - Dark (near-black) translucent background
 *   - Vivid type-specific accent color for bar, icon, progress
 *   - Icon character rendered before the title
 */
public enum NotificationType {

    //  title      icon  bg (ARGB)      accent (ARGB)    progress (ARGB)
    INFO   ("Info",    "\u24d8",  0xE0101318,  0xFF3AB4F5,  0xFF3AB4F5),  // sky blue
    SUCCESS("Success", "\u2714",  0xE0101318,  0xFF3ECF72,  0xFF3ECF72),  // green
    WARNING("Warning", "\u26a0",  0xE0101318,  0xFFFFA726,  0xFFFFA726),  // amber
    ERROR  ("Error",   "\u2716",  0xE0101318,  0xFFEF5350,  0xFFEF5350); // red

    private final String title;
    private final String icon;
    private final int    backgroundColor;
    private final int    accentColor;
    private final int    progressColor;

    NotificationType(String title,
                     String icon,
                     int    backgroundColor,
                     int    accentColor,
                     int    progressColor)
    {
        this.title           = title;
        this.icon            = icon;
        this.backgroundColor = backgroundColor;
        this.accentColor     = accentColor;
        this.progressColor   = progressColor;
    }

    public String getTitle()           { return title;           }
    public String getIcon()            { return icon;            }
    public int    getBackgroundColor() { return backgroundColor; }
    public int    getAccentColor()     { return accentColor;     }
    public int    getProgressColor()   { return progressColor;   }
}
