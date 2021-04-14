package io.openems.edge.bridge.genibus.api.task;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public enum UnitTable {


    /**
     * The Standard Unit Table for Grundfos pumps.
     * Beneath is a list of the Standard unit tables supporting only temperature, active power and pressure values.
     * The Number is written in the information Data of each HeatPump Task.
     * Needed by the task for calculating the correct value to the Channel.
     * */
    //only pressure,temperature watt and rotations/time atm.
    //temperature, Watt, bar
    Standard_Unit_Table(
            new int[]{
                    // temperature
                    20, 21, 57, 84, 110, 111,
                    // powerActive
                    7, 8, 9, 44, 45,
                    // current
                    1,
                    // pressure
                    51,27,28,29,61,55,60,
                    91,83,24,25,26,
                    // percentage
                    113, 107, 12, 30, 76,
                    // flow
                    22, 23, 41, 92,
                    // frequency
                    105, 11, 16, 38, 17

            },
            new String[]{
                    // temperature
                    "Celsius/10", "Celsius", "Fahrenheit", "Kelvin/100", "diff-Kelvin/100", "diff-Kelvin",
                    // powerActive
                    "Watt", "Watt*10", "Watt*100", "kW", "kW*10",
                    // current
                    "Ampere*0.1",
                    // pressure
                    "bar/1000", "bar/100", "bar/10", "bar", "kPa", "psi", "psi*10",
                    "m/10000", "m/100", "m/10", "m", "m*10",
                    // percentage
                    "ppm", "0.01%", "0.1%", "1%", "10%",
                    // flow
                    "0.1*m³/h", "m³/h", "5*m³/h", "10*m³/h",
                    // frequency
                    "0.01*Hz", "0.5*Hz", "Hz", "2*Hz", "2.5*Hz"
            });

    private Map<Integer, String> informationData = new HashMap<>();

    UnitTable(int[] keys, String[] values) {
        AtomicInteger counter = new AtomicInteger();
        counter.set(0);
        Arrays.stream(keys).forEach(key -> {
            this.informationData.put(key, values[counter.get()]);
            counter.getAndIncrement();
        });

    }

    public Map<Integer, String> getInformationData() {
        return informationData;
    }
}

