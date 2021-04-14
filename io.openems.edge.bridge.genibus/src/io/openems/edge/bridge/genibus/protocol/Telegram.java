package io.openems.edge.bridge.genibus.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.openems.edge.bridge.genibus.api.PumpDevice;
import io.openems.edge.bridge.genibus.api.task.GenibusTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.snksoft.crc.CRC;

public class Telegram {
    byte startDelimiter;
    byte length;
    byte destinationAddress;
    byte sourceAddress;
    ProtocolDataUnit protocolDataUnit = new ProtocolDataUnit();
    short crcHighOrder;
    short crcLowOrder;
    Map<Integer, ArrayList<GenibusTask>> telegramTaskList = new HashMap<>();
    private PumpDevice pumpDevice;

    // This variable is used to store the estimate, as well as the actual. What it is depends on the timing.
    private int answerTelegramLength = 0;

    private final Logger log = LoggerFactory.getLogger(Telegram.class);

    public void setAnswerTelegramLength(int answerTelegramLength) {
        this.answerTelegramLength = answerTelegramLength;
    }

    public int getAnswerTelegramLength() {
        return answerTelegramLength;
    }

    public void setPumpDevice(PumpDevice pumpDevice) {
        this.pumpDevice = pumpDevice;
    }

    public PumpDevice getPumpDevice() { return pumpDevice; }

    public byte getStartDelimiter() {
        return startDelimiter;
    }

    /**
     * Set byte
     *
     * @param startDelimiter
     */
    public void setStartDelimiter(byte startDelimiter) {
        this.startDelimiter = startDelimiter;
    }

    /**
     * Set StartDelimiter to 0x27
     */
    public void setStartDelimiterDataRequest() {
        setStartDelimiter((byte) 0x27);
    }

    /**
     * Set StartDelimiter to 0x26
     */
    public void setStartDelimiterDataMessage() {
        setStartDelimiter((byte) 0x26);
    }

    /**
     * Set StartDelimiter to 0x24
     */
    public void setStartDelimiterDataReply() {
        setStartDelimiter((byte) 0x24);
    }

    public byte getLength() {
        return updateLength();
    }

    /**
     * @param length
     */
    public byte setLength(int length) {
        this.length = (byte) length;
        return this.length;
    }

    /**
     * Get pdu length, add 2 for dest and src address
     */
    public byte updateLength() {
        return setLength(protocolDataUnit.getPduLength() + 2);
    }

    public byte getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(int destinationAddress) {
        this.destinationAddress = (byte) destinationAddress;
    }

    public byte getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(int sourceAddress) {
        this.sourceAddress = (byte) sourceAddress;
    }

    public ProtocolDataUnit getProtocolDataUnit() {
        return protocolDataUnit;
    }

    public void setProtocolDataUnit(ProtocolDataUnit protocolDataUnit) {
        this.protocolDataUnit = protocolDataUnit;
    }

    public void setTelegramTaskList(Map<Integer, ArrayList<GenibusTask>> telegramTaskList) {
        this.telegramTaskList = telegramTaskList;
    }

    public Map<Integer, ArrayList<GenibusTask>> getTelegramTaskList() {
        return telegramTaskList;
    }

    public short getCrcHighOrder() {
        return crcHighOrder;
    }

    public void setCrcHighOrder(short crcHighOrder) {
        this.crcHighOrder = crcHighOrder;
    }

    public short getCrcLowOrder() {
        return crcLowOrder;
    }

    public void setCrcLowOrder(short crcLowOrder) {
        this.crcLowOrder = crcLowOrder;
    }

    public byte[] getBytesForCRC() {
        ByteArrayOutputStream byteList = new ByteArrayOutputStream();

        // Add length
        byteList.write(getLength());
        // Add destination address
        byteList.write(getDestinationAddress());
        byteList.write(getSourceAddress());
        // Add pdu
        try {
            byteList.write(getProtocolDataUnit().getBytes());
        } catch (IOException e) {
            log.info(e.getMessage());
        }

        return byteList.toByteArray();
    }

    public byte[] getBytes() {

        byte[] byteListForCRC = getBytesForCRC();

        ByteArrayOutputStream byteList = new ByteArrayOutputStream();
        // Add start delimiter (sd)
        byteList.write(getStartDelimiter());
        try {
            byteList.write(byteListForCRC);
        } catch (IOException e) {
            log.info(e.getMessage());
        }
        // Calc crchigh and crclow, create complete telegram
        try {
            byteList.write(getCRC(byteListForCRC));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return byteList.toByteArray();
    }

    /**
     * Create new telegram from bytes assuming correct crc
     *
     * @param bytes
     * @return
     */
    public static Telegram parseEventStream(byte[] bytes) {
        try {
            Telegram telegram = new Telegram();
            telegram.setStartDelimiter(bytes[0]);    //Start Delimiter (SD)
            telegram.setLength(bytes[1]);            //Length (LE)
            telegram.setDestinationAddress(bytes[2]);//Destination Address (DA)
            telegram.setSourceAddress(bytes[3]);    //Source Address (SA)
            telegram.setProtocolDataUnit(ProtocolDataUnit.parseBytes(bytes));//Protocol Data Unit (PDU)
            return telegram;
        } catch (Exception e) {
            System.out.println("Error parsing bytes for telegram: " + e.getMessage());
        }
        return null;
    }

    public static byte[] getCRC(byte[] bytes) {
        long crc = 0;

        try {
            crc = CRC.calculateCRC(CRC.Parameters.CCITT, bytes) ^ 0xFFFF;
        } catch (Exception e) {

        }

        byte ret[] = new byte[2];
        ret[1] = (byte) (crc & 0xff);
        ret[0] = (byte) ((crc >> 8) & 0xff);

        return ret;
    }
}
