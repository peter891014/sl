package com.example.sl.energy.entity;

import lombok.Data;

import java.time.LocalTime;

/**
 * - 充电宝的充放指令
 */
@Data

public class DispatchCommand {
    private LocalTime startTime;
    private LocalTime endTime;
    // 正数表⽰充电，负数表⽰放电
    private Double power;

    public DispatchCommand(LocalTime startTime, LocalTime endTime, Double power) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.power = power;
    }

    public DispatchCommand() {
    }
}