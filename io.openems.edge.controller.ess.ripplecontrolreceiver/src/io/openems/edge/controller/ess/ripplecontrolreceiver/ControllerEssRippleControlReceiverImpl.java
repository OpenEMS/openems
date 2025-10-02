package io.openems.edge.controller.ess.ripplecontrolreceiver;

import static io.openems.edge.controller.ess.ripplecontrolreceiver.EssRestrictionLevel.NO_RESTRICTION;
import static io.openems.edge.controller.ess.ripplecontrolreceiver.EssRestrictionLevel.SIXTY_PERCENT;
import static io.openems.edge.controller.ess.ripplecontrolreceiver.EssRestrictionLevel.THIRTY_PERCENT;
import static io.openems.edge.controller.ess.ripplecontrolreceiver.EssRestrictionLevel.ZERO_PERCENT;

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
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
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

	private static final double NO_LIMITATION_FACTOR = 1.0;

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

	// 1 = 100% (no limit)
	private double currentLimitationFactor = NO_LIMITATION_FACTOR;
	// 0% limit
	private ChannelAddress inputChannelAddress1;
	// 30% limit
	private ChannelAddress inputChannelAddress2;
	// 60% limit
	private ChannelAddress inputChannelAddress3;

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
		boolean limitationApplied = false;
		limitationApplied |= this.applyRestriction(this.inputChannelAddress3, SIXTY_PERCENT);
		limitationApplied |= this.applyRestriction(this.inputChannelAddress2, THIRTY_PERCENT);
		limitationApplied |= this.applyRestriction(this.inputChannelAddress1, ZERO_PERCENT);
		if (!limitationApplied) {
			this._setRestrictionMode(NO_RESTRICTION); //
			this.cumulatedRestrictionTime.update(false);
			this.currentLimitationFactor = NO_LIMITATION_FACTOR; //
		}
	}

	protected boolean applyRestriction(ChannelAddress inputChannelAddress, EssRestrictionLevel restrictionLevel)
			throws OpenemsNamedException {
		if (!this.isRestrictionActive(inputChannelAddress)) {
			return false;
		}
		this._setRestrictionMode(restrictionLevel);
		this.currentLimitationFactor = restrictionLevel.getLimitationFactor();
		this.cumulatedRestrictionTime.update(true);
		return true;
	}

	protected boolean isRestrictionActive(ChannelAddress inputChannelAddress) throws OpenemsNamedException {
		BooleanReadChannel inputChannel = this.componentManager.getChannel(inputChannelAddress);
		var onGrid = this.sum.getGridMode().equals(GridMode.ON_GRID);
		// 0/1 is reversed on relays board
		var isActive = !inputChannel.value().orElse(true);
		return onGrid && isActive;
	}

	@Override
	public double limitationFactor() {
		return this.currentLimitationFactor;
	}

	@Override
	public int limitationValue() {
		return this.meta.getMaximumGridFeedInLimit();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	private void applyConfig(Config config) throws OpenemsNamedException {
		this.inputChannelAddress1 = ChannelAddress.fromString(config.inputChannelAddress1());
		this.inputChannelAddress2 = ChannelAddress.fromString(config.inputChannelAddress2());
		this.inputChannelAddress3 = ChannelAddress.fromString(config.inputChannelAddress3());
	}

	@Override
	public String debugLog() {
		return "Current limitation: " + this.getRestrictionMode().getName();
	}

}