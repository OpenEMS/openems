package io.openems.edge.solaredge.pvinverter;

import java.util.Map;

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

import com.google.common.collect.ImmutableMap;
import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;

import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.pvinverter.sunspec.AbstractSunSpecPvInverter;
//import io.openems.edge.pvinverter.sunspec.Phase;
import io.openems.edge.pvinverter.sunspec.SunSpecPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "SolarEdge.PV-Inverter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=PRODUCTION" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class SolarEdgePvInverterImpl extends AbstractSunSpecPvInverter implements SunSpecPvInverter, ManagedSymmetricPvInverter,
		AsymmetricMeter, SymmetricMeter, OpenemsComponent, EventHandler, ModbusSlave, SolarEdgePvinverterChannelId 		{
	
	private Config config;
	
	private static final int READ_FROM_MODBUS_BLOCK = 1;

	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) //
			.put(DefaultSunSpecModel.S_101, Priority.LOW) //
			.put(DefaultSunSpecModel.S_102, Priority.LOW) //
			.put(DefaultSunSpecModel.S_103, Priority.LOW) //
			.put(DefaultSunSpecModel.S_111, Priority.LOW) //
			.put(DefaultSunSpecModel.S_112, Priority.LOW) //
			.put(DefaultSunSpecModel.S_113, Priority.LOW) //
			.put(DefaultSunSpecModel.S_120, Priority.LOW) //
			.put(DefaultSunSpecModel.S_121, Priority.LOW) //
			.put(DefaultSunSpecModel.S_122, Priority.LOW) //
			.put(DefaultSunSpecModel.S_123, Priority.LOW) //
			.put(DefaultSunSpecModel.S_124, Priority.LOW) //
			.put(DefaultSunSpecModel.S_125, Priority.LOW) //
			.put(DefaultSunSpecModel.S_127, Priority.LOW) //
			.put(DefaultSunSpecModel.S_128, Priority.LOW) //
			.put(DefaultSunSpecModel.S_145, Priority.LOW) //
			.build();
	@Reference
	protected ConfigurationAdmin cm;

	public SolarEdgePvInverterImpl() throws OpenemsException {
		super(//
				ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				SunSpecPvInverter.ChannelId.values(),				
				SolarEdgePvinverterChannelId.ChannelId.values()
		);
		
		
			addStaticModbusTasksDcPower(this.getModbusProtocol());
			addStaticModbusTasksGridPower(this.getModbusProtocol());
		
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id(), READ_FROM_MODBUS_BLOCK, config.phase())) {
			return;
		}
		this.config = config;
		
	}
	
	/**
	 * Adds static modbus tasks.
	 * 
	 * @param protocol the {@link ModbusProtocol}
	 * @throws OpenemsException on error
	 */
	private void addStaticModbusTasksDcPower(ModbusProtocol protocol) throws OpenemsException {

		protocol.addTask(//
				new FC3ReadRegistersTask(0xE174, Priority.LOW, //
						m(SolarEdgePvinverterChannelId.ChannelId.DC_DISCHARGE_POWER, new FloatDoublewordElement(0xE174).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.INVERT)) //	
				);
	    
	}
	
	private void addStaticModbusTasksGridPower(ModbusProtocol protocol) throws OpenemsException {

		protocol.addTask(//
				new FC3ReadRegistersTask(0x9d0e, Priority.LOW, //
						m(SolarEdgePvinverterChannelId.ChannelId.GRID_POWER,new SignedWordElement(0x9d0e)),
						new DummyRegisterElement(0x9d0f, 0x9d11),						
						m(SolarEdgePvinverterChannelId.ChannelId.GRID_POWER_SCALE,new SignedWordElement(0x9d12)))
				);
	    
	}	
	
	
	public void _setMyActivePower() {
		
		// Aktuelle Erzeugung durch den Hybrid-WR ist der aktuelle Verbrauch + Batterie-Ladung/Entladung *-1
		// Actual power from inverter comes from house consumption + battery inverter power (*-1)
		try {
		int production_power 	= this.getProductionPowerChannel().value().get(); // Leistung Inverter
		//int consumption_power 	= this.getConsumptionPowerChannel().value().get(); // Leistung Haus <- Unsinn!
		int battery_power 		= this.getDcDischargePowerChannel().value().get() * -1; // DC-Discharge 0xe172: negative while Charging, so we have to negate
		double grid_power_scale	= this.getGridPowerScaleChannel().value().get();
		int grid_power 			= this.getGridPowerChannel().value().get() * (int) Math.pow(10, grid_power_scale); // postive while buying from grid
		grid_power				= grid_power * -1;
		
		// If Grid-Power is negative then the actual PV production is the Consumption Power (AC Power from inverter which is SunSpec 103W, Modbus 0x9c93)
		// If Grid-Power is positive the PV production is the sum from consumption power + battery-power (positive while charging) + grid-power (negative while consuming)
		
		int value				= 0;
		if (grid_power < 0 && battery_power  == 0)  value  = production_power ;
		else  value				= production_power + battery_power - grid_power;
		
		if (value < 0) value =0; // Negative Values are not allowed for PV production
		
		this._setActivePower(value);
		}
		catch (Exception e){
			return;
		}
	
	}	
	
	// Method seems only to be called once. Either here or in AbstractSunSpecInverter
	// So we call it here to have full control over channels
	@Override
	protected void onSunSpecInitializationCompleted() {
		// TODO Add mappings for registers from S1 and S103

		// Example:
		// this.mapFirstPointToChannel(//
		// SymmetricEss.ChannelId.ACTIVE_POWER, //
		// ElementToChannelConverter.DIRECT_1_TO_1, //
		// DefaultSunSpecModel.S103.W);
	
		// CONSUMPTION_POWER takes Power from Inverter
		// We have to build ACTIVE_POWER for inverter afterwards


		if (config.hybrid() == true ) // ACTIVE_POWER channel needs to be calculated
		{
			this.mapFirstPointToChannel(//
			SolarEdgePvinverterChannelId.ChannelId.PRODUCTION_POWER, //
			ElementToChannelConverter.DIRECT_1_TO_1, //
			DefaultSunSpecModel.S103.W);	
		}
		else 
		{
			this.mapFirstPointToChannel(//
			SymmetricMeter.ChannelId.ACTIVE_POWER, //
			ElementToChannelConverter.DIRECT_1_TO_1, //
			DefaultSunSpecModel.S111.W, DefaultSunSpecModel.S112.W, DefaultSunSpecModel.S113.W,
			DefaultSunSpecModel.S101.W, DefaultSunSpecModel.S102.W, DefaultSunSpecModel.S103.W);
		}



		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.FREQUENCY, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				DefaultSunSpecModel.S111.HZ, DefaultSunSpecModel.S112.HZ, DefaultSunSpecModel.S113.HZ,
				DefaultSunSpecModel.S101.HZ, DefaultSunSpecModel.S102.HZ, DefaultSunSpecModel.S103.HZ);


		/*
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.ACTIVE_POWER_SCALE, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S111.W, DefaultSunSpecModel.S112.W, DefaultSunSpecModel.S113.W,
				DefaultSunSpecModel.S101.W, DefaultSunSpecModel.S102.W, DefaultSunSpecModel.S103.W);		
*/
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.REACTIVE_POWER, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S111.V_AR, DefaultSunSpecModel.S112.V_AR, DefaultSunSpecModel.S113.V_AR,
				DefaultSunSpecModel.S101.V_AR, DefaultSunSpecModel.S102.V_AR, DefaultSunSpecModel.S103.V_AR);

		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S111.WH, DefaultSunSpecModel.S112.WH, DefaultSunSpecModel.S113.WH,
				DefaultSunSpecModel.S101.WH, DefaultSunSpecModel.S102.WH, DefaultSunSpecModel.S103.WH);

		this.mapFirstPointToChannel(//
				ManagedSymmetricPvInverter.ChannelId.MAX_APPARENT_POWER, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S120.W_RTG);

		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.CURRENT, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				DefaultSunSpecModel.S111.A, DefaultSunSpecModel.S112.A, DefaultSunSpecModel.S113.A,
				DefaultSunSpecModel.S101.A, DefaultSunSpecModel.S102.A, DefaultSunSpecModel.S103.A);

		/*
		 * SymmetricMeter
		*/
		if (this.isSinglePhase() == false) {
			this.mapFirstPointToChannel(//
					SymmetricMeter.ChannelId.VOLTAGE, //
					ElementToChannelConverter.SCALE_FACTOR_3, //
					DefaultSunSpecModel.S112.PH_VPH_A, DefaultSunSpecModel.S112.PH_VPH_B,
					DefaultSunSpecModel.S112.PH_VPH_C, //
					DefaultSunSpecModel.S113.PH_VPH_A, DefaultSunSpecModel.S113.PH_VPH_B,
					DefaultSunSpecModel.S113.PH_VPH_C, //
					DefaultSunSpecModel.S102.PH_VPH_A, DefaultSunSpecModel.S102.PH_VPH_B,
					DefaultSunSpecModel.S102.PH_VPH_C, //
					DefaultSunSpecModel.S103.PH_VPH_A, DefaultSunSpecModel.S103.PH_VPH_B,
					DefaultSunSpecModel.S103.PH_VPH_C);
			return;
		}
		else {
			
		

		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.VOLTAGE, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S101.PH_VPH_A, DefaultSunSpecModel.S111.PH_VPH_A, //
				DefaultSunSpecModel.S101.PH_VPH_B, DefaultSunSpecModel.S111.PH_VPH_B, //
				DefaultSunSpecModel.S101.PH_VPH_C, DefaultSunSpecModel.S111.PH_VPH_C);		
		}
		
	}	
	
	

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}
	

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
		
		if (config.hybrid() == true ) 
		{
			switch (event.getTopic()) {
			case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
				this._setMyActivePower();
				break;	
			}
		}
	}		


	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricPvInverter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(SolarEdgePvInverterImpl.class, accessMode, 100) //
						.build());
	}

}
