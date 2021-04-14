package io.openems.edge.bridge.genibus.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ApplicationProgramDataUnit {
    short apduHead;
    byte apduHeadClass;
    byte apduHeadOSACK;
    boolean isOperationSpecifier;
    // 0...63 Byte
    ByteArrayOutputStream apduDataFields = new ByteArrayOutputStream();

    public int getHead() {
        return apduHead;
    }

    public void setHead(short apduHead) {
        this.apduHead = apduHead;
    }

    public byte[] getDataFields() {
        return apduDataFields.toByteArray();
    }

    public void setDataFields(ByteArrayOutputStream apduDataFields) {
        this.apduDataFields = apduDataFields;
    }

    public void putDataField(int apduDataFieldByte) {
        this.apduDataFields.write((byte) apduDataFieldByte);
    }

    public byte getHeadClass() {
        return apduHeadClass;
    }

    public void setHeadClass(int apduHeadClass) {
        this.apduHeadClass = (byte) apduHeadClass;
    }

    /**
     * Set Class = 2
     */
    public void setHeadClassMeasuredData() {
        this.apduHeadClass = 2;
    }

    /**
     * Set Class = 3
     */
    public void setHeadClassCommands() {
        this.apduHeadClass = 3;
    }

    /**
     * Set Class = 4
     */
    public void setHeadClassConfigurationParameters() {
        this.apduHeadClass = 4;
    }

    /**
     * Set Class = 5
     */
    public void setHeadClassReferenceValues() {
        this.apduHeadClass = 5;
    }

    /**
     * Set Class = 7
     */
    public void setHeadClassASCIIStrings() {
        this.apduHeadClass = 7;
    }

    public byte getHeadOSACKShifted() {
        return (byte) (apduHeadOSACK << 6);
    }

    /**
     * If isOperationSpecifier (OS) 00 / 0: GET, to read the value of Data Items 10
     * / 2: SET, to write the value of Data Items 11 / 3: INFO, to read the scaling
     * info of Data Items, an Info Data Structure will be returned Else (ACK) 00 /
     * 0: OK 01 / 1: Data Class unknown, reply APDU data field will be empty 10 / 2:
     * Data Item ID unknown, reply APDU data field contains first unknown ID 11 / 3:
     * Operation illegal or Data Class write buffer full, APDU data field will be
     * empty
     *
     * @param apduHeadOSACK
     */
    public void setHeadOSACK(int apduHeadOSACK) {
        this.apduHeadOSACK = (byte) apduHeadOSACK;
    }

    /**
     * OS=0
     */
    public void setOSGet() {
        this.apduHeadOSACK = 0;
    }

    /**
     * OS=2, SET
     */
    public void setOSSet() {
        this.apduHeadOSACK = 2;
    }

    /**
     * OS=3
     */
    public void setOSInfo() {
        this.apduHeadOSACK = 3;
    }

    public void setACKOK() {
        this.apduHeadOSACK = 0;
    }

    public void setACKDataClassUnknown() {
        this.apduHeadOSACK = (byte) 0x01;
    }

    public void setACKDataItemIDUnknown() {
        this.apduHeadOSACK = (byte) 0x10;
    }

    /**
     * Operation illegal or Data Class write buffer is full, APDU data field will be
     * empty
     */
    public void setACKIllegalOrBuffFull() {
        this.apduHeadOSACK = (byte) 0x11;
    }

    /**
     * Get num bytes in apdu without header bytes
     *
     * @return
     */
    public byte getLength() {
        return (byte) apduDataFields.size();
    }

    /**
     * Get apdu num bytes including header bytes
     *
     * @return
     */
    public byte getAPDULength() {
        return (byte) (getLength() + 2);
    }

    public boolean isOperationSpecifier() {
        return isOperationSpecifier;
    }

    public void setOperationSpecifier(boolean isOperationSpecifier) {
        this.isOperationSpecifier = isOperationSpecifier;
    }

    public byte getHeadOSACKLength() {
        return (byte) (getHeadOSACKShifted() | getLength());
    }

    public byte[] getBytes() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        // Head bytes
        bytes.write(getHeadClass()); // Set Class
        bytes.write(getHeadOSACKLength()); // Set OS/ACK, Length of APDU data field
        // Data bytes
        try {
            bytes.write(apduDataFields.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bytes.toByteArray();
    }

    public byte getHeadOSACKforRequest() {
        return this.apduHeadOSACK;
    }
}
