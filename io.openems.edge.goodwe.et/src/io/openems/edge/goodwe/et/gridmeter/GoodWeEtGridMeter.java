package io.openems.edge.goodwe.et.gridmeter;

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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "GoodWe.ET.Grid-Meter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
				"type=GRID" //
		})
public class GoodWeEtGridMeter extends AbstractOpenemsModbusComponent
		implements AsymmetricMeter, SymmetricMeter, OpenemsComponent, TimedataProvider, EventHandler {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	public GoodWeEtGridMeter() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				GridMeterChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.unit_id(), this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //

				// active and reactive power
				new FC3ReadRegistersTask(36005, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new SignedWordElement(36005),
								ElementToChannelConverter.INVERT), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new SignedWordElement(36006),
								ElementToChannelConverter.INVERT), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new SignedWordElement(36007),
								ElementToChannelConverter.INVERT), //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedWordElement(36008),
								ElementToChannelConverter.INVERT),
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new SignedWordElement(36009),
								ElementToChannelConverter.INVERT)), //

				// Voltage, current and Grid Frequency of each phase
				new FC3ReadRegistersTask(35121, Priority.LOW, //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(35121),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(35122),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GridMeterChannelId.F_GRID_R, new UnsignedWordElement(35123),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						new DummyRegisterElement(35124, 35125), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(35126),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new UnsignedWordElement(35127),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GridMeterChannelId.F_GRID_S, new UnsignedWordElement(35128),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						new DummyRegisterElement(35129, 35130), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(35131),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new UnsignedWordElement(35132),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GridMeterChannelId.F_GRID_T, new UnsignedWordElement(35133),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)), //

				// Power factor and frequency
				new FC3ReadRegistersTask(36013, Priority.LOW, //
						m(GridMeterChannelId.METER_POWER_FACTOR, new UnsignedWordElement(36013),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedWordElement(36014),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2))); //

		// Energy values
		/*
		 * NOTE: Energy values are calculated manually from ActivePower, because the
		 * data in registers 35200 and 35195 is not correct. GoodWe is informed,
		 * acknowledged the problem and is working on fixing it.
		 */
		// new FC3ReadRegistersTask(35200, Priority.LOW, //
		// m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new
		// UnsignedDoublewordElement(35200),
		// ElementToChannelConverter.SCALE_FACTOR_2)), //
		// new FC3ReadRegistersTask(35195, Priority.LOW, //
		// m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new
		// UnsignedDoublewordElement(35195),
		// ElementToChannelConverter.SCALE_FACTOR_2)))
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateEnergy();
			break;
		}
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// Calculate Energy
		Integer activePower = this.getActivePower().get();
		if (activePower == null) {
			// Not available
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower > 0) {
			// Buy-From-Grid
			this.calculateProductionEnergy.update(activePower);
			this.calculateConsumptionEnergy.update(0);
		} else {
			// Sell-To-Grid
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(activePower * -1);
		}
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

}
