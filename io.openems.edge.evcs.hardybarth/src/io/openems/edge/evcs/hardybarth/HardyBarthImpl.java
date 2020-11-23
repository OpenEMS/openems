package io.openems.edge.evcs.hardybarth;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "io.openems.edge.evcs.hardybarth", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		} //
)
public class HardyBarthImpl extends AbstractOpenemsComponent
		implements OpenemsComponent, EventHandler, HardyBarth, Evcs {

	protected final Logger log = LoggerFactory.getLogger(HardyBarthImpl.class);
	private Config config = null;
	private HardyBarthApi api = null;
	private HardyBarthWorker worker = null;

	public HardyBarthImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				HardyBarth.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this._setChargingType(ChargingType.AC);

		if (config.enabled()) {
			this.api = new HardyBarthApi(config.ip());

			this.worker = new HardyBarthWorker(this, this.api);
			this.worker.activate(config.id());
			this.worker.triggerNextRun();
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();

		if (this.worker != null) {
			this.worker.deactivate();
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:

			this.worker.triggerNextRun();
			// TODO: Need to be set.
			// this._setChargePower(); // possible
			// this._setChargingstationCommunicationFailed(); // possible
			// this._setEnergySession(); // seems like only total energy given
			// this._setMaximumHardwarePower(); // possible
			// this._setMinimumHardwarePower(); // given by config
			// this._setPhases(); // could be calculated out of the power/L1 ... or maybe
			// given on basic/phase_count
			// this._setStatus(); // Missing infos: Plug: "unlocked", cp: "A",
			// contactor: "opened", pwm: "100"
			break;
		}
	}

	/**
	 * Debug Log.
	 * 
	 * <p>
	 * Logging only if the debug mode is enabled
	 * 
	 * @param message text that should be logged
	 */
	public void debugLog(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}
}
