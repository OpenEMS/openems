package io.openems.edge.controller.ess.limiter14a;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
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
public class ControllerEssLimiter14aImpl extends AbstractOpenemsComponent implements //
		ControllerEssLimiter14a, Controller, OpenemsComponent, TimedataProvider  {
	
	@Reference
	private Sum sum;
	
	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	private ChannelAddress inputChannelAddress;
	
	private final CalculateActiveTime cumulatedRestrictionTime = new CalculateActiveTime(this,
			ControllerEssLimiter14a.ChannelId.CUMULATED_RESTRICTION_TIME);

	public ControllerEssLimiter14aImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssLimiter14a.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.inputChannelAddress = ChannelAddress.fromString(config.inputChannelAddress());
		
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		BooleanReadChannel inputChannel = this.componentManager.getChannel(this.inputChannelAddress);
		var onGrid = this.sum.channel(Sum.ChannelId.GRID_MODE).value().asEnum() != GridMode.OFF_GRID;
		// 0/1 is reversed on relays board
		var isActive = onGrid && !inputChannel.value().orElse(true);
		if (isActive) {
			this.ess.setActivePowerGreaterOrEquals(-4200);
		}
		
		this.channel(ControllerEssLimiter14a.ChannelId.RESTRICTION_MODE).setNextValue(isActive);
		this.cumulatedRestrictionTime.update(isActive);
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}