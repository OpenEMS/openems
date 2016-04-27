package de.fenecon.femscore.modbus.device.ess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.femscore.modbus.device.ModbusDeviceWritable;
import de.fenecon.femscore.modbus.protocol.BooleanBitElement;
import de.fenecon.femscore.modbus.protocol.ElementBuilder;
import de.fenecon.femscore.modbus.protocol.ElementRange;
import de.fenecon.femscore.modbus.protocol.ElementType;
import de.fenecon.femscore.modbus.protocol.ModbusProtocol;

public class Cess extends Ess implements ModbusDeviceWritable {
	@SuppressWarnings("unused")
	private final static Logger log = LoggerFactory.getLogger(Cess.class);

	public Cess(String name, String modbusid, int unitid) {
		super(name, modbusid, unitid);
	}

	@Override
	public void executeModbusWrite() {
		// TODO Auto-generated method stub
	}

	@Override
	protected ModbusProtocol getMainProtocol() {
		ModbusProtocol protocol = new ModbusProtocol();
		protocol.addElementRange(new ElementRange(0x0210,
				new ElementBuilder(0x0210).name(EssProtocol.ActivePower).multiplier(100).signed(true).unit("W").build(),
				new ElementBuilder(0x0211).name(EssProtocol.ReactivePower).multiplier(100).signed(true).unit("VA")
						.build(),
				new ElementBuilder(0x0212).name(EssProtocol.ApparentPower).multiplier(100).unit("Var").build(),
				new ElementBuilder(0x0213).type(ElementType.PLACEHOLDER).length(0x230 - 0x213).build(),
				new ElementBuilder(0x0230).name(EssProtocol.AllowedCharge).multiplier(100).signed(true).unit("W")
						.build(),
				new ElementBuilder(0x0231).name(EssProtocol.AllowedDischarge).multiplier(100).unit("W").build(),
				new ElementBuilder(0x0232).name(EssProtocol.AllowedApparent).multiplier(100).unit("Var").build()));
		return protocol;
	}

	@Override
	protected ModbusProtocol getInitProtocol() {
		ModbusProtocol protocol = new ModbusProtocol();
		protocol.addElementRange(new ElementRange(0x0101,
				new ElementBuilder(0x0101).name(EssProtocol.SystemState) //
						.bit(new BooleanBitElement(1, EssProtocol.SystemStates.Stop.name())) //
						.bit(new BooleanBitElement(2, EssProtocol.SystemStates.PvCharging.name())) //
						.bit(new BooleanBitElement(3, EssProtocol.SystemStates.Standby.name())) //
						.bit(new BooleanBitElement(4, EssProtocol.SystemStates.Running.name())) //
						.bit(new BooleanBitElement(5, EssProtocol.SystemStates.Fault.name())) //
						.bit(new BooleanBitElement(6, EssProtocol.SystemStates.Debug.name())).build()));
		return protocol;
	}

	@Override
	public String toString() {
		return "CESS [name=" + name + ", unitid=" + unitid + "]";
	}

}
