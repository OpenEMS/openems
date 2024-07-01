package io.openems.edge.meter.camillebauer.aplus;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT_IF_TRUE;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
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
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Camillebauer.Aplus", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class MeterCamillebauerAplusImpl extends AbstractOpenemsModbusComponent implements MeterCamillebauerAplus,
		ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave, TimedataProvider, EventHandler {

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private MeterType metertype = MeterType.PRODUCTION;
	private Config config = null;

	public MeterCamillebauerAplusImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterCamillebauerAplus.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		this.metertype = this.config.type();
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @implNote Illustrates the calculation of a Modbus register address.
	 *           <ul>
	 *           <li>Register address (x): 40100</li>
	 *           <li>Implicit base address (y): 40000</li>
	 *           <li>Offset (c): 1</li>
	 *           </ul>
	 *           The actual register address is calculated as x - y - c, resulting
	 *           in 99.
	 */
	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(99, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.VOLTAGE, //
								new FloatDoublewordElement(99).wordOrder(LSWMSW), //
								SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new FloatDoublewordElement(101).wordOrder(LSWMSW), //
								SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(103).wordOrder(LSWMSW), //
								SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(105).wordOrder(LSWMSW), //
								SCALE_FACTOR_3), //
						new DummyRegisterElement(107, 114), //
						m(ElectricityMeter.ChannelId.CURRENT, new FloatDoublewordElement(115).wordOrder(LSWMSW), //
								SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(117).wordOrder(LSWMSW), //
								SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(119).wordOrder(LSWMSW), //
								SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(121).wordOrder(LSWMSW), //
								SCALE_FACTOR_3), //
						new DummyRegisterElement(123, 132), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, //
								new FloatDoublewordElement(133).wordOrder(LSWMSW), //
								INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(135).wordOrder(LSWMSW), //
								INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(137).wordOrder(LSWMSW), //
								INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(139).wordOrder(LSWMSW), //
								INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(141).wordOrder(LSWMSW), //
								INVERT_IF_TRUE(this.config.invert())),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1,
								new FloatDoublewordElement(143).wordOrder(LSWMSW), //
								INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2,
								new FloatDoublewordElement(145).wordOrder(LSWMSW), //
								INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3,
								new FloatDoublewordElement(147).wordOrder(LSWMSW), //
								INVERT_IF_TRUE(this.config.invert())), //
						new DummyRegisterElement(149, 156), //
						m(ElectricityMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(157).wordOrder(LSWMSW), //
								SCALE_FACTOR_3)));
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public MeterType getMeterType() {
		return this.metertype;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(MeterCamillebauerAplusImpl.class, accessMode, 100) //
						.build());
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> this.calculateEnergy();
		}
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// Calculate Energy
		final var activePower = this.getActivePower().get();
		if (activePower == null) {
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower >= 0) {
			this.calculateProductionEnergy.update(activePower);
			this.calculateConsumptionEnergy.update(0);
		} else {
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(-activePower);
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
