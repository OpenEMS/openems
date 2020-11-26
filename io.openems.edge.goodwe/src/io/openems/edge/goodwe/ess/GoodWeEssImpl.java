package io.openems.edge.goodwe.ess;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.goodwe.common.AbstractGoodWe;
import io.openems.edge.goodwe.common.GoodWe;
import io.openems.edge.goodwe.common.applypower.ApplyPowerStateMachine;
import io.openems.edge.goodwe.common.applypower.Context;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "GoodWe.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
) //
public class GoodWeEssImpl extends AbstractGoodWe implements GoodWeEss, GoodWe, HybridEss, ManagedSymmetricEss,
		SymmetricEss, OpenemsComponent, TimedataProvider, EventHandler {

	private final Logger log = LoggerFactory.getLogger(GoodWeEssImpl.class);

	private Config config;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Power power;

	private final CalculateEnergyFromPower calculateAcChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateAcDischargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);

	private final ApplyPowerStateMachine applyPowerStateMachine = new ApplyPowerStateMachine(
			ApplyPowerStateMachine.State.UNDEFINED);

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this._setCapacity(this.config.capacity());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public GoodWeEssImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				HybridEss.ChannelId.values(), //
				GoodWe.ChannelId.values(), //
				GoodWeEss.ChannelId.values() //
		);
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		Integer pvProduction = this.calculatePvProduction();
		int soc = this.getSoc().orElse(0);
		ApplyPowerStateMachine.State state = ApplyPowerStateMachine.evaluateState(this.getGoodweType(),
				config.readOnlyMode(), pvProduction, soc, activePower);

		// Store the current State
		this.channel(GoodWe.ChannelId.APPLY_POWER_STATE_MACHINE).setNextValue(state);

		// Prepare Context
		Context context = new Context(this, pvProduction, activePower);

		this.applyPowerStateMachine.forceNextState(state);
		this.applyPowerStateMachine.run(context); // apply the force next state
		this.applyPowerStateMachine.run(context); // execute correct handler

		IntegerWriteChannel emsPowerSetChannel = this.channel(GoodWe.ChannelId.EMS_POWER_SET);
		emsPowerSetChannel.setNextWriteValue(context.getEssPowerSet());

		EnumWriteChannel emsPowerModeChannel = this.channel(GoodWe.ChannelId.EMS_POWER_MODE);
		emsPowerModeChannel.setNextWriteValue(context.getNextPowerMode());
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|" + this.getGridModeChannel().value().asOptionString()//
				+ "|Allowed:" + this.getAllowedChargePower().asStringWithoutUnit() + ";"
				+ this.getAllowedDischargePower().asString();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.updatechannels();
			break;
		}
	}

	private void updatechannels() {
		/*
		 * Update ActivePower from P_BATTERY1 and chargers ACTUAL_POWER
		 */
		Integer productionPower = this.calculatePvProduction();
		final Channel<Integer> batteryPower = this.channel(GoodWe.ChannelId.P_BATTERY1);
		Integer activePower = TypeUtils.sum(productionPower, batteryPower.value().get());
		this._setActivePower(activePower);

		/*
		 * Calculate AC Energy
		 */
		if (activePower == null) {
			// Not available
			this.calculateAcChargeEnergy.update(null);
			this.calculateAcDischargeEnergy.update(null);
		} else if (activePower > 0) {
			// Discharge
			this.calculateAcChargeEnergy.update(0);
			this.calculateAcDischargeEnergy.update(activePower);
		} else {
			// Charge
			this.calculateAcChargeEnergy.update(activePower * -1);
			this.calculateAcDischargeEnergy.update(0);
		}

		/*
		 * Update Allowed charge and Allowed discharge
		 */

		Integer soc = this.getSoc().get();
		Integer maxBatteryPower = this.config.maxBatteryPower();

		Integer allowedCharge = null;
		Integer allowedDischarge = null;

		if (productionPower == null) {
			productionPower = 0;
		}

		if (soc == null) {

			allowedCharge = 0;
			allowedDischarge = 0;

		} else if (soc == 100) {

			allowedDischarge = maxBatteryPower + productionPower;
			allowedCharge = 0;

		} else if (soc > 0) {

			allowedDischarge = maxBatteryPower + productionPower;
			allowedCharge = maxBatteryPower;

		} else if (soc == 0) {

			allowedDischarge = productionPower;
			allowedCharge = maxBatteryPower;

		}

		// to avoid charging when production is greater than maximum battery power.
		if (allowedCharge < 0) {
			allowedCharge = 0;
		}

		this._setAllowedChargePower(TypeUtils.multiply(allowedCharge * -1));
		this._setAllowedDischargePower(allowedDischarge);
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@Override
	public Constraint[] getStaticConstraints() throws OpenemsNamedException {
		// Handle Read-Only mode -> no charge/discharge
		if (this.config.readOnlyMode()) {
			return new Constraint[] { //
					this.createPowerConstraint("Read-Only-Mode", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0), //
					this.createPowerConstraint("Read-Only-Mode", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) //
			};
		}

		return Power.NO_CONSTRAINTS;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public Integer getSurplusPower() {
		if (this.getSoc().orElse(0) < 99) {
			return null;
		}
		Integer productionPower = this.calculatePvProduction();
		if (productionPower == null || productionPower < 100) {
			return null;
		}
		return productionPower;
	}

}
