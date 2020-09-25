package io.openems.edge.simulator.evcs;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Simulator.Evcs", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class SimulatedEvcs extends AbstractOpenemsComponent
		implements ManagedEvcs, Evcs, OpenemsComponent, EventHandler {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SIMULATED_CHARGE_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	public SimulatedEvcs() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ChannelId.values() //

		);
	}

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected SimulatorDatasource datasource;

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		// update filter for 'datasource'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "datasource", config.datasource_id())) {
			return;
		}

		this._setMaximumHardwarePower(22800);
		this._setMinimumHardwarePower(6000);
		this._setPhases(3);
		this._setStatus(Status.CHARGING);

	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.updateChannels();
			break;
		}
	}

	private LocalDateTime lastUpdate = LocalDateTime.now();
	private double exactEnergySession = 0;

	private void updateChannels() {
		int simulatedChargePower = 0;

		Optional<Integer> chargePowerLimitOpt = this.getSetChargePowerLimitChannel().getNextWriteValueAndReset();
		if (chargePowerLimitOpt.isPresent()) {

			// copy write value to read value
			this._setSetChargePowerLimit(chargePowerLimitOpt.get());

			// get and store Simulated Charge Power
			simulatedChargePower = this.datasource.getValue(OpenemsType.INTEGER, "ActivePower");
			this.channel(ChannelId.SIMULATED_CHARGE_POWER).setNextValue(simulatedChargePower);

			// Apply Charge Limit
			int chargePowerLimit = chargePowerLimitOpt.get();
			simulatedChargePower = Math.min(simulatedChargePower, chargePowerLimit);
			this._setSetChargePowerLimit(chargePowerLimit);
		}

		this._setChargePower(simulatedChargePower);

		long timeDiff = ChronoUnit.MILLIS.between(this.lastUpdate, LocalDateTime.now());
		double energieTransfered = (timeDiff / 1000.0 / 60 / 60) * this.getChargePower().orElse(0);
		this.exactEnergySession = this.exactEnergySession + energieTransfered;
		this._setEnergySession((int) this.exactEnergySession);

		this.lastUpdate = LocalDateTime.now();
	}

	@Override
	public String debugLog() {
		return this.getChargePower().asString();
	}

}
