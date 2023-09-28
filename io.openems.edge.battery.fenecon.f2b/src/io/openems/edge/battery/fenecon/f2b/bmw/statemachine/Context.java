package io.openems.edge.battery.fenecon.f2b.bmw.statemachine;

import java.time.Clock;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery.ChannelId;
import io.openems.edge.battery.fenecon.f2b.bmw.BatteryFeneconF2bBmw;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.CoolingApproval;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.InsulationMeasurement;
import io.openems.edge.battery.fenecon.f2b.common.enums.F2bCanCommunication;
import io.openems.edge.battery.fenecon.f2b.common.enums.F2bTerminal15Sw;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<BatteryFeneconF2bBmw> {

	protected final Clock clock;

	public Context(BatteryFeneconF2bBmw parent, Clock clock) {
		super(parent);
		this.clock = clock;
	}

	/**
	 * Gets the value of {@link ChannelId#F2B_CAN_COMMUNICATION} and proofs whether
	 * {@link F2bCanCommunication CAN_ON} is or not.
	 * 
	 * @return true if {@link ChannelId#F2B_CAN_COMMUNICATION} is
	 *         {@link F2bCanCommunication CAN_ON}
	 */
	protected boolean isF2bCanCommunicationOn() {
		var state = this.getParent().getF2bCanCommunication();
		return state.isDefined() && state.asEnum() == F2bCanCommunication.CAN_ON;
	}

	/**
	 * Before switch on the CAN communication between F2B and battery, CAN signals
	 * can be initialized with sending required signals.
	 *
	 * @throws OpenemsNamedException on error
	 */
	protected void initializeBatteryCanSignals() throws OpenemsNamedException {
		var battery = this.getParent();
		battery.setF2bTerminal15Sw(F2bTerminal15Sw.KL_15_ON);
		battery.setHvContactor(false);
		battery.setInsulationMeasurement(InsulationMeasurement.DO_NOT_PERFORM_MEASUREMENT);
		battery.setCoolingApproval(CoolingApproval.NOT_GRANTED);
	}

	/**
	 * Proof whether any critical cat errors occurred.
	 * 
	 * @return true if is there any.
	 */
	public boolean isAnyActiveCatError() {
		final var battery = this.getParent();
		record ApplyCatFaultChannels(Channel<Boolean> readchannel, Consumer<Boolean> writechannel) {
		}

		return Stream.of(//
				new ApplyCatFaultChannels(battery.getCat7EmergencyContactorOpenChannel(), battery::_setCat7Fault), //
				new ApplyCatFaultChannels(battery.getCat5BatteryPowerWillBeLimitedChannel(), battery::_setCat5Fault), //
				new ApplyCatFaultChannels(battery.getCat1PrechargeSystemIsLockedChannel(), battery::_setCat1Fault), //
				new ApplyCatFaultChannels(battery.getCat6ZeroCurrentRequestChannel(), battery::_setCat6Fault) //
		).reduce(false, (a, channels) -> {
			final var result = channels.readchannel().value().get();
			channels.writechannel().accept(result);
			return result || a;
		}, Boolean::logicalOr);
	}
}