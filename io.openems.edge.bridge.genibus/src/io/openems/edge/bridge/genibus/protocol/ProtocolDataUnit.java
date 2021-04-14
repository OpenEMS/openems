package io.openems.edge.bridge.genibus.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtocolDataUnit {
    List<ApplicationProgramDataUnit> applicationProgramDataUnitList = new ArrayList<ApplicationProgramDataUnit>();
    char requestFromSlave;
    int pduLength = 0;
    private final Logger log = LoggerFactory.getLogger(ProtocolDataUnit.class);

    public List<ApplicationProgramDataUnit> getApplicationProgramDataUnitList() {
        return applicationProgramDataUnitList;
    }

    public void setApplicationProgramDataUnitList(List<ApplicationProgramDataUnit> applicationProgramDataUnitList) {
        this.applicationProgramDataUnitList = applicationProgramDataUnitList;
    }

    public char getRequestFromSlave() {
        return requestFromSlave;
    }

    public void setRequestFromSlave(char requestFromSlave) {
        this.requestFromSlave = requestFromSlave;
    }

    public void putAPDU(ApplicationProgramDataUnit applicationProgramDataUnit) {
        applicationProgramDataUnitList.add(applicationProgramDataUnit);
        updatePduLength();
    }

    public void pullAPDU(ApplicationProgramDataUnit applicationProgramDataUnit) {
        applicationProgramDataUnitList.remove(applicationProgramDataUnit);
        updatePduLength();
    }

    public void updatePduLength() {
        int length = 0;
        for (ApplicationProgramDataUnit applicationProgramDataUnit : getApplicationProgramDataUnitList()) {
            // Add apdu length + 2 for apdu head
            length += applicationProgramDataUnit.getAPDULength();
        }
        this.pduLength = length;
    }

    public int getPduLength() {
        return this.pduLength;
    }

    public byte[] getBytes() {
        ByteArrayOutputStream byteList = new ByteArrayOutputStream();

        // APDU units
        for (ApplicationProgramDataUnit applicationProgramDataUnit : getApplicationProgramDataUnitList()) {
            try {
                byteList.write(applicationProgramDataUnit.getBytes());
            } catch (IOException e) {
                log.info(e.getMessage());
            }
        }
        // Request from slave (rfs), optional, ignored for now, used in multi master
        // networks

        return byteList.toByteArray();
    }

    /**
     * Create PDU from incoming bytes
     *
     * @param bytes
     * @return
     */
    public static ProtocolDataUnit parseBytes(byte[] bytes) {
        ProtocolDataUnit protocolDataUnit = new ProtocolDataUnit();
        try {
            // Parse APDU Blocks
            int apduStartIndex = 4;
            while (apduStartIndex < bytes.length - 3) {
                ApplicationProgramDataUnit applicationProgramDataUnit = new ApplicationProgramDataUnit();
                applicationProgramDataUnit.setHeadClass(bytes[apduStartIndex]);
                byte osasklength = bytes[apduStartIndex + 1]; // prepare block os/ack and length for splitting
                byte apduLength = (byte) (osasklength & 0x3F); // cast length
                byte osack = (byte) ((osasklength >> 6) & 0x03); // shift last two bytes to right corner, cast other
                // bytes
                // away
                applicationProgramDataUnit.setHeadOSACK(osack);

                ByteArrayOutputStream bytesStreamAPDURelevant = new ByteArrayOutputStream();
                bytesStreamAPDURelevant.write(bytes, apduStartIndex + 2, apduLength);
                applicationProgramDataUnit.setDataFields(bytesStreamAPDURelevant);
                protocolDataUnit.putAPDU(applicationProgramDataUnit);
                apduStartIndex += apduLength + 2;
            }
        } catch (Exception e) {
            System.out.println("Error in parseBytes of ProtocolDataUnit: " + e.getMessage());
        }

        return protocolDataUnit;
    }
}
