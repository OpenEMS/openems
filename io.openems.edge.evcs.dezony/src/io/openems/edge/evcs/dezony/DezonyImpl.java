package io.openems.edge.evcs.dezony;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.AbstractManagedEvcsComponent;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Phases;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Dezony", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
})
public class DezonyImpl extends AbstractManagedEvcsComponent
		implements OpenemsComponent, EventHandler, Dezony, Evcs, ManagedEvcs {

	private final Logger log = LoggerFactory.getLogger(DezonyImpl.class);
	private final DezonyReadWorker readWorker = new DezonyReadWorker(this);

	protected Config config;
	protected DezonyApi api;
	protected boolean masterEvcs = true;

	@Reference
	private EvcsPower evcsPower;

	public DezonyImpl() {
		super(OpenemsComponent.ChannelId.values(), Evcs.ChannelId.values(), ManagedEvcs.ChannelId.values(),
				Dezony.ChannelId.values());
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.config = config;
		this._setChargingType(ChargingType.AC);
		this._setFixedMinimumHardwarePower(
				config.minHwCurrent() / 1000 * DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue());
		this._setFixedMaximumHardwarePower(
				config.maxHwCurrent() / 1000 * DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue());
		this._setPowerPrecision(230);

		if (config.enabled()) {
			this.api = new DezonyApi(config.ip(), config.port(), this);
			this.readWorker.activate(config.id());
			this.readWorker.triggerNextRun();
		}
	}

	@Override
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

		super.handleEvent(event);

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.readWorker.triggerNextRun();

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
	public boolean getConfiguredDebugMode() {
		return this.config.debugMode();
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws OpenemsNamedException {
		this.api.enableCharging();

		final var current = (int) Math.round(power / (double) this.getPhasesAsInt() / DEFAULT_VOLTAGE);

		return this.setTarget(current);
	}

	@Override
	public boolean pauseChargeProcess() throws OpenemsNamedException {
		return this.api.disableCharging();
	}

	/**
	 * Set current target to the charger.
	 * 
	 * @param current current target in A
	 * @return boolean if the target was set
	 * @throws OpenemsNamedException on error
	 */
	private boolean setTarget(int current) throws OpenemsNamedException {
		return this.api.setCurrent(current).orElse("").equals("ok");
	}

	@Override
	public boolean applyDisplayText(String text) throws OpenemsException {
		return false;
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		return 30;
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return Math.round(this.config.minHwCurrent() / 1000f) * DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return Math.round(this.config.maxHwCurrent() / 1000f) * DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}
}
