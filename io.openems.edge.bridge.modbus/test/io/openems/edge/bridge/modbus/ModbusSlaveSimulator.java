package io.openems.edge.bridge.modbus;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.procimg.ProcessImage;
import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;

public class ModbusSlaveSimulator {

	private static final int UNIT_ID = 1;
	private static final int PORT = 8502;
	private static final int CONCURRENT_CONNECTIONS = 5;

	/**
	 * Start the {@link ModbusSlaveSimulator}.
	 * 
	 * <p>
	 * See https://openems.github.io/openems.io/openems/latest/edge/implement.html
	 * 
	 * @param args the command line arguments
	 * @throws ModbusException on error
	 */
	public static void main(String[] args) throws ModbusException {
		// Start Modbus/TCP slave
		var image = createProcessImage();
		var slave = ModbusSlaveFactory.createTCPSlave(PORT, CONCURRENT_CONNECTIONS);
		slave.addProcessImage(1, image);
		slave.open();

		System.out.println("Modbus-Slave Simulator started. Port [" + PORT + "] Unit-ID [" + UNIT_ID + "]");

		// Stop Modbus/TCP slave on shutdown
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				ModbusSlaveFactory.close();
				System.out.println("Modbus-Slave Simulator stopped.");
			}
		});

		// Wait forever
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private static ProcessImage createProcessImage() {
		var image = new SimpleProcessImage(UNIT_ID);
		image.addRegister(1000, new SimpleRegister(500));
		image.addRegister(1001, new SimpleRegister(100));
		image.addRegister(2000, new SimpleRegister(123));
		return image;
	}

}
