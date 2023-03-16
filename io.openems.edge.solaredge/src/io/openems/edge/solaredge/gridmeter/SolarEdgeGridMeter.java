package io.openems.edge.solaredge.gridmeter;

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


import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.sunspec.AbstractSunSpecMeter;


@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "SolarEdge.Grid-Meter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=GRID" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class SolarEdgeGridMeter extends AbstractSunSpecMeter
		implements AsymmetricMeter, SymmetricMeter, ModbusComponent, OpenemsComponent, EventHandler, SolarEdgeGridMeterChannelId {

	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) //
			.put(DefaultSunSpecModel.S_201, Priority.LOW) //
			.put(DefaultSunSpecModel.S_202, Priority.LOW) //
			.put(DefaultSunSpecModel.S_203, Priority.LOW) //
			.put(DefaultSunSpecModel.S_204, Priority.LOW) //
			.build();

	private static final int READ_FROM_MODBUS_BLOCK = 2;
	private Config config;

	public SolarEdgeGridMeter() throws OpenemsException {
		super(//
				ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SolarEdgeGridMeterChannelId.ChannelId.values(),				
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values() //
				
		);
		addStaticModbusTasks(this.getModbusProtocol());
	}

	@Reference
	protected ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id(), READ_FROM_MODBUS_BLOCK)) {
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
	private void addStaticModbusTasks(ModbusProtocol protocol) throws OpenemsException {
		protocol.addTask(//
				new FC3ReadRegistersTask(0xE174, Priority.LOW, //
						m(SolarEdgeGridMeterChannelId.ChannelId.DC_DISCHARGE_POWER, //
								new FloatDoublewordElement(0xE174).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.INVERT)));
	}
	

	@Override
	protected void onSunSpecInitializationCompleted() {
	//this.logInfo(this.log, "SunSpec initialization finished. " + this.channels().size() + " Channels available.");
	// TODO Add mappings for registers from S1 and S103

	// Example:
	// this.mapFirstPointToChannel(//
	// SymmetricEss.ChannelId.ACTIVE_POWER, //
	// ElementToChannelConverter.DIRECT_1_TO_1, //
	// DefaultSunSpecModel.S103.W);

	if (config.hybrid() == true ){
	// CONSUMPTION_POWER takes Power from Inverter
	// We have to build ACTIVE_POWER for inverter afterwards
	this.mapFirstPointToChannel(//
			SolarEdgeGridMeterChannelId.ChannelId.GRID_POWER, //
			ElementToChannelConverter.INVERT, //
			DefaultSunSpecModel.S203.W);	
	}
	else {
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.ACTIVE_POWER, //
				ElementToChannelConverter.INVERT, //
				DefaultSunSpecModel.S204.W, DefaultSunSpecModel.S203.W, DefaultSunSpecModel.S202.W,
				DefaultSunSpecModel.S201.W);		
	}
	this.mapFirstPointToChannel(//
			SymmetricMeter.ChannelId.FREQUENCY, //
			ElementToChannelConverter.SCALE_FACTOR_3, //
			DefaultSunSpecModel.S204.HZ, DefaultSunSpecModel.S203.HZ, DefaultSunSpecModel.S202.HZ,
			DefaultSunSpecModel.S201.HZ);

	this.mapFirstPointToChannel(//
			SymmetricMeter.ChannelId.REACTIVE_POWER, //
			ElementToChannelConverter.INVERT, //
			DefaultSunSpecModel.S204.VAR, DefaultSunSpecModel.S203.VAR, DefaultSunSpecModel.S202.VAR,
			DefaultSunSpecModel.S201.VAR);
	this.mapFirstPointToChannel(//
			SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
			ElementToChannelConverter.DIRECT_1_TO_1, //
			DefaultSunSpecModel.S204.TOT_WH_IMP, DefaultSunSpecModel.S203.TOT_WH_IMP,
			DefaultSunSpecModel.S202.TOT_WH_IMP, DefaultSunSpecModel.S201.TOT_WH_IMP);
	this.mapFirstPointToChannel(//
			SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, //
			ElementToChannelConverter.DIRECT_1_TO_1, //
			DefaultSunSpecModel.S204.TOT_WH_EXP, DefaultSunSpecModel.S203.TOT_WH_EXP,
			DefaultSunSpecModel.S202.TOT_WH_EXP, DefaultSunSpecModel.S201.TOT_WH_EXP);
	this.mapFirstPointToChannel(//
			SymmetricMeter.ChannelId.VOLTAGE, //
			ElementToChannelConverter.SCALE_FACTOR_3, //
			DefaultSunSpecModel.S204.PH_V, DefaultSunSpecModel.S203.PH_V, DefaultSunSpecModel.S202.PH_V,
			DefaultSunSpecModel.S201.PH_V, //
			DefaultSunSpecModel.S204.PH_VPH_A, DefaultSunSpecModel.S203.PH_VPH_A, DefaultSunSpecModel.S202.PH_VPH_A,
			DefaultSunSpecModel.S201.PH_VPH_A, //
			DefaultSunSpecModel.S204.PH_VPH_B, DefaultSunSpecModel.S203.PH_VPH_B, DefaultSunSpecModel.S202.PH_VPH_B,
			DefaultSunSpecModel.S201.PH_VPH_B, //
			DefaultSunSpecModel.S204.PH_VPH_C, DefaultSunSpecModel.S203.PH_VPH_C, DefaultSunSpecModel.S202.PH_VPH_C,
			DefaultSunSpecModel.S201.PH_VPH_C);
	this.mapFirstPointToChannel(//
			SymmetricMeter.ChannelId.CURRENT, //
			ElementToChannelConverter.SCALE_FACTOR_3, //
			DefaultSunSpecModel.S204.A, DefaultSunSpecModel.S203.A, DefaultSunSpecModel.S202.A,
			DefaultSunSpecModel.S201.A);
	}


	public void _setMyActivePower() {
		
		// Aktuelle Erzeugung durch den Hybrid-WR ist der aktuelle Verbrauch + Batterie-Ladung/Entladung *-1
		// Actual power from inverter comes from house consumption + battery inverter power (*-1)
		try {
		var power_grid = 	this.getGridPowerChannel().value().get();  // akt: Netzbezug. Positiv bei Bezug / S203W
		var power_dc_ess =  this.getDcDischargePowerChannel().value().get() ;  // akt: wird positiv wenn Batterie geladen wird
		
		//int value = val1 - val2;
		 this._setActivePower(power_grid);
		}
		catch (Exception e){
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
		
		
		if (config.hybrid() == true ) 
		{
			switch (event.getTopic()) {
			case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
				//				
				this._setMyActivePower();
				break;	
			}
		}
	}
	
	
	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}
	
/*
	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
			OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
			SymmetricMeter.getModbusSlaveNatureTable(accessMode), //
			ModbusSlaveNatureTable.of(SymmetricMeter.class, accessMode, 100) //
					.build());
}	
*/
}
