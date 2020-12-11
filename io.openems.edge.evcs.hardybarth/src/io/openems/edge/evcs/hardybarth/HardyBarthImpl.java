package io.openems.edge.evcs.hardybarth;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
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
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;

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
		implements OpenemsComponent, EventHandler, HardyBarth, Evcs, ManagedEvcs {

	protected final Logger log = LoggerFactory.getLogger(HardyBarthImpl.class);
	protected HardyBarthApi api;
	private Config config;
	final private HardyBarthReadWorker readWorker = new HardyBarthReadWorker(this);
	final private HardyBarthWriteHandler writeHandler = new HardyBarthWriteHandler(this);
	private boolean firmwareUpdated = false;
	protected boolean masterEVCS = true;

	@Reference
	private EvcsPower evcsPower;

	public HardyBarthImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				HardyBarth.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this._setChargingType(ChargingType.AC);
		this._setMinimumHardwarePower(config.minHwCurrent() * 3 * 230);
		this._setMaximumHardwarePower(config.maxHwCurrent() * 3 * 230);

		if (config.enabled()) {
			this.api = new HardyBarthApi(config.ip(), this);
			this.readWorker.activate(config.id());
			this.readWorker.triggerNextRun();
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();

		if (this.readWorker != null) {
			this.readWorker.deactivate();
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:

			this.readWorker.triggerNextRun();

			// handle writes
			this.writeHandler.run();

			if (!this.firmwareUpdated) {
				// TODO: intelligent firmware update
				// try {
				// Update Firmware
				// this.api.sendPutRequest("/api/secc", "salia/updatefirmware", "http://moon.echarge.de/firmware/stable/");
				// this.firmwareUpdated = true;
				// } catch (OpenemsNamedException e) {
				// 		e.printStackTrace();
				// }
			}
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

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}
}
