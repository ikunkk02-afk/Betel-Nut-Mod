package betel.nut.addiction;

import net.minecraft.nbt.CompoundTag;

/** Personal use history on the overworld calendar. Offline gaps and clock rewinds never settle days. */
public final class DailyUseTracker {
    private long observedDay = Long.MIN_VALUE;
    private long highestDay = Long.MIN_VALUE;
    private long lastUseDay = Long.MIN_VALUE;
    private boolean usedToday;
    private boolean fullDayObserved;
    private int soberDays;

    public void beginSession(long day) {
        observedDay = day;
        highestDay = Math.max(highestDay, day);
        usedToday = lastUseDay == day;
        fullDayObserved = false;
    }

    /** Returns true exactly when one observed, complete abstinent day finishes. */
    public boolean advance(long day) {
        if (observedDay == Long.MIN_VALUE) { beginSession(day); return false; }
        if (day == observedDay) return false;
        boolean sober = day > highestDay && day == observedDay + 1 && fullDayObserved && !usedToday;
        if (sober) soberDays = Math.min(10000, soberDays + 1);
        else if (usedToday) soberDays = 0;
        fullDayObserved = day > highestDay;
        observedDay = day;
        highestDay = Math.max(highestDay, day);
        usedToday = lastUseDay == day;
        return sober;
    }

    public boolean eat(long day) {
        advance(day);
        soberDays = 0;
        if (usedToday || day <= lastUseDay || day < highestDay) return false;
        usedToday = true;
        lastUseDay = day;
        return true;
    }

    public boolean usedToday() { return usedToday; }
    public boolean canSettleToday() { return fullDayObserved && !usedToday; }
    public int soberDays() { return soberDays; }
    public void clear() { observedDay = highestDay = lastUseDay = Long.MIN_VALUE; usedToday = fullDayObserved = false; soberDays = 0; }
    public void write(CompoundTag tag) {
        tag.putLong("calendarHighestDay", highestDay);
        tag.putLong("calendarLastUseDay", lastUseDay);
        tag.putInt("calendarSoberDays", soberDays);
    }
    public void read(CompoundTag tag) {
        clear();
        if (tag.contains("calendarHighestDay")) highestDay = tag.getLong("calendarHighestDay");
        if (tag.contains("calendarLastUseDay")) lastUseDay = tag.getLong("calendarLastUseDay");
        soberDays = Math.max(0, Math.min(10000, tag.getInt("calendarSoberDays")));
    }
    public void copyFrom(DailyUseTracker other) {
        observedDay = other.observedDay; highestDay = other.highestDay; lastUseDay = other.lastUseDay;
        usedToday = other.usedToday; fullDayObserved = other.fullDayObserved; soberDays = other.soberDays;
    }
}
