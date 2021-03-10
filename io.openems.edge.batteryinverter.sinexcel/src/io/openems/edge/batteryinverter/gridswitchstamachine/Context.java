package io.openems.edge.batteryinverter.gridswitchstamachine;

import java.util.Optional;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.batteryinverter.sinexcel.Sinexcel;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<Sinexcel> {

	
//	protected Optional<Boolean> getMainContactor() throws IllegalArgumentException, OpenemsNamedException {
//		BooleanReadChannel inChannel1 = this.cm
//				.getChannel(ChannelAddress.fromString("io0/DigitalInputM1C1"));
//		return inChannel1.value().asOptional();
//
//	}
//
//	protected Optional<Boolean> getGridDetector() throws IllegalArgumentException, OpenemsNamedException {
//		BooleanReadChannel inChannel2 = this.cm
//				.getChannel(ChannelAddress.fromString("io0/DigitalInputM1C2"));
//		return inChannel2.value().asOptional();
//	}
//
//	protected Optional<Boolean> getGrounding() throws IllegalArgumentException, OpenemsNamedException {
//		BooleanReadChannel inChannel3 = this.cm
//				.getChannel(ChannelAddress.fromString("io0/DigitalInputM2C1"));
//		return inChannel3.value().asOptional();
//	}
}
