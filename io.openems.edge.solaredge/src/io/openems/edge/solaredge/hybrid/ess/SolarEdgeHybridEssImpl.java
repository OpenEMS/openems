package io.openems.edge.solaredge.hybrid.ess;


//import java.util.ArrayList;
//import java.util.List;
import java.util.Map;
//import java.util.Objects;

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
import io.openems.edge.common.event.EdgeEventConstants;
import org.osgi.service.metatype.annotations.Designate;
import com.google.common.collect.ImmutableMap;
import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
//import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
//import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.dccharger.api.EssDcCharger;

//import io.openems.edge.ess.power.api.Constraint;
//import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
//import io.openems.edge.ess.power.api.Pwr;
//import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.ess.sunspec.AbstractSunSpecEss;
import io.openems.edge.ess.sunspec.SunSpecEss;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.solaredge.enums.ControlMode;
//import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
//import io.openems.edge.sma.sunnyisland.SunnyIslandEss;
import io.openems.edge.solaredge.enums.StorageChargePolicy;


@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "SolarEdge.Hybrid.ESS", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE) //

@EventTopics({ //
	EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})

public class SolarEdgeHybridEssImpl extends AbstractSunSpecEss
		//implements SunSpecEss, SymmetricEss, HybridEss, EssDcCharger, ModbusComponent, OpenemsComponent, ModbusSlave, EventHandler {
		implements SunSpecEss, SymmetricEss, HybridEss,  ModbusComponent, OpenemsComponent, ModbusSlave, EventHandler,ManagedSymmetricEss {


	private static final int READ_FROM_MODBUS_BLOCK = 1;
	
	// Hardware-Limits
	protected static final int HW_MAX_APPARENT_POWER = 5200;
	protected static final int HW_ALLOWED_CHARGE_POWER = -5000;
	protected static final int HW_ALLOWED_DISCHARGE_POWER = 5000;
	
	private Config config;
	
	@Reference
	private Power power;
	private int cycleCounter = 0;
	private int lastSetActivePower = 0;
	
	
	
	
	
	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) //
			.put(DefaultSunSpecModel.S_103, Priority.LOW) //
			.put(DefaultSunSpecModel.S_120, Priority.LOW) //
			.put(DefaultSunSpecModel.S_802, Priority.LOW) //
						
