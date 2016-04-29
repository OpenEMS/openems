package de.fenecon.femscore.modbus.device;

import java.util.Set;

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
	protected final ModbusProtocol protocol;

	private final ModbusProtocol initProtocol;
	private final ModbusProtocol mainProtocol;
	protected final ModbusProtocol remainingProtocol;

	public ModbusDevice(String name, String modbusid, int unitid) {
		this.unitid = unitid;
		this.name = name;
		this.modbusId = modbusid;

		// Initialize protocols
		this.protocol = getProtocol();
		Set<String> allElements = this.protocol.getElementIds();
		this.initProtocol = new ModbusProtocol(); // Init-Protocol
		Set<String> initElements = getInitElements();
		if (initElements != null) {
			for (String id : initElements) {
				initProtocol.addElementRange(protocol.getElement(id).getElementRange());
				allElements.remove(id);
			}
		}
		this.mainProtocol = new ModbusProtocol(); // Main-Protocol
		Set<String> mainElements = getMainElements();
		if (mainElements != null) {
			for (String id : mainElements) {
				mainProtocol.addElementRange(protocol.getElement(id).getElementRange());
				allElements.remove(id);
			}
		}
		this.remainingProtocol = new ModbusProtocol(); // Remaining-Protocol
		if (allElements != null) {
			for (String id : allElements) {
				remainingProtocol.addElementRange(protocol.getElement(id).getElementRange());
				// TODO: split remainingProtocol in small pieces
			}
		}
	}

	public String getModbusid() {
		return modbusId;
	}

	public String getName() {
		return name;
	}

	public Element<?> getElement(String id) {
		return protocol.getElement(id);
	}

	public abstract Set<String> getInitElements();

	public abstract Set<String> getMainElements();

	public void executeInitQuery(ModbusConnection modbusConnection) throws Exception {
		modbusConnection.updateProtocol(this.unitid, this.initProtocol);
	}

	public void executeMainQuery(ModbusConnection modbusConnection) throws Exception {
		modbusConnection.updateProtocol(this.unitid, this.mainProtocol);
	}

	public void executeRemainingQuery(ModbusConnection modbusConnection) throws Exception {
		modbusConnection.updateProtocol(this.unitid, this.remainingProtocol);
	};

	protected abstract ModbusProtocol getProtocol();

	@Override
	public String toString() {
		return "ModbusDevice [name=" + name + ", unitid=" + unitid + "]";
	}
}
