package ch.uzh.ifi.hase.soprafs26.constant;

import java.time.LocalTime;

public enum TimeWindow {
    MORNING(LocalTime.of(6, 0), LocalTime.of(12, 0)),
    AFTERNOON(LocalTime.of(12, 0), LocalTime.of(18, 0)),
    EVENING(LocalTime.of(18, 0), LocalTime.of(22, 0)),
    NIGHT(LocalTime.of(22, 0), LocalTime.of(6, 0)),
    CUSTOM(null, null);

    private final LocalTime startTime;
    private final LocalTime endTime;

    TimeWindow(LocalTime startTime, LocalTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static TimeWindow findWindow(LocalTime time) {
        for (TimeWindow window : values()) {
            if (window != CUSTOM && window.isWithinWindow(time)) {
                return window;
            }
        }
        return CUSTOM;
    }

    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }

    public boolean isWithinWindow(LocalTime time) {
        if (startTime.isBefore(endTime)) {
            return !time.isBefore(startTime) && time.isBefore(endTime);
        } else {
            return !time.isBefore(startTime) || time.isBefore(endTime);
        }
    }
}