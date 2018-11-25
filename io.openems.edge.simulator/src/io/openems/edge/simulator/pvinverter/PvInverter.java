package io.openems.edge.simulator.pvinverter;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.pvinverter.api.SymmetricPvInverter;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;

import java.io.IOException;
import java.util.Optional;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Simulator.PvInverter", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class PvInverter extends AbstractOpenemsComponent
		implements SymmetricPvInverter, SymmetricMeter, OpenemsComponent, EventHandler {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		SIMULATED_ACTIVE_POWER(new Doc().unit(Unit.WATT));
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected SimulatorDatasource datasource;

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.service_pid(), config.id(), config.enabled());

		// update filter for 'datasource'
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "datasource", config.datasource_id())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public PvInverter() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.updateChannels();
			break;
		}
	}

	private void updateChannels() {
		/*
		 * get and store Simulated Active Power
		 */
		int simulatedActivePower = this.datasource.getValue(OpenemsType.INTEGER, "ActivePower");
		this.channel(ChannelId.SIMULATED_ACTIVE_POWER).setNextValue(simulatedActivePower);

		// Apply Active Power Limit
		Optional<Integer> activePowerLimitOpt = this.getActivePowerLimit().value().asOptional();
		if (activePowerLimitOpt.isPresent()) {
			int activePowerLimit = activePowerLimitOpt.get();
			simulatedActivePower = Math.min(simulatedActivePower, activePowerLimit);
		}

		this.getActivePower().setNextValue(simulatedActivePower);
	}

	@Override
	public String debugLog() {
		return this.getActivePower().value().asString();
	}
}
