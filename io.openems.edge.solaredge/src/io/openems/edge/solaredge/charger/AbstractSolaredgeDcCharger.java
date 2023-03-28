package io.openems.edge.solaredge.charger;

import java.util.Map;

import org.osgi.service.event.EventHandler;



import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.sunspec.AbstractOpenemsSunSpecComponent;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.common.component.OpenemsComponent;
//import io.openems.edge.common.component.OpenemsComponent.ChannelId;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.dccharger.api.EssDcCharger;



public abstract class AbstractSolaredgeDcCharger extends AbstractOpenemsSunSpecComponent 
	implements EssDcCharger, ModbusComponent, OpenemsComponent,
	EventHandler, ModbusSlave {



	protected void onSunSpecInitializationCompleted() {
		// TODO Auto-generated method stub
		
	}

	
	public AbstractSolaredgeDcCharger(Map<SunSpecModel, Priority> activeModels,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) throws OpenemsException {
		
		super(activeModels, firstInitialChannelIds, furtherInitialChannelIds);
		
	}

	
	

}
