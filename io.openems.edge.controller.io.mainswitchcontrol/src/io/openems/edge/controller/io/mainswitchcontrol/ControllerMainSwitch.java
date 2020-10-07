package io.openems.edge.controller.io.mainswitchcontrol;

import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.io.MainSwitchControl", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerMainSwitch extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ControllerMainSwitch.class);

	public final static int DEFAULT_DISCHARGE_POWER = 20_000;
	public final static int DEFAULT_CHECKPOINT_DISCHARGE_POWER = 19_000;

	private int attemptCounter = 0;
	private Config config = null;

	@Reference
	protected ComponentManager componentManager;

	/**
	 * Stores the ChannelAddress of the WriteChannel.
	 */
	private ChannelAddress outputChannelAddress = null;

	/**
	 * The current state in the State Machine.
	 */
	private State state = State.UNDEFINED;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public ControllerMainSwitch() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.outputChannelAddress = ChannelAddress.fromString(config.outputChannelAddress());
		
		// just to start with the open contactors
		setOutput(false);
		
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess());
		SymmetricMeter meter = this.componentManager.getComponent(this.config.meter_id());
		int allowedChargePower = ess.getAllowedChargePower().orElse(0);

		boolean stateChanged;

		do {
			stateChanged = false;
			switch (this.state) {
			
			case UNDEFINED:
				if (allowedChargePower > 0) {
					stateChanged = changeState(State.START);
				}
				break;
			
			
			case START:
				attemptCounter--;

				if (attemptCounter == 0) {
					// can you discharge 20 kw?
					applyPower(ess, DEFAULT_DISCHARGE_POWER);
					if (ess.getActivePower().orElse(0) > DEFAULT_DISCHARGE_POWER) {
						stateChanged = changeState(State.CLOSE_CONTACTOR);
					}
				}else {
					stateChanged = changeState(State.OPEN_CONTACTOR);
				}

				break;
			
			
			case OPEN_CONTACTOR:
				// Open the contactors
				setOutput(false);
				break;
			
			
			case CLOSE_CONTACTOR:
				// close the contactor
				setOutput(true);
				// goto peak shave
				stateChanged = changeState(State.PEAK_SHAVE);
				break;
			
			
			case PEAK_SHAVE:
				doPeakShave(ess, meter);
				break;

			}
		} while (stateChanged);

	}

	private void doPeakShave(ManagedSymmetricEss ess, SymmetricMeter meter) throws OpenemsNamedException {
		/*
		 * Check that we are On-Grid (and warn on undefined Grid-Mode)
		 */
		GridMode gridMode = ess.getGridMode();
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

		// Calculate 'real' grid-power (without current ESS charge/discharge)
		int gridPower = meter.getActivePower().orElse(0) /* current buy-from/sell-to grid */
				+ ess.getActivePower().orElse(0) /* current charge/discharge Ess */;

		int calculatedPower;
		if (gridPower >= this.config.peakShavingPower()) {
			/*
			 * Peak-Shaving
			 */
			calculatedPower = gridPower -= this.config.peakShavingPower();

		} else if (gridPower <= this.config.rechargePower()) {
			/*
			 * Recharge
			 */
			calculatedPower = gridPower -= this.config.rechargePower();

		} else {
			/*
			 * Do nothing
			 */
			calculatedPower = 0;
		}

		/*
		 * set result
		 */
		ess.setActivePowerEquals(calculatedPower);
		ess.setReactivePowerEquals(0);

	}

	/**
	 * A flag to maintain change in the state.
	 * 
	 * @param nextState the target state
	 * @return Flag that the state is changed or not
	 */
	private boolean changeState(State nextState) {
		if (this.state != nextState) {
			this.state = nextState;
			return true;
		} else {
			return false;
		}
	}

	private void setOutput(boolean value) throws IllegalArgumentException, OpenemsNamedException {
		WriteChannel<Boolean> channel = this.componentManager.getChannel(this.outputChannelAddress);
		if (channel.value().asOptional().equals(Optional.of(value))) {
			// it is already in the desired state
		} else {
			channel.setNextWriteValue(value);
		}
	}

	/**
	 * Applies the power on the Ess.
	 * 
	 * @param ess         {@link ManagedSymmetricEss} where the power needs to be
	 *                    set
	 * @param activePower the power to be set on ess, or null to set no power
	 * @throws OpenemsNamedException on error
	 */
	private void applyPower(ManagedSymmetricEss ess, Integer activePower) throws OpenemsNamedException {
		if (activePower != null) {
			ess.setActivePowerEquals(activePower);
			ess.setReactivePowerEquals(0);
		}
	}
}
