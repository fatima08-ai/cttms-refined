import java.awt.*;
import javax.swing.*;

public class AppCoordinator {

    // ─────────────────────────────────────────────────────────────────────────
    //  Theme Constants  (Dark + Light palettes)
    // ─────────────────────────────────────────────────────────────────────────
    public static final class ThemeConstants {

        /* ── DARK MODE ── */
        public static final Color DARK_BG_DEEP     = new Color(8,  10, 16);
        public static final Color DARK_BG_PANEL    = new Color(14, 18, 28);
        public static final Color DARK_BG_CARD     = new Color(20, 26, 40);
        public static final Color DARK_BG_HEADER   = new Color(12, 16, 26);
        public static final Color DARK_BG_INPUT    = new Color(10, 14, 22);
        public static final Color DARK_TEXT_PRI    = new Color(210,225,245);
        public static final Color DARK_TEXT_DIM    = new Color(110,130,158);
        public static final Color DARK_TEXT_BRT    = new Color(240,248,255);

        /* ── LIGHT MODE ── */
        public static final Color LIGHT_BG_DEEP    = new Color(235,240,250);
        public static final Color LIGHT_BG_PANEL   = new Color(220,228,242);
        public static final Color LIGHT_BG_CARD    = new Color(245,248,255);
        public static final Color LIGHT_BG_HEADER  = new Color(200,210,230);
        public static final Color LIGHT_BG_INPUT   = new Color(255,255,255);
        public static final Color LIGHT_TEXT_PRI   = new Color(18, 24, 40);
        public static final Color LIGHT_TEXT_DIM   = new Color(80,  95,120);
        public static final Color LIGHT_TEXT_BRT   = new Color(10,  18, 35);

        /* ── SHARED ACCENTS ── */
        public static final Color NEON_GREEN   = new Color(0,  200, 100);
        public static final Color NEON_BLUE    = new Color(0,  160, 240);
        public static final Color NEON_CYAN    = new Color(0,  210, 190);
        public static final Color NEON_PURPLE  = new Color(160, 80, 240);
        public static final Color ALERT_RED    = new Color(235, 45, 70);
        public static final Color ALERT_ORANGE = new Color(240,130,  0);
        public static final Color WARN_YELLOW  = new Color(230,195,  0);
        public static final Color MUTED_GRAY   = new Color( 90,110,140);

        // Currently active – defaults to dark
        private static boolean dark = true;

        public static boolean isDark()                { return dark; }
        public static void setDark(boolean d)         { dark = d; }

        public static Color bgDeep()    { return dark ? DARK_BG_DEEP   : LIGHT_BG_DEEP;   }
        public static Color bgPanel()   { return dark ? DARK_BG_PANEL  : LIGHT_BG_PANEL;  }
        public static Color bgCard()    { return dark ? DARK_BG_CARD   : LIGHT_BG_CARD;   }
        public static Color bgHeader()  { return dark ? DARK_BG_HEADER : LIGHT_BG_HEADER; }
        public static Color bgInput()   { return dark ? DARK_BG_INPUT  : LIGHT_BG_INPUT;  }
        public static Color textPri()   { return dark ? DARK_TEXT_PRI  : LIGHT_TEXT_PRI;  }
        public static Color textDim()   { return dark ? DARK_TEXT_DIM  : LIGHT_TEXT_DIM;  }
        public static Color textBrt()   { return dark ? DARK_TEXT_BRT  : LIGHT_TEXT_BRT;  }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Wiring
    // ─────────────────────────────────────────────────────────────────────────
    private static BackendEngine.SystemBootstrap backend;
    private static CyberDashboardViewer          viewer;

        public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                applyUIDefaults(ThemeConstants.isDark());
                backend = new BackendEngine.SystemBootstrap();
                wireDetectorCallback();
                
                viewer  = new CyberDashboardViewer(backend);
                viewer.launch();
                
                backend.getSecurityLog().writeLog("INFO",
                    "AppCoordinator launched CTTMS desktop UI — theme: "
                        + (ThemeConstants.isDark() ? "DARK" : "LIGHT"));
            } catch (Exception e) {
                System.err.println("CRITICAL FAULT LAUNCHING APPLICATION DASHBOARD:");
                e.printStackTrace(); // This prints hidden errors to your terminal!
            }
        });
    }


    /** Apply Swing UIManager defaults for current theme. */
    public static void applyUIDefaults(boolean dark) {
        ThemeConstants.setDark(dark);
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("sun.java2d.opengl", "false");
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}

        Color bg    = ThemeConstants.bgDeep();
        Color card  = ThemeConstants.bgCard();
        Color input = ThemeConstants.bgInput();
        Color txt   = ThemeConstants.textPri();
        Color green = ThemeConstants.NEON_GREEN;
        Color blue  = ThemeConstants.NEON_BLUE;
        Color grid  = dark ? new Color(30,40,60) : new Color(180,195,220);
        Color scroll= dark ? new Color(22,28,42) : new Color(200,210,230);
        Color tip   = dark ? new Color(22,28,42) : new Color(245,248,255);

        UIManager.put("OptionPane.background",        bg);
        UIManager.put("Panel.background",             bg);
        UIManager.put("OptionPane.messageForeground", txt);
        UIManager.put("Button.background",            card);
        UIManager.put("Button.foreground",            green);
        UIManager.put("TextField.background",         input);
        UIManager.put("TextField.foreground",         green);
        UIManager.put("TextField.caretForeground",    green);
        UIManager.put("ComboBox.background",          card);
        UIManager.put("ComboBox.foreground",          blue);
        UIManager.put("ScrollBar.thumb",              blue);
        UIManager.put("ScrollBar.track",              scroll);
        UIManager.put("Table.background",             bg);
        UIManager.put("Table.foreground",             txt);
        UIManager.put("Table.gridColor",              grid);
        UIManager.put("TableHeader.background",       card);
        UIManager.put("TableHeader.foreground",       blue);
        UIManager.put("Label.foreground",             txt);
        UIManager.put("ToolTip.background",           tip);
        UIManager.put("ToolTip.foreground",           green);
        UIManager.put("ToolTip.border",
            BorderFactory.createLineBorder(blue, 1));
    }

    private static void wireDetectorCallback() {
        backend.getSimulationController().getDetector().setAlertCallback(msg ->
            SwingUtilities.invokeLater(() ->
                backend.getSecurityLog().writeLog("CRITICAL", msg)));
    }
}
