package io.openems.edge.ess.cluster;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.calculate.CalculateAverage;
import io.openems.edge.common.channel.calculate.CalculateIntegerSum;
import io.openems.edge.common.channel.calculate.CalculateLongSum;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.CalculateGridMode;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.Cluster", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		})
public class EssCluster extends AbstractOpenemsComponent implements ManagedAsymmetricEss, AsymmetricEss,
		ManagedSymmetricEss, SymmetricEss, MetaEss, OpenemsComponent, EventHandler, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(EssCluster.class);

	@Reference
	private Power power = null;

	@Reference
	protected ComponentManager componentManager;

	private Config config;

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

	public EssCluster() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ManagedAsymmetricEss.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
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
			// TODO calculate channel values using onChangeListeners; do avoid possible lag
			// of one Cycle
			this.calculateChannelValues();
			break;
		}
	}

	/**
	 * Calculates the sum-value for each Channel.
	 */
	private void calculateChannelValues() {
		final CalculateAverage soc = new CalculateAverage();
		final CalculateIntegerSum capacity = new CalculateIntegerSum();
		final CalculateGridMode gridMode = new CalculateGridMode();
		final CalculateIntegerSum activePower = new CalculateIntegerSum();

		final CalculateIntegerSum allowedChargePower = new CalculateIntegerSum();
		final CalculateIntegerSum allowedDischargePower = new CalculateIntegerSum();

		final CalculateIntegerSum reactivePower = new CalculateIntegerSum();
		final CalculateIntegerSum maxApparentPower = new CalculateIntegerSum();
		final CalculateLongSum activeChargeEnergy = new CalculateLongSum();
		final CalculateLongSum activeDischargeEnergy = new CalculateLongSum();

		final CalculateIntegerSum activePowerL1 = new CalculateIntegerSum();
		final CalculateIntegerSum reactivePowerL1 = new CalculateIntegerSum();
		final CalculateIntegerSum activePowerL2 = new CalculateIntegerSum();
		final CalculateIntegerSum reactivePowerL2 = new CalculateIntegerSum();
		final CalculateIntegerSum activePowerL3 = new CalculateIntegerSum();
		final CalculateIntegerSum reactivePowerL3 = new CalculateIntegerSum();

		for (String essId : this.config.ess_ids()) {
			SymmetricEss ess;
			try {
				ess = this.componentManager.getComponent(essId);
			} catch (OpenemsNamedException e) {
				this.logError(this.log, e.getMessage());
				continue;
			}

			soc.addValue(ess.getSocChannel());
			capacity.addValue(ess.getCapacityChannel());
			gridMode.addValue(ess.getGridModeChannel());
			activePower.addValue(ess.getActivePowerChannel());

			reactivePower.addValue(ess.getReactivePowerChannel());
			maxApparentPower.addValue(ess.getMaxApparentPowerChannel());
			activeChargeEnergy.addValue(ess.getActiveChargeEnergyChannel());
			activeDischargeEnergy.addValue(ess.getActiveDischargeEnergyChannel());

			if (ess instanceof AsymmetricEss) {
				AsymmetricEss e = (AsymmetricEss) ess;
				activePowerL1.addValue(e.getActivePowerL1Channel());
				reactivePowerL1.addValue(e.getReactivePowerL1Channel());
				activePowerL2.addValue(e.getActivePowerL2Channel());
				reactivePowerL2.addValue(e.getReactivePowerL2Channel());
				activePowerL3.addValue(e.getActivePowerL3Channel());
				reactivePowerL3.addValue(e.getReactivePowerL3Channel());
			}
		}

		// Setting the allowed charge and discharge power
		for (String essId : this.config.ess_ids()) {
			ManagedSymmetricEss ess;
			try {
				ess = this.componentManager.getComponent(essId);
			} catch (OpenemsNamedException e) {
				this.logError(this.log, e.getMessage());
				continue;
			}

			allowedChargePower.addValue(ess.getAllowedChargePowerChannel());
			allowedDischargePower.addValue(ess.getAllowedDischargePowerChannel());
		}

		// Set values
		this._setSoc(soc.calculateRounded());
		this._setCapacity(capacity.calculate());
		this._setGridMode(gridMode.calculate());
		this._setActivePower(activePower.calculate());
		this._setReactivePower(reactivePower.calculate());
		this._setMaxApparentPower(maxApparentPower.calculate());
		this._setActiveChargeEnergy(activeChargeEnergy.calculate());
		this._setActiveDischargeEnergy(activeDischargeEnergy.calculate());

		this._setAllowedChargePower(allowedChargePower.calculate());
		this._setAllowedDischargePower(allowedDischargePower.calculate());

		this._setActivePowerL1(activePowerL1.calculate());
		this._setReactivePowerL1(reactivePowerL1.calculate());
		this._setActivePowerL2(activePowerL2.calculate());
		this._setReactivePowerL2(reactivePowerL2.calculate());
		this._setActivePowerL3(activePowerL3.calculate());
		this._setReactivePowerL3(reactivePowerL3.calculate());
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsException {
		throw new OpenemsException("EssClusterImpl.applyPower() should never be called.");
	}

	@Override
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) throws OpenemsException {
		throw new OpenemsException("EssClusterImpl.applyPower() should never be called.");
	}

	@Override
	public int getPowerPrecision() {
		// TODO kleinstes gemeinsames Vielfaches von PowerPrecision
		return 0;
	}

	@Override
	public synchronized String[] getEssIds() {
		return this.config.ess_ids();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode), //
				AsymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedAsymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(EssCluster.class, accessMode, 300) //
						.build());
	}
}
