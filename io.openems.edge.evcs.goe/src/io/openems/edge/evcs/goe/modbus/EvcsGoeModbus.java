package io.openems.edge.evcs.goe.modbus;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.goe.api.EvcsGoe;
import io.openems.edge.meter.api.ElectricityMeter;

public interface EvcsGoeModbus extends EvcsGoe, Evcs, ElectricityMeter, OpenemsComponent {


}