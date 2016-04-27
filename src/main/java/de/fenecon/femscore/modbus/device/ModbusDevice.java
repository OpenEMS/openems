package de.fenecon.femscore.modbus.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.femscore.modbus.ModbusConnection;
import de.fenecon.femscore.modbus.protocol.Element;
import de.fenecon.femscore.modbus.protocol.ModbusProtocol;

public abstract class ModbusDevice {
	@SuppressWarnings("unused")
	private final static Logger log = LoggerFactory.getLogger(ModbusDevice.class);

	protected final Integer unitid;
	protected final String modbusId;
	protected final String name;
	protected final ModbusProtocol initProtocol;
	protected final ModbusProtocol mainProtocol;

	public ModbusDevice(String name, String modbusid, int unitid) {
		this.unitid = unitid;
		this.name = name;
		this.modbusId = modbusid;
		this.initProtocol = getInitProtocol();
		this.mainProtocol = getMainProtocol();
	}

	public String getModbusid() {
		return modbusId;
	}

	public String getName() {
		return name;
	}

	public Element<?> getElement(String id) {
		if (mainProtocol != null) {
			Element<?> element = mainProtocol.getElement(id);
			if (element != null)
				return element;
		}
		// TODO try the other protocols
		if (initProtocol != null) {
			Element<?> element = initProtocol.getElement(id);
			if (element != null)
				return element;
		}
		return null;
	}

	public void executeModbusInitQuery(ModbusConnection modbusConnection) throws Exception {
		log.info("executeModbusInitQuery {}", modbusConnection);
		if (this.initProtocol != null) {
			this.initProtocol.query(modbusConnection, this.unitid);
		}
	}

	public void executeModbusMainQuery(ModbusConnection modbusConnection) throws Exception {
		if (this.mainProtocol != null) {
			this.mainProtocol.query(modbusConnection, this.unitid);
		}
	}

	public void executeModbusNextSmallQuery(ModbusConnection modbusConnection) {
		// TODO
	};

	protected abstract ModbusProtocol getMainProtocol();

	/**
	 * Defining the "InitProtocol". This protocol is queried once at the
	 * beginning. It can be used to make sure, that the ModbusDevice is in a
	 * running state.
	 * 
	 * @return the InitProtocol, or null for no initialization protocol
	 */
	protected abstract ModbusProtocol getInitProtocol();

	@Override
	public String toString() {
		return "ModbusDevice [name=" + name + ", unitid=" + unitid + "]";
	}
}
