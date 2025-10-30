package io.openems.edge.controller.ess.ripplecontrolreceiver;

import static io.openems.edge.controller.ess.ripplecontrolreceiver.EssRestrictionLevel.NO_RESTRICTION;

import java.util.OptionalInt;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.GridFeedInLimitationType;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.RippleControlReceiver", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEssRippleControlReceiverImpl extends AbstractOpenemsComponent implements //
		ControllerEssRippleControlReceiver, Controller, OpenemsComponent, TimedataProvider {

	@Reference
	private Sum sum;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private Meta meta;

	private EssRestrictionLevel currentRestriction = NO_RESTRICTION;
	private ChannelAddress zeroPercentChannelAddress;
	private ChannelAddress thirtyPercentChannelAddress;
	private ChannelAddress sixtyPercentChannelAddress;

	private final CalculateActiveTime cumulatedRestrictionTime = new CalculateActiveTime(this,
			ControllerEssRippleControlReceiver.ChannelId.CUMULATED_RESTRICTION_TIME);

	public ControllerEssRippleControlReceiverImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssRippleControlReceiver.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	@Modified
	protected void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, config.id(), config.alias(), config.enabled()); //
		this.applyConfig(config);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		var currentRestriction = NO_RESTRICTION;
		if (this.sum.getGridMode().equals(GridMode.ON_GRID)) {
			currentRestriction = EssRestrictionLevel.getRestrictionLevelByPriority(//
					this.isRestrictionActive(this.zeroPercentChannelAddress), //
					this.isRestrictionActive(this.thirtyPercentChannelAddress), //
					this.isRestrictionActive(this.sixtyPercentChannelAddress));
		}

		switch (currentRestriction) {
		case NO_RESTRICTION -> {
			this.cumulatedRestrictionTime.update(false);
		}
		case ZERO_PERCENT, THIRTY_PERCENT, SIXTY_PERCENT -> {
			// This may split into different cumulated times for each restriction level
			this.cumulatedRestrictionTime.update(true);
		}
		}

		this.currentRestriction = currentRestriction;
		this._setRestrictionMode(currentRestriction);
	}

	protected boolean isRestrictionActive(ChannelAddress inputChannelAddress) throws OpenemsNamedException {
		BooleanReadChannel inputChannel = this.componentManager.getChannel(inputChannelAddress);
		// 0/1 is reversed on relays board
		return !inputChannel.value().orElse(true);
	}

	@Override
	public EssRestrictionLevel essRestrictionLevel() {
		return this.currentRestriction;
	}

	@Override
	public OptionalInt maximumGridFeedInLimit() {
		return feedInLimitFromMetaLimits(this.meta.getGridFeedInLimitationType(),
				this.meta.getMaximumGridFeedInLimitValue());
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	private void applyConfig(Config config) throws OpenemsNamedException {
		this.zeroPercentChannelAddress = ChannelAddress.fromString(config.inputChannelAddress1());
		this.thirtyPercentChannelAddress = ChannelAddress.fromString(config.inputChannelAddress2());
		this.sixtyPercentChannelAddress = ChannelAddress.fromString(config.inputChannelAddress3());
	}

	@Override
	public String debugLog() {
		return "Current limitation: " + this.getRestrictionMode().getName();
	}

	/**
	 * Calculates the feed-in limit from the meta component's grid feed-in
	 * limitation.
	 * 
	 * @param type  the type of grid feed-in limitation
	 * @param limit the limit value (in W) if type is DYNAMIC_LIMITATION
	 * @return the dynamic feed-in limit as OptionalInt
	 */
	public static OptionalInt feedInLimitFromMetaLimits(GridFeedInLimitationType type, Value<Integer> limit) {
		return switch (type) {
		case DYNAMIC_LIMITATION -> {
			if (limit.isDefined()) {
				yield OptionalInt.of(limit.get());
			}
			yield OptionalInt.empty();
		}
		case NO_LIMITATION -> OptionalInt.empty();
		case UNDEFINED -> OptionalInt.empty();
		};
	}
}