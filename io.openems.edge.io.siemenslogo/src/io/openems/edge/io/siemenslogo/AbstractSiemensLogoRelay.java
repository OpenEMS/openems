package io.openems.edge.io.siemenslogo;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import io.openems.common.channel.AccessMode;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.io.api.DigitalOutput;

public abstract class AbstractSiemensLogoRelay extends AbstractOpenemsModbusComponent
		implements DigitalOutput, ModbusComponent, OpenemsComponent, ModbusSlave {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private final BooleanWriteChannel[] digitalOutputChannels;

	protected AbstractSiemensLogoRelay(io.openems.edge.common.channel.ChannelId[] siemenslogoChannelIds) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				siemenslogoChannelIds //
		);
		this.digitalOutputChannels = Stream.of(siemenslogoChannelIds) //
				.filter(channelId -> channelId.doc().getAccessMode() == AccessMode.READ_WRITE) //
				.map(channelId -> this.channel(channelId)) //
				.toArray(BooleanWriteChannel[]::new);
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.digitalOutputChannels;
	}


	@Override
	public String debugLog() {
		return (String) Arrays.stream(this.digitalOutputChannels).map(chan -> chan.value().asOptional()).map(t -> {
			if (t.isPresent()) {
				return t.get() ? "X" : "-";
			} else {
				return "?";
			}
		}).collect(Collectors.joining(","));
	}


}
