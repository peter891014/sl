package com.example.sl.energy.service;

import com.example.sl.energy.entity.DispatchCommand;
import com.example.sl.energy.enums.TouState;
import com.example.sl.energy.record.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class DispatchService {
    /*  • 提⽰⼀：只需要考虑单台设备的充放调度。
        • 提⽰⼆：以「输出⼀天的充放计划」为⽬标，不需要考虑⻓周期的优化策略。
        • 提⽰三：⽆需考虑设备运⾏的损耗。
        • 提⽰四：⽆需考虑设备过充（设备满电后继续充电）和过放（设备亏电后继续放电）的问题*/
    public List<DispatchCommand> dispatch(DispatchContext context) {

        List<DispatchCommand> commands = new ArrayList<>();
        List<TouPeriodDTO> sortedPeriods = context.touDTO().periods().stream()
                .sorted(Comparator.comparing(TouPeriodDTO::startTime))
                .toList();
        Map<TouState, Double> pricesMap = buildTouPricesMap();
        DeviceSpecDTO deviceSpec = context.deviceSpecDTO();
        double totalProfit = 0;
        int currentStorage = 0;
        int gonglv = deviceSpec.standardPower().intValue();
        int maxStorage = deviceSpec.capacity().intValue();
        int i = 0;
        int hour;
        boolean isCircle = true;
        while (i < sortedPeriods.size() && isCircle) {
            TouPeriodDTO currentPeriod = sortedPeriods.get(i);
            for (hour = currentPeriod.startTime().getHour(); hour < currentPeriod.endTime().getHour() || currentPeriod.endTime().getHour() == 0; hour++) {

                DispatchCommand command = determineTouState(currentPeriod, hour, currentStorage, maxStorage, gonglv, sortedPeriods, i);
                switch (command.getPower().intValue()) {
                    case 1: //充电
                        currentStorage = Math.min(currentStorage + gonglv, maxStorage);
                        totalProfit -= gonglv * pricesMap.get(currentPeriod.state());
                        break;
                    case -1: //放电
                        currentStorage = Math.max(currentStorage - gonglv, 0);
                        totalProfit += gonglv * pricesMap.get(currentPeriod.state());
                        break;
                    case 0:
                        break;
                }
                printStatus(hour, currentPeriod.state().name(), currentStorage, command.getPower().intValue());
                commands.add(command);
                int newIndex = getIndex(sortedPeriods, hour, i);
                if (newIndex != i) {
                    i = newIndex;
                    break;
                }

                if (hour == 23) {
                    isCircle = false;
                    break;
                }
            }
        }
        System.out.println("Total Profit: " + totalProfit + " yuan");
        return commands;
    }

    private static void printStatus(int hour, String typeName, int currentStorage, int power) {
        System.out.print("时间：" + hour);
        System.out.print(" 当前电网状态：" + typeName);
        System.out.print(" 容量：" + currentStorage + " 当前状态:");
        System.out.println(power == 1 ? " 充电" : power == -1 ? " 放电" : power == 0 ? " 保持不变" : "");
    }

    public static void main(String[] args) {
        DispatchService service = new DispatchService();
        Set<TouPeriodDTO> periods = service.buildPeriods();
        Set<TouPriceDTO> prices = service.buildTouPrices();
        DeviceSpecDTO deviceSpec = new DeviceSpecDTO(100.0, 50.0);

        // 单日峰谷时段配置
        LocalDate date = LocalDate.now();
        TouDTO touDTO = new TouDTO(date, periods, prices);
        DispatchContext context = new DispatchContext(touDTO, deviceSpec);
        List<DispatchCommand> commands = service.dispatch(context);
//        System.out.println("Dispatch Commands:");
//        for (DispatchCommand command : commands) {
//            System.out.printf("Time %s  %s 充电状态：%s %n", command.getStartTime(),command.getEndTime(), command.getPower());
//        }
    }

    private Set<TouPeriodDTO> buildPeriods() {
        return Stream.of(
                new TouPeriodDTO(TouState.VALLEY, LocalTime.of(0, 0), LocalTime.of(1, 0)),
                new TouPeriodDTO(TouState.PEAK, LocalTime.of(1, 0), LocalTime.of(2, 0)),
                new TouPeriodDTO(TouState.VALLEY, LocalTime.of(2, 0), LocalTime.of(3, 0)),
                new TouPeriodDTO(TouState.PEAK, LocalTime.of(3, 0), LocalTime.of(5, 0)),
                new TouPeriodDTO(TouState.CRITICAL_PEAK, LocalTime.of(5, 0), LocalTime.of(8, 0)),
                new TouPeriodDTO(TouState.VALLEY, LocalTime.of(8, 0), LocalTime.of(13, 0)),
                new TouPeriodDTO(TouState.PEAK, LocalTime.of(13, 0), LocalTime.of(15, 0)),
                new TouPeriodDTO(TouState.CRITICAL_PEAK, LocalTime.of(15, 0), LocalTime.of(17, 0)),
                new TouPeriodDTO(TouState.PEAK, LocalTime.of(17, 0), LocalTime.of(20, 0)),
                new TouPeriodDTO(TouState.CRITICAL_PEAK, LocalTime.of(20, 0), LocalTime.of(21, 0)),
                new TouPeriodDTO(TouState.PEAK, LocalTime.of(21, 0), LocalTime.of(23, 0)),
                new TouPeriodDTO(TouState.CRITICAL_PEAK, LocalTime.of(23, 0), LocalTime.of(0, 0))
        ).collect(Collectors.toSet());
    }

    private Set<TouPriceDTO> buildTouPrices() {
        return Stream.of(
                new TouPriceDTO(TouState.PEAK, 0.92),
                new TouPriceDTO(TouState.VALLEY, 0.31),
                new TouPriceDTO(TouState.CRITICAL_PEAK, 1.20)
        ).collect(Collectors.toSet());
    }

    private Map<TouState, Double> buildTouPricesMap() {
        return Stream.of(
                new AbstractMap.SimpleEntry<>(TouState.PEAK, 0.92),
                new AbstractMap.SimpleEntry<>(TouState.VALLEY, 0.31),
                new AbstractMap.SimpleEntry<>(TouState.CRITICAL_PEAK, 1.20)
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static DispatchCommand determineTouState(TouPeriodDTO currentPeriod, int hour, int currentStorage, int maxStorage, int gonglv, List<TouPeriodDTO> touPeriodDTOList, int currentIndex) {
        TouState touState = currentPeriod.state();
        DispatchCommand command = new DispatchCommand(LocalTime.of(hour, 0), null, null);
        if (touState.equals(TouState.VALLEY)) { //谷
            if (findHasBiggerInNext(touPeriodDTOList, currentIndex, currentStorage, gonglv) && currentStorage < maxStorage) {
                command.setPower(1.0);
                return command;
            }
            command.setPower(0.0);
            return command;
        } else if (touState.equals(TouState.CRITICAL_PEAK)) {//尖
            if (currentStorage > 0) {
                command.setPower(-1.0);
                return command;
            }
            command.setPower(0.0);
            return command;
        } else if (touState.equals(TouState.PEAK)) {//峰
            if ((findIsTheLast(touPeriodDTOList, currentIndex) || judgePeakNeedToDisCharge(touPeriodDTOList, currentIndex, currentStorage, gonglv, maxStorage)) && currentStorage > 0) {
                command.setPower(-1.0);
                return command;
            }
            if (judgePeakNeedToCharge(touPeriodDTOList, currentIndex, currentStorage, maxStorage, gonglv) && currentStorage < maxStorage) {
                command.setPower(1.0);
                return command;
            }
            command.setPower(0.0);
            return command;
        }
        command.setPower(0.0);
        return command;
    }

    //判断峰是否需要充电
    static boolean judgePeakNeedToCharge(List<TouPeriodDTO> priceRangeEnumList, int i, int currentStorage, int maxStorage, int gonglv) {
        if (priceRangeEnumList.get(i + 1).state().equals(TouState.CRITICAL_PEAK)) { //后面两个周期中，如果第一个是尖,或者后面只有一个周期
            int h = getEndHour(priceRangeEnumList, i + 1) - priceRangeEnumList.get(i + 1).startTime().getHour();
            return h * gonglv > currentStorage;
        }
        if (i + 2 > priceRangeEnumList.size() - 1) {
            return false;
        } else if (priceRangeEnumList.get(i + 2).state().equals(TouState.CRITICAL_PEAK)) { //如果第二个是尖 第一个 是谷充不满剩余
            int h1 = getEndHour(priceRangeEnumList, i + 2) - priceRangeEnumList.get(i + 2).startTime().getHour();
            int h2 = getEndHour(priceRangeEnumList, i + 1) - priceRangeEnumList.get(i + 1).startTime().getHour();
            return h1 - h2 > 0 && h2 * gonglv + currentStorage < maxStorage;
        }
        return false;
    }

    static int getEndHour(List<TouPeriodDTO> priceRangeEnumList, int i) {
        if (findIsTheLast(priceRangeEnumList, i)) {
            return 24;
        }
        return priceRangeEnumList.get(i).endTime().getHour();
    }

    //判断峰是否需要放电
    private static boolean judgePeakNeedToDisCharge(List<TouPeriodDTO> priceRangeEnumList, int i, int currentStorage, int gonglv, int maxStorage) {

        if (priceRangeEnumList.get(i + 1).state().equals(TouState.CRITICAL_PEAK)) { //后面两个周期中，如果第一个是尖
            int h = getEndHour(priceRangeEnumList, i + 1) - priceRangeEnumList.get(i + 1).startTime().getHour();
            return h * gonglv < currentStorage;
        }
        if (priceRangeEnumList.get(i + 1).state().equals(TouState.VALLEY)) { //后面两个周期中，如果第一个是谷
            int h = getEndHour(priceRangeEnumList, i + 1) - priceRangeEnumList.get(i + 1).startTime().getHour();
            return maxStorage - (currentStorage - gonglv) <= h * gonglv; //如果后续谷充不满，则不充电
        }
        if (i + 2 > priceRangeEnumList.size() - 1) {
            return true;
        } else if (priceRangeEnumList.get(i + 2).state().equals(TouState.CRITICAL_PEAK)) { //如果第二个是尖 第一个 是谷充不满剩余
            int h1 = getEndHour(priceRangeEnumList, i + 2) - priceRangeEnumList.get(i + 2).startTime().getHour();
            int h2 = getEndHour(priceRangeEnumList, i + 1) - priceRangeEnumList.get(i + 1).startTime().getHour();
            return h1 - h2 < 0 || h2 * gonglv + currentStorage < maxStorage;
        }
        return false;
    }

    private static boolean findIsTheLast(List<TouPeriodDTO> periodDTOList, int i) {
        return periodDTOList.get(i).endTime().getHour() == 0;
    }

    private static int getIndex(List<TouPeriodDTO> periodDTOList, int hour, int i) {
        if (getEndHour(periodDTOList, i) - 1 == hour) {
            return Math.min(i + 1, periodDTOList.size() - 1);
        }
        return i;

    }

    //判断从此刻，到0点，充电后的容量/功率 <= 后续峰和尖的时间
    private static boolean findHasBiggerInNext(List<TouPeriodDTO> touPeriodDTOList, int i, int currentStorage, int gonglv) {

        boolean r = false;
        List<Integer> durationList = IntStream.range(i, touPeriodDTOList.size())
                .mapToObj(touPeriodDTOList::get)
                .filter(e -> TouState.PEAK.equals(e.state()) || TouState.CRITICAL_PEAK.equals(e.state()))
                .map(e -> getEndHour(e) - e.startTime().getHour()) // 直接计算差值
                .toList();
        if (!durationList.isEmpty()) {
            int sum = durationList.stream().mapToInt(Integer::intValue).sum();
            if (sum >= (currentStorage + gonglv) / gonglv) {
                r = true;
            }
        }
        return r;
    }

    private static int getEndHour(TouPeriodDTO t) {
        if (t.endTime().getHour() == 0) {
            return 24;
        }
        return t.endTime().getHour();
    }

}