package io.openems.edge.evcs.openwb;

import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.Evcs;

public interface EvcsOpenWb extends Evcs, OpenemsComponent, MqttComponent {

}
