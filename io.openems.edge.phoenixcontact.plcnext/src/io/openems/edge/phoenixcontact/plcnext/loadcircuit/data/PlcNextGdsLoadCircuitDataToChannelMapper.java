package io.openems.edge.phoenixcontact.plcnext.loadcircuit.data;

import org.osgi.service.component.annotations.Component;

import io.openems.edge.phoenixcontact.plcnext.common.mapper.AbstractPlcNextGdsDataToChannelMapper;

@Component(service = PlcNextGdsLoadCircuitDataToChannelMapper.class)
public class PlcNextGdsLoadCircuitDataToChannelMapper extends AbstractPlcNextGdsDataToChannelMapper {

	@Override
	public PlcNextGdsLoadCircuitDataVariableDefinition[] getVariableDefinitions() {
		return PlcNextGdsLoadCircuitDataVariableDefinition.values();
	}
}
