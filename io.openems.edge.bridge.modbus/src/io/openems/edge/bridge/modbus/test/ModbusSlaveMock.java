package io.openems.edge.bridge.modbus.test;

import java.util.function.Supplier;

import com.ghgande.j2mod.modbus.io.ModbusRTUTransport;
import com.ghgande.j2mod.modbus.net.AbstractModbusListener;
import com.ghgande.j2mod.modbus.procimg.DefaultProcessImageFactory;
import com.ghgande.j2mod.modbus.slave.ModbusSlave;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;
import com.ghgande.j2mod.modbus.util.SerialParameters;

public class ModbusSlaveMock {
	private ModbusSlaveMock() {
	}

	/**
	 * Creates a ModbusSlave with a SerialConnectionMock and a DummySerialListener
	 * and registers it in j2mod.
	 *
	 * @param connection            Serial binary connection that should be attached
	 *                              to the slave
	 * @param serialParams          Serial paramters of this slave
	 * @param unitId                Modbus Unit ID
	 * @param serialListenerFactory Factory method that is creating the modbus
	 *                              listener
	 * @param transport             Transport to use (most of the time this is
	 *                              standard {@link ModbusRTUTransport})
	 * @return New slave instance
	 * @throws Exception if an error occurrs
	 */
	public static ModbusSlave register(SerialConnectionMock connection, SerialParameters serialParams, int unitId,
			Supplier<AbstractModbusListener> serialListenerFactory, ModbusRTUTransport transport) throws Exception {
		connection.open();

		transport.setCommPort(connection);
		connection.setModbusTransport(transport);

		var processImage = new DefaultProcessImageFactory().createProcessImageImplementation();

		var slave = ModbusSlaveFactory.createSerialSlave(serialParams, serialListenerFactory);
		slave.addProcessImage(unitId, processImage);

		slave.open();
		return slave;
	}
}
