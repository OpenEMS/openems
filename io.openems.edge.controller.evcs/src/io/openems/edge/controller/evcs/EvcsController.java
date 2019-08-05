package io.openems.edge.controller.evcs;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Evcs;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Evcs", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EvcsController extends AbstractOpenemsComponent implements Controller, OpenemsComponent, ModbusSlave {

	private static final int RUN_EVERY_SECONDS = 5;

	// private final Logger log = LoggerFactory.getLogger(EvcsController.class);
	private final Clock clock;

	private boolean enabledCharging;
	private int forceChargeMinPower = 0;
	private int defaultChargeMinPower = 0;
	private ChargeMode chargeMode;
	private Priority priority;
	private String evcsId;
	private LocalDateTime lastRun = LocalDateTime.MIN;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected Sum sum;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CHARGE_MODE(Doc.of(ChargeMode.values()) //
				.initialValue(ChargeMode.FORCE_CHARGE) //
				.text("Configured Charge-Mode")), //
		FORCE_CHARGE_MINPOWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).text("Minimum value for the force charge")),
		DEFAULT_CHARGE_MINPOWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text("Minimum value for a default charge")),
		PRIORITY(Doc.of(Priority.values()).initialValue(Priority.CAR).text("Which component getting preferred")), //
		ENABLED_CHARGING(Doc.of(OpenemsType.BOOLEAN).text("Aktivates or deaktivates the Charging"));//

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public EvcsController() {
		this(Clock.systemDefaultZone());
	}

	protected EvcsController(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
		this.clock = clock;
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.enabledCharging = config.enabledCharging();
		this.forceChargeMinPower = Math.max(0, config.forceChargeMinPower()); // at least '0'
		this.defaultChargeMinPower = Math.max(0, config.defaultChargeMinPower());
		this.chargeMode = config.chargeMode();
		this.priority = config.priority();
		this.evcsId = config.evcs_id();

		switch (chargeMode) {
		case EXCESS_POWER:
			this.channel(ChannelId.DEFAULT_CHARGE_MINPOWER).setNextValue(defaultChargeMinPower);
			break;
		case FORCE_CHARGE:
			this.channel(ChannelId.FORCE_CHARGE_MINPOWER).setNextValue(forceChargeMinPower);
			break;
		}
		this.channel(ChannelId.CHARGE_MODE).setNextValue(chargeMode);
		this.channel(ChannelId.PRIORITY).setNextValue(priority);
		this.channel(ChannelId.ENABLED_CHARGING).setNextValue(enabledCharging);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		ManagedEvcs evcs = this.componentManager.getComponent(this.evcsId);

		Optional<Integer> maxChargePower = evcs.getMaximumPower().value().asOptional();

		// If a maximum charge power is defined.
		// The calculated charge power must be lower then this
		if (maxChargePower.isPresent()) {
			if (maxChargePower.get() < 1380) {
				return;
			}
		}

		// Executes only if charging is enabled
		if (!this.enabledCharging) {
			evcs.setChargePower().setNextWriteValue(0);
			return;
		}

		// Execute only every ... minutes
		if (this.lastRun.plusSeconds(RUN_EVERY_SECONDS).isAfter(LocalDateTime.now(this.clock))) {
			return;
		}

		int nextChargePower = 0;
		int nextMinPower = 0;

		switch (this.chargeMode) {
		case EXCESS_POWER:

			switch (priority) {
			case CAR:

				nextChargePower = nextChargePower_PvMinusConsumtion();
				break;

			case STORAGE:

				int storageSoc = this.sum.getEssSoc().value().orElse(0);

				if (storageSoc > 97) {
					nextChargePower = nextChargePower_PvMinusConsumtion();
				} else {
					nextChargePower = 0;
				}
				break;
			}
			nextMinPower = defaultChargeMinPower;
			break;

		case FORCE_CHARGE:
			nextChargePower = nextMinPower = forceChargeMinPower;
			break;
		}

		// test min-Power
		if (nextChargePower < nextMinPower) {
			nextChargePower = nextMinPower;
		}

		// Charge not more then the maximum if there is one present
		if (maxChargePower.isPresent()) {
			evcs.setChargePowerRequest().setNextWriteValue(nextChargePower);
		} else {
			// set charge power
			evcs.setChargePower().setNextWriteValue(nextChargePower);
		}
		lastRun = LocalDateTime.now();

	}

	/**
	 * Calculates the next charging power, depending on the current PV production
	 * and house consumption
	 * 
	 * @return
	 * @throws OpenemsNamedException
	 */
	private int nextChargePower_PvMinusConsumtion() throws OpenemsNamedException {
		int nextChargePower;

		ManagedEvcs evcs = this.componentManager.getComponent(this.evcsId);

		int buyFromGrid = this.sum.getGridActivePower().value().orElse(0);
		int essDischarge = this.sum.getEssActivePower().value().orElse(0);
		int evcsCharge = evcs.getChargePower().value().orElse(0);

		nextChargePower = evcsCharge - buyFromGrid - essDischarge;

		Channel<Integer> minChannel = evcs.channel(Evcs.ChannelId.MINIMUM_HARDWARE_POWER);
		if (nextChargePower < minChannel.value().orElse(0)) { /* charging under 6A isn't possible */
			nextChargePower = 0;
		}
		return nextChargePower;
	}

	@Override
	protected void logDebug(Logger log, String message) {
		super.logDebug(log, message);
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(OpenemsComponent.getModbusSlaveNatureTable(accessMode));
	}
}
