package io.openems.edge.controller.ess.ripplecontrolreceiver.powerproduction;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.meta.GridFeedInLimitationType;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.ess.ripplecontrolreceiver.EssRestrictionLevel;
import io.openems.edge.controller.ess.ripplecontrolreceiver.PowerProductionLimiter;
import io.openems.edge.controller.ess.ripplecontrolreceiver.PowerProductionLimiterComponent;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import java.util.OptionalInt;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.RippleControlReceiver", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class RelaisSignalPowerProductionLimiterImpl extends AbstractOpenemsComponent implements RelaisSignalPowerProductionLimiter,
		PowerProductionLimiterComponent, PowerProductionLimiter, OpenemsComponent, TimedataProvider, EventHandler {

	@Reference
	private Sum sum;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private ComponentManager componentManager;

	private ChannelAddress zeroPercentChannelAddress;
	private ChannelAddress thirtyPercentChannelAddress;
	private ChannelAddress sixtyPercentChannelAddress;

	private Integer maxNominalProductionPowerInW;

	private final CalculateActiveTime cumulatedRestrictionTime = new CalculateActiveTime(this,
			PowerProductionLimiterComponent.ChannelId.CUMULATED_RESTRICTION_TIME);

	public RelaisSignalPowerProductionLimiterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				PowerProductionLimiterComponent.ChannelId.values(), //
				RelaisSignalPowerProductionLimiter.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	@Modified
	protected void modified(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
		super.modified(context, config.id(), config.alias(), config.enabled()); //
		this.applyConfig(config);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void applyConfig(Config config) throws OpenemsError.OpenemsNamedException {
		this.zeroPercentChannelAddress = ChannelAddress.fromString(config.inputChannelAddress1());
		this.thirtyPercentChannelAddress = ChannelAddress.fromString(config.inputChannelAddress2());
		this.sixtyPercentChannelAddress = ChannelAddress.fromString(config.inputChannelAddress3());
	}

	protected void run() {
		try {
			if (this.sum.getGridMode() != GridMode.ON_GRID) {
				this.setRestriction(EssRestrictionLevel.NO_RESTRICTION);
				return;
			}

			this.setRestriction(this.calculateRestrictionByRelayPorts());
			setValue(this, RelaisSignalPowerProductionLimiter.ChannelId.UPDATE_FAILURE, false);
		} catch (Exception ex) {
			// TODO
			setValue(this, RelaisSignalPowerProductionLimiter.ChannelId.UPDATE_FAILURE, true);
		}
	}

	protected EssRestrictionLevel calculateRestrictionByRelayPorts() throws OpenemsError.OpenemsNamedException {
		return EssRestrictionLevel.getRestrictionLevelByPriority(//
				this.isRelayPortActive(this.zeroPercentChannelAddress), //
				this.isRelayPortActive(this.thirtyPercentChannelAddress), //
				this.isRelayPortActive(this.sixtyPercentChannelAddress));
	}

	private boolean isRelayPortActive(ChannelAddress inputChannelAddress) throws OpenemsError.OpenemsNamedException {
		BooleanReadChannel inputChannel = this.componentManager.getChannel(inputChannelAddress);
		// 0/1 is reversed on relays board
		return !inputChannel.value().orElse(true);
	}

	protected void setRestriction(EssRestrictionLevel restrictionLevel) {
		setValue(this, RelaisSignalPowerProductionLimiter.ChannelId.RESTRICTION_MODE, restrictionLevel);
		if (restrictionLevel == EssRestrictionLevel.NO_RESTRICTION) {
			this.cumulatedRestrictionTime.update(false);
			setValue(this, PowerProductionLimiterComponent.ChannelId.RESTRICTION, null);
		} else {
			this.cumulatedRestrictionTime.update(true);
			setValue(this, PowerProductionLimiterComponent.ChannelId.RESTRICTION, restrictionLevel.getLimitationFactor() * this.maxNominalProductionPowerInW);
		}
	}

	@Override
	public String debugLog() {
		return "Current limitation: " + this.getRestrictionLevel().getName();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
			case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
				this.run();
				break;
		}
	}

	@Override
	public void setMaxNominalProductionPower(int maxNominalProductionPowerInW) {
		this.maxNominalProductionPowerInW = maxNominalProductionPowerInW;
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
