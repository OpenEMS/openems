package io.openems.edge.bridge.genibus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import io.openems.edge.bridge.genibus.api.PumpDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/*
import org.openmuc.jrxtx.DataBits;
import org.openmuc.jrxtx.Parity;
import org.openmuc.jrxtx.SerialPort;
import org.openmuc.jrxtx.SerialPortBuilder;
import org.openmuc.jrxtx.StopBits;
*/
import com.fazecast.jSerialComm.SerialPort;

import io.openems.edge.bridge.genibus.protocol.Telegram;


public class Handler {

    private SerialPort serialPort;
    protected String portName;
    LocalDateTime errorTimestamp;
    LocalDateTime errorTimestamp2;

    OutputStream os;
    InputStream is;

    private final Logger log = LoggerFactory.getLogger(Handler.class);
    protected final GenibusImpl parent;

    public Handler(GenibusImpl parent) {
        errorTimestamp = LocalDateTime.now();
        this.parent = parent;
    }

    // Trying fazecast library for serial port. jrxrx library could not find port sc1 on leaflet.
    public boolean start(String portName) {
        this.portName = portName;
        SerialPort[] serialPortsOfSystem = SerialPort.getCommPorts();
        boolean serialPortFound = false;
        if (serialPortsOfSystem.length == 0) {
            // So that error message is only sent once.
            if (parent.connectionOk || ChronoUnit.SECONDS.between(errorTimestamp, LocalDateTime.now()) >= 5) {
                errorTimestamp = LocalDateTime.now();
                this.parent.logError(this.log, "Couldn't start serial connection. No serial ports found or nothing plugged in.");
            }
            return false;
        }
        StringBuilder portList = new StringBuilder();
        for (SerialPort tmpSerialPort : serialPortsOfSystem) {
            String systemPortName = "/dev/" + tmpSerialPort.getSystemPortName();
            portList.append(tmpSerialPort).append(", ");
            if (systemPortName.equals(portName)) {
                serialPort = tmpSerialPort;
                serialPortFound = true;
            }
        }
        // Delete ", " at end
        if (portList.length() > 2) {
            portList.delete(portList.length() - 2, portList.length());
        }

        if (serialPortFound) {
            this.parent.logInfo(this.log, "--Starting serial connection--");
            this.parent.logInfo(this.log, "Ports found: " + portList);
            serialPort.setNumDataBits(8);
            serialPort.setParity(SerialPort.NO_PARITY);
            serialPort.setNumStopBits(1);
            serialPort.setBaudRate(9600);
            serialPort.openPort();

            is = serialPort.getInputStream();
            os = serialPort.getOutputStream();

            this.parent.logInfo(this.log, "Connection opened on port " + portName);
            return true;
        } else {
            if (parent.connectionOk || ChronoUnit.SECONDS.between(errorTimestamp, LocalDateTime.now()) >= 5) {
                errorTimestamp = LocalDateTime.now();
                this.parent.logError(this.log, "Configuration error: The specified serial port " + portName
                        + " does not match any of the available ports or nothing is plugged in. " +
                        "Please check configuration and/or make sure the connector is plugged in.");
                this.parent.logError(this.log, "Ports found: " + portList);
            }
            return false;
        }
    }

    /* // Old serial port handling, uses jrxtx libraries
    public boolean start(String portName) {
        this.portName = portName;
        String[] serialPortsOfSystem =  SerialPortBuilder.getSerialPortNames();
        boolean portFound = false;
        if (serialPortsOfSystem.length == 0) {
            // So that error message is only sent once.
            if (parent.connectionOk || ChronoUnit.SECONDS.between(errorTimestamp, LocalDateTime.now()) >= 5) {
                errorTimestamp = LocalDateTime.now();
                this.parent.logError(this.log, "Couldn't start serial connection. No serial ports found or nothing plugged in.");
            }
            return false;
        }
        StringBuilder portList = new StringBuilder();
        for (String entry : serialPortsOfSystem) {
            portList.append(entry).append(", ");
            if (entry.contains(this.portName)) {
                portFound = true;
            }
        }
        // Delete ", " at end
        if (portList.length() > 2) {
            portList.delete(portList.length() - 2, portList.length());
        }

        if (portFound) {
            this.parent.logInfo(this.log, "--Starting serial connection--");
            this.parent.logInfo(this.log, "Ports found: " + portList);
            try {
                serialPort = SerialPortBuilder.newBuilder(portName).setBaudRate(9600)
                        .setDataBits(DataBits.DATABITS_8).setParity(Parity.NONE).setStopBits(StopBits.STOPBITS_1).build();
                is = serialPort.getInputStream();
                os = serialPort.getOutputStream();
            } catch (IOException e) {
                // So that error message is only sent once.
                if (parent.connectionOk || ChronoUnit.SECONDS.between(errorTimestamp, LocalDateTime.now()) >= 5) {
                    errorTimestamp = LocalDateTime.now();
                    this.parent.logError(this.log, "Failed to open connection on port " + portName);
                }
                e.printStackTrace();
                return false;
            }
            this.parent.logInfo(this.log, "Connection opened on port " + portName);
            return true;
        } else {
            if (parent.connectionOk || ChronoUnit.SECONDS.between(errorTimestamp, LocalDateTime.now()) >= 5) {
                errorTimestamp = LocalDateTime.now();
                this.parent.logError(this.log, "Configuration error: The specified serial port " + portName
                        + " does not match any of the available ports or nothing is plugged in. " +
                        "Please check configuration and/or make sure the connector is plugged in.");
                this.parent.logError(this.log, "Ports found: " + portList);
            }
            return false;
        }

    }
    */


