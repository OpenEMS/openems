package io.openems.edge.controller.heatnetwork.multipleheatercombined;

import io.openems.edge.thermometer.api.Thermometer;

import java.util.HashMap;
import java.util.Map;

/**
 * This class helps to get the Correct Thermometer of the corresponding Heater as well as "setpoints" for the Thermometer
 * Provides the methods to check, if min or max temperature is reached --> therefore heater can activate/deactivate correctly.
 */

class ThermometerWrapper {

    //Map the Thermometer to their min/max Value
    private Map<Thermometer, Integer> thermometerAndValue = new HashMap<>();
    //ThermometerKind == Activate/Deactivate on Heatcontrol. Mapped thermometerkind to Thermometer
    private Map<ThermometerKind, Thermometer> thermometerKindThermometerMap = new HashMap<>();


    ThermometerWrapper(Thermometer minThermometer, Thermometer maxThermometer, int minValue, int maxValue) {

        this.thermometerKindThermometerMap.put(ThermometerKind.ACTIVATE_THERMOMETER, minThermometer);
        this.thermometerAndValue.put(minThermometer, minValue);
        this.thermometerKindThermometerMap.put(ThermometerKind.DEACTIVATE_THERMOMETER, maxThermometer);
        this.thermometerAndValue.put(maxThermometer, maxValue);
    }

    //Thermometer where heater should deactivate --> Max Temperature
    Thermometer getMaxThermometer() {
        return this.thermometerKindThermometerMap.get(ThermometerKind.DEACTIVATE_THERMOMETER);
    }

    //Thermometer where heeater should activate --> Min Temperature
    Thermometer getMinThermometer() {
        return this.thermometerKindThermometerMap.get(ThermometerKind.ACTIVATE_THERMOMETER);
    }

    private int minThermometerValue() {
        return this.thermometerAndValue.get(this.thermometerKindThermometerMap.get(ThermometerKind.ACTIVATE_THERMOMETER));
    }

    private int maxThermometerValue() {
        return this.thermometerAndValue.get(this.thermometerKindThermometerMap.get(ThermometerKind.DEACTIVATE_THERMOMETER));
    }

    //Current Temperature above max allowed Temp.
    boolean offTemperatureAboveMaxValue() {
        return this.getMaxThermometer().getTemperatureValue() > this.maxThermometerValue();
    }

    //current Temp. beneath min Temp.
    boolean onTemperatureBelowMinValue() {
        return this.getMinThermometer().getTemperatureValue() < this.minThermometerValue();
    }

}
