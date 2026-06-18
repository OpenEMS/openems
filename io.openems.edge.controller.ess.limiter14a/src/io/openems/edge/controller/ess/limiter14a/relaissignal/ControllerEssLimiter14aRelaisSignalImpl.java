package io.openems.edge.controller.ess.limiter14a.relaissignal;

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
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.limiter14a.ControllerEssLimiter14a;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.Limiter14a", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEssLimiter14aRelaisSignalImpl extends AbstractOpenemsComponent implements //
		ControllerEssLimiter14aRelaisSignal, ControllerEssLimiter14a, Controller, OpenemsComponent, TimedataProvider {

	@Reference
	private Sum sum;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference(target = "(&(id=${config.ess_id})(enabled=true))", //
			policy = ReferencePolicy.STATIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	private ChannelAddress inputChannelAddress;

	private final CalculateActiveTime cumulatedRestrictionTime = new CalculateActiveTime(this,
			ControllerEssLimiter14a.ChannelId.CUMULATED_RESTRICTION_TIME);

	public ControllerEssLimiter14aRelaisSignalImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssLimiter14a.ChannelId.values() //
		);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	@Modified
	protected void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, config.id(), config.alias(), config.enabled()); //
		this.applyConfig(config);
	}

	protected void applyConfig(Config config) throws OpenemsNamedException {
		this.inputChannelAddress = ChannelAddress.fromString(config.inputChannelAddress());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		var isLimitActive = this.determinateIfLimitIsActive();
		if (isLimitActive) {
			this.ess.setActivePowerGreaterOrEquals(ESS_LIMIT_14A_ENWG);
		}

		this._setRestrictionMode(isLimitActive);
		this.cumulatedRestrictionTime.update(isLimitActive);
	}

	protected boolean determinateIfLimitIsActive() throws OpenemsNamedException {
		if (this.sum.getGridMode() != GridMode.ON_GRID) {
			return false;
		}

		BooleanReadChannel inputChannel = this.componentManager.getChannel(this.inputChannelAddress);
		return !inputChannel.value().orElse(true); // 0/1 is reversed on relays board
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}