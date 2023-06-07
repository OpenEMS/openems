package io.openems.edge.controller.ess.mindischargeperiod;

import java.util.concurrent.TimeUnit;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.MinimumDischargePower", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEssMinimumDischargePowerImpl extends AbstractOpenemsComponent
		implements ControllerEssMinimumDischargePower, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ControllerEssMinimumDischargePowerImpl.class);

	@Reference
	private ComponentManager componentManager;

	private Config config = null;
	private Stopwatch stopwatch;

	public ControllerEssMinimumDischargePowerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssMinimumDischargePower.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.stopwatch = Stopwatch.createUnstarted();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());

		/*
		 * Check that we are On-Grid (and warn on undefined Grid-Mode)
		 */
		var gridMode = ess.getGridMode();
		if (gridMode.isUndefined()) {
			this.logWarn(this.log, "Grid-Mode is [UNDEFINED]");
		}
		switch (gridMode) {
		case ON_GRID:
		case UNDEFINED:
			break;
		case OFF_GRID:
			return;
		}

		if (!this.stopwatch.isRunning()) {
			int essActivePower = ess.getActivePower().getOrError();
			if (essActivePower >= this.config.activateDischargePower()) {
				this.stopwatch.start();
				this.logInfo(this.log,
						"Started the stopwatch. Trying to discharge with " + this.config.minDischargePower());
			}
			this.channel(ControllerEssMinimumDischargePower.ChannelId.TIME_PASSED)
					.setNextValue(this.stopwatch.elapsed(TimeUnit.SECONDS));
			this.channel(ControllerEssMinimumDischargePower.ChannelId.STATE_MACHINE).setNextValue(State.NOT_ACTIVE);
			return;
		}

		if (this.stopwatch.elapsed(TimeUnit.SECONDS) > this.config.dischargeTime()) {
			this.stopwatch.stop();
			this.stopwatch.reset();
			return;
		}

		this.channel(ControllerEssMinimumDischargePower.ChannelId.TIME_PASSED)
				.setNextValue(this.stopwatch.elapsed(TimeUnit.SECONDS));
		this.channel(ControllerEssMinimumDischargePower.ChannelId.STATE_MACHINE).setNextValue(State.ACTIVE);

		/*
		 * set result
		 */
		try {
			ess.addPowerConstraintAndValidate("MinDischargePeriod", Phase.ALL, Pwr.ACTIVE,
					Relationship.GREATER_OR_EQUALS, this.config.minDischargePower());

		} catch (OpenemsException e) {
			this.logWarn(this.log, e.getMessage());
			this.logInfo(this.log, "Make sure that the controller is running before balancing or peakshaving.");
		}
	}
}
