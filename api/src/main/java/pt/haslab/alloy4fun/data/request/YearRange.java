package pt.haslab.alloy4fun.data.request;

import org.jboss.resteasy.reactive.RestQuery;

import java.time.LocalDateTime;
import java.time.Month;

public class YearRange {
    @RestQuery
    Integer yearMin;
    @RestQuery
    Integer yearMax;

    LocalDateTime minDate, maxDate;

    public YearRange() {
    }

    public YearRange(Integer yearMin, Integer yearMax) {
        this.yearMin = yearMin;
        this.yearMax = yearMax;
    }

    @Override
    public String toString() {
        return "YearRange{" + yearMin + "," + yearMax + '}';
    }

    public boolean testDate(LocalDateTime d) {
        try {
            return d.isBefore(maxDate) && d.isAfter(minDate);
        } catch (NullPointerException e) {
            this.cacheDates();
            return d.isBefore(maxDate) && d.isAfter(minDate);
        }
    }

    public void cacheDates() {
        minDate = yearMin == null ? LocalDateTime.MIN : LocalDateTime.of(yearMin, Month.SEPTEMBER, 1, 6, 0);
        maxDate = yearMax == null ? LocalDateTime.MAX : LocalDateTime.of(yearMax, Month.SEPTEMBER, 1, 6, 0);
    }

}
