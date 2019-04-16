package io.openems.edge.controller.evcs;

import java.time.Clock;
import java.time.LocalDateTime;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.evcs.api.Evcs;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Evcs", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EvcsController extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private static final int RUN_EVERY_MINUTES = 1;

	// private final Logger log = LoggerFactory.getLogger(EvcsController.class);
	private final Clock clock;

	private int forceChargeMinPower = 0;
	private int defaultChargeMinPower = 0;
	private ChargeMode chargeMode;
	private String evcsId;
	private LocalDateTime lastRun = LocalDateTime.MIN;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Sum sum;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CHARGE_MODE(Doc.of(ChargeMode.values()) //
				.initialValue(ChargeMode.FORCE_CHARGE) //
				.text("Configured Charge-Mode")), //
		FORCE_CHARGE_MINPOWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).text("Minimum value for the force charge")),
		DEFAULT_CHARGE_MINPOWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text("Minimum value for a default charge")); //

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
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.enabled());

		this.forceChargeMinPower = Math.max(0, config.forceChargeMinPower()); // at least '0'
		this.defaultChargeMinPower = Math.max(0, config.defaultChargeMinPower());
		this.chargeMode = config.chargeMode();

		switch (config.chargeMode()) {
		case EXCESS_POWER:
			this.channel(ChannelId.DEFAULT_CHARGE_MINPOWER).setNextValue(defaultChargeMinPower);
			break;
		case FORCE_CHARGE:
			this.channel(ChannelId.FORCE_CHARGE_MINPOWER).setNextValue(forceChargeMinPower);
			break;

		}
		this.channel(ChannelId.CHARGE_MODE).setNextValue(config.chargeMode());
		this.evcsId = config.evcs_id();

		// update filter for 'evcs'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "evcs", config.evcs_id())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		// Execute only every ... minutes
		if (this.lastRun.plusMinutes(RUN_EVERY_MINUTES).isAfter(LocalDateTime.now(this.clock))) {
			return;
		}

		Evcs evcs = this.componentManager.getComponent(this.evcsId);

		int nextChargePower = 0;
		int nextMinPower = 0;
		switch (this.chargeMode) {
		case EXCESS_POWER:
			int buyFromGrid = this.sum.getGridActivePower().value().orElse(0);
			int essDischarge = this.sum.getEssActivePower().value().orElse(0);
			int evcsCharge = evcs.getChargePower().value().orElse(0);
			nextChargePower = evcsCharge - buyFromGrid - essDischarge;
			if (nextChargePower < 1380 /* min 6A */ ) {
				nextChargePower = 0;
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

		// set charge power
		evcs.setChargePower().setNextWriteValue(nextChargePower);
	}

	@Override
	protected void logDebug(Logger log, String message) {
		super.logDebug(log, message);
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

}
