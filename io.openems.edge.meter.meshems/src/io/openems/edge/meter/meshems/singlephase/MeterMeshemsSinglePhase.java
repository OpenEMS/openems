package io.openems.edge.meter.meshems.singlephase;

import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;

public interface MeterMeshemsSinglePhase
		extends ElectricityMeter, SinglePhaseMeter, OpenemsComponent, MqttComponent {

}
