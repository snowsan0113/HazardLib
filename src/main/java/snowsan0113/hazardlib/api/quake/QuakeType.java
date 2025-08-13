package snowsan0113.hazardlib.api.quake;

import java.awt.*;
import java.util.Arrays;

public enum QuakeType {
    @Deprecated ZERO("震度0", 0, "0", Color.LIGHT_GRAY), //P2PAPIの仕様にないため
    ONE("震度1", 10, "1", Color.WHITE),
    TWO("震度2", 20, "2", Color.CYAN),
    THREE("震度3", 30, "3", Color.BLUE),
    FOUR("震度4", 40, "4", Color.YELLOW),
    FIVE_LOWER("震度5弱", 45, "5-", new Color(241, 181, 64)),
    FIVE_UPPER("震度5強", 50, "5+", new Color(138, 93, 6)),
    SIX_LOWER("震度6弱",55, "6-", Color.RED),
    SIX_UPPER("震度6強", 60, "6+", new Color(90, 0, 0)),
    SEVEN("震度7", 70, "7", new Color(128, 0, 128)),
    UNKNWON("不明", -1, "不明", Color.WHITE);

    private final String jp_name;
    private final String name;
    private final int raw_int;
    private final Color color;

    QuakeType(String jp_name, int raw_int, String name, Color color) {
        this.jp_name = jp_name;
        this.raw_int = raw_int;
        this.name = name;
        this.color = color;
    }

    public String getJPName() {
        return jp_name;
    }

    public int getRawData() {
        return raw_int;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }

    public static QuakeType getRawToType(int raw_int) {
        return Arrays.stream(QuakeType.values())
                .filter(type -> type.getRawData() == raw_int)
                .findFirst()
                .orElse(null);
    }
}
