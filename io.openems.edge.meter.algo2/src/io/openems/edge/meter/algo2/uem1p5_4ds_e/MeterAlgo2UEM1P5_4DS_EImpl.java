package io.openems.edge.meter.algo2.uem1p5_4ds_e;

import java.util.Formatter;
import java.util.Locale;

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
// import org.osgi.service.event.Event;
// import org.osgi.service.event.EventConstants;
// import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import com.google.common.primitives.SignedBytes;
import com.google.common.primitives.UnsignedLong;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
// import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.algo2.algotypes.Algo3WordImpl;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
// import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.AsymmetricMeter.ChannelId;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "io.openems.edge.meter.algo2.uem1p5_4ds_e", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //, //
		// property = { //
		// 		EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		// } //
)
// public class EmulatorImpl extends AbstractOpenemsComponent 
//    implements Emulator, OpenemsComponent, EventHandler {
public class MeterAlgo2UEM1P5_4DS_EImpl extends AbstractOpenemsModbusComponent
		implements MeterAlgo2UEM1P5_4DS_E, SymmetricMeter, AsymmetricMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	private MeterType meterType = MeterType.PRODUCTION;

	/*
	 * Invert power values
	 */
	private Config config;
	private boolean invert = false;

	@Reference
	protected ConfigurationAdmin cm;

	// private Config config = null;

	public MeterAlgo2UEM1P5_4DS_EImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				MeterAlgo2UEM1P5_4DS_E.ChannelId.values() //
		);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.meterType = config.type();
		this.config = config;
		this.invert = config.invert();

		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}


