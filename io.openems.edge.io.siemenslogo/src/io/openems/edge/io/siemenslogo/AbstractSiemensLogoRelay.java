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
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;

public abstract class AbstractSiemensLogoRelay extends AbstractOpenemsModbusComponent
		implements DigitalOutput, DigitalInput, ModbusComponent, OpenemsComponent, ModbusSlave {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private final BooleanWriteChannel[] digitalOutputChannels;
	private final BooleanReadChannel[] digitalInputChannels;

	protected AbstractSiemensLogoRelay(io.openems.edge.common.channel.ChannelId[] siemenslogoChannelIds) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				DigitalInput.ChannelId.values(), //
				siemenslogoChannelIds //
		);
		this.digitalOutputChannels = Stream.of(siemenslogoChannelIds) //
				.filter(channelId -> channelId.doc().getAccessMode() == AccessMode.READ_WRITE) //
				.map(channelId -> this.channel(channelId)) //
				.toArray(BooleanWriteChannel[]::new);
		
		this.digitalInputChannels = Stream.of(siemenslogoChannelIds) //
				.filter(channelId -> channelId.doc().getAccessMode() == AccessMode.READ_ONLY) //
				.map(channelId -> this.channel(channelId)) //
				.toArray(BooleanReadChannel[]::new);
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.digitalOutputChannels;
	}
	
	@Override
	public BooleanReadChannel[] digitalInputChannels() {
		return this.digitalInputChannels;
	}
	

	@Override
	public String debugLog() {
        String outputLog = Arrays.stream(this.digitalOutputChannels)
                .map(chan -> chan.value().asOptional())
                .map(t -> t.isPresent() ? (t.get() ? "X" : "-") : "?")
                .collect(Collectors.joining(","));
        
        String inputLog = Arrays.stream(this.digitalInputChannels)
                .map(chan -> chan.value().asOptional())
                .map(t -> t.isPresent() ? (t.get() ? "I" : "O") : "?")
                .collect(Collectors.joining(","));
        
        return "Output: [" + outputLog + "], Input: [" + inputLog + "]";
	}


}