/*			.put(DefaultSunSpecModel.S_203, Priority.LOW) //
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
*/
			.build();

	@Reference
	protected ConfigurationAdmin cm;
	

	public SolarEdgeHybridEssImpl() throws OpenemsException {
		super(//
				ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				HybridEss.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				SunSpecEss.ChannelId.values(), //
				SolarEdgeHybridEss.ChannelId.values()
		);

		addStaticModbusTasks(this.getModbusProtocol());
	}



	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(),  this.cm,
				"Modbus", config.modbus_id(), READ_FROM_MODBUS_BLOCK)) {
			return;
		}
		this.config = config;
	}
	
	
	

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		if (this.config.readOnlyMode()) {
			// Switch to automatic mode
			EnumWriteChannel setControlMode = this.channel(SolarEdgeHybridEss.ChannelId.SET_CONTROL_MODE);
			setControlMode.setNextWriteValue(ControlMode.SE_CTRL_MODE_MAX_SELF_CONSUMPTION);	// auto mode for max. self consumption	
			return;
		}
		
		if((lastSetActivePower != activePower)  || cycleCounter > 59) {
			cycleCounter = 0;
			lastSetActivePower = activePower;
			
			
			/*  ToDo: 
			 * - Satemachine hinzufügen oder zumindest abfragen welcher Control-Modus aktiv ist
			 * - Channels für max charge/discharge implementieren
			 * - ggf. Werte für Charge / Discharge im Controller als Paar einsetzen oder zwei Controller aktivieren???
			 * 
			 * 
			 * 
			 * */
			//var gridMode = this.getControlMode();
	
			EnumWriteChannel setControlMode = this.channel(SolarEdgeHybridEss.ChannelId.SET_CONTROL_MODE);
			EnumWriteChannel setChargePolicy = this.channel(SolarEdgeHybridEss.ChannelId.SET_STORAGE_CHARGE_POLICY);
	
			
			setControlMode.setNextWriteValue(ControlMode.SE_CTRL_MODE_REMOTE);	// Now the device can be remote controlled	
			setChargePolicy.setNextWriteValue(StorageChargePolicy.SE_CHARGE_POLICY_MAX_SELF_CONSUMPTION);	// Optimize self-consumption
	
			_setAllowedChargePower(HW_ALLOWED_CHARGE_POWER);
			_setAllowedDischargePower(HW_ALLOWED_DISCHARGE_POWER);
			_setMaxApparentPower(HW_MAX_APPARENT_POWER); // Still don´t know what for...
	
			if (activePower < 0) // Charge-Power limit was set
			{
				IntegerWriteChannel setActiveChargeChannel = this.channel(SolarEdgeHybridEss.ChannelId.SET_MAX_CHARGE_POWER);
				setActiveChargeChannel.setNextWriteValue(activePower * -1); // Writing the channel with positive values
			}
			else // Discharge-Power limit was set
			{
				IntegerWriteChannel setActiveDisChargeChannel = this.channel(SolarEdgeHybridEss.ChannelId.SET_MAX_DISCHARGE_POWER);
				setActiveDisChargeChannel.setNextWriteValue(activePower);	
			}
		}
		else
		{
			cycleCounter++;

		}


	}
	
	
	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}
	
	
	// Methoden, die umgesetzt werden m�ssem
	@Override
	public int getPowerPrecision() {
		return 100; // the modbus field for SetActivePower has the unit 0.1 kW
	}
	
	@Override
	public Power getPower() {
		return this.power;
	}

	//@Override
	//protected ModbusProtocol defineModbusProtocol() throws OpenemsException {	
	
	

	/**
	 * Adds static modbus tasks.
	 * 
	 * @param protocol the {@link ModbusProtocol}
	 * @throws OpenemsException on error
	 */
	private void addStaticModbusTasks(ModbusProtocol protocol) throws OpenemsException {
		protocol.addTask(//
				new FC3ReadRegistersTask(0xE142, Priority.LOW, //

						m(SolarEdgeHybridEss.ChannelId.RATED_ENERGY, //
								new FloatDoublewordElement(0xE142).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.MAX_CHARGE_CONTINUES_POWER, //
								new FloatDoublewordElement(0xE144).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.MAX_DISCHARGE_CONTINUES_POWER, //
								new FloatDoublewordElement(0xE146).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.MAX_CHARGE_PEAK_POWER, //
								new FloatDoublewordElement(0xE148).wordOrder(WordOrder.LSWMSW)),						
						m(SolarEdgeHybridEss.ChannelId.MAX_DISCHARGE_PEAK_POWER, //
								new FloatDoublewordElement(0xE14A).wordOrder(WordOrder.LSWMSW)),
								
						new DummyRegisterElement(0xE14C, 0xE16B), // Reserved
						m(SolarEdgeHybridEss.ChannelId.BATT_AVG_TEMPERATURE, //
								new FloatDoublewordElement(0xE16C).wordOrder(WordOrder.LSWMSW)),	
						m(SolarEdgeHybridEss.ChannelId.BATT_MAX_TEMPERATURE, //
								new FloatDoublewordElement(0xE16E).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.BATT_ACTUAL_VOLTAGE, //
								new FloatDoublewordElement(0xE170).wordOrder(WordOrder.LSWMSW)),						
						m(SolarEdgeHybridEss.ChannelId.BATT_ACTUAL_CURRENT, //
								new FloatDoublewordElement(0xE172).wordOrder(WordOrder.LSWMSW)),							
						m(HybridEss.ChannelId.DC_DISCHARGE_POWER, //
								new FloatDoublewordElement(0xE174).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.INVERT), //
						//new DummyRegisterElement(0xE176, 0xE17D), 
						m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, //
								new UnsignedQuadruplewordElement(0xE176).wordOrder(WordOrder.LSWMSW)),
						m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY, //
								new UnsignedQuadruplewordElement(0xE17A).wordOrder(WordOrder.LSWMSW)),
						m(SymmetricEss.ChannelId.CAPACITY, //
								new FloatDoublewordElement(0xE17E).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.AVAIL_ENERGY, //
								new FloatDoublewordElement(0xE180).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.SOH, //
								new FloatDoublewordElement(0xE182).wordOrder(WordOrder.LSWMSW)),
						m(SymmetricEss.ChannelId.SOC, //
								new FloatDoublewordElement(0xE184).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.BATTERY_STATUS, //
								new UnsignedDoublewordElement(0xE186).wordOrder(WordOrder.LSWMSW))
						
						));
		
		
		protocol.addTask(//
		new FC3ReadRegistersTask(0xE004, Priority.LOW, //
				m(SolarEdgeHybridEss.ChannelId.CONTROL_MODE, 
						new UnsignedWordElement(0xE004)),
				m(SolarEdgeHybridEss.ChannelId.STORAGE_CHARGE_POLICY, 
						new UnsignedWordElement(0xE005)),					
				m(SolarEdgeHybridEss.ChannelId.MAX_CHARGE_LIMIT, 
						new FloatDoublewordElement(0xE006).wordOrder(WordOrder.LSWMSW)),					
				m(SolarEdgeHybridEss.ChannelId.STORAGE_BACKUP_LIMIT, 
						new FloatDoublewordElement(0xE008).wordOrder(WordOrder.LSWMSW)),
				m(SolarEdgeHybridEss.ChannelId.CHARGE_DISCHARGE_MODE, 
						new UnsignedWordElement(0xE00A))));
				
				/*new DummyRegisterElement(0xE00B, 0xE00D),
				m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, 
						new FloatDoublewordElement(0xE00E).wordOrder(WordOrder.LSWMSW)),
				m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, 
						new FloatDoublewordElement(0xE010).wordOrder(WordOrder.LSWMSW))));			
						
						
				*/
		
		protocol.addTask(//
		new FC16WriteRegistersTask(0xE004, //
				m(SolarEdgeHybridEss.ChannelId.SET_CONTROL_MODE, new SignedWordElement(0xE004)))); // Disabled, automatic, remote controlled, etc.
		
		protocol.addTask(//
		new FC16WriteRegistersTask(0xE00A, //
				m(SolarEdgeHybridEss.ChannelId.SET_STORAGE_CHARGE_POLICY, new SignedWordElement(0xE00A)))); //Used for charging from AC		
		
		protocol.addTask(//
		new FC16WriteRegistersTask(0xE00E, //
				m(SolarEdgeHybridEss.ChannelId.SET_MAX_CHARGE_POWER, new FloatDoublewordElement(0xE00E).wordOrder(WordOrder.LSWMSW)),  // Max. charge power. Negative values
				m(SolarEdgeHybridEss.ChannelId.SET_MAX_DISCHARGE_POWER, new FloatDoublewordElement(0xE010).wordOrder(WordOrder.LSWMSW)))); // Max. discharge power. Positive values
				
						
	}


	

	@Override
	protected void onSunSpecInitializationCompleted() {
		// TODO Add mappings for registers from S1 and S103

		// Example:
		// this.mapFirstPointToChannel(//
		// SymmetricEss.ChannelId.ACTIVE_POWER, //
		// ElementToChannelConverter.DIRECT_1_TO_1, //
		// DefaultSunSpecModel.S103.W);
	
		// this.mapFirstPointToChannel(//
		// SymmetricEss.ChannelId.CONSUMPTION_POWER, //
		// ElementToChannelConverter.DIRECT_1_TO_1, //
		// DefaultSunSpecModel.S103.W);
		
		 //DefaultSunSpecModel.S103.W);
		
		this.mapFirstPointToChannel(//
				ManagedSymmetricPvInverter.ChannelId.MAX_APPARENT_POWER, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S120.W_RTG);
		

	}


	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|Allowed Charge Power/Peak:"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER).value().asStringWithoutUnit() + " / " + this.channel(SolarEdgeHybridEss.ChannelId.MAX_CHARGE_PEAK_POWER).value().asStringWithoutUnit() + ";"
				+ "|Allowed DisCharge Power/Peak:"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER).value().asStringWithoutUnit() + " / " + this.channel(SolarEdgeHybridEss.ChannelId.MAX_DISCHARGE_PEAK_POWER).value().asStringWithoutUnit() + ";"
				+ "|" + this.getGridModeChannel().value().asOptionString() //
				+ "|Feed-In:";
	}	

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ACTIVE_POWER}
	 * Channel.
	 *
	 * @param value the next value
	*/
	public void _setActivePower() {
	
		// Actual Charge/Discharge-power of battery
		var value = this.getDcDischargePowerChannel().value().get() ;
		this._setActivePower(value);
		


		
	}
	 /*
	@Override
	public Constraint[] getStaticConstraints() throws OpenemsNamedException {
		// Read-Only-Mode
		
			return new Constraint[] { //
					this.createPowerConstraint("Read-Only-Mode", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0), //
					this.createPowerConstraint("Read-Only-Mode", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) //
			};
		


	}	
	*/
	
	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}
	
	@Override
	public void handleEvent(Event event) {
		//super.handleEvent(event);
		
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
		
			this._setActivePower();
			break;	
		}
	}	

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(SolarEdgeHybridEssImpl.class, accessMode, 100) //
						.build());
	}

	@Override
	public Integer getSurplusPower() {
		// TODO Auto-generated method stub
		return null;
	}

}
