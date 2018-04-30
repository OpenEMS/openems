package tools;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.util.SerialParameters;

public class ModbusMaster {

	public static void main(String[] args) {
		// ModbusTCPMaster master = new ModbusTCPMaster("localhost", 502, 10000, true);
		// try {
		// master.connect();
		// Register[] registers = master.readMultipleRegisters(100, 0x1402, 1);
		// for (Register register : registers) {
		// System.out.println(register);
		// }
		// } catch (Exception e) {
		// e.printStackTrace();
		// } finally {
		// master.disconnect();
		// }
		System.out.println("Start.");
		ModbusSerialMaster master;
		try {
			// master = new ModbusTCPMaster(<address>); // Uses port 502 and a timeout of
			// 3000ms
			// master = new ModbusTCPMaster(<address>, <port>); // Uses a timeout of 3000ms
			SerialParameters params = new SerialParameters();
			params.setPortName("/dev/ttySC0");
			params.setBaudRate(38400);
			params.setDatabits(8);
			params.setStopbits(1);
			params.setParity("even");
			params.setEncoding(Modbus.SERIAL_ENCODING_RTU);
			params.setEcho(false);
			master = new ModbusSerialMaster(params);
			master.connect();

			System.out.println("Connected. Start read.");
			Register[] registers = master.readMultipleRegisters(5, 50520, 36);
			for (Register register : registers) {
				System.out.println("Result: " + register);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Finished");
	}

}
