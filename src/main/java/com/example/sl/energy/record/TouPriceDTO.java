package com.example.sl.energy.record;

import com.example.sl.energy.enums.TouState;
 /**
 - 峰⾕电价配置
 */
public record TouPriceDTO(TouState state, Double price) {
}