    public boolean checkStatus() {
        if (os != null) {
            // os in not null when a connection was established at some point by the start() method.
            try {
                // Test the connection by trying to write something to the output stream os. Writes a single 0, should
                // not interfere with anything.
                os.write(0);
            } catch (IOException e) {

                // So that error message is only sent once.
                if (parent.connectionOk || ChronoUnit.SECONDS.between(errorTimestamp, LocalDateTime.now()) >= 5) {
                    errorTimestamp2 = LocalDateTime.now();
                    this.parent.logError(this.log, "Serial connection lost on port " + portName + ". Attempting to reconnect...");
                }

                if (serialPort != null) {
                    serialPort.closePort();
                    /*
                    try {
                        serialPort.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    */
                }
                return false;

            }
        } else {
            // If os is null, there has not been a connection yet.
            return false;
        }
        return true;
    }


    public boolean packageOK(byte[] bytesCurrentPackage, boolean verbose) {
        //Look for Start Delimiter (SD)
        boolean sdOK = false;
        if (bytesCurrentPackage.length >= 1) {
            switch (bytesCurrentPackage[0]) {
                case 0x27:
                case 0x26:
                case 0x24:
                    sdOK = true;
                    break;
                default:
                    sdOK = false;
            }
        }
        if (!sdOK) { //wrong package start, reset
            if (verbose) {
                this.parent.logWarn(this.log, "SD not OK");
            }
            return false;
        }
        //Look for Length (LE), Check package length match
        boolean lengthOK = false;
        if (bytesCurrentPackage.length >= 2 && bytesCurrentPackage[1] == bytesCurrentPackage.length - 4) {
            lengthOK = true;
        }
        if (!lengthOK) { //collect more data
            if (verbose) {
                this.parent.logWarn(this.log, "Length not OK");
            }
            return false;
        }
        //Check crc from relevant message part

        ByteArrayOutputStream bytesCRCRelevant = new ByteArrayOutputStream();
        bytesCRCRelevant.write(bytesCurrentPackage, 1, bytesCurrentPackage.length - 3);
        byte[] crc = Telegram.getCRC(bytesCRCRelevant.toByteArray());
        int length = bytesCurrentPackage.length;

        if (bytesCurrentPackage[length - 2] != crc[0] || bytesCurrentPackage[length - 1] != crc[1]) {
            this.parent.logWarn(this.log, "CRC compare not OK");
            return false; //cancel operation
        }
        return true;
    }

