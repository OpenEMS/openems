package io.openems.edge.bridge.genibus.api.task;


import io.openems.edge.bridge.genibus.api.PumpDevice;

public abstract class AbstractPumpTask implements GenibusTask {

    double unitCalc;
    String unitString;
    UnitTable unitTable;
    private byte address;
    private int headerNumber;
    //Scale information Factor
    int sif;
    //Value interpretation
    //
    boolean vi;
    int range = 254;
    //Byte Order 0 = HighOrder 1 = Low Order
    //
    boolean bo;
    int unitIndex = -66;
    int scaleFactorHighOrder;
    int scaleFactorLowOrder;
    int zeroScaleFactor;
    int rangeScaleFactor;
    private boolean informationAvailable = false;
    boolean wasAdded;
    PumpDevice pumpDevice;
    final int dataByteSize;
    private int apduIdentifier;

    public AbstractPumpTask(int address, int headerNumber, String unitString, int dataByteSize) {
        this.address = (byte) address;
        this.headerNumber = headerNumber;
        this.dataByteSize = dataByteSize;
        switch (unitString) {
            case "Standard":
            default:
                this.unitTable = UnitTable.Standard_Unit_Table;
        }

    }

    @Override
    public byte getAddress() {
        return address;
    }

    @Override
    public int getHeader() {
        return headerNumber;
    }

    /**
     * gets the Information of One byte from the Genibus bridge (from handleResponse() method).
     *
     * @param vi  Value information if set range is 255 else 254. Comes from 5th bit
     * @param bo  Byte order information 0 is high order 1 is low order byte. 4th bit
     * @param sif Scale information Format 0 = not available 1= bitwise interpreted value.
     */

    @Override
    public void setOneByteInformation(int vi, int bo, int sif) {
        this.vi = vi != 0;
        this.bo = bo != 0;
        this.sif = sif;
        this.informationAvailable = true;
        if (this.vi) {
            range = 255;
        } else {
            range = 254;
        }
    }

