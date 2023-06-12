package io.openems.edge.timedata.api.utils;

import java.time.Duration;
import java.time.Instant;

import org.osgi.service.event.EventHandler;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

/**
 * Calculates the value for energy channels in [Wh_Σ] from Power values in [W].
 *
 * <p>
 * This is commonly used by SymmetricEss or ElectricityMeter to calculate energy
 * channels from power channels. To use it, you have to:
 *
 * <ol>
 * <li>Make your OpenemsComponent implement {@link TimedataProvider}:
 *
 * <pre>
 * public class ComponentImpl extends AbstractOpenemsComponent implements
 * OpenemsComponent, TimedataProvider, EventHandler {
 * </pre>
 *
 * <li>Add a @Reference to {@link Timedata}. It's a good idea to make this
 * reference 'dynamic', otherwise your component will not start if there is no
 * Timedata service configured.
 *
 * <pre>
 * &#64;Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
 * private volatile Timedata timedata = null;
 * </pre>
 *
 * <li>Add a private instance of {@link CalculateEnergyFromPower} for each
 * energy channel:
 *
 * <pre>
 * private final CalculateEnergyFromPower calculateEnergy = new CalculateEnergyFromPower(this,
 * 		SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);
 * </pre>
 *
 * <li>Call the {@link #update(Integer)} method on each cycle - e.g. via an
 * {@link EventHandler}.
 *
 * <pre>
 * this.calculateEnergy.update(power);
 * </pre>
 * </ol>
 */
public class CalculateEnergyFromPower {

	/**
	 * Available States.
	 *
	 * <p>
	 * IMPLEMENTATION NOTE: we are using a custom StateMachine here and not the
	 * generic implementation in 'io.openems.edge.common.statemachine', because one
	 * State-Machine per EnergyCalculator object is required, which is not possible
	 * in the generic static enum implementation.
	 */
	private static enum State {
		TIMEDATA_QUERY_NOT_STARTED, TIMEDATA_QUERY_IS_RUNNING, CALCULATE_ENERGY_OPERATION;
	}

	/**
	 * Keeps the current State.
	 */
	private State state = State.TIMEDATA_QUERY_NOT_STARTED;

	private final TimedataProvider component;

	/**
	 * Keeps the target {@link ChannelId} of the Energy channel.
	 */
	private final ChannelId channelId;

	/**
	 * BaseCumulatedEnergy keeps the energy in [Wh]. It is initialized during
	 * TIMEDATA_QUERY_* states.
	 */
	private Long baseCumulatedEnergy = null;

	/**
	 * ContinuousCumulatedEnergy keeps the exceeding energy in [Wmsec]. It is
	 * continuously updated during CALCULATE_ENERGY_OPERATION state.
	 */
	private long continuousCumulatedEnergy = 0L;

	/**
	 * Keeps the timestamp of the last data.
	 */
	private Instant lastTimestamp = null;

	/**
	 * Keeps the last power value.
	 */
	private Integer lastPower = null;

	public CalculateEnergyFromPower(TimedataProvider component, ChannelId channelId) {
		this.component = component;
		this.channelId = channelId;
	}

	/**
	 * Calculate the Energy and update the Channel.
	 *
	 * @param power the latest power value in [W]
	 */
	public void update(Integer power) {
		switch (this.state) {
		case TIMEDATA_QUERY_NOT_STARTED:
			this.initializeCumulatedEnergyFromTimedata();
			break;

		case TIMEDATA_QUERY_IS_RUNNING:
			// wait for result
			break;

		case CALCULATE_ENERGY_OPERATION:
			this.calculateEnergy();
			break;
		}

		// keep last data for next run
		this.lastTimestamp = Instant.now();
		this.lastPower = power;
	}

	/**
	 * Initialize cumulated energy value from from Timedata service.
	 */
	private void initializeCumulatedEnergyFromTimedata() {
		var timedata = this.component.getTimedata();
		var componentId = this.component.id();
		if (timedata == null || componentId == null) {
			// Wait for Timedata service to appear or Component to be activated
			this.state = State.TIMEDATA_QUERY_NOT_STARTED;

		} else {
			// do not query Timedata twice
			this.state = State.TIMEDATA_QUERY_IS_RUNNING;

			timedata.getLatestValue(new ChannelAddress(this.component.id(), this.channelId.id()))
					.thenAccept(cumulatedEnergyOpt -> {
						this.state = State.CALCULATE_ENERGY_OPERATION;

						if (cumulatedEnergyOpt.isPresent()) {
							try {
								this.baseCumulatedEnergy = TypeUtils.getAsType(OpenemsType.LONG,
										cumulatedEnergyOpt.get());
							} catch (IllegalArgumentException e) {
								this.baseCumulatedEnergy = 0L;
							}
						} else {
							this.baseCumulatedEnergy = 0L;
						}
					});
		}
	}

	/**
	 * Calculate the cumulated energy.
	 */
	private void calculateEnergy() {
		if (this.lastTimestamp == null || this.lastPower == null || this.baseCumulatedEnergy == null) {
			// data is not available

		} else {
			// calculate duration since last value
			var duration /* [msec] */ = Duration.between(this.lastTimestamp, Instant.now()).toMillis();

			// calculate energy since last run in [Wmsec]
			var continuousEnergy /* [Wmsec] */ = this.lastPower /* [W] */ * duration /* [msec] */;

			// add to continuous cumulated energy
			this.continuousCumulatedEnergy += continuousEnergy;

			// Update base energy if 1 Wh passed
			if (this.continuousCumulatedEnergy > 3_600_000 /* 1 Wh */) {
				this.baseCumulatedEnergy += this.continuousCumulatedEnergy / 3_600_000;
				this.continuousCumulatedEnergy %= 3_600_000;
			}
		}

		// update 'cumulatedEnergy'
		this.component.channel(this.channelId).setNextValue(this.baseCumulatedEnergy);
	}
}
