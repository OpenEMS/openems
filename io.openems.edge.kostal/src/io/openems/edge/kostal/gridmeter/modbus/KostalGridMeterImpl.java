package io.openems.edge.kostal.gridmeter.modbus;

import static io.openems.edge.bridge.modbus.api.element.WordOrder.LSWMSW;

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
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(
		//
		name = "Grid-Meter.Kostal.KSEM", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=GRID", //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class KostalGridMeterImpl extends AbstractOpenemsModbusComponent
		implements
			KostalGridMeter,
			ElectricityMeter,
			ModbusComponent,
			OpenemsComponent,
			TimedataProvider,
			EventHandler,
			ModbusSlave {

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(
			this, ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(
			this, ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	public KostalGridMeterImpl() {
		super(
				//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				KostalGridMeter.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config)
			throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(),
				config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //

//				// Power of each backup up phase
//				new FC3ReadRegistersTask(40072, Priority.HIGH, //
//						m(ElectricityMeter.ChannelId.CURRENT_L1,
//								new SignedWordElement(40072)), //
//						m(ElectricityMeter.ChannelId.CURRENT_L2,
//								new SignedWordElement(40073)), //
//						m(ElectricityMeter.ChannelId.CURRENT_L3,
//								new SignedWordElement(40074)), //
//						m(KostalGridMeter.ChannelId.SCALE_FACTOR_CURRENT,
//								new SignedWordElement(40075)), //
//						//
//						new DummyRegisterElement(40076),
//						m(ElectricityMeter.ChannelId.VOLTAGE_L1,
//								new SignedWordElement(40077)), //
//						m(ElectricityMeter.ChannelId.VOLTAGE_L2,
//								new SignedWordElement(40078)), //
//						m(ElectricityMeter.ChannelId.VOLTAGE_L3,
//								new SignedWordElement(40079)), //
//
//						//
//						new DummyRegisterElement(40080, 40083),
//						m(KostalGridMeter.ChannelId.SCALE_FACTOR_VOLTAGE,
//								new SignedWordElement(40084)), //
//						m(ElectricityMeter.ChannelId.FREQUENCY,
//								new SignedWordElement(40085)), //
//						m(KostalGridMeter.ChannelId.SCALE_FACTOR_FREQUENCY,
//								new SignedWordElement(40086)), //
//						//
//						new DummyRegisterElement(40087),
//						// m(ElectricityMeter.ChannelId.ACTIVE_POWER,
//						// new SignedWordElement(40087)), //
//						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1,
//								new SignedWordElement(40088)), //
//						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2,
//								new SignedWordElement(40089)), //
//						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3,
//								new SignedWordElement(40090)),
//						m(KostalGridMeter.ChannelId.SCALE_FACTOR_POWER,
//								new SignedWordElement(40091)),
//						//
//						new DummyRegisterElement(40092, 40106),
//						m(KostalGridMeter.ChannelId.REAL_EXPORTED_ENERGY,
//								new UnsignedDoublewordElement(40107)),
//						//
//						new DummyRegisterElement(40109, 40114),
//						m(KostalGridMeter.ChannelId.REAL_IMPORTED_ENERGY,
//								new UnsignedDoublewordElement(40115)),
//						new DummyRegisterElement(40117, 40122),
//						m(KostalGridMeter.ChannelId.SCALE_FACTOR_ENERGY,
//								new SignedWordElement(40123))),
				new FC3ReadRegistersTask(40972, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER,
								new SignedDoublewordElement(40972)) //
				));
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
			case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE :
				//this.applyScaleFactors();
				break;
			case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE :
				this.calculateEnergy();
				break;
		}
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// Calculate Energy
		var activePower = this.getActivePower().get();
		if (activePower == null) {
			// Not available
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower < 0) {
			this.calculateProductionEnergy.update(activePower * -1);
			this.calculateConsumptionEnergy.update(0);
		} else {
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(activePower);
		}
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void applyScaleFactors() {
		// scale values
		var scaleVoltage = this.getScaleFactorVoltageValue();
		var scaleCurrent = this.getScaleFactorCurrentValue();
		// var scalePower = this.getScaleFactorPowerValue();
		var scaleFrequency = this.getScaleFactorFrequencyValue();
		var scaleEnergy = this.getScaleFactorEnergyValue();

		_setVoltageL1(getVoltageL1().get()
				* (int) Math.round(Math.pow(10, scaleVoltage * 1000)));
		_setVoltageL2(getVoltageL2().get()
				* (int) Math.round(Math.pow(10, scaleVoltage * 1000)));
		_setVoltageL3(getVoltageL3().get()
				* (int) Math.round(Math.pow(10, scaleVoltage * 1000)));
		_setCurrentL1(getCurrentL1().get()
				* (int) Math.round(Math.pow(10, scaleCurrent * 1000)));
		_setCurrentL2(getCurrentL1().get()
				* (int) Math.round(Math.pow(10, scaleCurrent * 1000)));
		_setCurrentL3(getCurrentL1().get()
				* (int) Math.round(Math.pow(10, scaleCurrent * 1000)));
		// _setActivePower(getActivePower().get()
		// * (int) Math.round(Math.pow(10, scalePower)));
		// _setActivePowerL1(getActivePowerL1().get()
		// * (int) Math.round(Math.pow(10, scalePower)));
		// _setActivePowerL2(getActivePowerL1().get()
		// * (int) Math.round(Math.pow(10, scalePower)));
		// _setActivePowerL3(getActivePowerL1().get()
		// * (int) Math.round(Math.pow(10, scalePower)));
		_setRealExportedEnergy(getRealExportedEnergyValue()
				* Math.round(Math.pow(10, scaleEnergy)));
		_setRealImportedEnergy(getRealImportedEnergyValue()
				* Math.round(Math.pow(10, scaleEnergy)));
		_setFrequency(getFrequency().get()
				* (int) Math.round(Math.pow(10, scaleFrequency)));

	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable
						.of(KostalGridMeter.class, accessMode, 100).build() //
		);
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID;
	}
}