    /**
     * get the Information written in 4 byte from the genibus bridge (handleResponse() method).
     *
     * @param vi                    see OneByteInformation.
     * @param bo                    see OneByteInformation.
     * @param sif                   see OneByteInformation.
     * @param unitIndex             index Number for the Unit of the task.
     * @param scaleFactorRangeOrLow range scale factor or low order byte.
     * @param scaleFactorZeroOrHigh either Zero scale factor or factor for high order byte
     *
     *                              <p> Unit calc depends on the unitString ---> unitCalc needed for default Channel Unit.
     *                              </p>
     */
    @Override
    public void setFourByteInformation(int vi, int bo, int sif, byte unitIndex, byte scaleFactorZeroOrHigh, byte scaleFactorRangeOrLow) {
        setOneByteInformation(vi, bo, sif);
        this.unitIndex = (unitIndex & 127);
        if (sif == 3) {
            this.scaleFactorHighOrder = Byte.toUnsignedInt(scaleFactorZeroOrHigh);
            this.scaleFactorLowOrder = Byte.toUnsignedInt(scaleFactorRangeOrLow);
        } else {
            this.zeroScaleFactor = Byte.toUnsignedInt(scaleFactorZeroOrHigh);
            this.rangeScaleFactor = Byte.toUnsignedInt(scaleFactorRangeOrLow);
        }
        if (this.unitIndex > 0) {
            this.unitString = this.unitTable.getInformationData().get(this.unitIndex);

            if (this.unitString != null) {
                switch (unitString) {

                    case "Celsius/10":
                    case "bar/10":
                    case "Ampere*0.1":
                    case "0.1*m³/h":
                    case "10%":
                        unitCalc = 0.1;
                        break;

                    case "Kelvin/100":
                    case "diff-Kelvin/100":
                    case "bar/100":
                    case "kPa":
                    case "0.01*Hz":
                    case "1%":
                        unitCalc = 0.01;
                        break;

                    case "bar/1000":
                    case "0.1%":
                        unitCalc = 0.001;
                        break;

                    case "0.01%":
                        unitCalc = 0.0001;
                        break;

                    case "ppm":
                        unitCalc = 0.000001;
                        break;

                    case "psi":
                        unitCalc = 0.06895; // convert to bar since channel unit is bar
                        break;

                    case "psi*10":
                        unitCalc = 0.6895; // convert to bar
                        break;

                    case "2*Hz":
                        unitCalc = 2.0;
                        break;

                    case "2.5*Hz":
                        unitCalc = 2.5;
                        break;

                    case "5*m³/h":
                        unitCalc = 5.0;
                        break;

                    case "Watt*10":
                    case "10*m³/h":
                        unitCalc = 10.0;
                        break;

                    case "Watt*100":
                        unitCalc = 100.0;
                        break;

                    case "kW":
                        unitCalc = 1000.0;
                        break;

                    case "kW*10":
                        unitCalc = 10000.0;
                        break;

                    case "Celsius":
                    case "Fahrenheit":  // <- conversion to °C in PumpReadTask.java
                    case "Kelvin":      // <- conversion to °C in PumpReadTask.java
                    case "diff-Kelvin":
                    case "Watt":
                    case "bar":
                    case "m³/h":
                    case "Hz":
                    default:
                        unitCalc = 1.0;
                }

                if ((unitIndex & 128) == 128) {
                    unitCalc = unitCalc * (-1);
                }
            }
        }

        // Extract pressure sensor interval from h (used to be h_diff (2, 23), but that does not work with pump MGE).
        if (headerNumber == 2 && address == 37) {
            if (unitString != null) {
                switch (unitString) {
                    case "m/10000":
                    case "m/100":
                    case "m/10":
                    case "m":
                    case "m*10":
                        pumpDevice.setPressureSensorMinBar(zeroScaleFactor * unitCalc / 10);
                        pumpDevice.setPressureSensorRangeBar(rangeScaleFactor * unitCalc / 10);
                        break;
                    default:
                        pumpDevice.setPressureSensorMinBar(zeroScaleFactor * unitCalc);
                        pumpDevice.setPressureSensorRangeBar(rangeScaleFactor * unitCalc);
                }
            }
        }
    }

    @Override
    public boolean informationDataAvailable() {
        return informationAvailable;
    }

    @Override
    public void resetInfo() { informationAvailable = false; }

    @Override
    public void setPumpDevice(PumpDevice pumpDevice) {
        this.pumpDevice = pumpDevice;
    }

    @Override
    public int getDataByteSize() {
        return dataByteSize;
    }

    @Override
    public void setApduIdentifier(int identifier) {
        this.apduIdentifier = identifier;
    }

    @Override
    public int getApduIdentifier() {
        return this.apduIdentifier;
    }

    @Override
    public String printInfo() {
        StringBuilder returnString = new StringBuilder();
        returnString.append("Task " + headerNumber + ", " + Byte.toUnsignedInt(address) + " - ");
        if (headerNumber == 7) {
            returnString.append("ASCII");
            return returnString.toString();
        }
        if (informationAvailable) {
            returnString.append("Unit: " + unitString + ", Format: " + dataByteSize*8 + " bit ");
            switch (sif) {
                case 1:
                    returnString.append("bit wise interpreted value");
                    break;
                case 2:
                    returnString.append("scaled value, min: " + zeroScaleFactor + ", range: " + rangeScaleFactor);

                    break;
                case 3:
                    int exponent = dataByteSize - 2;
                    if (exponent < 0) {
                        exponent = 0;
                    }
                    returnString.append("extended precision, min: " + (Math.pow(256, exponent) * (256 * scaleFactorHighOrder + scaleFactorLowOrder)));
                    break;
                case 0:
                default:
                    returnString.append("no scale info available");
                    break;
            }
        } else {
            returnString.append("no INFO yet.");
        }
        return returnString.toString();
    }
}
