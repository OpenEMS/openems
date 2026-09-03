package io.openems.edge.meter.mqtt;

import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;

public interface MeterMqtt extends ElectricityMeter, OpenemsComponent, MqttComponent {

}