    public void stop() {
        if (os != null) {
            try {
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        os = null;
        is = null;
        if (serialPort == null) {
            //this.parent.logError(this.log, "serialPort is null. This should never happen."); // Happens when no valid serial port was selected when starting the module.
            return;
        }

        if (serialPort.isOpen() == false) {
            return;
        }
        serialPort.closePort();

        /*
        if (serialPort.isClosed()) {
            //this.parent.logInfo(this.log, "serialPort is already closed.");
            return;
        }

        try {
            serialPort.close();
        } catch (IOException e) {
            this.parent.logError(this.log, "Error closing port: " + e.getMessage());
        }
        */
    }


    /**
     * Writes the content
     * @param timeout
     * @param telegram
     * @param debug
     * @return
     */
    public Telegram writeTelegram(long timeout, Telegram telegram, boolean debug) {

        // Make sure the input is empty before sending a new telegram
        try {
            if (is.available() > 0) {
                this.parent.logWarn(this.log, "Input buffer should be empty, but it is not. Trying to clear it.");
                long startTime = System.currentTimeMillis();
                int numRead;
                byte[] readBuffer = new byte[1024];
                while (is.available() > 0 || (System.currentTimeMillis() - startTime) < 60) {
                    numRead = is.available();
                    if (numRead > 1024) {
                        numRead = 1024; // readBuffer is size 1024.
                    }
                    is.read(readBuffer, 0, numRead);
                }
                if (is.available() > 0) {
                    this.parent.logError(this.log, "Can't send telegram, something is flooding the input buffer!");
                    return null;
                }
            }
        } catch (Exception e) {
            this.parent.logError(this.log, "Error while receiving data: " + e.getMessage());
            e.printStackTrace();
        }
        /*
         * Send data and save return handling telegram
         */
        try {
            // Send Reqeust

            byte[] bytes = telegram.getBytes();
            os.write(bytes);
            if (debug) {
                // Debug output data hex values
                //this.parent.logInfo(this.log, "Bytes send: " + bytesToHex(bytes));
                this.parent.logInfo(this.log, "Bytes send: " + bytesToInt(bytes));
            }

            // Save return function/Task
            return handleResponse(timeout, debug, telegram.getPumpDevice());

        } catch (Exception e) {
            this.parent.logError(this.log, "Error while sending data: " + e.getMessage());
        }
        return null;
    }

    private Telegram handleResponse(long timeout, boolean debug, PumpDevice pumpDevice) {
        try {
            long startTime = System.currentTimeMillis();
            //timeout = 500;

            // This "while" only tests for timeout as long as is.available() <= 0, since there is a break at the end.
            // Timeout time is the estimated time it should take for a response to arrive. One would think that this is
            // the time it takes to send the request telegram + some process time. However, testing revealed that the
            // response is buffered and there is a delay between data arriving and "is.available() != 0".
            // From the tests it was found that a practical timeout time is the estimated telegram execution time
            // (request + answer). Add 60 ms to that, as that is the suggested GENIbus Master timeout duration.
            while ((System.currentTimeMillis() - startTime) < timeout + 60) {
                if (is.available() <= 0) {
                    continue;
                }
                if (debug) {
                    this.parent.logInfo(this.log, "Telegram answer time: " + (System.currentTimeMillis() - startTime)
                            + ", timeout: " + (timeout + 60));
                }
                int numRead;
                byte[] readBuffer = new byte[1024];
                List<Byte> completeInput = new ArrayList<>();
                boolean transferOk = false;
                // Reset timer. This timeout exits the loop in case the telegram is corrupted and transferOk will not
                // become true. The timeout length should be greater than the expected telegram transfer time. Tests have
                // shown that the received data is buffered, greatly reducing the time the code has to wait. For small
                // telegrams the "answer transmit clock" will show 0 ms.
                startTime = System.currentTimeMillis();
                while (transferOk == false && (System.currentTimeMillis() - startTime) < timeout + 60) {
                    if (is.available() <= 0) {
                        continue;
                    }
                    numRead = is.available();
                    if (numRead > 1024) {
                        numRead = 1024; // readBuffer is size 1024.
                    }
                    is.read(readBuffer, 0, numRead);
                    for (int counter1 = 0 ; counter1 < numRead; counter1++) {
                        completeInput.add(readBuffer[counter1]);
                    }
                    byte[] receivedDataTemp = new byte[completeInput.size()];
                    int counter2 = 0;
                    for (byte entry:completeInput) {
                        receivedDataTemp[counter2] = entry;
                        counter2++;
                    }
                    transferOk = packageOK(receivedDataTemp, false);
                    if (debug && transferOk) {
                        this.parent.logInfo(this.log, "Telegram answer transmit time: " + (System.currentTimeMillis() - startTime)
                                + ", timeout: " + (timeout + 60));
                    }
                }
                byte[] receivedData = new byte[completeInput.size()];
                int counter2 = 0;
                for (byte entry:completeInput) {
                    receivedData[counter2] = entry;
                    counter2++;
                }

                if (debug) {
                    // Debug return data hex or int values
                    //this.parent.logInfo(this.log, "Data received: " + bytesToHex(receivedData));
                    this.parent.logInfo(this.log, "Data received: " + bytesToInt(receivedData));
                }

                if (packageOK(receivedData, true)) {
                    // GENIbus requirement: wait 3 ms after reception of a telegram before sending the next one.
                    Thread.sleep(3);
                    if (debug) {
                        this.parent.logInfo(this.log, "CRC Check ok.");
                    }
                    // if all done, return the response telegram.
                    return Telegram.parseEventStream(receivedData);
                }
                break;
            }

        } catch (Exception e) {
            this.parent.logError(this.log, "Error while receiving data: " + e.getMessage());
            e.printStackTrace();
        }
        this.parent.logWarn(this.log, "Telegram response timeout");
        int timeoutCounter = pumpDevice.getTimeoutCounter();
        if (timeoutCounter < 3) {
            timeoutCounter++;
        }
        pumpDevice.setTimeoutCounter(timeoutCounter);
        return null;
    }

    private static String bytesToHex(byte[] hashInBytes) {

        StringBuilder sb = new StringBuilder();
        for (byte b : hashInBytes) {
            sb.append(String.format("0x%02x ", b));
        }
        return sb.toString();

    }

    private static String bytesToInt(byte[] hashInBytes) {

        StringBuilder sb = new StringBuilder();
        for (byte b : hashInBytes) {
            int convert = Byte.toUnsignedInt(b);
            sb.append(String.format("%d ", convert));
        }
        return sb.toString();

    }

}
