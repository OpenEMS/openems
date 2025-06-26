package io.openems.edge.controller.ess.delaycharge;

import java.time.LocalDateTime;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.DelayCharge", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEssDelayChargeImpl extends AbstractOpenemsComponent
		implements ControllerEssDelayCharge, Controller, OpenemsComponent {

	@Reference
	private ComponentManager componentManager;

	private Config config = null;

	public ControllerEssDelayChargeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssDelayCharge.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		// Get required variables
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());
		int capacity = ess.getCapacity().getOrError();
		var targetSecondOfDay = this.config.targetHour() * 3600;

		// calculate remaining capacity in Ws
		var remainingCapacity = capacity * (100 - ess.getSoc().getOrError()) * 36;

		// No remaining capacity -> no restrictions
		if (remainingCapacity < 0) {
			this.setChannels(State.NO_REMAINING_CAPACITY, 0);
			return;
		}

		// calculate remaining time
		var remainingTime = targetSecondOfDay - this.currentSecondOfDay();

		// We already passed the "target hour of day" -> no restrictions
		if (remainingTime < 0) {
			this.setChannels(State.PASSED_TARGET_HOUR, 0);
			return;
		}

		// calculate charge power limit
		var limit = remainingCapacity / remainingTime * -1;

		// reduce limit to MaxApparentPower to avoid very high values in the last
		// seconds
		limit = Math.min(limit, ess.getMaxApparentPower().orElse(0));

		// set ActiveLimit channel
		this.setChannels(State.ACTIVE_LIMIT, limit * -1);

		// Set limitation for ChargePower
		ess.setActivePowerGreaterOrEquals(limit);
	}

	private int currentSecondOfDay() {
		var now = LocalDateTime.now(this.componentManager.getClock());
		return now.getHour() * 3600 + now.getMinute() * 60 + now.getSecond();
	}

	private void setChannels(State state, int limit) {
		EnumReadChannel stateMachineChannel = this.channel(ControllerEssDelayCharge.ChannelId.STATE_MACHINE);
		stateMachineChannel.setNextValue(state);

		IntegerReadChannel chargePowerLimitChannel = this
				.channel(ControllerEssDelayCharge.ChannelId.CHARGE_POWER_LIMIT);
		chargePowerLimitChannel.setNextValue(limit);
	}
}
