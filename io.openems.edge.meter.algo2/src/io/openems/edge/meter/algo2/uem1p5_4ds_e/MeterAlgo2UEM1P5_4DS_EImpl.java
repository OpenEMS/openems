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
import io.openems.edge.bridge.modbus.api.element.SignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.FloatReadChannel;
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
	public String readEnablerMask =	
		"[VFloat]"
		+ "[AFloat]"
		+ "[PFactorFloat]" 
		+ "[PActiveFloat]" 
		+ "[PApparentFloat]" 
		+ "[PReactiveFloat]"
		+ "[FreqPhSeqFloat]"
		+ "[EImportExportActiveFloat]"
		
		

// olds ...
//		+ "[CEnActProd]" 
//		+ "[CEnActCons]" 
//		+ "[PAct]"
		// + "[AAlt]"
		// + "[A]"
		//+ "[VCross]"
		//+ "[V]"
		;
	
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
		
		ModbusProtocol modbusProtocol = new ModbusProtocol(this);
		
		if (readEnablerMask.indexOf("[VFloat]") >= 0) {
			modbusProtocol.addTask(
				new FC3ReadRegistersTask(0x1000, Priority.HIGH, //
				    m(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_FL1, new FloatDoublewordElement(0x1000), 
			    		ElementToChannelConverter.DIRECT_1_TO_1), //
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_FL2, new FloatDoublewordElement(0x1002),
					    ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_FL3, new FloatDoublewordElement(0x1004),
						    ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_FL12, new FloatDoublewordElement(0x1006),
						    ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_FL23, new FloatDoublewordElement(0x1008),
						    ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_FL31, new FloatDoublewordElement(0x100A),
						    ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_FSYS, new FloatDoublewordElement(0x100C),
						    ElementToChannelConverter.DIRECT_1_TO_1)
				)
			);		
		}
		if (readEnablerMask.indexOf("[AFloat]") >= 0) {
			modbusProtocol.addTask(
				new FC3ReadRegistersTask(0x100E, Priority.HIGH, //
				    m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_FA1, new FloatDoublewordElement(0x100E), 
				    		ElementToChannelConverter.DIRECT_1_TO_1), //
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_FA2, new FloatDoublewordElement(0x1010),
					    ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_FA3, new FloatDoublewordElement(0x1012),
						    ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_FN, new FloatDoublewordElement(0x1014),
						    ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_FSYS, new FloatDoublewordElement(0x1016),
						    ElementToChannelConverter.DIRECT_1_TO_1) // ,
				)
			);
		} 
		if (readEnablerMask.indexOf("[PFactorFloat]") >= 0) {
			modbusProtocol.addTask(
				new FC3ReadRegistersTask(0x1018, Priority.HIGH, //
				    m(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWERFACTOR_FPF1, new FloatDoublewordElement(0x1018), 
				    		ElementToChannelConverter.DIRECT_1_TO_1), //
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWERFACTOR_FPF2, new FloatDoublewordElement(0x101A),
					    ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWERFACTOR_FPF3, new FloatDoublewordElement(0x101C),
						    ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWERFACTOR_FPFSYS, new FloatDoublewordElement(0x101E),
						    ElementToChannelConverter.DIRECT_1_TO_1)
				)
			);
		} 
		if (readEnablerMask.indexOf("[PActiveFloat]") >= 0) {
			modbusProtocol.addTask(
				new FC3ReadRegistersTask(0x1020, Priority.HIGH, //
				    m(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FP1, new FloatDoublewordElement(0x1020), 
				    		ElementToChannelConverter.DIRECT_1_TO_1), //
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FP2, new FloatDoublewordElement(0x1022),
					    ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FP3, new FloatDoublewordElement(0x1024),
						    ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FPSYS, new FloatDoublewordElement(0x1026),
						    ElementToChannelConverter.DIRECT_1_TO_1)
				)
			);
		} 
		if (readEnablerMask.indexOf("[PApparentFloat]") >= 0) {
			modbusProtocol.addTask(
				new FC3ReadRegistersTask(0x1028, Priority.HIGH, //
				    m(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FS1, new FloatDoublewordElement(0x1028), 
				    		ElementToChannelConverter.DIRECT_1_TO_1), //
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FS2, new FloatDoublewordElement(0x102A),
					    ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FS3, new FloatDoublewordElement(0x102C),
						    ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FSSYS, new FloatDoublewordElement(0x102E),
						    ElementToChannelConverter.DIRECT_1_TO_1)
				)
			);
		} 
		if (readEnablerMask.indexOf("[PReactiveFloat]") >= 0) {
			modbusProtocol.addTask(
				new FC3ReadRegistersTask(0x1030, Priority.HIGH, //
				    m(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FQ1, new FloatDoublewordElement(0x1030), 
				    		ElementToChannelConverter.DIRECT_1_TO_1), //
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FQ2, new FloatDoublewordElement(0x1032),
					    ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FQ3, new FloatDoublewordElement(0x1034),
						    ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FQSYS, new FloatDoublewordElement(0x1036),
						    ElementToChannelConverter.DIRECT_1_TO_1)
				)
			);
		}
		//
		if (readEnablerMask.indexOf("[FreqPhSeqFloat]") >= 0) {
			modbusProtocol.addTask(
				new FC3ReadRegistersTask(0x1038, Priority.HIGH, //
				    m(MeterAlgo2UEM1P5_4DS_E.ChannelId.FREQUENCY_FF, new FloatDoublewordElement(0x1038), 
				    		ElementToChannelConverter.DIRECT_1_TO_1), //
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.PHASES_FSEQ, new FloatDoublewordElement(0x103A),
					    ElementToChannelConverter.DIRECT_1_TO_1)
				)
			);
		}
		//
		//
		//
		//
		if (readEnablerMask.indexOf("[EImportExportActiveFloat]") >= 0) {
			modbusProtocol.addTask(
				new FC3ReadRegistersTask(0x1100, Priority.HIGH, //
				    m(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEACT1, new FloatDoublewordElement(0x1100), 
				    		ElementToChannelConverter.DIRECT_1_TO_1), //
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEACT2, new FloatDoublewordElement(0x1102),
					    ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEACT3, new FloatDoublewordElement(0x1104),
						    ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEACTSYS, new FloatDoublewordElement(0x1106),
						    ElementToChannelConverter.DIRECT_1_TO_1),
				    m(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEACT1, new FloatDoublewordElement(0x1108), 
				    		ElementToChannelConverter.DIRECT_1_TO_1), //
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEACT2, new FloatDoublewordElement(0x110A),
					    ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEACT3, new FloatDoublewordElement(0x110C),
						    ElementToChannelConverter.DIRECT_1_TO_1),
					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEACTSYS, new FloatDoublewordElement(0x110E),
						    ElementToChannelConverter.DIRECT_1_TO_1)
				)
			);
		}
		
		
		
		//
		modbusProtocol.addTask(
			new FC3ReadRegistersTask(MODBUSREG_SET0_REGSETINUSE, Priority.LOW, //
				m(MeterAlgo2UEM1P5_4DS_E.ChannelId.METAS_COUNTER_REGSET_IN_USE, new UnsignedWordElement(MODBUSREG_SET0_REGSETINUSE))
			)
		);
		
		
		
