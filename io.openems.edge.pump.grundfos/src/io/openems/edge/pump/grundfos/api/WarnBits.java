package io.openems.edge.pump.grundfos.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum WarnBits {
    MAGNA_3(
            new String[]{
                    "Motor drive-end (DE) bearing temperature warning limit, code 148 \n",
                    "Motor non-drive-end (NDE) bearing temperature warning limit, code 149 \n",
                    "Limit 1 exceeded ( limit_exc_1_limit ), code 190\n",
                    "Limit 2 exceeded ( limit_exc_2_limit ), code 191\n",
                    "Multi-pump (Twin pump) pump communication fault, code 10 (code 77)\n",
                    "Motor bearings need lubrication (Service notification), code 240\n",
                    "Motor bearings need change (Service notification), code 30\n",
                    "Motor varistor(s) need change (Service notification), code 31\n",},
            new String[]{
                    "Signal fault, Analog input 1 (AI1), code 165\n",
                    "Signal fault, Analog input 2 (AI2), code 166\n",
                    "Signal fault, Analog input 3 (AI3), code 167\n",
                    "Temperature sensor 1 signal fault, code 91\n",
                    "Temperature sensor 2 signal fault, code 175\n",
                    "LiqTec sensor signal fault, code 164\n",
                    "Real time clock fault, code 157\n",
                    "General sensor/ Grundfos sensor fault , code 88\n",},
            new String[]{
                    "Sensor supply fault, 5 V, code 161\n",
                    "Sensor supply fault, 24 V, code 162\n",
                    "Soft pressure build up timeout, code 215\n",
                    "Underload, code 56\n",
                    "Dry running, code 57\n",
                    "Bit 5 was set\n",
                    "Bit 6 was set\n",
                    "Bit 7 was set\n",},
            new String[]{
                    "Bit 0 was set\n",
                    "Bit 1 was set\n",
                    "Bit 2 was set\n",
                    "Bit 3 was set\n",
                    "Bit 4 was set\n",
                    "Bit 5 was set\n",
                    "Bit 6 was set\n",
                    "Bit 7 was set\n",});

    private List<String> errorBits1 = new ArrayList<>();
    private List<String> errorBits2 = new ArrayList<>();
    private List<String> errorBits3 = new ArrayList<>();
    private List<String> errorBits4 = new ArrayList<>();

    WarnBits(String[] errorBits1, String[] errorBits2, String[] errorBits3, String[] errorBits4) {

        this.errorBits1.addAll(Arrays.asList(errorBits1));
        this.errorBits2.addAll(Arrays.asList(errorBits2));
        this.errorBits3.addAll(Arrays.asList(errorBits3));
        this.errorBits4.addAll(Arrays.asList(errorBits4));
    }


    public List<String> getErrorBits1() {
        return errorBits1;
    }

    public List<String> getErrorBits2() {
        return errorBits2;
    }

    public List<String> getErrorBits3() {
        return errorBits3;
    }

    public List<String> getErrorBits4() {
        return errorBits4;
    }
}
