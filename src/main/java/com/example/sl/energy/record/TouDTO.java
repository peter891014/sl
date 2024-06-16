package com.example.sl.energy.record;

import java.time.LocalDate;
import java.util.Set;

/**
- 单⽇峰⾕时段与电价配置
  */
public record TouDTO(LocalDate date, Set<TouPeriodDTO> periods,
                     Set<TouPriceDTO> prices) {
}