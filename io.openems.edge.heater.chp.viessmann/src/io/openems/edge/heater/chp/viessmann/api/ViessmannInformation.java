package io.openems.edge.heater.chp.viessmann.api;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;

public interface ViessmannInformation extends ViessmannPowerPercentage {
    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Module Modus Channel.
         * 0 Off
         * 1 Hand
         * 2 Auto
         * <ul>
         *
         *  <li>Type: Integer
         * *</ul>
         */
        MODE(Doc.of(OpenemsType.INTEGER)),
        /**
         * ModuleStatus.
         * 0 Off
         * 1 Ready
         * 2 Start
         * 3 Running
         * 4 Disturbance
         */
        STATUS(Doc.of(OpenemsType.INTEGER)),
        /**
         * Operating Mode Type.
         * 0 Off
         * 1 Hand
         * 2 Grid substitute
         * 3 --
         * 4 100%
         * 5 Between 0-100%
         */
        OPERATING_MODE(Doc.of(OpenemsType.INTEGER)),
        /**
         * SetPoint Operation Mode.
         * Format: n (Int 16)
         * Signed Int
         */
        SET_POINT_OPERATION_MODE(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
        /**
         * ErrorBits. Length 2 Byte --> Each bit acts as a flag --> Vitobloc Gateway
         */
        ERROR_BITS_1(Doc.of(OpenemsType.INTEGER)),
        ERROR_BITS_2(Doc.of(OpenemsType.INTEGER)),
        ERROR_BITS_3(Doc.of(OpenemsType.INTEGER)),
        ERROR_BITS_4(Doc.of(OpenemsType.INTEGER)),
        ERROR_BITS_5(Doc.of(OpenemsType.INTEGER)),
        ERROR_BITS_6(Doc.of(OpenemsType.INTEGER)),
        ERROR_BITS_7(Doc.of(OpenemsType.INTEGER)),
        ERROR_BITS_8(Doc.of(OpenemsType.INTEGER)),
        /**
         * Operating-Time of the Chp.
         * <li>Type: Integer</li>
         * <li> Unit: Hours</li>
         */
        OPERATING_HOURS(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),
        /**
         * Operating Time of Chp in Min.
         * <li>Type: Integer</li>
         * <li> Unit: Minutes</li>
         */
        OPERATING_MINUTES(Doc.of(OpenemsType.INTEGER).unit(Unit.MINUTE)),
        /**
         * How often was the Chp started.
         */
        START_COUNTER(Doc.of(OpenemsType.INTEGER)),
        /**
         * Intervall of Maintenance in Hours. Signed Int
         * <li>Type: Integer</li>
         * <li>Unit: Hour</li>
         */
        MAINTENANCE_INTERVAL(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),
        /**
         * Locking the Module.
         * <li>Type: Signed Int</li>
         * <li>Unit: Hour </li>
         */
        MODULE_LOCK(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),
        /**
         * Time when a warning should appear.
         * <li>Type: Signed Int</li>
         * <li>Unit: Hour</li>
         */
        WARNING_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),
        /**
         * Time till the next Maintenance should happen.
         */
        NEXT_MAINTENANCE(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),
        /**
         * Exhaustion Values.
         * <li>Type: Signed Int</li>
         * <li>Unit: Degree Celsius</li>
         */
        EXHAUST_A(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
        EXHAUST_B(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
        EXHAUST_C(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
        EXHAUST_D(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
        /**
         * TemperatureSensors and their values.
         * <li>Type: Signed Int</li>
         * <li> Unit: Deci-degree Celsius</li>
         */
        PT_100_1(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        PT_100_2(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        PT_100_3(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        PT_100_4(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        PT_100_5(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        PT_100_6(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Voltage of the Battery.
         * <li>Unit: Deci-Volt</li>
         */
        BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.DECI_VOLT)),
        /**
         * Pressure of the Oil in the Chp.
         * <li>Unit: Bar</li>
         */
        OIL_PRESSURE(Doc.of(OpenemsType.INTEGER).unit(Unit.BAR)),
        /**
         * Value of comparison between Oxygen left in the Exhaust with Oxygen of it's Reference.
         * <li>Unit: Ten-thousandth - Volt</li>
         */
        LAMBDA_PROBE_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.TEN_THOUSANDTH_VOLT)),
        /**
         * Rotations per minute.
         * <li>Unit: R/min</li>
         */
        ROTATION_PER_MIN(Doc.of(OpenemsType.INTEGER).unit(Unit.ROTATION_PER_MINUTE)),
        /**
         * Temperature Controller.
         * <li>Unit: Degree Celsius</li>
         * <li>Type: Signed Int</li>
         */
        TEMPERATURE_CONTROLLER(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Temperature Clearance/release.
         * <li>Unit: Degree Celsius</li>
         * <li>Type: Signed Int</li>
         */
        TEMPERATURE_CLEARANCE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
        /**
         * Supply Voltages L1-L3.
         * <li>Unit: Volt</li>
         * <li>Type: Signed Int</li>
         */
        SUPPLY_VOLTAGE_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),
        SUPPLY_VOLTAGE_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),
        SUPPLY_VOLTAGE_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),
        /**
         * Generator Voltage L1-L3.
         * <li>Unit: Volt</li>
         * <li>Type: Signed Int</li>
         */
        GENERATOR_VOLTAGE_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),
        GENERATOR_VOLTAGE_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),
        GENERATOR_VOLTAGE_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),
        /**
         * Generator Electricity in Ampere.
         * <li>Unit: Ampere</li>
         * <li>Type: Signed Int</li>
         */
        GENERATOR_ELECTRICITY_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)),
        GENERATOR_ELECTRICITY_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)),
        GENERATOR_ELECTRICITY_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)),
        /**
         * Total Voltage of Supply.
         * <li>Unit: Volt</li>
         * <li>Type: Signed int</li>
         */
        SUPPLY_VOLTAGE_TOTAL(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),
        /**
         * Total generator Voltage.
         * <li>Unit: Volt</li>
         * <li>Type: Signed Int</li>
         */
        GENERATOR_VOLTAGE_TOTAL(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),
        /**
         * Total electricity Usage in Ampere.
         * <li>Unit: Ampere</li>
         * <li>Type: Signed INt</li>
         */
        GENERATOR_ELECTRICITY_TOTAL(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)),

        ENGINE_PERFORMANCE(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT)),

        SUPPLY_FREQUENCY(Doc.of(OpenemsType.FLOAT).unit(Unit.HERTZ)),
        GENERATOR_FREQUENCY(Doc.of(OpenemsType.FLOAT).unit(Unit.HERTZ)),
        /**
         * CosPhi of Chp.
         * <p>
         *
         * </p>
         */
        ACTIVE_POWER_FACTOR(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLI_DEGREE)),

        /**
         * Reserved by Chp.
         * <li>Unit: kWh</li>
         * <li>Type: Integer</li>
         */
        RESERVE(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),
        /**
         * All occuring Errors as String.
         */
        ERROR_CHANNEL(Doc.of(OpenemsType.STRING)),

        ERROR_OCCURED(Doc.of(OpenemsType.BOOLEAN));


        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }
    }

    default Channel<Integer> getModus() {
        return this.channel(ChannelId.MODE);
    }

    default Channel<Integer> getStatus() {
        return this.channel(ChannelId.STATUS);
    }

    default Channel<Integer> getOperatingMode() {
        return this.channel(ChannelId.OPERATING_MODE);
    }

    default Channel<Integer> getSetPointOperationMode() {
        return this.channel(ChannelId.SET_POINT_OPERATION_MODE);
    }

    default Channel<Integer> getErrorOne() {
        return this.channel(ChannelId.ERROR_BITS_1);
    }

    default Channel<Integer> getErrorTwo() {
        return this.channel(ChannelId.ERROR_BITS_2);
    }

    default Channel<Integer> getErrorThree() {
        return this.channel(ChannelId.ERROR_BITS_3);
    }

    default Channel<Integer> getErrorFour() {
        return this.channel(ChannelId.ERROR_BITS_4);
    }

    default Channel<Integer> getErrorFive() {
        return this.channel(ChannelId.ERROR_BITS_5);
    }

    default Channel<Integer> getErrorSix() {
        return this.channel(ChannelId.ERROR_BITS_6);
    }

    default Channel<Integer> getErrorSeven() {
        return this.channel(ChannelId.ERROR_BITS_7);
    }

    default Channel<Integer> getErrorEight() {
        return this.channel(ChannelId.ERROR_BITS_8);
    }

    default Channel<Integer> getOperatingHours() {
        return this.channel(ChannelId.OPERATING_HOURS);
    }

    default Channel<Integer> getOperatingMinutes() {
        return this.channel(ChannelId.OPERATING_MINUTES);
    }

    default Channel<Integer> getStartCounter() {
        return this.channel(ChannelId.START_COUNTER);
    }

    default Channel<Integer> getMaintenanceInterval() {
        return this.channel(ChannelId.MAINTENANCE_INTERVAL);
    }

    default Channel<Integer> getModuleLock() {
        return this.channel(ChannelId.MODULE_LOCK);
    }

    default Channel<Integer> getWarningTime() {
        return this.channel(ChannelId.WARNING_TIME);
    }

    default Channel<Integer> getNextMaintenance() {
        return this.channel(ChannelId.NEXT_MAINTENANCE);
    }

    default Channel<Integer> getExhaustA() {
        return this.channel(ChannelId.EXHAUST_A);
    }

    default Channel<Integer> getExhaustB() {
        return this.channel(ChannelId.EXHAUST_B);
    }

    default Channel<Integer> getExhaustC() {
        return this.channel(ChannelId.EXHAUST_C);
    }

    default Channel<Integer> getExhaustD() {
        return this.channel(ChannelId.EXHAUST_D);
    }

    default Channel<Integer> getPt100_1() {
        return this.channel(ChannelId.PT_100_1);
    }

    default Channel<Integer> getPt100_2() {
        return this.channel(ChannelId.PT_100_1);
    }

    default Channel<Integer> getPt100_3() {
        return this.channel(ChannelId.PT_100_1);
    }

    default Channel<Integer> getPt100_4() {
        return this.channel(ChannelId.PT_100_1);
    }

    default Channel<Integer> getPt100_5() {
        return this.channel(ChannelId.PT_100_1);
    }

    default Channel<Integer> getPt100_6() {
        return this.channel(ChannelId.PT_100_1);
    }

    default Channel<Integer> getBatteryVoltage() {
        return this.channel(ChannelId.BATTERY_VOLTAGE);
    }

    default Channel<Integer> getOilPressure() {
        return this.channel(ChannelId.OIL_PRESSURE);
    }

    default Channel<Integer> getLambdaProbeVoltage() {
        return this.channel(ChannelId.LAMBDA_PROBE_VOLTAGE);
    }

    default Channel<Integer> getRotationPerMinute() {
        return this.channel(ChannelId.ROTATION_PER_MIN);
    }

    default Channel<Integer> getTemperatureController() {
        return this.channel(ChannelId.TEMPERATURE_CONTROLLER);
    }

    default Channel<Integer> getTemperatureClearance() {
        return this.channel(ChannelId.TEMPERATURE_CLEARANCE);
    }

    default Channel<Integer> getSupplyVoltageL1() {
        return this.channel(ChannelId.SUPPLY_VOLTAGE_L1);
    }

    default Channel<Integer> getSupplyVoltageL2() {
        return this.channel(ChannelId.SUPPLY_VOLTAGE_L2);
    }

    default Channel<Integer> getSupplyVoltageL3() {
        return this.channel(ChannelId.SUPPLY_VOLTAGE_L3);
    }

    default Channel<Integer> getGeneratorElectricityL1() {
        return this.channel(ChannelId.GENERATOR_ELECTRICITY_L1);
    }

    default Channel<Integer> getGeneratorElectricityL2() {
        return this.channel(ChannelId.GENERATOR_ELECTRICITY_L2);
    }

    default Channel<Integer> getGeneratorElectricityL3() {
        return this.channel(ChannelId.GENERATOR_ELECTRICITY_L3);
    }

    default Channel<Integer> getSupplyVoltageTotal() {
        return this.channel(ChannelId.SUPPLY_VOLTAGE_TOTAL);
    }

    default Channel<Integer> getGeneratorVoltageTotal() {
        return this.channel(ChannelId.GENERATOR_VOLTAGE_TOTAL);
    }

    default Channel<Integer> getGeneratorElectricityTotal() {
        return this.channel(ChannelId.GENERATOR_ELECTRICITY_TOTAL);
    }

    default Channel<Integer> getEnginePerformance() {
        return this.channel(ChannelId.ENGINE_PERFORMANCE);
    }

    default Channel<Float> getSupplyFrequency() {
        return this.channel(ChannelId.SUPPLY_FREQUENCY);
    }

    default Channel<Float> getGeneratorFrequency() {
        return this.channel(ChannelId.GENERATOR_FREQUENCY);
    }

    default Channel<Float> getActivePowerFactor() {
        return this.channel(ChannelId.ACTIVE_POWER_FACTOR);
    }

    default Channel<Integer> getReserve() {
        return this.channel(ChannelId.RESERVE);
    }

    default Channel<String> getErrorChannel() {
        return this.channel(ChannelId.ERROR_CHANNEL);
    }

    default Channel<Boolean> isErrorOccured() {
        return this.channel(ChannelId.ERROR_OCCURED);
    }
}