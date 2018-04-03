package tools.modbus;

import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.procimg.InputRegister;

public class StreetscooterTest {

	public static void main(String[] args) {
		ModbusTCPMaster master = new ModbusTCPMaster("localhost", 502, 10000, true);
		try {
			master.connect();
//			Register[] registers = master.readMultipleRegisters(30011, 2);
			InputRegister[] registers = master.readInputRegisters(100, 6, 4);
			for (InputRegister register : registers) {
				System.out.println(register);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			master.disconnect();
		}
	}

}
