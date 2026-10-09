package io.openems.edge.battery.custom;



import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface BatteryCustom
        extends Battery, ModbusComponent, OpenemsComponent {
	
	

   
}