//		if (readEnablerMask.indexOf("[V]") >= 0) {
//			modbusProtocol.addTask(
//				new FC3ReadRegistersTask(0x00, Priority.HIGH, //
//				    m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new SignedDoublewordElement(0x00), 
//			    		ElementToChannelConverter.SCALE_FACTOR_MINUS_3), //
//					m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new SignedDoublewordElement(0x02),
//					    ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
//					m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new SignedDoublewordElement(0x04),
//					    ElementToChannelConverter.SCALE_FACTOR_MINUS_3)
//				)
//			);		
//		}
//		if (readEnablerMask.indexOf("[VCross]") >= 0) {
//			modbusProtocol.addTask(
//				new FC3ReadRegistersTask(0x06, Priority.HIGH, //
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_L12, new SignedDoublewordElement(0x06),
//						ElementToChannelConverter.DIRECT_1_TO_1),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_L23, new SignedDoublewordElement(0x08),
//						ElementToChannelConverter.DIRECT_1_TO_1),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_L31, new SignedDoublewordElement(0x0A),
//						ElementToChannelConverter.DIRECT_1_TO_1),
//				    m(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_SYS, new SignedDoublewordElement(0x0C), 
//				    		ElementToChannelConverter.DIRECT_1_TO_1) //
//				)
//			);				    
//		}
//		// the following are mutually exclusive
//		if (readEnablerMask.indexOf("[A]") >= 0) {
//			modbusProtocol.addTask(
//				new FC3ReadRegistersTask(0x0E, Priority.HIGH, //
//				    m(AsymmetricMeter.ChannelId.CURRENT_L1, new SignedDoublewordElement(0x0E), 
//				    		ElementToChannelConverter.DIRECT_1_TO_1), //
//					m(AsymmetricMeter.ChannelId.CURRENT_L2, new SignedDoublewordElement(0x10),
//					    ElementToChannelConverter.DIRECT_1_TO_1),
//					m(AsymmetricMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(0x12),
//						    ElementToChannelConverter.DIRECT_1_TO_1),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_N, new SignedDoublewordElement(0x14),
//						    ElementToChannelConverter.DIRECT_1_TO_1),
//					
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_SYS, new SignedDoublewordElement(0x16),
//						    ElementToChannelConverter.DIRECT_1_TO_1) // ,
//				)
//			);
//		} else
////		else if (readEnablerMask.indexOf("[AAlt]") >= 0) {
////			modbusProtocol.addTask(
////				new FC3ReadRegistersTask(0x0E, Priority.HIGH, //
////				    m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_A1, new SignedDoublewordElement(0x0E), 
////				    		ElementToChannelConverter.DIRECT_1_TO_1), //
////					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_A2, new SignedDoublewordElement(0x10),
////					    ElementToChannelConverter.DIRECT_1_TO_1),
////					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_A3, new SignedDoublewordElement(0x12),
////						    ElementToChannelConverter.DIRECT_1_TO_1),
////					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_N, new SignedDoublewordElement(0x14),
////						    ElementToChannelConverter.DIRECT_1_TO_1)
////				)
////			);
////		}
//		if (readEnablerMask.indexOf("[PAct]") >= 0) {
//			modbusProtocol.addTask(
//				new FC3ReadRegistersTask(0x1C, Priority.LOW, //
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CACTIVE_POWER_L1, new Algo3WordImpl(0x1C),
//					ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CACTIVE_POWER_L2, new Algo3WordImpl(0x1F), // 0x1F
//					ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CACTIVE_POWER_L3, new Algo3WordImpl(0x22), //0x22
//					ElementToChannelConverter.INVERT_IF_TRUE(this.invert)/*, ElementToChannelConverter.SCALE_FACTOR_MINUS_3 */),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CACTIVE_POWER, new Algo3WordImpl(0x25), // 0x25
//					ElementToChannelConverter.INVERT_IF_TRUE(this.invert))
//				)
//			);
//		}
//		if (readEnablerMask.indexOf("[CEnActCons]") >= 0) {
//			modbusProtocol.addTask(
//				new FC3ReadRegistersTask(MODBUSREG_SET0_SYSIMPORTEDACTIVEENERGY_l1, Priority.HIGH, //
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVECONSUMPTION_ENERGY_L1, new Algo3WordImpl(MODBUSREG_SET0_SYSIMPORTEDACTIVEENERGY_l1),
//							ElementToChannelConverter.DIRECT_1_TO_1),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVECONSUMPTION_ENERGY_L2, new Algo3WordImpl(MODBUSREG_SET0_SYSIMPORTEDACTIVEENERGY_L2),
//							ElementToChannelConverter.DIRECT_1_TO_1),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVECONSUMPTION_ENERGY_L3, new Algo3WordImpl(MODBUSREG_SET0_SYSIMPORTEDACTIVEENERGY_L3),
//							ElementToChannelConverter.DIRECT_1_TO_1),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVECONSUMPTION_ENERGY, new Algo3WordImpl(MODBUSREG_SET0_SYSIMPORTEDACTIVEENERGY),
//							ElementToChannelConverter.DIRECT_1_TO_1)
//				)
//			);
//		}
//		if (readEnablerMask.indexOf("[CEnActProd]") >= 0) {
//			modbusProtocol.addTask(
//					new FC3ReadRegistersTask(MODBUSREG_SET0_SYSEXPORTEDACTIVEENERGY_L1, Priority.HIGH, //
//			
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVEPRODUCTION_ENERGY_L1, new Algo3WordImpl(MODBUSREG_SET0_SYSEXPORTEDACTIVEENERGY_L1),
//							ElementToChannelConverter.DIRECT_1_TO_1),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVEPRODUCTION_ENERGY_L2, new Algo3WordImpl(MODBUSREG_SET0_SYSEXPORTEDACTIVEENERGY_L2),
//							ElementToChannelConverter.DIRECT_1_TO_1),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVEPRODUCTION_ENERGY_L3, new Algo3WordImpl(MODBUSREG_SET0_SYSEXPORTEDACTIVEENERGY_L3),
//							ElementToChannelConverter.DIRECT_1_TO_1),
//					m(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVEPRODUCTION_ENERGY, new Algo3WordImpl(MODBUSREG_SET0_SYSEXPORTEDACTIVEENERGY),
//							ElementToChannelConverter.DIRECT_1_TO_1)
//				)
//			);
//		}
		
		
		
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
		long runningRegsSet = this.getRunningRegs().asOptional().get();
		theMessage.append("\n - runningRegs " + runningRegsSet);
		//
		//
		//
		//
				
		theMessage.append("\n********************************************");
		theMessage.append("\n*                                          *");
		theMessage.append("\n*        device id: " + this.id() +"                *");
		theMessage.append("\n*                                          *");
		msgFormatter.format("\n* runningRegs: %d                           *", runningRegsSet );
		msgFormatter.format("\n* dump registers set: %s    	           *", readEnablerMask);	
		theMessage.append("\n*                                          *");
		theMessage.append("\n*                                          *");
		theMessage.append("\n********************************************");
		if (readEnablerMask.indexOf("[VFloat]") >= 0) {
			//
			theMessage.append("\n - V1, V2, V3 - ");
			theMessage.append(this.getFVoltageL1().asString() + " " + this.getFVoltageL2().asString() + " " + this.getFVoltageL3().asString());			
			theMessage.append("\n - VSys - " + this.getFVoltageSys().asString());
			theMessage.append("\n - V12, V23, V31  - ");
			theMessage.append(this.getFVoltageL12().asString() + " " + this.getFVoltageL23().asString() + " " + this.getFVoltageL31().asString());
			
		}
		if (readEnablerMask.indexOf("[AFloat]") >= 0) {
			theMessage.append("\n - A1, A2, A3  - ");
			theMessage.append(this.getFCurrentL1().asString() + " " + this.getFCurrentL2().asString() + " " + this.getFCurrentL3().asString());
			theMessage.append("\n - AN  - " + this.getFCurrentNeutral().asString());
			theMessage.append("\n - ASys  - " + this.getFCurrentSys().asString());
		}
		if (readEnablerMask.indexOf("[PActiveFloat]") >= 0) {
			double ACP1 = ((double) this.getFPowerActiveL1().asOptional().get())/1000;
			double ACP2 = ((double) this.getFPowerActiveL2().asOptional().get())/1000;
			double ACP3 = ((double) this.getFPowerActiveL3().asOptional().get())/1000;
			double ACPSys = ((double) this.getFPowerActiveSys().asOptional().get())/1000;
			
			msgFormatter.format("\n\n - Active power (KW)  ACP1: %f, ACP2: %f, ACP3: %f"
					, (float) ACP1, (float) ACP2, (float) ACP3);
			msgFormatter.format("\n - Total Active power (KW) ACPSys: %f", (float) ACPSys);			
		}
		if (readEnablerMask.indexOf("[EImportExportActiveFloat]") >= 0) {
			float activeConsumptionEnergyL1 = this.getFActiveImportedEnergy_L1().asOptional().get()/1000;
			float activeConsumptionEnergyL2 = this.getFActiveImportedEnergy_L2().asOptional().get()/1000;
			float activeConsumptionEnergyL3 = this.getFActiveImportedEnergy_L3().asOptional().get()/1000;
			float activeConsumptionEnergyL123 = activeConsumptionEnergyL1 + activeConsumptionEnergyL2 + activeConsumptionEnergyL3;
			float activeConsumptionEnergy = this.getFActiveImportedEnergy_Sys().asOptional().get()/1000;
			msgFormatter.format("\n - Active Imported Energy (KW) L1: %f, L2: %f, L3: %f"
					, (float) activeConsumptionEnergyL1, (float) activeConsumptionEnergyL2, (float) activeConsumptionEnergyL3);
			msgFormatter.format("\n - Total Active Imported Energy sum (KW) : %f, from meter: %f"
					, (float) activeConsumptionEnergyL123, (float) activeConsumptionEnergy);			
			//
			float activeProductionEnergyL1 = this.getFActiveExportedEnergy_L1().asOptional().get()/1000;
			float activeProductionEnergyL2 = this.getFActiveExportedEnergy_L2().asOptional().get()/1000;
			float activeProductionEnergyL3 = this.getFActiveExportedEnergy_L3().asOptional().get()/1000;
			float activeProductionEnergyL123 = activeProductionEnergyL1 + activeProductionEnergyL2 + activeProductionEnergyL3;
			float activeProductionEnergy = this.getFActiveExportedEnergy_Sys().asOptional().get()/1000;
			msgFormatter.format("\n\n - Active Exported Energy (KW) L1: %f, L2: %f, L3: %f"
					, (float) activeProductionEnergyL1, (float) activeProductionEnergyL2, (float) activeProductionEnergyL3);
			msgFormatter.format("\n - Total Active Exported Energy sum (KW): %f ; from meter: %f"
					, (float) activeProductionEnergyL123, (float) activeProductionEnergy);
			
		}
		
		//
		//
		//
		//
		//
		//
		//
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
		
		msgFormatter.close();
		return theMessage.toString();
	}
	
	
	//
	// generic ChannelId get LongReadChannel
	public LongReadChannel getLongGenericChannel(io.openems.edge.common.channel.ChannelId theChannel) {
		return this.channel(theChannel);
	}
	//
	// generic ChannelId get FloatReadChannel
	public FloatReadChannel getFloatGenericChannel(io.openems.edge.common.channel.ChannelId theChannel) {
		return this.channel(theChannel);
	}
	

	public Value<Float> getFVoltageL1() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_FL1).value();
	}
	public Value<Float> getFVoltageL2() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_FL2).value();
	}
	public Value<Float> getFVoltageL3() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_FL3).value();
	}
	public Value<Float> getFCurrentNeutral() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_FN).value();
	}
	//
	public Value<Float> getFVoltageL12() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_FL12).value();
	}
	public Value<Float> getFVoltageL23() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_FL23).value();
	}
	public Value<Float> getFVoltageL31() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_FL31).value();
	}
	public Value<Float> getFVoltageSys() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_FSYS).value();
	}
	//
	public Value<Float> getFCurrentL1() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_FA1).value();
	}
	public Value<Float> getFCurrentL2() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_FA2).value();
	}
	public Value<Float> getFCurrentL3() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_FA3).value();
	}
	public Value<Float> getFCurrentSys() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_FSYS).value();
	}
	//
	public Value<Float> getFPowerActiveL1() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FP1).value();
	}
	public Value<Float> getFPowerActiveL2() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FP2).value();
	}
	public Value<Float> getFPowerActiveL3() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FP3).value();
	}
	public Value<Float> getFPowerActiveSys() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FPSYS).value();
	}
	//
	public Value<Float> getFPowerApparentL1() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FQ1).value();
	}
	public Value<Float> getFPowerApparentL2() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FQ2).value();
	}
	public Value<Float> getFPowerApparentL3() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FQ3).value();
	}
	public Value<Float> getFPowerApparentSys() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FQSYS).value();
	}
	//
	public Value<Float> getFPowerReactiveL1() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FS1).value();
	}
	public Value<Float> getFPowerReactiveL2() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FS2).value();
	}
	public Value<Float> getFPowerReactiveL3() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FS3).value();
	}
	public Value<Float> getFPowerReactiveSys() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FSSYS).value();
	}
	//
	public Value<Float> getFPowerFactorL1() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWERFACTOR_FPF1).value();
	}
	public Value<Float> getFPowerFactorL2() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWERFACTOR_FPF2).value();
	}
	public Value<Float> getFPowerFactorL3() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWERFACTOR_FPF3).value();
	}
	public Value<Float> getFPowerFactorSys() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWERFACTOR_FPFSYS).value();
	}
	//
	public Value<Float> getFPhasesSequence() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWERFACTOR_FPF1).value();
	}
	public Value<Float> getFFrequency() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWERFACTOR_FPF2).value();
	}
	//
	// ================================================================================================================
	//
	// imported energy components
	//
	// ACTIVE
	public Value<Float> getFActiveImportedEnergy_L1() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEACT1).value();
	}
	public Value<Float> getFActiveImportedEnergy_L2() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEACT2).value();
	}
	public Value<Float> getFActiveImportedEnergy_L3() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEACT3).value();
	}
	public Value<Float> getFActiveImportedEnergy_Sys() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEACTSYS).value();
	}
	//
	// APPARENT INDUCTIVE
	public Value<Float> getFApparentImportedIndEnergy_L1() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEAPPIND1).value();
	}
	public Value<Float> getFApparentImportedIndEnergy_L2() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEAPPIND2).value();
	}
	public Value<Float> getFApparentImportedIndEnergy_L3() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEAPPIND3).value();
	}
	public Value<Float> getFApparentImportedIndEnergy_LSys() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEAPPINDSYS).value();
	}
	// APPARENT CAPACITIVE
	public Value<Float> getFApparentImportedCapEnergy_L1() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEAPPCAP1).value();
	}
	public Value<Float> getFApparentImportedCapEnergy_L2() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEAPPCAP2).value();
	}
	public Value<Float> getFApparentImportedCapEnergy_L3() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEAPPCAP3).value();
	}
	public Value<Float> getFApparentImportedCapEnergy_LSys() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEAPPCAPSYS).value();
	}
	//
	// REACTIVE INDUCTIVE
	public Value<Float> getFReactiveImportedIndEnergy_L1() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEREAIND1).value();
	}
	public Value<Float> getFReactiveImportedIndEnergy_L2() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEREAIND2).value();
	}
	public Value<Float> getFReactiveImportedIndEnergy_L3() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEREAIND3).value();
	}
	public Value<Float> getFReactiveImportedIndEnergy_LSys() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEREAINDSYS).value();
	}
	// REACTIVE CAPACITIVE
	public Value<Float> getFReactiveImportedCapEnergy_L1() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEREACAP1).value();
	}
	public Value<Float> getFReactiveImportedCapEnergy_L2() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEREACAP2).value();
	}
	public Value<Float> getFReactiveImportedCapEnergy_L3() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEREACAP3).value();
	}
	public Value<Float> getFReactiveImportedCapEnergy_LSys() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_IMP_FEREACAPSYS).value();
	}
	//
	// ================================================================================================================
	//
	// exported energy components
	//
	// ACTIVE
	public Value<Float> getFActiveExportedEnergy_L1() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEACT1).value();
	}
	public Value<Float> getFActiveExportedEnergy_L2() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEACT2).value();
	}
	public Value<Float> getFActiveExportedEnergy_L3() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEACT3).value();
	}
	public Value<Float> getFActiveExportedEnergy_Sys() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEACTSYS).value();
	}
	//
	// APPARENT INDUCTIVE
	public Value<Float> getFApparentExportedIndEnergy_L1() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEAPPIND1).value();
	}
	public Value<Float> getFApparentExportedIndEnergy_L2() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEAPPIND2).value();
	}
	public Value<Float> getFApparentExportedIndEnergy_L3() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEAPPIND3).value();
	}
	public Value<Float> getFApparentExportedIndEnergy_LSys() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEAPPINDSYS).value();
	}
	// APPARENT CAPACITIVE
	public Value<Float> getFApparentExportedCapEnergy_L1() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEAPPCAP1).value();
	}
	public Value<Float> getFApparentExportedCapEnergy_L2() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEAPPCAP2).value();
	}
	public Value<Float> getFApparentExportedCapEnergy_L3() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEAPPCAP3).value();
	}
	public Value<Float> getFApparentExportedCapEnergy_LSys() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEAPPCAPSYS).value();
	}
	//
	// REACTIVE INDUCTIVE
	public Value<Float> getFReactiveExportedIndEnergy_L1() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEREAIND1).value();
	}
	public Value<Float> getFReactiveExportedIndEnergy_L2() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEREAIND2).value();
	}
	public Value<Float> getFReactiveExportedIndEnergy_L3() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEREAIND3).value();
	}
	public Value<Float> getFReactiveExportedIndEnergy_LSys() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEREAINDSYS).value();
	}
	// REACTIVE CAPACITIVE
	public Value<Float> getFReactiveExportedCapEnergy_L1() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEREACAP1).value();
	}
	public Value<Float> getFReactiveExportedCapEnergy_L2() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEREACAP2).value();
	}
	public Value<Float> getFReactiveExportedCapEnergy_L3() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEREACAP3).value();
	}
	public Value<Float> getFReactiveExportedCapEnergy_LSys() {
		return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_EXP_FEREACAPSYS).value();
	}

	
	
	//public Value<Float> getFActiveExportedEnergy_L1() {
