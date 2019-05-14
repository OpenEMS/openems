package io.openems.edge.simulator.ess.asymmetric.reacting;

import java.io.IOException;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Simulator.EssAsymmetric.Reacting", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS)
public class EssAsymmetric extends AbstractOpenemsComponent implements ManagedAsymmetricEss, AsymmetricEss,
		ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler, ModbusSlave {

	/**
	 * Current state of charge.
	 */
	private float soc = 0;

	/**
	 * Total configured capacity in Wh.
	 */
	private int capacity = 0;

	/**
	 * Configured max Apparent Power in VA.
	 */
	private int maxApparentPower = 0;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Reference
	private Power power;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected SimulatorDatasource datasource;

	@Reference
	protected ConfigurationAdmin cm;

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		// update filter for 'datasource'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "datasource", config.datasource_id())) {
			return;
		}

		this.getSoc().setNextValue(config.initialSoc());
		this.soc = config.initialSoc();
		this.capacity = config.capacity();
		this.maxApparentPower = config.maxApparentPower();
		this.getMaxApparentPower().setNextValue(config.maxApparentPower());
		this.getAllowedCharge().setNextValue(this.maxApparentPower * -1);
		this.getAllowedDischarge().setNextValue(this.maxApparentPower);
		this.getGridMode().setNextValue(config.gridMode());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public EssAsymmetric() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ManagedAsymmetricEss.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			this.updateChannels();
			break;
		}
	}

	private void updateChannels() {
		// nothing to do
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value().asString() //
				+ "|L:" + this.getActivePower().value().asString() //
				+ "|" + this.getGridMode().value().asOptionString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) throws OpenemsException {
		int activePower = activePowerL1 + activePowerL2 + activePowerL3;
		/*
		 * calculate State of charge
		 */
		float watthours = (float) activePower * this.datasource.getTimeDelta() / 3600;
		float socChange = watthours / this.capacity;
		this.soc -= socChange;
		if (this.soc > 100) {
			this.soc = 100;
		} else if (this.soc < 0) {
			this.soc = 0;
		}
		this.getSoc().setNextValue(this.soc);
		/*
		 * Apply Active/Reactive power to simulated channels
		 */
		this.getActivePowerL1().setNextValue(activePowerL1);
		this.getActivePowerL2().setNextValue(activePowerL2);
		this.getActivePowerL3().setNextValue(activePowerL3);
		this.getActivePower().setNextValue(activePower);

		int reactivePower = reactivePowerL1 + reactivePowerL2 + reactivePowerL3;
		this.getReactivePowerL1().setNextValue(reactivePowerL1);
		this.getReactivePowerL2().setNextValue(reactivePowerL2);
		this.getReactivePowerL3().setNextValue(reactivePowerL3);
		this.getReactivePower().setNextValue(reactivePower);
		/*
		 * Set AllowedCharge / Discharge based on SoC
		 */
		if (this.soc == 100) {
			this.getAllowedCharge().setNextValue(0);
		} else {
			this.getAllowedCharge().setNextValue(this.maxApparentPower * -1);
		}
		if (this.soc == 0) {
			this.getAllowedDischarge().setNextValue(0);
		} else {
			this.getAllowedDischarge().setNextValue(this.maxApparentPower);
		}
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode), //
				AsymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedAsymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(EssAsymmetric.class, accessMode, 300) //
						.build());
	}

}
