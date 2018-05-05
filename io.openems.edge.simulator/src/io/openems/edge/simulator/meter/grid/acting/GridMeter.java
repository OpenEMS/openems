package io.openems.edge.simulator.meter.grid.acting;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

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

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.controllerexecutor.EdgeEventConstants;
import io.openems.edge.ess.symmetric.api.SymmetricEss;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.asymmetric.api.AsymmetricMeter;
import io.openems.edge.meter.symmetric.api.SymmetricMeter;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;
import io.openems.edge.simulator.meter.MeterUtils;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Simulator.GridMeter.Acting", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class GridMeter extends AbstractOpenemsComponent
		implements SymmetricMeter, AsymmetricMeter, OpenemsComponent, EventHandler {

	// private final Logger log = LoggerFactory.getLogger(GridMeter.class);

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

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	private volatile List<SymmetricEss> symmetricEsss = new CopyOnWriteArrayList<>();

	// @Reference(policy = ReferencePolicy.DYNAMIC, policyOption =
	// ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	// private List<Meter> productionMeters = new CopyOnWriteArrayList<>();

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.service_pid(), config.id(), config.enabled());

		// update filter for 'datasource'
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "Datasource", config.datasource_id())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public GridMeter() {
		MeterUtils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID;
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

		/*
		 * Calculate Active Power
		 */
		int activePower = simulatedActivePower;
		for (SymmetricEss ess : this.symmetricEsss) {
			Optional<Integer> essPowerOpt = ess.getActivePower().getActiveValueOpt();
			if (essPowerOpt.isPresent()) {
				activePower -= essPowerOpt.get();
			}
		}

		this.getActivePower().setNextValue(activePower);
		this.getActivePowerL1().setNextValue(activePower / 3);
		this.getActivePowerL2().setNextValue(activePower / 3);
		this.getActivePowerL3().setNextValue(activePower / 3);
		this.getProductionActivePower().setNextValue(activePower);
		this.getProductionActivePowerL1().setNextValue(activePower / 3);
		this.getProductionActivePowerL2().setNextValue(activePower / 3);
		this.getProductionActivePowerL3().setNextValue(activePower / 3);
		this.getConsumptionActivePower().setNextValue(0);
		this.getConsumptionActivePowerL1().setNextValue(0);
		this.getConsumptionActivePowerL2().setNextValue(0);
		this.getConsumptionActivePowerL3().setNextValue(0);
	}

	@Override
	public String debugLog() {
		return this.getActivePower().format();
	}
}
