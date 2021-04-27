package io.openems.edge.bridge.genibus.api.task;


import io.openems.edge.bridge.genibus.api.PumpDevice;
import io.openems.edge.common.taskmanager.ManagedTask;

public interface GenibusTask extends ManagedTask {


    // This method is used to see if this task has a SET to send, as well as to get the value of a SET. This is tied to
    // the contents of the associated write channel. If "nextWrite" of the channel is empty/false, the method returns
    // "nothing to write" (-1 or -256).
    // The point of "clearChannel" is then to tell the task that it has been added to a telegram (with SET) and the
    // command is executed. The channel is cleared and further getRequests will return "nothing to write", until
    // something is written in the channel again.
    default int getRequest(int byteCounter, boolean clearChannel) {
        return -1;
    };

    void setResponse(byte data);

    byte getAddress();

    int getHeader();

    void setOneByteInformation(int vi, int bo, int sif);

    void setFourByteInformation(int vi, int bo, int sif, byte unitIndex, byte scaleFactorZeroOrHigh, byte scaleFactorRangeOrLow);

    boolean informationDataAvailable();

    void resetInfo();

    void setPumpDevice(PumpDevice pumpDevice);

    int getDataByteSize();

    void setApduIdentifier(int identifier);

    int getApduIdentifier();

    String printInfo();

}