//	@Override
//	public void handleEvent(Event event) {
//		if (!this.isEnabled()) {
//			return;
// 		}
//		switch (event.getTopic()) {
//		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
//			// TODO: fill channels
//			break;
//		}
//	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		/*
		 * We are using the FLOAT registers from the modbus table, because they are all
		 * reachable within one ReadMultipleRegistersRequest.
		 */
		ModbusProtocol modbusProtocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(0, Priority.HIGH, //
				    m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new SignedDoublewordElement(0), 
			    		ElementToChannelConverter.SCALE_FACTOR_MINUS_3), //
					m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new SignedDoublewordElement(2),
					    ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
					m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new SignedDoublewordElement(4),
					    ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
					
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_L12, new SignedDoublewordElement(6),
						ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_L23, new SignedDoublewordElement(8),
						ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_L31, new SignedDoublewordElement(0x0a),
						ElementToChannelConverter.DIRECT_1_TO_1),
				    m(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_SYS, new SignedDoublewordElement(0x0C), 
				    		ElementToChannelConverter.DIRECT_1_TO_1), //
				    
				    m(AsymmetricMeter.ChannelId.CURRENT_L1, new SignedDoublewordElement(0x0E), 
				    		ElementToChannelConverter.DIRECT_1_TO_1), //
					m(AsymmetricMeter.ChannelId.CURRENT_L2, new SignedDoublewordElement(0x10),
					    ElementToChannelConverter.DIRECT_1_TO_1),
					m(AsymmetricMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(0x12),
						    ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_N, new SignedDoublewordElement(0x14),
						    ElementToChannelConverter.DIRECT_1_TO_1),
					
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_SYS, new SignedDoublewordElement(0x16),
						    ElementToChannelConverter.DIRECT_1_TO_1) // ,
					
					// m(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FACTOR_L1And2, new SignedDoublewordElement(0x18),
					//	    ElementToChannelConverter.DIRECT_1_TO_1),
					
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FACTOR_L1, /* new Algo1Byte(0x18) */ new UnsignedWordElement(0x18),
//						    ElementToChannelConverter.DIRECT_1_TO_1),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FACTOR_L2, /* new Algo1Byte(0x19) */ new UnsignedWordElement(0x19),
//						    ElementToChannelConverter.DIRECT_1_TO_1),
//					
//					// m(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FACTOR_L3AndSys, new SignedDoublewordElement(0x1A),
//					//	    ElementToChannelConverter.DIRECT_1_TO_1),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FACTOR_L3, /* new Algo1Byte(0x1A) */ new UnsignedWordElement(0x1A),
//						    ElementToChannelConverter.DIRECT_1_TO_1),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FACTOR_SYS, /* new Algo1Byte(0x1B) */ new UnsignedWordElement(0x1B),
//						    ElementToChannelConverter.DIRECT_1_TO_1),
//					
//					m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new Algo3Bytes(0x1c),
//							ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
//					m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new Algo3Bytes(0x1f), // 0x1F
//							ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CACTIVE_POWER_L3, new Algo3Bytes(0x22), //0x22
//							ElementToChannelConverter.INVERT_IF_TRUE(this.invert)/*, ElementToChannelConverter.SCALE_FACTOR_MINUS_3 */),
//					// new DummyRegisterElement(0x25, 0x27), 
//					
//					m(SymmetricMeter.ChannelId.ACTIVE_POWER, new Algo3Bytes(0x25), // 0x25
//							ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
//					
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.APPARENT_POWER_L1, new Algo3Bytes(0x28),
//							ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.APPARENT_POWER_L2, new Algo3Bytes(0x2b), // 0x1F
//							ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.APPARENT_POWER_L3, new Algo3Bytes(0x2e), //0x22
//							ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.APPARENT_POWER_SYS, new Algo3Bytes(0x31), // 0x25
//							ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
//					
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTREACTIVE_POWER_L1,  new Algo3Bytes(0x34),
//							ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTREACTIVE_POWER_L2, new Algo3Bytes(0x37), // 0x1F
//							ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTREACTIVE_POWER_L3, new Algo3Bytes(0x3a), //0x22
//							ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTREACTIVE_POWER, new Algo3Bytes(0x3d), // 0x25
//							ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
//					
//					// new DummyRegisterElement(0x3d, 0x3F), 
//						
//					m(SymmetricMeter.ChannelId.FREQUENCY, new SignedDoublewordElement(0x40),
//							ElementToChannelConverter.DIRECT_1_TO_1),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.PHASE_SEQUENCE, new SignedDoublewordElement(0x42),
//							ElementToChannelConverter.DIRECT_1_TO_1) // ,
//						
//
//					//
//					/*								m(AsymmetricMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(860),
//															ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)),
//													m(AsymmetricMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(862),
//															ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)),
//													m(AsymmetricMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(864),
//															ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)),
//													m(SymmetricMeter.ChannelId.CURRENT, new FloatDoublewordElement(866),
//															ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)),
//													m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(876),
//															ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
//													m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(878),
//															ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
//													m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(880),
//															ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
//													m(SymmetricMeter.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(882),
//															ElementToChannelConverter.INVERT_IF_TRUE(this.invert))));*/
//																
//					
						
						
				)
		);
		
		modbusProtocol.addTask(
				new FC3ReadRegistersTask(0x1c, Priority.LOW, //
						m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CACTIVE_POWER_L1, new Algo3WordImpl(0x1c),
						ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
						m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CACTIVE_POWER_L2, new Algo3WordImpl(0x1f), // 0x1F
						ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
						m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CACTIVE_POWER_L3, new Algo3WordImpl(0x22), //0x22
						ElementToChannelConverter.INVERT_IF_TRUE(this.invert)/*, ElementToChannelConverter.SCALE_FACTOR_MINUS_3 */),
//				// new DummyRegisterElement(0x25, 0x27), 
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new Algo3WordImpl(0x25), // 0x25
						ElementToChannelConverter.INVERT_IF_TRUE(this.invert))
				)
			);

		
		modbusProtocol.addTask(
			new FC3ReadRegistersTask(MODBUSREG_SET0_SYSIMPORTEDACTIVEENERGY_l1, Priority.HIGH, //
				m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVECONSUMPTION_ENERGY_L1, new Algo3WordImpl(MODBUSREG_SET0_SYSIMPORTEDACTIVEENERGY_l1),
						ElementToChannelConverter.DIRECT_1_TO_1),
				m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVECONSUMPTION_ENERGY_L2, new Algo3WordImpl(MODBUSREG_SET0_SYSIMPORTEDACTIVEENERGY_L2),
						ElementToChannelConverter.DIRECT_1_TO_1),
				m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVECONSUMPTION_ENERGY_L3, new Algo3WordImpl(MODBUSREG_SET0_SYSIMPORTEDACTIVEENERGY_L3),
						ElementToChannelConverter.DIRECT_1_TO_1),
				m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVECONSUMPTION_ENERGY, new Algo3WordImpl(MODBUSREG_SET0_SYSIMPORTEDACTIVEENERGY),
						ElementToChannelConverter.DIRECT_1_TO_1),
				
				m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVEPRODUCTION_ENERGY_L1, new Algo3WordImpl(MODBUSREG_SET0_SYSEXPORTEDACTIVEENERGY_L1),
						ElementToChannelConverter.DIRECT_1_TO_1),
				m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVEPRODUCTION_ENERGY_L2, new Algo3WordImpl(MODBUSREG_SET0_SYSEXPORTEDACTIVEENERGY_L2),
						ElementToChannelConverter.DIRECT_1_TO_1),
				m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVEPRODUCTION_ENERGY_L3, new Algo3WordImpl(MODBUSREG_SET0_SYSEXPORTEDACTIVEENERGY_L3),
						ElementToChannelConverter.DIRECT_1_TO_1),
				m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVEPRODUCTION_ENERGY, new Algo3WordImpl(MODBUSREG_SET0_SYSEXPORTEDACTIVEENERGY),
						ElementToChannelConverter.DIRECT_1_TO_1)
			)
		);
		
		
		
		
		modbusProtocol.addTask(
			new FC3ReadRegistersTask(MODBUSREG_SET0_REGSETINUSE, Priority.LOW, //
				m(MeterAlgo2UEM1P5_4DS_E.ChannelId.METAS_COUNTER_REGSET_IN_USE, new UnsignedWordElement(MODBUSREG_SET0_REGSETINUSE))
			)
		);
