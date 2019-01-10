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
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.doc.Doc;
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

	private final static int RUN_EVERY_MINUTES = 1;

	private final Logger log = LoggerFactory.getLogger(EvcsController.class);
	private final Clock clock;

	private Config config;
	private LocalDateTime lastRun = LocalDateTime.MIN;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Sum sum;

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
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

	public EvcsController() {
		this(Clock.systemDefaultZone());
	}

	protected EvcsController(Clock clock) {
		this.clock = clock;
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.enabled());
		this.config = config;

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
	public void run() {
		// Execute only every ... minutes
		if (this.lastRun.plusMinutes(RUN_EVERY_MINUTES).isAfter(LocalDateTime.now(this.clock))) {
			return;
		}

		Evcs evcs = this.componentManager.getComponent(this.config.evcs_id());

		int nextChargePower = 0;
		switch (this.config.chargeMode()) {
		case DEFAULT:
			nextChargePower = //
					this.sum.getGridActivePower().value().orElse(0) // Sell-to-Grid Power
							+ this.sum.getEssActivePower().value().orElse(0) // ESS Charge Power
							+ evcs.getChargePower().value().orElse(0); // EVCS Charge Power
			break;

		case FORCE_CHARGE:
			nextChargePower = 22_000; // TODO intelligently find max Charge Power
			break;
		}

		// test min-Power
		if (nextChargePower < this.config.minPower()) {
			nextChargePower = this.config.minPower();
		}

		// set charge power
		try {
			evcs.setChargePower().setNextWriteValue(nextChargePower);
		} catch (OpenemsException e) {
			this.logError(this.log, e.getMessage());
		}
	}

}
