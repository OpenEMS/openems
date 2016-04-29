package de.fenecon.femscore.modbus.device.ess;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.femscore.modbus.protocol.BooleanBitElement;
import de.fenecon.femscore.modbus.protocol.ElementBuilder;
import de.fenecon.femscore.modbus.protocol.ElementRange;
import de.fenecon.femscore.modbus.protocol.ElementType;
import de.fenecon.femscore.modbus.protocol.ModbusProtocol;
import de.fenecon.femscore.modbus.protocol.SignedIntegerWordElement;
import de.fenecon.femscore.modbus.protocol.UnsignedShortWordElement;

public class Cess extends Ess {
	@SuppressWarnings("unused")
	private final static Logger log = LoggerFactory.getLogger(Cess.class);

	public Cess(String name, String modbusid, int unitid) {
		super(name, modbusid, unitid);
	}

	@Override
	public String toString() {
		return "CESS [name=" + name + ", unitid=" + unitid + "]";
	}

	@Override
	public Set<String> getInitElements() {
		return new HashSet<String>(Arrays.asList( //
				EssProtocol.SystemState.name()));
	}

	@Override
	public Set<String> getMainElements() {
		return new HashSet<String>(Arrays.asList( //
				EssProtocol.ActivePower.name(), //
				EssProtocol.ReactivePower.name(), //
				EssProtocol.ApparentPower.name(), //
				EssProtocol.AllowedCharge.name(), //
				EssProtocol.AllowedDischarge.name(), //
				EssProtocol.AllowedApparent.name(), //
				EssProtocol.BatteryStringSoc.name()));
	}

	@Override
	public Set<String> getWriteElements() {
		return new HashSet<String>(Arrays.asList( //
				EssProtocol.SetActivePower.name()));
	}

	@Override
	protected ModbusProtocol getProtocol() {
		ModbusProtocol protocol = new ModbusProtocol();
		protocol.addElementRange(new ElementRange(0x0210,
				new ElementBuilder(0x0210).name(EssProtocol.ActivePower).multiplier(100).signed(true).unit("W").build(),
				new ElementBuilder(0x0211).name(EssProtocol.ReactivePower).multiplier(100).signed(true).unit("VA")
						.build(),
				new ElementBuilder(0x0212).name(EssProtocol.ApparentPower).multiplier(100).unit("Var").build(),
				new ElementBuilder(0x0213).type(ElementType.PLACEHOLDER).intLength(0x230 - 0x213).build(),
				new ElementBuilder(0x0230).name(EssProtocol.AllowedCharge).multiplier(100).signed(true).unit("W")
						.build(),
				new ElementBuilder(0x0231).name(EssProtocol.AllowedDischarge).multiplier(100).unit("W").build(),
				new ElementBuilder(0x0232).name(EssProtocol.AllowedApparent).multiplier(100).unit("Var").build()));
		protocol.addElementRange(new ElementRange(0x0101,
				new ElementBuilder(0x0101).name(EssProtocol.SystemState) //
						.bit(new BooleanBitElement(1, EssProtocol.SystemStates.Stop.name())) //
						.bit(new BooleanBitElement(2, EssProtocol.SystemStates.PvCharging.name())) //
						.bit(new BooleanBitElement(3, EssProtocol.SystemStates.Standby.name())) //
						.bit(new BooleanBitElement(4, EssProtocol.SystemStates.Running.name())) //
						.bit(new BooleanBitElement(5, EssProtocol.SystemStates.Fault.name())) //
						.bit(new BooleanBitElement(6, EssProtocol.SystemStates.Debug.name())).build()));
		protocol.addElementRange(new ElementRange(0x0501, new ElementBuilder(0x0501).name(EssProtocol.SetActivePower)
				.multiplier(100).signed(true).unit("W").build()));
		protocol.addElementRange(new ElementRange(0x1402,
				new ElementBuilder(0x1402).name(EssProtocol.BatteryStringSoc).unit("%").build()));
		return protocol;
	}

	public UnsignedShortWordElement getSoc() {
		return (UnsignedShortWordElement) getElement(EssProtocol.BatteryStringSoc.name());
	}

	public SignedIntegerWordElement getActivePower() {
		return (SignedIntegerWordElement) getElement(EssProtocol.ActivePower.name());
	}

	public SignedIntegerWordElement getReactivePower() {
		return (SignedIntegerWordElement) getElement(EssProtocol.ReactivePower.name());
	}

	public UnsignedShortWordElement getApparentPower() {
		return (UnsignedShortWordElement) getElement(EssProtocol.ApparentPower.name());
	}

	public SignedIntegerWordElement getAllowedCharge() {
		return (SignedIntegerWordElement) getElement(EssProtocol.AllowedCharge.name());
	}

	public UnsignedShortWordElement getAllowedDischarge() {
		return (UnsignedShortWordElement) getElement(EssProtocol.AllowedDischarge.name());
	}

	public UnsignedShortWordElement getAllowedApparent() {
		return (UnsignedShortWordElement) getElement(EssProtocol.AllowedApparent.name());
	}

	public SignedIntegerWordElement getSetActivePower() {
		return (SignedIntegerWordElement) getElement(EssProtocol.SetActivePower.name());
	}
}