//	return this.getFloatGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.ENERGY_FEACT1).value();
//}

	
	
	
	
	
//	//
//	public Value<Long> getVoltageL12() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_L12).value();
//	}
//	public Value<Long> getVoltageL23() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_L23).value();
//	}
//	public Value<Long> getVoltageL31() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_L31).value();
//	}
//	public Value<Long> getVoltageSys() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.VOLTAGE_SYS).value();
//	}
//	//
//	//
//	//
//	public Value<Long> getCurrentNeutral() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_N).value();
//	}
//	public Value<Long> getCurrentSys() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_SYS).value();
//	}
//	//
//	//
//	//
//	public Value<Long> getPowerFactorL1() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FACTOR_L1).value();
//	}
//	public Value<Long> getPowerFactorL2() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FACTOR_L2).value();
//	}
//	public Value<Long> getPowerFactorL3() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FACTOR_L3).value();
//	}
//	public Value<Long> getPowerFactorSys() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FACTOR_SYS).value();
//	}
//	public Value<Long> getPowerFactorL1And2() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FACTOR_L1And2).value();
//	}
//	public Value<Long> getPowerFactorL3AndSys() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.POWER_FACTOR_L3AndSys).value();
//	}
//	//
//	//
//	//
//	public Value<Long> getApparentPowerL1() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.APPARENT_POWER_L1).value();
//	}
//	public Value<Long> getApparentPowerL2() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.APPARENT_POWER_L2).value();
//	}		
//	public Value<Long> getApparentPowerL3() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.APPARENT_POWER_L3).value();
//	}
//	public Value<Long> getApparentPowerSys() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.APPARENT_POWER_SYS).value();
//	}
//	
//	//
//	//
//	//
//	public Value<Long> getCustReactivePowerL1() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTREACTIVE_POWER_L1).value();
//	}
//	public Value<Long> getCustReactivePowerL2() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTREACTIVE_POWER_L2).value();
//	}
//	public Value<Long> getCustReactivePowerL3() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTREACTIVE_POWER_L3).value();
//	}
//	public Value<Long> getCustReactivePower() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTREACTIVE_POWER).value();
//	}
//
//	
//	public Value<Long> getCCurrentL1() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_A1).value();
//	}
//	public Value<Long> getCCurrentL2() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_A2).value();
//	}
//	public Value<Long> getCCurrentL3() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_A3).value();
//	}
//	public Value<Long> getCCurrentN() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CURRENT_N).value();
//	}



	
	
	
	
	
//	public Value<Long> getCActivePowerL1() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CACTIVE_POWER_L1).value();
//	}
//	public Value<Long> getCActivePowerL2() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CACTIVE_POWER_L2).value();
//	}
//	public Value<Long> getCActivePowerL3() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CACTIVE_POWER_L3).value();
//	}
//	public Value<Long> getCActivePower() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CACTIVE_POWER).value();
//	}
//	
//
//	
//	
//	
//	
//	public Value<Long> getCustActiveProductionEnergy_L1() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVEPRODUCTION_ENERGY_L1).value();
//	}
//	public Value<Long> getCustActiveProductionEnergy_L2() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVEPRODUCTION_ENERGY_L2).value();
//	}
//	public Value<Long> getCustActiveProductionEnergy_L3() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVEPRODUCTION_ENERGY_L3).value();
//	}
//	public Value<Long> getCustActiveProductionEnergy() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVEPRODUCTION_ENERGY).value();
//	}
//	
//	public Value<Long> getCustActiveConsumptionEnergy_L1() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVECONSUMPTION_ENERGY_L1).value();
//	}
//	public Value<Long> getCustActiveConsumptionEnergy_L2() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVECONSUMPTION_ENERGY_L2).value();
//	}
//	public Value<Long> getCustActiveConsumptionEnergy_L3() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVECONSUMPTION_ENERGY_L3).value();
//	}
//	public Value<Long> getCustActiveConsumptionEnergy() {
//		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.CUSTACTIVECONSUMPTION_ENERGY).value();
//	}
	
	
	public Value<Long> getRunningRegs(){
		return this.getLongGenericChannel(MeterAlgo2UEM1P5_4DS_E.ChannelId.METAS_COUNTER_REGSET_IN_USE).value();
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
