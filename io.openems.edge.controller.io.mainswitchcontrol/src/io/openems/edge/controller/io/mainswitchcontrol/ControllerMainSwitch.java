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

	public final static int DEFAULT_DISCHARGE_POWER = 10_000;
	public final static int DEFAULT_CHECKPOINT_DISCHARGE_POWER = 9_000;
	public final static int OWN_CONSUMPTION_LIMIT = 700;

	private int attemptCounter = 10;
	private int loadAttemptCounter = 600;
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
		; //

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
		int allowedChargePower = -1 * ess.getAllowedChargePower().orElse(0);
		int allowedDischargePower = ess.getAllowedDischargePower().orElse(0);

		// -----------------------------------------------------------------------------------
		boolean stateChanged;

		do {
			stateChanged = false;
			switch (this.state) {

			case UNDEFINED:
				if (allowedDischargePower > 0) {
					stateChanged = changeState(State.START);
				} else {
					stateChanged = changeState(State.OPEN_CONTACTOR);
				}

				this.logInfo(this.log, "the current state is : " + this.state);
				break;

			case START:
				attemptCounter--;
				setOutput(false);

				if (attemptCounter == 0) {

					stateChanged = changeState(State.CLOSE_CONTACTOR);
					this.logInfo(this.log,
							"applied discharge for  : " + attemptCounter + " cycles , now closing the contactors");
					attemptCounter = 10;

				} else {
					// can you discharge 20 kw?
					applyPower(ess, DEFAULT_DISCHARGE_POWER);
					this.logInfo(this.log, "trying discharge : " + DEFAULT_DISCHARGE_POWER + " Watts");

				}
				this.logInfo(this.log, "the current state is : " + this.state + attemptCounter
						+ " cycles , now closing the contactors");
				break;

			case OPEN_CONTACTOR:
				// Open the contactors
				setOutput(false);
				if (allowedChargePower > 0) {
					stateChanged = changeState(State.START);
				}
				this.logInfo(this.log, "the current state is : " + this.state);
				break;

			case CLOSE_CONTACTOR:
				// close the contactor
				setOutput(true);
				applyPower(ess, DEFAULT_DISCHARGE_POWER);

				// Check if load is there ?
				int loadPower = meter.getActivePower().orElse(0) + ess.getActivePower().orElse(0);

				loadAttemptCounter--;
				this.logInfo(this.log, "the Load was  : " + loadPower + " watts the counter was " + loadAttemptCounter);

				if (loadPower > OWN_CONSUMPTION_LIMIT) {
					this.logInfo(this.log, "the Load was : " + loadPower
							+ " watts was greater then own consumeption " + OWN_CONSUMPTION_LIMIT);
					stateChanged = changeState(State.PEAK_SHAVE);
				}else {
					this.logInfo(this.log, "No Load  ");
					if(loadAttemptCounter != 0) {
						//stateChanged = changeState(State.PEAK_SHAVE);						
						this.logInfo(this.log, "Counting loadAttemptCounter :" + loadAttemptCounter);
					}else {
						stateChanged = changeState(State.PEAK_SHAVE);	
						loadAttemptCounter = 600;
					}
				}				


				this.logInfo(this.log, "the current state is : " + this.state);
				break;

			case PEAK_SHAVE:
				if (allowedDischargePower <= 0) {
					stateChanged = changeState(State.UNDEFINED);
				}
				doPeakShave(ess, meter);
				this.logInfo(this.log, "the current state is : " + this.state);
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

		this.logInfo(this.log, "in peak shave the grid power was   : " + gridPower + " watts");
		int calculatedPower;
		if (gridPower >= this.config.peakShavingPower()) {
			/*
			 * Peak-Shaving
			 */
			calculatedPower = gridPower -= this.config.peakShavingPower();
			this.logInfo(this.log, "peak shaveing   : " + calculatedPower + " watts");

		} else if (gridPower <= this.config.rechargePower()) {
			/*
			 * Recharge
			 */
			calculatedPower = gridPower -= this.config.rechargePower();
			this.logInfo(this.log, "recharge power   : " + calculatedPower + " watts");

		} else {
			/*
			 * Do nothing
			 */
			calculatedPower = 0;
			this.logInfo(this.log, "Do nothing   : " + calculatedPower + " watts");
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
