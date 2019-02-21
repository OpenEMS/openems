package io.openems.edge.controller.evcs.fixactivepower;

import java.time.Clock;
import java.time.LocalDateTime;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.evcs.api.Evcs;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.evcs.FixActivePower", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class EvcsFixActivePower extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Clock clock;
	private static final int RUN_EVERY_MINUTES = 1;
	private LocalDateTime lastRun = LocalDateTime.MIN;

	@Reference
	protected ComponentManager componentManager;

	private Config config;

	public EvcsFixActivePower() {
		this(Clock.systemDefaultZone());
	}

	protected EvcsFixActivePower(Clock clock) {
		this.clock = clock;
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

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

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.enabled());
		this.config = config;
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

		Evcs evcs = this.componentManager.getComponent(this.config.evcs_id());

		// set charge power
		evcs.setChargePower().setNextWriteValue(this.config.power());

	}

}
