package io.openems.edge.victron.ess;

import java.util.ArrayList;
import java.util.List;

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
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.ShortWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSinglePhaseEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SinglePhase;
import io.openems.edge.ess.api.SinglePhaseEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;
import io.openems.edge.victron.battery.VictronBattery;
import io.openems.edge.victron.charger.VictronDCCharger;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Victron.Multiplus2GX", //
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		})
public class VictronEssImpl extends AbstractOpenemsModbusComponent
		implements VictronEss, ManagedSinglePhaseEss, ManagedAsymmetricEss, ManagedSymmetricEss, AsymmetricEss, 
		SymmetricEss, HybridEss, OpenemsComponent, ModbusComponent, TimedataProvider, EventHandler {

	private static final int POWER_PRECISION = 1;

	private Config config;
	
	private List<VictronDCCharger> chargers = new ArrayList<>();
	
	@Reference
	protected ConfigurationAdmin cm;
	
	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private Power power;
	
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected VictronBattery battery;

	private final CalculateEnergyFromPower calculateAcChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateAcDischargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateDcChargeEnergy = new CalculateEnergyFromPower(this,
			HybridEss.ChannelId.DC_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateDcDischargeEnergy = new CalculateEnergyFromPower(this,
			HybridEss.ChannelId.DC_DISCHARGE_ENERGY);
	
	public VictronEssImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ManagedAsymmetricEss.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				SinglePhaseEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				HybridEss.ChannelId.values(), //
				ManagedSinglePhaseEss.ChannelId.values(), //
				VictronEss.ChannelId.values() //
		);
		AsymmetricEss.initializePowerSumChannels(this);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}
	
	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id0())) {
			return;
		}
		
		// update filter for 'Ess'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Battery", config.battery_id())) {
			return; 
		}
		
		this.config = config;
	}
	
	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}
	
	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				AsymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedAsymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode) //
		);
	}

	@Override
	public Integer getSurplusPower() {
		Integer chargerActualPower = 0;
		Integer chargeMaxAllowedPower = this.calculateAllowedDcChargePower() * -1;
		
		for (VictronDCCharger charger : chargers) {
			chargerActualPower += charger.getActualPower().get();
		}
		
		if (chargerActualPower > chargeMaxAllowedPower) {
			return chargerActualPower - chargeMaxAllowedPower;
		} else {
			return null;
		}
	}

	@Override
	public Power getPower() {
		return power;
	}

	@Override
	public int getPowerPrecision() {
		return POWER_PRECISION;
	}

	@Override
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) throws OpenemsNamedException {
		ShortWriteChannel setActivePowerL1Channel = this.channel(VictronEss.ChannelId.SET_ACTIVE_POWER_L1);
		setActivePowerL1Channel.setNextWriteValue((short) (activePowerL1/getPowerPrecision() * -1));
		ShortWriteChannel setActivePowerL2Channel = this.channel(VictronEss.ChannelId.SET_ACTIVE_POWER_L2);
		setActivePowerL2Channel.setNextWriteValue((short) (activePowerL2/getPowerPrecision() * -1));
		ShortWriteChannel setActivePowerL3Channel = this.channel(VictronEss.ChannelId.SET_ACTIVE_POWER_L3);
		setActivePowerL3Channel.setNextWriteValue((short) (activePowerL3/getPowerPrecision() * -1));
	}

	@Override
	public void addCharger(VictronDCCharger charger) {
		this.chargers.add(charger);
	}
	
	@Override
	public void removeCharger(VictronDCCharger charger) {
		this.chargers.remove(charger);
	}

	@Override
	public String getModbusBridgeId() {
		return config.modbus_id0();
	}

	@Override
	public Integer getUnitId() {
		return config.modbusUnitId();
	}

	@Override
	public SinglePhase getPhase() {
		return config.phase();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		
		return new ModbusProtocol(this, 
				new FC3ReadRegistersTask(12, Priority.LOW, 
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L1, new SignedWordElement(12), ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(true)),
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L2, new SignedWordElement(13), ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(true)),
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L3, new SignedWordElement(14), ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(true)),
						new DummyRegisterElement(15,28),
						m(SymmetricEss.ChannelId.GRID_MODE, new UnsignedWordElement(29), VICTRON_GRID_MODE_CONVERTER)),
				new FC6WriteRegisterTask(37,
						m(VictronEss.ChannelId.SET_ACTIVE_POWER_L1, new SignedWordElement(37))),
				new FC6WriteRegisterTask(40,
						m(VictronEss.ChannelId.SET_ACTIVE_POWER_L2, new SignedWordElement(40))),
				new FC6WriteRegisterTask(41,
						m(VictronEss.ChannelId.SET_ACTIVE_POWER_L3, new SignedWordElement(41))));
	}
	
	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|Allowed:"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER).value().asStringWithoutUnit() + ";"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER).value().asString() //
				+ "|" + this.getGridModeChannel().value().asOptionString();
	}
	
	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.getSocChannel().setNextValue(battery.getSoc());
			this.getCapacityChannel().setNextValue(battery.getCapacity());
			this.getAllowedDischargePowerChannel().setNextValue(calculateAllowedDischargePower());
			this.getAllowedChargePowerChannel().setNextValue(calculateAllowedChargePower());
			this.getMaxApparentPowerChannel().setNextValue(calculateMaxApparentPower());
			this.calculateEnergy();
			break;
		}
	}

	private static final ElementToChannelConverter VICTRON_GRID_MODE_CONVERTER = new ElementToChannelConverter(value -> {
		switch ((Integer) value) {
		case 0:
			return GridMode.ON_GRID;
		case 240:
			return GridMode.OFF_GRID;
		default:
			return GridMode.UNDEFINED;
		}
	});
	
	private Value<Integer> calculateMaxApparentPower() {
		Integer maxApparentPower = Math.max(calculateAllowedDischargePower().get() + 
				getDCChargerPower(), calculateAllowedChargePower().get());

		if (maxApparentPower > config.victron_installation().getApparentPowerLimit()) {
			maxApparentPower = config.victron_installation().getApparentPowerLimit();
		}
		
		return new Value<Integer>(getMaxApparentPowerChannel(), maxApparentPower);
	}
	
	private Integer calculateAllowedDcChargePower() {
		Integer maxChargeCurrent = battery.getChargeMaxCurrent().get();
		Integer voltage = battery.getChargeMaxVoltage().get();
		
		Integer maxDcChargePower = maxChargeCurrent*voltage * -1;
		
		return maxDcChargePower;
	}
	
	private Value<Integer> calculateAllowedChargePower() {
		Integer maxChargeCurrent = battery.getChargeMaxCurrent().get();
		Integer voltage = battery.getChargeMaxVoltage().get();
		
		Integer maxChargePower = maxChargeCurrent*voltage * -1;
		
		if (maxChargePower < config.victron_installation().getAcInputLimit()) {
			maxChargePower = config.victron_installation().getAcInputLimit();
		}
		
		return new Value<Integer>(getAllowedChargePowerChannel(), maxChargePower);
	}
	
	private Value<Integer> calculateAllowedDischargePower() {
		Integer maxDischargeCurrent = battery.getDischargeMaxCurrent().get();
		Integer voltage = battery.getDischargeMinVoltage().get();
		
		Integer dcChargerPower = getDCChargerPower();
		Integer maxDischargePower = maxDischargeCurrent*voltage + (dcChargerPower > config.dcFeedInThreshold() ? dcChargerPower : 0);
		
		if (maxDischargePower > config.victron_installation().getAcOutputLimit()) {
			maxDischargePower = config.victron_installation().getAcOutputLimit();
		}
		
		return new Value<Integer>(getAllowedDischargePowerChannel(), maxDischargePower);
	}
	
	private Integer getDCChargerPower() {
		var dcDischargePower = 0;
		for (VictronDCCharger charger : this.chargers) {
			dcDischargePower = TypeUtils.sum(dcDischargePower,
					charger.getActualPowerChannel().getNextValue().get());
		}
		return dcDischargePower;
	}

	private Value<Integer> calculateDcDischargePower() {
		Integer dcDischargePower = (battery.getCurrent().get()) * battery.getVoltage().get() * -1;
		return new Value<Integer>(getDcDischargePowerChannel(), dcDischargePower);
	}
	
	private void calculateEnergy() {
		/*
		 * Calculate AC Energy
		 */
		var acActivePower = this.getActivePowerChannel().getNextValue().get();
		if (acActivePower == null) {
			// Not available
			this.calculateAcChargeEnergy.update(null);
			this.calculateAcDischargeEnergy.update(null);
		} else if (acActivePower > 0) {
			// Discharge
			this.calculateAcChargeEnergy.update(0);
			this.calculateAcDischargeEnergy.update(acActivePower);
		} else {
			// Charge
			this.calculateAcChargeEnergy.update(acActivePower * -1);
			this.calculateAcDischargeEnergy.update(0);
		}
		/*
		 * Calculate DC Power and Energy
		 */
		var dcDischargePower = calculateDcDischargePower().get();
		this._setDcDischargePower(dcDischargePower);

		if (dcDischargePower == null) {
			// Not available
			this.calculateDcChargeEnergy.update(null);
			this.calculateDcDischargeEnergy.update(null);
		} else if (dcDischargePower > 0) {
			// Discharge
			this.calculateDcChargeEnergy.update(0);
			this.calculateDcDischargeEnergy.update(dcDischargePower);
		} else {
			// Charge
			this.calculateDcChargeEnergy.update(dcDischargePower * -1);
			this.calculateDcDischargeEnergy.update(0);
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
