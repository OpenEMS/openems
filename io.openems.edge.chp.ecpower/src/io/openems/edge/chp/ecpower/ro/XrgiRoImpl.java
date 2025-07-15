package io.openems.edge.chp.ecpower.ro;

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
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.generator.api.SymmetricGenerator;
import io.openems.edge.meter.api.ElectricityMeter;


@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "CHP.ECcpower.ro", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=PRODUCTION" //
		}		
)
@EventTopics({ //
	EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS, //
	EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
})
public class XrgiRoImpl extends AbstractOpenemsModbusComponent implements XrgiRo, ModbusComponent, OpenemsComponent, EventHandler, ElectricityMeter, SymmetricGenerator  {

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public XrgiRoImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(),				
				SymmetricGenerator.ChannelId.values(), //	
				XrgiRo.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if(super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
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
	public void handleEvent(Event event) {
		// not used yet
		


		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			this._setActivePowerL1(331);
			this._setActivePowerL2(332);
			this._setActivePowerL3(333);
			
			this._setVoltageL1(221000);
			this._setVoltageL2(222000);
			this._setVoltageL3(223000);
			
			this._setCurrentL1(21000);
			this._setCurrentL2(22000);
			this._setCurrentL3(23000);
			this._setActiveProductionEnergy(4711L);			
			
			this._setGeneratorActivePower(34);	
			
			break;
		//case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
		//	//this._setActivePower(666);
		//	break;
		}
	}	

		
		@Override
		protected ModbusProtocol defineModbusProtocol() {
		    return new ModbusProtocol(this,
		        // BOOL-Register (1 bit)
		        new FC3ReadRegistersTask(0, Priority.LOW,
		            m(XrgiRo.ChannelId.ERROR, new UnsignedWordElement(0)), // 0x0000: Störung BOOL
		            m(XrgiRo.ChannelId.OPERATING, new UnsignedWordElement(1)), // 0x0001: Betrieb BOOL
		            m(XrgiRo.ChannelId.CHP_READY_FOR_OPERATION, new UnsignedWordElement(2)), // 0x0002: BHKW Einsatzbereit BOOL
		            m(XrgiRo.ChannelId.CHP_NOT_READY_FOR_OPERATION, new UnsignedWordElement(3)), // 0x0003: BHKW nicht Einsatzbereit BOOL
		            m(XrgiRo.ChannelId.MULTIPLE_STORAGE_UNITS_PRESENT, new UnsignedWordElement(4)), // 0x0004: mehrere Speicher BOOL
		            m(XrgiRo.ChannelId.STORAGE_SENSOR_ORDER_DETECTED, new UnsignedWordElement(5)) // 0x0005: Speicherfühler-Reihenfolge erkannt BOOL
		        ),
		        // INT16/UIN16/UINT32 Register
		        new FC3ReadRegistersTask(0, Priority.LOW,
		            m(XrgiRo.ChannelId.STORAGE_TEMPERATURE_TOP, new SignedWordElement(0)), // 0x0000: INT16, °C x100
		            m(XrgiRo.ChannelId.STORAGE_TEMPERATURE_BOTTOM, new SignedWordElement(1)), // 0x0001: INT16, °C x100
		            m(XrgiRo.ChannelId.FLOW_MASTER_FLOW_TEMPERATURE, new SignedWordElement(2)), // 0x0002: INT16, °C x100
		            m(XrgiRo.ChannelId.FLOW_MASTER_RETURN_TEMPERATURE, new SignedWordElement(3)), // 0x0003: INT16, °C x100
		            m(XrgiRo.ChannelId.CHP_NETWORK_TEMPERATURE, new SignedWordElement(4)), // 0x0004: INT16, °C x100
		            m(XrgiRo.ChannelId.OUTDOOR_TEMPERATURE, new SignedWordElement(5)), // 0x0005: INT16, °C x100
		            m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedWordElement(6)), // 0x0006: UINT16, kW x10
		            m(XrgiRo.ChannelId.CURRENT_HEAT_OUTPUT, new UnsignedWordElement(7)), // 0x0007: UINT16, %
		            m(XrgiRo.ChannelId.TOTAL_ACTIVE_ENERGY, new UnsignedDoublewordElement(8)), // 0x0008: UINT32, kWh
		            m(XrgiRo.ChannelId.TOTAL_HEAT_ENERGY, new UnsignedDoublewordElement(10)), // 0x000A: UINT32, kWh
		            m(XrgiRo.ChannelId.ACTIVE_ENERGY_15MIN, new UnsignedWordElement(12)), // 0x000C: UINT16, kWh
		            m(XrgiRo.ChannelId.HEAT_ENERGY_15MIN, new UnsignedWordElement(13)), // 0x000D: UINT16, kWh
		            m(XrgiRo.ChannelId.TOTAL_GAS_CONSUMPTION, new UnsignedDoublewordElement(14)), // 0x000E: UINT32, kWh
		            m(XrgiRo.ChannelId.TOTAL_OPERATING_HOURS, new UnsignedWordElement(16)), // 0x0010: UINT16, Stunden
		            m(XrgiRo.ChannelId.REMAINING_HOURS_UNTIL_MAINTENANCE, new UnsignedWordElement(17)), // 0x0011: UINT16, Stunden
		            m(XrgiRo.ChannelId.LAST_ERROR_CODE, new UnsignedWordElement(18)), // 0x0012: UINT16
		            m(XrgiRo.ChannelId.TOTAL_GENERATOR_STARTS, new UnsignedWordElement(19)), // 0x0013: UINT16
		            m(XrgiRo.ChannelId.MIXER_TMV_TEMPERATURE, new UnsignedWordElement(20)), // 0x0014: UINT16, °C x100
		            m(XrgiRo.ChannelId.MIXER_TMK_TEMPERATURE, new UnsignedWordElement(21)), // 0x0015: UINT16, °C x100
		            m(XrgiRo.ChannelId.MIXER_TLV_TEMPERATURE, new UnsignedWordElement(22)), // 0x0016: UINT16, °C x100
		            m(XrgiRo.ChannelId.MIXER_TLK_TEMPERATURE, new UnsignedWordElement(23)), // 0x0017: UINT16, °C x100
		            m(XrgiRo.ChannelId.MIXER_RETURN_TEMPERATUR, new UnsignedWordElement(24)), // 0x0018: UINT16, °C x100
		            m(XrgiRo.ChannelId.OPERATIONAL_SETPOINT_TMV, new UnsignedWordElement(25)), // 0x0019: UINT16, °C x100
		            m(XrgiRo.ChannelId.FLOW_MASTER_BYPASS_TEMPERATURE, new UnsignedWordElement(26)), // 0x001A: UINT16, °C x100
		            m(XrgiRo.ChannelId.FLOW_MASTER_SUPPLY_TEMPERATURE, new UnsignedWordElement(27)), // 0x001B: UINT16, °C x100
		            m(XrgiRo.ChannelId.FLOW_MASTER_SETPOINT_TEMPERATURE, new UnsignedWordElement(28)), // 0x001C: UINT16, °C x100
		            m(XrgiRo.ChannelId.FLOW_MASTER_OPERATIONAL_SETPOINT, new UnsignedWordElement(29)), // 0x001D: UINT16, °C x100
		            m(XrgiRo.ChannelId.MIXER_ENGINE_LOOP_PUMP, new UnsignedWordElement(30)), // 0x001E: UINT16, %
		            m(XrgiRo.ChannelId.MIXER_SECONDARY_PUMP, new UnsignedWordElement(31)), // 0x001F: UINT16, %
		            m(XrgiRo.ChannelId.FLOW_MASTER_PUMP_POWER, new UnsignedWordElement(32)), // 0x0020: UINT16, %
		            m(XrgiRo.ChannelId.MIXER_VALVE_POSITION, new UnsignedWordElement(33)), // 0x0021: UINT16, %
		            m(XrgiRo.ChannelId.FLOW_MASTER_VALVE_POSITION, new UnsignedWordElement(34)), // 0x0022: UINT16, %
		            m(XrgiRo.ChannelId.CURRENT_ENGINE_HEAT_OUTPUT, new UnsignedWordElement(35)), // 0x0023: UINT16, kW x100
		            m(XrgiRo.ChannelId.HEAT_TRANSFER_VALUE, new UnsignedWordElement(36)), // 0x0024: UINT16, kW/K x10
		            m(XrgiRo.ChannelId.LAYER_SEPARATION_TEMPERATURE, new UnsignedWordElement(37)), // 0x0025: UINT16, °C x100
		            m(XrgiRo.ChannelId.POWER_UNIT_REQUESTED_POWER, new UnsignedWordElement(38)), // 0x0026: UINT16, W
		            m(XrgiRo.ChannelId.POWER_UNIT_POWER_LIMIT, new UnsignedWordElement(39)), // 0x0027: UINT16, W
		            m(XrgiRo.ChannelId.POWER_UNIT_TARGET_POWER, new UnsignedWordElement(40)), // 0x0028: UINT16, W
		            m(XrgiRo.ChannelId.POWER_UNIT_ENGINE_VALVE_POSITION, new UnsignedWordElement(41)), // 0x0029: UINT16
		            m(XrgiRo.ChannelId.POWER_UNIT_MAP_PRESSURE, new UnsignedWordElement(42)), // 0x002A: UINT16, mBar
		            m(XrgiRo.ChannelId.POWER_UNIT_FUEL_VALVE_POSITION, new UnsignedWordElement(43)), // 0x002B: UINT16
		            m(XrgiRo.ChannelId.POWER_UNIT_IGNITION_ANGLE, new UnsignedWordElement(44)), // 0x002C: UINT16, °C x10
		            m(XrgiRo.ChannelId.POWER_UNIT_ENGINE_TEMPERATURE, new UnsignedWordElement(45)), // 0x002D: UINT16, °C x100
		            m(XrgiRo.ChannelId.POWER_UNIT_RPM, new UnsignedWordElement(46)), // 0x002E: UINT16
		            m(XrgiRo.ChannelId.L1_L2_VOLTAGE, new UnsignedWordElement(47)), // 0x002F: UINT16, Volt
		            m(XrgiRo.ChannelId.L2_L3_VOLTAGE, new UnsignedWordElement(48)), // 0x0030: UINT16, Volt
		            m(XrgiRo.ChannelId.L3_L1_VOLTAGE, new UnsignedWordElement(49)), // 0x0031: UINT16, Volt
		            m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedWordElement(50)), // 0x0032: UINT16, Hz x100
		            m(XrgiRo.ChannelId.ALERT_STATUS, new UnsignedWordElement(51)) // 0x0033: UINT16
		        ),
		        new FC3ReadRegistersTask(0, Priority.LOW,
		            m(XrgiRo.ChannelId.VPP_ENABLE, new UnsignedWordElement(0)), // 0x0000: VPP Freigabe
		            m(XrgiRo.ChannelId.CHP_POWER_CONTROL, new UnsignedWordElement(1)) // 0x0001: BHKW Leistungsregelung %
		        )

		        
		        // ggf. weitere Tasks für andere Bereiche
		    );
		}

/*		
		
		
		new FC3ReadRegistersTask(6, Priority.LOW, //
					m(Xrgi.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(6))
					));
		}
		*/
		


	@Override
	public String debugLog() {
		
		return "RO-Device Power: " + this.getActivePower().asString();
	}

	

	
	@Override
	public MeterType getMeterType() {
		return MeterType.PRODUCTION;
	}
}