//
//		if (this.config.invert()) {
//			modbusProtocol.addTask(
//				new FC3ReadRegistersTask(0x215, Priority.LOW, //
//					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new SignedDoublewordElement(0x215))
//				)
//			);
//		} else {
//			modbusProtocol.addTask(
//				new FC3ReadRegistersTask(0x209, Priority.LOW, //
//					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new SignedDoublewordElement(0x209))
//				)
//			);
//		}

		// Calculates required Channels from other existing Channels.
		// this.addCalculateChannelListeners();

		return modbusProtocol;
	}

	@Override
	public String debugLog() {
		
		StringBuilder theMessage = new StringBuilder();
		// Send all output to the Appendable object theMessage
		Formatter msgFormatter = new Formatter(theMessage, Locale.US);
		//
		theMessage.append("\n device: " + this.id());
		//
		long runningRegsSet = this.getRunningRegs().asOptional().get();
		theMessage.append("\n - runningRegs " + runningRegsSet);
		//
		// --- 
		theMessage.append("\n\n\n Energy values:\n");
		//
		long activeConsumptionEnergyL1 = this.getCustActiveConsumptionEnergy_L1().asOptional().get();
		long activeConsumptionEnergyL2 = this.getCustActiveConsumptionEnergy_L2().asOptional().get();
		long activeConsumptionEnergyL3 = this.getCustActiveConsumptionEnergy_L3().asOptional().get();
		long activeConsumptionEnergyL123 = activeConsumptionEnergyL1 + activeConsumptionEnergyL2 + activeConsumptionEnergyL3;
		long activeConsumptionEnergy = this.getCustActiveConsumptionEnergy().asOptional().get();
		msgFormatter.format("\n - Active Consumption Energy L1: %f, L2: %f, L3: %f"
				, (float) activeConsumptionEnergyL1/10000, (float) activeConsumptionEnergyL2/10000, (float) activeConsumptionEnergyL3/10000);
		msgFormatter.format("\n - Total Active Consumption Energy %f, (%f)"
				, (float) activeConsumptionEnergy/10000, (float) activeConsumptionEnergyL123/10000);
		//
		long activeProductionEnergyL1 = this.getCustActiveProductionEnergy_L1().asOptional().get();
		long activeProductionEnergyL2 = this.getCustActiveProductionEnergy_L2().asOptional().get();
		long activeProductionEnergyL3 = this.getCustActiveProductionEnergy_L3().asOptional().get();
		long activeProductionEnergyL123 = activeProductionEnergyL1 + activeProductionEnergyL2 + activeProductionEnergyL3;
		long activeProductionEnergy = this.getCustActiveProductionEnergy().asOptional().get();
		msgFormatter.format("\n\n - Active Production Energy L1: %f, L2: %f, L3: %f"
				, (float) activeProductionEnergyL1/10000, (float) activeProductionEnergyL2/10000, (float) activeProductionEnergyL3/10000);
		msgFormatter.format("\n - Total Active Production Energy %f, (%f)"
				, (float) activeProductionEnergy/10000, (float) activeProductionEnergyL123/10000);
		//
		//
		//
//		
		double ACP1 = ((double) this.getCActivePowerL1().asOptional().get())/1000000;
		double ACP2 = ((double) this.getCActivePowerL2().asOptional().get())/1000000;
		double ACP3 = ((double) this.getCActivePowerL3().asOptional().get())/1000000;
		double ACPSys = ((double) this.getActivePower().asOptional().get())/1000000;
		
		msgFormatter.format("\n\n - Active power  ACP1: %f, ACP2: %f, ACP3: %f"
				, (float) ACP1/10000, (float) ACP2/10000, (float) ACP3/10000);
		msgFormatter.format("\n - Total Active power ACPSys: %f", (float) ACPSys/10000);
//		+ "\n - P1-ACP1, P2-ACP2, P3-ACP3   - "  + ACP1 + " " + ACP2 + " " + ACP3
//		+ "\n - PSig-ACPSys (or L in example) - " + ACPSys
		
		
		
		theMessage.append("\n\n\n Volt and Amp values:\n");
		//
		theMessage.append("\n - V1, V2, V3 - ");
		theMessage.append(this.getVoltageL1().asString() + " " + this.getVoltageL2().asString() + " " + this.getVoltageL3().asString());
		//
		theMessage.append("\n - V12, V23, V31  - ");
		theMessage.append(this.getVoltageL12().asString() + " " + this.getVoltageL23().asString() + " " + this.getVoltageL31().asString());
		//
		theMessage.append("\n - VSys - " + this.getVoltageSys().asString());
		//
		theMessage.append("\n - A1, A2, A3  - ");
		theMessage.append(this.getCurrentL1().asString() + " " + this.getCurrentL2().asString() + " " + this.getCurrentL3().asString());
		//
		theMessage.append("\n - AN  - " + this.getCurrentNeutral().asString());
		//
		theMessage.append("\n - ASys  - " + this.getCurrentSys().asString());
		//
		//
		//
			
//		double PF1 = 1.0 - ((double) this.getPowerFactorL1Channel().value().asOptional().get())/1000000;
//		double PF2 = 1.0 - ((double) this.getPowerFactorL2Channel().value().asOptional().get())/1000000;
//		double PF3 = 1.0 - ((double) this.getPowerFactorL3Channel().value().asOptional().get())/1000000;
//		double PFS = 1.0 - ((double) this.getPowerFactorSysChannel().value().asOptional().get())/1000000;
//		
//		double APP1 = ((double) this.getApparentPowerL1().asOptional().get())/1000000;
//		double APP2 = ((double) this.getApparentPowerL2().asOptional().get())/1000000;
//		double APP3 = ((double) this.getApparentPowerL3().asOptional().get())/1000000;
//		double APPSys = ((double) this.getApparentPowerSys().asOptional().get())/1000000;
//		
//		double REP1 = ((double) this.getCustReactivePowerL1Channel().value().asOptional().get());
//		double REP2 = ((double) this.getCustReactivePowerL2().asOptional().get());
//		double REP3 = ((double) this.getCustReactivePowerL3().asOptional().get());
//		double REPSys = ((double) this.getCustReactivePower().asOptional().get());
//		//
//				+ "\n - PF1, PF2, PF3   - "  + PF1 + " " + PF2 + " " + PF3
//				+ "\n - PFSys  - " + PFS
//
//				// + "\n - PF1, PF2 - "  + this.getPowerFactorL1And2().asString()
//				// + "\n - PF3, PFSys  - " + this.getPowerFactorL3AndSys().asString()
//				
//				
//				+ "\n - S1-APP1, S2-APP2, S3-APP3   - "  + APP1 + " " + APP2 + " " + APP3
//				+ "\n - SSig-APPSys  - " + APPSys
//				
//				+ "\n - Q1-REP1, Q2-REP2, Q3-REP3   - "  + REP1 + " " + REP2 + " " + REP3
//				+ "\n - QSig-REPSys  - " + REPSys
//				
//				
//				+ "\n - C - " + this.getActiveConsumptionEnergyChannel().toString()
//				+ "\n - P - " + this.getActiveProductionEnergyChannel().toString()
		theMessage.append("\n");
		
		
		return theMessage.toString();
	}
	
	
	/**
	 * Gets the Channel for {@link ChannelId#VOLTAGE_L1}.
	 *
	 * @return the Channel
	 */
	//
	public LongReadChannel getVoltageL12Channel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_L12);
	}
	public Value<Long> getVoltageL12() {
		return this.getVoltageL12Channel().value();
	}
	//
	public LongReadChannel getVoltageL23Channel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_L23);
	}
	public Value<Long> getVoltageL23() {
		return this.getVoltageL23Channel().value();
	}
	//
	public LongReadChannel getVoltageL31Channel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_L31);
	}
	public Value<Long> getVoltageL31() {
		return this.getVoltageL31Channel().value();
	}
	//
	public LongReadChannel getVoltageSysChannel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_SYS);
	}
	public Value<Long> getVoltageSys() {
		return this.getVoltageSysChannel().value();
	}
	//
	//
	//
	public LongReadChannel getCurrentNeutralChannel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_N);
	}
	public Value<Long> getCurrentNeutral() {
		return this.getCurrentNeutralChannel().value();
	}
	//
	public LongReadChannel getCurrentSysChannel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_SYS);
	}
	public Value<Long> getCurrentSys() {
		return this.getCurrentSysChannel().value();
	}
	//
	//
	//
	public LongReadChannel getPowerFactorL1Channel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FACTOR_L1);
	}
	public Value<Long> getPowerFactorL1() {
		return this.getPowerFactorL1Channel().value();
	}
	public LongReadChannel getPowerFactorL2Channel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FACTOR_L2);
	}
	public Value<Long> getPowerFactorL2() {
		return this.getPowerFactorL2Channel().value();
	}
	
	public LongReadChannel getPowerFactorL1And2Channel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FACTOR_L1And2);
	}
	public Value<Long> getPowerFactorL1And2() {
		return this.getPowerFactorL1And2Channel().value();
	}
	
	
	public LongReadChannel getPowerFactorL3Channel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FACTOR_L3);
	}
	public Value<Long> getPowerFactorL3() {
		return this.getPowerFactorL3Channel().value();
	}
	public LongReadChannel getPowerFactorSysChannel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FACTOR_SYS);
	}
	public Value<Long> getPowerFactorSys() {
		return this.getPowerFactorSysChannel().value();
	}
	

	public LongReadChannel getPowerFactorL3AndSysChannel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FACTOR_L3AndSys);
	}
	public Value<Long> getPowerFactorL3AndSys() {
		return this.getPowerFactorL3AndSysChannel().value();
	}

	//
	//
	//
	public LongReadChannel getApparentPowerL1Channel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.APPARENT_POWER_L1);
	}
	public Value<Long> getApparentPowerL1() {
		return this.getApparentPowerL1Channel().value();
	}
	public LongReadChannel getApparentPowerL2Channel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.APPARENT_POWER_L2);
	}
	public Value<Long> getApparentPowerL2() {
		return this.getApparentPowerL2Channel().value();
	}		
	public LongReadChannel getApparentPowerL3Channel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.APPARENT_POWER_L3);
	}
	public Value<Long> getApparentPowerL3() {
		return this.getApparentPowerL3Channel().value();
	}
	public LongReadChannel getApparentPowerSysChannel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.APPARENT_POWER_SYS);
	}
	public Value<Long> getApparentPowerSys() {
		return this.getApparentPowerSysChannel().value();
	}
	
	//
	//
	//
	public LongReadChannel getCustReactivePowerL1Channel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTREACTIVE_POWER_L1);
	}
	public Value<Long> getCustReactivePowerL1() {
		return this.getCustReactivePowerL1Channel().value();
	}
	public LongReadChannel getCustReactivePowerL2Channel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTREACTIVE_POWER_L2);
	}
	public Value<Long> getCustReactivePowerL2() {
		return this.getCustReactivePowerL2Channel().value();
	}
	public LongReadChannel getCustReactivePowerL3Channel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTREACTIVE_POWER_L3);
	}
	public Value<Long> getCustReactivePowerL3() {
		return this.getCustReactivePowerL3Channel().value();
	}
	public LongReadChannel getCustReactivePowerChannel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTREACTIVE_POWER);
	}
	public Value<Long> getCustReactivePower() {
		return this.getCustReactivePowerChannel().value();
	}

	

	
	public LongReadChannel getCActivePowerL1Channel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CACTIVE_POWER_L1);
	}
	public Value<Long> getCActivePowerL1() {
		return this.getCActivePowerL1Channel().value();
	}
	public LongReadChannel getCActivePowerL2Channel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CACTIVE_POWER_L2);
	}
	public Value<Long> getCActivePowerL2() {
		return this.getCActivePowerL3Channel().value();
	}
	public LongReadChannel getCActivePowerL3Channel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CACTIVE_POWER_L3);
	}
	public Value<Long> getCActivePowerL3() {
		return this.getCActivePowerL3Channel().value();
	}
	

	
	
	
	
	public LongReadChannel getCustActiveProductionEnergyChannel_L1() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVEPRODUCTION_ENERGY_L1);
	}
	public Value<Long> getCustActiveProductionEnergy_L1() {
		return this.getCustActiveProductionEnergyChannel_L1().value();
	}
	public LongReadChannel getCustActiveProductionEnergyChannel_L2() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVEPRODUCTION_ENERGY_L2);
	}
	public Value<Long> getCustActiveProductionEnergy_L2() {
		return this.getCustActiveProductionEnergyChannel_L2().value();
	}
	public LongReadChannel getCustActiveProductionEnergyChannel_L3() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVEPRODUCTION_ENERGY_L3);
	}
	public Value<Long> getCustActiveProductionEnergy_L3() {
		return this.getCustActiveProductionEnergyChannel_L3().value();
	}
	public LongReadChannel getCustActiveProductionEnergyChannel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVEPRODUCTION_ENERGY);
	}
	public Value<Long> getCustActiveProductionEnergy() {
		return this.getCustActiveProductionEnergyChannel().value();
	}
	
	public LongReadChannel getCustActiveConsumptionEnergyChannel_L1() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVECONSUMPTION_ENERGY_L1);
	}
	public Value<Long> getCustActiveConsumptionEnergy_L1() {
		return this.getCustActiveConsumptionEnergyChannel_L1().value();
	}
	public LongReadChannel getCustActiveConsumptionEnergyChannel_L2() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVECONSUMPTION_ENERGY_L2);
	}
	public Value<Long> getCustActiveConsumptionEnergy_L2() {
		return this.getCustActiveConsumptionEnergyChannel_L2().value();
	}
	public LongReadChannel getCustActiveConsumptionEnergyChannel_L3() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVECONSUMPTION_ENERGY_L3);
	}
	public Value<Long> getCustActiveConsumptionEnergy_L3() {
		return this.getCustActiveConsumptionEnergyChannel_L3().value();
	}
	public LongReadChannel getCustActiveConsumptionEnergyChannel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVECONSUMPTION_ENERGY);
	}
	public Value<Long> getCustActiveConsumptionEnergy() {
		return this.getCustActiveConsumptionEnergyChannel().value();
	}
	
	
	
	public LongReadChannel getRunningRegsChannel() {
		return this.channel(MeterAlgo2UEM1P5_4DS_E.ChannelId.METAS_COUNTER_REGSET_IN_USE);
	}
	public Value<Long> getRunningRegs(){
		return this.getRunningRegsChannel().value();
	}
	
	
	
	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				AsymmetricMeter.getModbusSlaveNatureTable(accessMode) //
		);

	}

}
