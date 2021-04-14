package io.openems.edge.bridge.genibus.api.task;

import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.taskmanager.Priority;


// ToDo: Beschreibung
public class PumpWriteTask16bitOrMore extends PumpReadTask16bitOrMore implements GenibusWriteTask{

    private WriteChannel<Double> channel;
    //private boolean sendGet = false;
    private int sendGet = 1;

    public PumpWriteTask16bitOrMore(int numberOfBytes, int address, int headerNumber, WriteChannel<Double> channel, String unitString, Priority priority, double channelMultiplier) {
        super(numberOfBytes, address, headerNumber, channel, unitString, priority, channelMultiplier);
        this.channel = channel;
        // The method "getRequest()" does not work without first calling "setFourByteInformation()". Normally
        // "setFourByteInformation()" is executed when an info response APDU is processed. For ref_rem (5, 1) we can call
        // "setFourByteInformation()" manually since the parameters are fix and don't change. This way we do not need
        // to send an info APDU for ref_rem to be able to use "getRequest()".
        if (headerNumber == 5 && address == 1) {
            super.setFourByteInformation(0,0,2, (byte)0b11110, (byte)0, (byte)0b1100100);
        }
    }

    public PumpWriteTask16bitOrMore(int numberOfBytes, int address, int headerNumber, WriteChannel<Double> channel, String unitString, Priority priority) {
        this(numberOfBytes, address, headerNumber, channel, unitString, priority, 1);
    }

    @Override
    public int getRequest(int byteCounter, boolean clearChannel) {

        // Maybe add option "direct" that passes channel value directly to GENIbus. Not sure if needed.

        int request = -256;
        if (byteCounter > dataByteSize - 1) {
            return request;
        }
        if (super.informationDataAvailable() && this.channel.getNextWriteValue().isPresent()) {
            byte returnValue;

            // For values of type scaled or extended precision:
            // With INFO available, the value in nextWrite is automatically converted to the correct bytes for GENIbus.
            double dataOfChannel = correctValueFromChannel(this.channel.getNextWriteValue().get()) / super.channelMultiplier;
            switch (super.sif) {
                case 2:
                    // Formula working for both 8 and 16 bit
                    long combinedByteValueScaled = Math.round((-super.zeroScaleFactor * super.unitCalc + dataOfChannel) * (254 * Math.pow(256, (dataByteSize - 1))) / (super.rangeScaleFactor * super.unitCalc));
                    if (byteCounter == 0) {
                        returnValue = (byte) (combinedByteValueScaled / Math.pow(256, (dataByteSize - 1)));
                    } else {
                        returnValue = (byte) ((combinedByteValueScaled % Math.pow(256, (byteCounter))) / Math.pow(256, (dataByteSize - 1 - byteCounter)));
                    }
                    break;
                case 3:
                    // Formula working for 8, 16, 24 and 32 bit.
                    long combinedByteValueExtended = Math.round(dataOfChannel / super.unitCalc) - (256 * super.scaleFactorHighOrder + super.scaleFactorLowOrder);
                    if (byteCounter == 0) {
                        returnValue = (byte) (combinedByteValueExtended / Math.pow(256, (dataByteSize - 1)));
                    } else {
                        returnValue = (byte) ((combinedByteValueExtended % Math.pow(256, (byteCounter))) / Math.pow(256, (dataByteSize - 1 - byteCounter)));
                    }
                    break;
                case 1:
                case 0:
                default:
                    returnValue = (byte) Math.round(dataOfChannel);
                    break;
            }

            // If the write task is added to a telegram, reset channel to null to send write just once.
            // Also, do GET next cycle to update value.
            if (clearChannel) {
                this.channel.getNextWriteValueAndReset();
                sendGet = 2;
            }
            return returnValue;

        }
        return request;
    }

    private double correctValueFromChannel(double tempValue) {
        //unitString
        if (super.unitString != null) {
            // Channel unit is dC.
            double temperatureFactor = 0.1;

            switch (super.unitString) {
                case "Celsius/10":
                case "Celsius":
                    //dC
                    return tempValue * temperatureFactor;
                case "Kelvin/100":
                case "Kelvin":
                    //dC
                    return (tempValue + 2731.5) * temperatureFactor;

                case "Fahrenheit":
                    //dC
                    return (tempValue * temperatureFactor * 1.8) + 32;

                case "m/10000":
                case "m/100":
                case "m/10":
                case "m":
                case "m*10":
                    if (this.channel.channelDoc().getUnit().equals(Unit.BAR)) {
                        return tempValue * 10;
                    }
                    return tempValue;
            }
        }

        return tempValue;
    }

    @Override
    public void setSendGet(int value) {
        sendGet = value;
    }

    @Override
    public int getSendGet() {
        return sendGet;
    }

}
