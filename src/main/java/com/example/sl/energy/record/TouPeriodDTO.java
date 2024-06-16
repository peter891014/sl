package com.example.sl.record;

import com.example.sl.enums.TouState;

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