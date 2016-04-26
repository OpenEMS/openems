package de.fenecon.femscore.modbus.device;

import de.fenecon.femscore.modbus.ModbusConnection;
import de.fenecon.femscore.modbus.protocol.Element;
import de.fenecon.femscore.modbus.protocol.ModbusProtocol;

public abstract class ModbusDevice {

	protected final Integer unitid;
	protected final String modbusId;
	protected final String name;
	protected final ModbusProtocol mainProtocol;

	public ModbusDevice(String name, String modbusid, int unitid) {
		this.unitid = unitid;
		this.name = name;
		this.modbusId = modbusid;
		this.mainProtocol = getMainProtocol();
	}

	public String getModbusid() {
		return modbusId;
	}

	public String getName() {
		return name;
	}

	public Element<?> getElement(String id) {
		Element<?> element = mainProtocol.getElement(id);
		if (element != null)
			return element;
		// TODO try the other protocols
		return null;
	}

	public void executeModbusMainQuery(ModbusConnection modbusConnection) throws Exception {
		this.mainProtocol.query(modbusConnection, this.unitid);
	}

	public void executeModbusNextSmallQuery(ModbusConnection modbusConnection) {
		// TODO
	};

	protected abstract ModbusProtocol getMainProtocol();

	@Override
	public String toString() {
		return "ModbusDevice [name=" + name + ", unitid=" + unitid + "]";
	}
}
