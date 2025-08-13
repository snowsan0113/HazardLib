package snowsan0113.hazardlib.api.tsunami;

import java.awt.*;
import java.util.Arrays;

public enum TsunamiType {

    MAJORWARNING("大津波警報", "MajorWarning", new Color(200, 0, 100)),
    WARNING("津波警報", "Warning", Color.RED),
    WATCH("津波注意報", "Watch", Color.YELLOW),
    UNKNOWN("不明", "Unknown", Color.LIGHT_GRAY);

    private final String jp_string;
    private final String api_name;
    private final Color color;

    TsunamiType(String jp_string, String api_name, Color color) {
        this.jp_string = jp_string;
        this.api_name = api_name;
        this.color = color;
    }

    public String getString() {
        return jp_string;
    }

    public String getAPIName() {
        return api_name;
    }

    public Color getColor() {
        return color;
    }

    public static TsunamiType getAPIConvertType(String api_name) {
        return Arrays.stream(TsunamiType.values())
                .filter(type -> type.getAPIName().equalsIgnoreCase(api_name))
                .findFirst().orElse(TsunamiType.UNKNOWN);
    }
}
