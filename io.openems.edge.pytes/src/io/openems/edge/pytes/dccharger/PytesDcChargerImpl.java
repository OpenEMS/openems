package io.openems.edge.pytes.dccharger;

import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

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
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pytes.enums.MeterDeviceType;
import io.openems.edge.pytes.ess.PytesJs3;
import io.openems.edge.pytes.metergrid.PytesMeterGrid;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Pytes.Hybrid.DcCharger", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class PytesDcChargerImpl extends AbstractOpenemsModbusComponent
		implements PytesDcCharger, EssDcCharger, ModbusComponent, OpenemsComponent, EventHandler, TimedataProvider, ModbusSlave {

	private Config config = null;
	
	

	public PytesDcChargerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				PytesDcCharger.ChannelId.values() //
		);
	}

	@Reference(
		    name = "ess",
		    policy = ReferencePolicy.STATIC,
		    policyOption = ReferencePolicyOption.GREEDY,
		    cardinality = ReferenceCardinality.MANDATORY
		)
		private volatile PytesJs3 ess;

	
	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;	
	
	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}	
	
	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;



	    if (super.activate(context, config.id(), config.alias(), config.enabled(),
	            this.ess.getUnitId(), this.cm, "Modbus",
	            this.ess.getModbusBridgeId())) {
	        return;
	    }	   
	    
	    if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(),
	            "ess", config.ess_id())) {
	        return;
	    }	    
	    
	    this.ess.addCharger(this);

	}

	@Deactivate
	protected void deactivate() {
	    if (this.ess != null) {
	        this.ess.removeCharger(this);
	    }
	    super.deactivate();
	}


	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			// TODO: fill channels
			break;
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this,


				// ---------------------------------------------------------------
				// PV Energy Counters (reg 33029..33039)
				// Priority LOW – historical totals, slow-changing
				// ---------------------------------------------------------------
				new FC4ReadInputRegistersTask(33029, Priority.LOW, //

						// Total PV energy since installation [kWh], resolution 1kWh
						m(PytesDcCharger.ChannelId.PV_ENERGY_TOTAL_KWH, new UnsignedDoublewordElement(33029)),
						
						// PV energy this month [kWh], resolution 1kWh
						m(PytesDcCharger.ChannelId.PV_ENERGY_MONTH_KWH, new UnsignedDoublewordElement(33031)),
						
						// PV energy last month [kWh], resolution 1kWh
						m(PytesDcCharger.ChannelId.PV_ENERGY_LAST_MONTH_KWH, new UnsignedDoublewordElement(33033)),
						
						// PV energy today [kWh], resolution 0.1kWh
						m(PytesDcCharger.ChannelId.PV_ENERGY_TODAY_KWH, new UnsignedWordElement(33035),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						
						// PV energy yesterday [1kWh], resolution 0.1kWh
						m(PytesDcCharger.ChannelId.PV_ENERGY_YESTERDAY_KWH, new UnsignedWordElement(33036),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						
						// PV energy this year [kWh], resolution 1kWh
						m(PytesDcCharger.ChannelId.PV_ENERGY_YEAR_KWH, new UnsignedDoublewordElement(33037)),
						
						// PV energy last year [kWh], resolution 1kWh
						m(PytesDcCharger.ChannelId.PV_ENERGY_LAST_YEAR_KWH, new UnsignedDoublewordElement(33039)),
						
						new DummyRegisterElement(33041, 33047),

						// DC Input Type (number of MPPT strings connected)
						// decoded to DcInputType enum
						m(PytesDcCharger.ChannelId.DC_INPUT_TYPE, new UnsignedWordElement(33048))
				),

				// ---------------------------------------------------------------
				// DC String Voltages and Currents – strings 1–4 (reg 33049..33056)
				// + Total PV Power (reg 33057–33058)
				// Priority HIGH – real-time PV monitoring
				// ---------------------------------------------------------------
				new FC4ReadInputRegistersTask(33049, Priority.HIGH,
 
						// DC string 1 voltage [mV], resolution 0.1V
						m(PytesDcCharger.ChannelId.DC_VOLTAGE_1, new UnsignedWordElement(33049),
								ElementToChannelConverter.SCALE_FACTOR_2),
 
						// DC string 1 current [mA], resolution 0.1A
						m(PytesDcCharger.ChannelId.DC_CURRENT_1, new UnsignedWordElement(33050),
								ElementToChannelConverter.SCALE_FACTOR_2),
 
						// DC string 2 voltage [mV], resolution 0.1V
						m(PytesDcCharger.ChannelId.DC_VOLTAGE_2, new UnsignedWordElement(33051),
								ElementToChannelConverter.SCALE_FACTOR_2),
 
						// reg 33052 – DC string 2 current [mA]
						m(PytesDcCharger.ChannelId.DC_CURRENT_2, new UnsignedWordElement(33052),
								ElementToChannelConverter.SCALE_FACTOR_2),
 
						// DC string 3 voltage [mV], resolution 0.1V
						m(PytesDcCharger.ChannelId.DC_VOLTAGE_3, new UnsignedWordElement(33053),
								ElementToChannelConverter.SCALE_FACTOR_2),
 
						// reg 33054 – DC string 3 current [mA]
						m(PytesDcCharger.ChannelId.DC_CURRENT_3, new UnsignedWordElement(33054),
								ElementToChannelConverter.SCALE_FACTOR_2),
 
						// DC string 4 voltage [mV], resolution 0.1V
						m(PytesDcCharger.ChannelId.DC_VOLTAGE_4, new UnsignedWordElement(33055),
								ElementToChannelConverter.SCALE_FACTOR_2),
 
						// reg 33056 – DC string 4 current [mA]
						m(PytesDcCharger.ChannelId.DC_CURRENT_4, new UnsignedWordElement(33056),
								ElementToChannelConverter.SCALE_FACTOR_2),
 
						// reg 33057–33058 – Total DC output power / Total PV Power [W]
						// U32 (2 registers), 1 W resolution → no converter needed
						m(EssDcCharger.ChannelId.ACTUAL_POWER, new UnsignedDoublewordElement(33057))
				),
 
				// ---------------------------------------------------------------
				// DC String Voltages and Currents – strings 5–8 (reg 33059..33066)
				// Priority HIGH – real-time PV monitoring
				// ---------------------------------------------------------------
				new FC4ReadInputRegistersTask(33059, Priority.HIGH,
 
						// reg 33059 – DC string 5 voltage [mV]
						m(PytesDcCharger.ChannelId.DC_VOLTAGE_5, new UnsignedWordElement(33059),
								ElementToChannelConverter.SCALE_FACTOR_2),
 
						// reg 33060 – DC string 5 current [mA]
						m(PytesDcCharger.ChannelId.DC_CURRENT_5, new UnsignedWordElement(33060),
								ElementToChannelConverter.SCALE_FACTOR_2),
 
						// reg 33061 – DC string 6 voltage [mV]
						m(PytesDcCharger.ChannelId.DC_VOLTAGE_6, new UnsignedWordElement(33061),
								ElementToChannelConverter.SCALE_FACTOR_2),
 
						// reg 33062 – DC string 6 current [mA]
						m(PytesDcCharger.ChannelId.DC_CURRENT_6, new UnsignedWordElement(33062),
								ElementToChannelConverter.SCALE_FACTOR_2),
 
						// reg 33063 – DC string 7 voltage [mV]
						m(PytesDcCharger.ChannelId.DC_VOLTAGE_7, new UnsignedWordElement(33063),
								ElementToChannelConverter.SCALE_FACTOR_2),
 
						// reg 33064 – DC string 7 current [mA]
						m(PytesDcCharger.ChannelId.DC_CURRENT_7, new UnsignedWordElement(33064),
								ElementToChannelConverter.SCALE_FACTOR_2),
 
						// reg 33065 – DC string 8 voltage [mV]
						m(PytesDcCharger.ChannelId.DC_VOLTAGE_8, new UnsignedWordElement(33065),
								ElementToChannelConverter.SCALE_FACTOR_2),
 
						// reg 33066 – DC string 8 current [mA]
						m(PytesDcCharger.ChannelId.DC_CURRENT_8, new UnsignedWordElement(33066),
								ElementToChannelConverter.SCALE_FACTOR_2)
				)
		);

	}


	@Override
	public String debugLog() {
		return "L:" + this.getActualPower().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
	    return new ModbusSlaveTable(
	            OpenemsComponent.getModbusSlaveNatureTable(accessMode),
	            EssDcCharger.getModbusSlaveNatureTable(accessMode)
	            // + ModbusSlaveNatureTable.of(PytesDcCharger.class, accessMode, 100).build()
	    );
	}


	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public void retryModbusCommunication() {
		// TODO Auto-generated method stub

	}

}
