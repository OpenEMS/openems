package io.openems.edge.bridge.genibus.api.task;

import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.taskmanager.Priority;

public class PumpReadTask16bitOrMore extends AbstractPumpTask {

    private Channel<Double> channel;
    private final Priority priority;
    protected final double channelMultiplier;
    private int refreshInfoCounter = 0;
    private int byteCounter = 0;
    private byte[] dataArray = new byte[dataByteSize];

    public PumpReadTask16bitOrMore(int numberOfBytes, int address, int headerNumber, Channel<Double> channel, String unitString, Priority priority, double channelMultiplier) {
        super(address, headerNumber, unitString, numberOfBytes);
        this.channel = channel;
        this.priority = priority;
        this.channelMultiplier = channelMultiplier;
    }

    public PumpReadTask16bitOrMore(int numberOfBytes, int address, int headerNumber, Channel<Double> channel, String unitString, Priority priority) {
        this(numberOfBytes, address, headerNumber, channel, unitString, priority, 1);
    }

    @Override
    public void setResponse(byte data) {

        // Collect the bytes.
        dataArray[byteCounter] = data;
        if (byteCounter != dataByteSize - 1) {
            byteCounter++;
            return;
        }

        // ref_norm changes INFO if control mode is changed. If task is ref_norm (2, 49), regularly update INFO.
        if (getHeader() == 2 && getAddress() == 49) {
            refreshInfoCounter++;
            if (refreshInfoCounter >= 5) {
                super.resetInfo();
                refreshInfoCounter = 0;
            }
        }

        // Data is complete, now calculate value and put it in the channel.
        if (byteCounter >= dataByteSize - 1) {
            byteCounter = 0;

            // When vi == 0 (false), then 0xFF means "data not available".
            if (super.vi == false) {
                if ((dataArray[0] & 0xFF) == 0xFF) {
                    this.channel.setNextValue(null);
                    return;
                }
            }

            int[] actualDataArray = new int[dataByteSize];
            for (int i = 0; i < dataByteSize; i++) {
                actualDataArray[i] = Byte.toUnsignedInt(dataArray[i]);
            }

            int range = 254;
            double tempValue;
            if (super.vi) {
                range = 255;
            }

            switch (super.sif) {
                case 2:
                    // Formula working for both 8 and 16 bit
                    double sumValue = 0;
                    for (int i = 0; i < dataByteSize; i++) {
                        sumValue = sumValue +  actualDataArray[i] * ((double) super.rangeScaleFactor / (double) range * Math.pow(256, i));
                    }
                    // value w.o considering Channel
                    tempValue = (super.zeroScaleFactor + sumValue) * super.unitCalc;

                    /* 16bit formula
                    tempValue = (super.zeroScaleFactor + (actualDataArray[0] * ((double) super.rangeScaleFactor / (double) range))
                            + (actualDataArray[1] * ((double) super.rangeScaleFactor / ((double) range * 256)))) * super.unitCalc;
                    */

                    this.channel.setNextValue(correctValueForChannel(tempValue) * channelMultiplier);
                    break;
                case 3:
                    // Formula working for 8, 16, 24 and 32 bit.
                    double highPrecisionValue = 0;
                    for (int i = 0; i < dataByteSize; i++) {
                        highPrecisionValue = highPrecisionValue + actualDataArray[i] * Math.pow(256, (dataByteSize - i - 1));
                    }
                    int exponent = dataByteSize - 2;
                    if (exponent < 0) {
                        exponent = 0;
                    }
                    tempValue = (Math.pow(256, exponent) * (256 * super.scaleFactorHighOrder + super.scaleFactorLowOrder)
                            + highPrecisionValue) * super.unitCalc;

                    // Extended precision, 8 bit formula.
                    //tempValue = ((256 * super.scaleFactorHighOrder + super.scaleFactorLowOrder) + actualData) * super.unitCalc;

                    this.channel.setNextValue(correctValueForChannel(tempValue) * channelMultiplier);
                    break;
                case 1:
                case 0:
                default:
                    // Formula works for 8 to 32 bit.
                    double unscaledMultiByte = 0;
                    for (int i = 0; i < dataByteSize; i++) {
                        unscaledMultiByte = unscaledMultiByte + actualDataArray[i] * Math.pow(256, (dataByteSize - i - 1));
                    }
                    this.channel.setNextValue(unscaledMultiByte * channelMultiplier);
                    break;

            }
        }


    }

    private double correctValueForChannel(double tempValue) {
        //unitString
        if (super.unitString != null) {
            // Channel unit is dC.
            int temperatureFactor = 10;

            switch (super.unitString) {
                case "Celsius/10":
                case "Celsius":
                    //dC
                    return tempValue * temperatureFactor;
                case "Kelvin/100":
                case "Kelvin":
                    //dC
                    return (tempValue - 273.15) * temperatureFactor;

                case "Fahrenheit":
                    //dC
                    return ((tempValue - 32) * (5.d / 9.d)) * temperatureFactor;

                case "m/10000":
                case "m/100":
                case "m/10":
                case "m":
                case "m*10":
                    if (this.channel.channelDoc().getUnit().equals(Unit.BAR)) {
                        return tempValue / 10.0;
                    }
                    return tempValue;
            }
        }

        return tempValue;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }
}
