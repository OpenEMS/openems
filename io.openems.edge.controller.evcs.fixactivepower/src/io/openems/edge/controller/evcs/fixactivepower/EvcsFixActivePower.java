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
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.evcs.api.ManagedEvcs;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Evcs.FixActivePower", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class EvcsFixActivePower extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private static final int RUN_EVERY_MINUTES = 1;

	private final Clock clock;

	private LocalDateTime lastRun = LocalDateTime.MIN;
	private Config config;

	@Reference
	protected ComponentManager componentManager;

	public EvcsFixActivePower() {
		this(Clock.systemDefaultZone());
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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

	protected EvcsFixActivePower(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
		this.clock = clock;
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Override
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

		ManagedEvcs evcs = this.componentManager.getComponent(this.config.evcs_id());

		// set charge power
		evcs.setChargePowerLimit().setNextWriteValue(this.config.power());

	}

}
