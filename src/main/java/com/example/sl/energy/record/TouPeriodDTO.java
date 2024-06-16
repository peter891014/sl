package com.example.sl.energy.record;

import com.example.sl.energy.enums.TouState;

import java.time.LocalTime;
/**

 - 峰⾕时段配置
 */
public record TouPeriodDTO(TouState state, LocalTime startTime, LocalTime
        endTime) implements Comparable<TouPeriodDTO> {

    @Override
    public int compareTo(TouPeriodDTO other) {
        return this.startTime.compareTo(other.startTime);
    }

}