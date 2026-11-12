package io.openems.edge.meter.meshems.threephase;

import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;

public interface MeterMeshemsThreePhase extends ElectricityMeter, OpenemsComponent, MqttComponent, ModbusSlave {

}
