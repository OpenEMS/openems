package io.openems.impl.device.commercial;

import java.math.BigInteger;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ConfigChannelBuilder;
import io.openems.api.channel.WriteableChannel;
import io.openems.api.device.nature.Ess;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.WriteableModbusChannel;
import io.openems.impl.protocol.modbus.internal.ElementBuilder;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.ModbusRange;
import io.openems.impl.protocol.modbus.internal.WritableModbusRange;
import io.openems.impl.protocol.modbus.internal.channel.ModbusChannelBuilder;
import io.openems.impl.protocol.modbus.internal.channel.WriteableModbusChannelBuilder;

public class FeneconCommercialEss extends ModbusDeviceNature implements Ess {

	private final ModbusChannel _activePower = new ModbusChannelBuilder().nature(this).unit("W").multiplier(100)
			.build();
	private final ModbusChannel _allowedCharge = new ModbusChannelBuilder().nature(this).unit("W").multiplier(100)
			.build();
	private final ModbusChannel _allowedDischarge = new ModbusChannelBuilder().nature(this).unit("W").multiplier(100)
			.build();
	private final ModbusChannel _apparentPower = new ModbusChannelBuilder().nature(this).unit("VA").multiplier(100)
			.build();
	private final ConfigChannel _minSoc = new ConfigChannelBuilder().nature(this).defaultValue(DEFAULT_MINSOC)
			.percentType().build();
	private final ModbusChannel _reactivePower = new ModbusChannelBuilder().nature(this).unit("var").multiplier(100)
			.build();
	private final WriteableModbusChannel _setActivePower = new WriteableModbusChannelBuilder().nature(this).unit("W")
			.multiplier(100).minWriteValue(_allowedCharge).maxWriteValue(_allowedDischarge).build();
	private final WriteableModbusChannel _setWorkState = new WriteableModbusChannelBuilder().nature(this) //
			.label(4, "Stop") //
			.label(64, "Start") //
			.build();
	private final ModbusChannel _soc = new ModbusChannelBuilder().nature(this).percentType().build();
	private final ModbusChannel _systemState = new ModbusChannelBuilder().nature(this) //
			.label(2, "Stop") //
			.label(8, "Standby") //
			.label(16, "Start") //
			.build();

	public FeneconCommercialEss(String thingId) {
		super(thingId);
	}

	@Override
	public Channel activePower() {
		return _activePower;
	}

	@Override
	public Channel allowedCharge() {
		return _allowedCharge;
	}

	@Override
	public Channel allowedDischarge() {
		return _allowedDischarge;
	}

	@Override
	public Channel apparentPower() {
		return _apparentPower;
	}

	@Override
	public Channel minSoc() {
		return _minSoc;
	}

	@Override
	public Channel reactivePower() {
		return _reactivePower;
	}

	@Override
	public WriteableChannel setActivePower() {
		return _setActivePower;
	}

	@Override
	public void setMinSoc(Integer minSoc) {
		this._minSoc.updateValue(BigInteger.valueOf(minSoc));
	}

	@Override
	public WriteableChannel setWorkState() {
		return _setWorkState;
	}

	@Override
	public Channel soc() {
		return _soc;
	}

	@Override
	public Channel systemState() {
		return _systemState;
	}

	@Override
	public String toString() {
		return "FeneconCommercialEss [setActivePower=" + _setActivePower + ", minSoc=" + _minSoc + ", soc=" + _soc
				+ "]";
	}

	@SuppressWarnings("null")
	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		return new ModbusProtocol( //
				new ModbusRange(0x0101, //
						new ElementBuilder().address(0x0101).channel(_systemState).build()), //
				new ModbusRange(0x0210, //
						new ElementBuilder().address(0x0210).channel(_activePower).signed().build(),
						new ElementBuilder().address(0x0211).channel(_reactivePower).signed().build(),
						new ElementBuilder().address(0x0212).channel(_apparentPower).build(),
						new ElementBuilder().address(0x0213).dummy().build(),
						// .name(EssProtocol.CurrentPhase1).signed(true).multiplier(100).unit("mA").build(),
						new ElementBuilder().address(0x0214).dummy().build(),
						// .name(EssProtocol.CurrentPhase2).signed(true).multiplier(100).unit("mA").build(),
						new ElementBuilder().address(0x0215).dummy().build(),
						// .name(EssProtocol.CurrentPhase3).signed(true).multiplier(100).unit("mA").build(),
						new ElementBuilder().address(0x0216).dummy(0x219 - 0x216).build(),
						// .type(ElementType.PLACEHOLDER).intLength(0x219 - 0x216).build(),
						new ElementBuilder().address(0x0219).dummy().build(),
						// .name(EssProtocol.VoltagePhase1).multiplier(100).unit("mV").build(),
						new ElementBuilder().address(0x021A).dummy().build(),
						// .name(EssProtocol.VoltagePhase2).multiplier(100).unit("mV").build(),
						new ElementBuilder().address(0x021B).dummy().build(),
						// .name(EssProtocol.VoltagePhase3).multiplier(100).unit("mV").build(),
						new ElementBuilder().address(0x021C).dummy().build(),
						// .name(EssProtocol.Frequency).multiplier(10).unit("mHZ").build(),
						new ElementBuilder().address(0x021D).dummy(0x222 - 0x21D).build(),
						// type(ElementType.PLACEHOLDER).intLength(0x222 - 0x21D).build(),
						new ElementBuilder().address(0x0222).dummy().build(),
						// .name(EssProtocol.InverterVoltagePhase1).signed(true).multiplier(100).unit("mV").build(),
						new ElementBuilder().address(0x0223).dummy().build(),
						// .name(EssProtocol.InverterVoltagePhase2).signed(true).multiplier(100).unit("mV").build(),
						new ElementBuilder().address(0x0224).dummy().build(),
						// .name(EssProtocol.InverterVoltagePhase3).signed(true).multiplier(100).unit("mV").build(),
						new ElementBuilder().address(0x0225).dummy().build(),
						// .name(EssProtocol.InverterCurrentPhase1).signed(true).multiplier(100).unit("mA").build(),
						new ElementBuilder().address(0x0226).dummy().build(),
						// .name(EssProtocol.InverterCurrentPhase2).signed(true).multiplier(100).unit("mA").build(),
						new ElementBuilder().address(0x0227).dummy().build(),
						// .name(EssProtocol.InverterCurrentPhase3).signed(true).multiplier(100).unit("mA").build(),
						new ElementBuilder().address(0x0228).dummy().build(),
						// .name(EssProtocol.InverterActivePower).signed(true).multiplier(100).unit("W").build(),
						new ElementBuilder().address(0x0229).dummy(0x230 - 0x229).build(),
						// .type(ElementType.PLACEHOLDER).intLength(0x230 - 0x229).build(),
						new ElementBuilder().address(0x0230).channel(_allowedCharge).signed().build(),
						new ElementBuilder().address(0x0231).channel(_allowedDischarge).build()),
				new ModbusRange(0x1402, //
						new ElementBuilder().address(0x1402).channel(_soc).build()),
				new WritableModbusRange(0x0500, //
						new ElementBuilder().address(0x0500).channel(_setWorkState).build(),
						new ElementBuilder().address(0x0501).channel(_setActivePower).signed().build() //
				));
	}
}
