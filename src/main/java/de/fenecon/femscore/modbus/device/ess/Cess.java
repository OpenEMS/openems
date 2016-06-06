package de.fenecon.femscore.modbus.device.ess;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.femscore.modbus.protocol.BitElement;
import de.fenecon.femscore.modbus.protocol.BitsElement;
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
						.bit(new BitElement(1, EssProtocol.SystemStates.Stop.name())) //
						.bit(new BitElement(2, EssProtocol.SystemStates.PvCharging.name())) //
						.bit(new BitElement(3, EssProtocol.SystemStates.Standby.name())) //
						.bit(new BitElement(4, EssProtocol.SystemStates.Running.name())) //
						.bit(new BitElement(5, EssProtocol.SystemStates.Fault.name())) //
						.bit(new BitElement(6, EssProtocol.SystemStates.Debug.name())).build()));
		protocol.addElementRange(new ElementRange(0x0501, new ElementBuilder(0x0501).name(EssProtocol.SetActivePower)
				.multiplier(100).signed(true).unit("W").build()));
		protocol.addElementRange(new ElementRange(0x1402,
				new ElementBuilder(0x1402).name(EssProtocol.BatteryStringSoc).unit("%").build()));
		protocol.addElementRange(new ElementRange(0xA600,
				new ElementBuilder(0xA600).name(EssProtocol.Pv1State) //
						.bit(new BitElement(1, EssProtocol.DcStates.Initial.name())) //
						.bit(new BitElement(2, EssProtocol.DcStates.Stop.name())) //
						.bit(new BitElement(3, EssProtocol.DcStates.Ready.name())) //
						.bit(new BitElement(4, EssProtocol.DcStates.Running.name())) //
						.bit(new BitElement(5, EssProtocol.DcStates.Fault.name())) //
						.bit(new BitElement(6, EssProtocol.DcStates.Debug.name())) //
						.bit(new BitElement(7, EssProtocol.DcStates.Locked.name())).build()));
		protocol.addElementRange(new ElementRange(0xA730,
				new ElementBuilder(0xA730).name(EssProtocol.Pv1OutputVoltage).multiplier(10).signed(true).unit("V")
						.build(), //
				new ElementBuilder(0xA731).name(EssProtocol.Pv1OutputCurrent).multiplier(10).signed(true).unit("A")
						.build(), //
				new ElementBuilder(0xA732).name(EssProtocol.Pv1OutputPower).multiplier(100).signed(true).unit("W")
						.build(), //
				new ElementBuilder(0xA733).name(EssProtocol.Pv1InputVoltage).multiplier(10).signed(true).unit("V")
						.build(), //
				new ElementBuilder(0xA734).name(EssProtocol.Pv1InputCurrent).multiplier(10).signed(true).unit("A")
						.build(), //
				new ElementBuilder(0xA735).name(EssProtocol.Pv1InputPower).multiplier(100).signed(true).unit("W")
						.build(), //
				new ElementBuilder(0xA736).name(EssProtocol.Pv1InputEnergy).multiplier(100).signed(true).unit("Wh")
						.build(), //
				new ElementBuilder(0xA737).name(EssProtocol.Pv1OutputEnergy).multiplier(100).signed(true).unit("Wh")
						.build()));
		protocol.addElementRange(new ElementRange(0xA900,
				new ElementBuilder(0xA900).name(EssProtocol.Pv2State) //
						.bit(new BitElement(1, EssProtocol.DcStates.Initial.name())) //
						.bit(new BitElement(2, EssProtocol.DcStates.Stop.name())) //
						.bit(new BitElement(3, EssProtocol.DcStates.Ready.name())) //
						.bit(new BitElement(4, EssProtocol.DcStates.Running.name())) //
						.bit(new BitElement(5, EssProtocol.DcStates.Fault.name())) //
						.bit(new BitElement(6, EssProtocol.DcStates.Debug.name())) //
						.bit(new BitElement(7, EssProtocol.DcStates.Locked.name())).build()));
		protocol.addElementRange(new ElementRange(0xAA30,
				new ElementBuilder(0xAA30).name(EssProtocol.Pv2OutputVoltage).multiplier(10).signed(true).unit("V")
						.build(), //
				new ElementBuilder(0xAA31).name(EssProtocol.Pv2OutputCurrent).multiplier(10).signed(true).unit("A")
						.build(), //
				new ElementBuilder(0xAA32).name(EssProtocol.Pv2OutputPower).multiplier(100).signed(true).unit("W")
						.build(), //
				new ElementBuilder(0xAA33).name(EssProtocol.Pv2InputVoltage).multiplier(10).signed(true).unit("V")
						.build(), //
				new ElementBuilder(0xAA34).name(EssProtocol.Pv2InputCurrent).multiplier(10).signed(true).unit("A")
						.build(), //
				new ElementBuilder(0xAA35).name(EssProtocol.Pv2InputPower).multiplier(100).signed(true).unit("W")
						.build(), //
				new ElementBuilder(0xAA36).name(EssProtocol.Pv2InputEnergy).multiplier(100).signed(true).unit("Wh")
						.build(), //
				new ElementBuilder(0xAA37).name(EssProtocol.Pv2OutputEnergy).multiplier(100).signed(true).unit("Wh")
						.build()));
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

	public SignedIntegerWordElement getPv1OutputPower() {
		return (SignedIntegerWordElement) getElement(EssProtocol.Pv1OutputPower.name());
	}

	public SignedIntegerWordElement getPv1InputPower() {
		return (SignedIntegerWordElement) getElement(EssProtocol.Pv1InputPower.name());
	}

	public SignedIntegerWordElement getPv2OutputPower() {
		return (SignedIntegerWordElement) getElement(EssProtocol.Pv2OutputPower.name());
	}

	public SignedIntegerWordElement getPv2InputPower() {
		return (SignedIntegerWordElement) getElement(EssProtocol.Pv2InputPower.name());
	}

	public Boolean getPv1StateInitial() {
		return ((BitsElement) getElement(EssProtocol.Pv1State.name())).getBit(EssProtocol.DcStates.Initial.name())
				.getValue();
	}

	public String getDcState(BitsElement s) {
		if (s.getBit(EssProtocol.DcStates.Initial.name()).getValue()) {
			return EssProtocol.DcStates.Initial.name();
		} else if (s.getBit(EssProtocol.DcStates.Stop.name()).getValue()) {
			return EssProtocol.DcStates.Stop.name();
		} else if (s.getBit(EssProtocol.DcStates.Ready.name()).getValue()) {
			return EssProtocol.DcStates.Ready.name();
		} else if (s.getBit(EssProtocol.DcStates.Running.name()).getValue()) {
			return EssProtocol.DcStates.Running.name();
		} else if (s.getBit(EssProtocol.DcStates.Fault.name()).getValue()) {
			return EssProtocol.DcStates.Fault.name();
		} else if (s.getBit(EssProtocol.DcStates.Debug.name()).getValue()) {
			return EssProtocol.DcStates.Debug.name();
		} else if (s.getBit(EssProtocol.DcStates.Locked.name()).getValue()) {
			return EssProtocol.DcStates.Locked.name();
		}
		return "NO STATUS";
	}

	public String getPv1State() {
		return getDcState((BitsElement) getElement(EssProtocol.Pv1State.name()));
	}

	public String getPv2State() {
		return getDcState((BitsElement) getElement(EssProtocol.Pv2State.name()));
	}
}
