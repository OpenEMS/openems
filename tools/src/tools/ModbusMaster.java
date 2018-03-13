package tools;

import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.procimg.Register;

public class ModbusMaster {

	public static void main(String[] args) {
		ModbusTCPMaster master = new ModbusTCPMaster("10.0.10.230", 502, 10000, true);
		try {
			master.connect();
			Register[] registers = master.readMultipleRegisters(40000, 1);
			for (Register register : registers) {
				System.out.println(register);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			master.disconnect();
		}
	}

}
