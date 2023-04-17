package me.alex.cryptotrader.instruction;

public enum TimePeriod {

    _5_MINUTES("5m", 300_000),
    _10_MINUTES("10m", 600_000),
    _15_MINUTES("15m", 900_000),
    _30_MINUTES("30m", 1_800_000),
    _1_HOUR("1h", 3_600_000),
    _3_HOURS("3h", 10_800_000),
    _6_HOURS("6h", 21_600_000),
    _12_HOURS("12h", 43_200_000),
    _24_HOURS("24h", 86_400_000),
    _48_HOURS("48h", 172_800_000),
    ;

    public static final TimePeriod[] SHORT = new TimePeriod[]{
            _5_MINUTES, _10_MINUTES, _15_MINUTES, _30_MINUTES, _1_HOUR, _3_HOURS, _6_HOURS, _12_HOURS, _24_HOURS
    };

    public static final TimePeriod[] TESTING = new TimePeriod[]{
            _3_HOURS, _6_HOURS, _12_HOURS, _24_HOURS, _48_HOURS
    };

    private final String shortName;
    private final long milliseconds;

    TimePeriod(String name, long millis) {
        this.shortName = name;
        this.milliseconds = millis;
    }

    public String getShortName() {
        return shortName;
    }

    public long getMilliseconds() {
        return milliseconds;
    }
}
