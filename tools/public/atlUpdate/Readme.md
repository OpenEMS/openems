## **@Pooran**

*I used BatteryUpdateV2.5.1.x  and  feneconR0.0.bin*

*I was not able to start the jar file. Maybe i missed something or the jar file is an old one.*

## Log
```bash
PortName : /dev/busUSB1| BaudRate : 19200| Size of the UpdateFile : 513227 kb | forceUpdate : false
[2022-10-11 15:57:56:287] -->  In Fc 40 state
[2022-10-11 15:57:56:343] -->  Send File Size!
[2022-10-11 15:57:56:345] -->  WriteData FC40: 00 00 00 00 00 07 01 40 04 00 07 D4 CB
log4j:WARN No appenders could be found for logger (com.ghgande.j2mod.modbus.io.ModbusRTUTransport).
log4j:WARN Please initialize the log4j system properly.
log4j:WARN See http://logging.apache.org/log4j/1.2/faq.html#noconfig for more info.
com.ghgande.j2mod.modbus.ModbusIOException: I/O exception - failed to read response for request [01 40 04 00 07 D4 CB] - Cannot read from serial port
        at com.ghgande.j2mod.modbus.io.ModbusRTUTransport.readResponseIn(ModbusRTUTransport.java:495)
        at com.ghgande.j2mod.modbus.io.ModbusSerialTransport.readResponse(ModbusSerialTransport.java:151)
        at com.ghgande.j2mod.modbus.io.ModbusSerialTransaction.execute(ModbusSerialTransaction.java:142)
        at com.automatic.batteryupdate.App.executeProcess(App.java:484)
        at com.automatic.batteryupdate.App.getFc40ResponseRTU(App.java:366)
        at com.automatic.batteryupdate.App.main(App.java:181)
java.lang.NullPointerException: Cannot invoke "com.ghgande.j2mod.modbus.msg.FC40WriteTaskResponse.getFunctionCode()" because "fc40WriteTaskResponse" is null
        at com.automatic.batteryupdate.App.main(App.java:185)
[2022-10-11 15:58:15:727] -->  Closing all the transports
[2022-10-11 15:58:15:731] -->  Killing the process
```