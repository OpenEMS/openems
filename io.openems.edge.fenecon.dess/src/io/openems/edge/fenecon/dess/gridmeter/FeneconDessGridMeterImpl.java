package io.openems.edge.fenecon.dess.gridmeter;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;

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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.fenecon.dess.FeneconDessConstants;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Fenecon.Dess.GridMeter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=GRID" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class FeneconDessGridMeterImpl extends AbstractOpenemsModbusComponent implements FeneconDessGridMeter,
		ElectricityMeter, ModbusComponent, OpenemsComponent, TimedataProvider, EventHandler {

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public FeneconDessGridMeterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				FeneconDessGridMeter.ChannelId.values() //
		);

		// Automatically calculate sum values from L1/L2/L3
		ElectricityMeter.calculateSumActivePowerFromPhases(this);
		ElectricityMeter.calculateSumReactivePowerFromPhases(this);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), FeneconDessConstants.UNIT_ID,
				this.cm, "Modbus", config.modbus_id())) {
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
				new FC3ReadRegistersTask(11109, Priority.LOW, //
						m(FeneconDessGridMeter.ChannelId.ORIGINAL_ACTIVE_CONSUMPTION_ENERGY,
								new UnsignedDoublewordElement(11109).wordOrder(WordOrder.MSWLSW), SCALE_FACTOR_2), //
						m(FeneconDessGridMeter.ChannelId.ORIGINAL_ACTIVE_PRODUCTION_ENERGY,
								new UnsignedDoublewordElement(11111).wordOrder(WordOrder.MSWLSW), SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(11136, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(11136), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(11137), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new UnsignedWordElement(11138), DELTA_10000), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new UnsignedWordElement(11139), DELTA_10000)), //
				new FC3ReadRegistersTask(11166, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedWordElement(11166), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(11167), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new UnsignedWordElement(11168), DELTA_10000), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new UnsignedWordElement(11169), DELTA_10000)), //
				new FC3ReadRegistersTask(11196, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedWordElement(11196), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(11197), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new UnsignedWordElement(11198), DELTA_10000), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new UnsignedWordElement(11199), DELTA_10000)) //
		); //
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID;
	}

	private static final ElementToChannelConverter DELTA_10000 = new ElementToChannelConverter(//
			// element -> channel
			value -> {
				if (value == null) {
					return null;
				}
				int intValue = (Integer) value;
				if (intValue == 0) {
					return 0; // ignore '0'
				}
				return (intValue - 10_000) * -1; // apply delta of 10_000 and invert
			}, //

			// channel -> element
			value -> value);

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
		var activePower = this.getActivePower().get();
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
	public Timedata getTimedata() {
		return this.timedata;
	}
}