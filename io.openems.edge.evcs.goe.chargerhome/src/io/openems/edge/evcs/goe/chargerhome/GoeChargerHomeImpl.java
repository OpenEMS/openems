package io.openems.edge.evcs.goe.chargerhome;

import java.net.UnknownHostException;

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
@Component(name = "Evcs.Goe.ChargerHome", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class GoeChargerHomeImpl extends AbstractOpenemsComponent
		implements ManagedEvcs, Evcs, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(GoeChargerHomeImpl.class);
	private GoeApi goeapi = null;

	protected Config config;

	@Reference
	private EvcsPower evcsPower;

	private GoeChargerWorker worker;

	public GoeChargerHomeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				GoeChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws UnknownHostException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.channel(GoeChannelId.ALIAS).setNextValue(config.alias());
		this.config = config;
		this._setChargingType(ChargingType.AC);
		this._setPowerPrecision(230);

		if (config.enabled()) {
			this.goeapi = new GoeApi(this);
			this.worker = new GoeChargerWorker(this, this.goeapi);
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
			break;
		default:
			break;
		}

	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public String debugLog() {
		String chPoLimit = this.channel(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT).getNextValue().asString();
		return "Limit:" + chPoLimit + "|" + this.getStatus().getName() + "|" + this.getState() + "|"
				+ this.getChargePower();
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

}
