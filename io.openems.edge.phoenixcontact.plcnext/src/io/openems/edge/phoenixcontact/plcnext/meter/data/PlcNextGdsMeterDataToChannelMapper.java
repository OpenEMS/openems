package io.openems.edge.phoenixcontact.plcnext.meter.data;

import org.osgi.service.component.annotations.Component;

import io.openems.edge.phoenixcontact.plcnext.common.mapper.AbstractPlcNextGdsDataToChannelMapper;

@Component(service = PlcNextGdsMeterDataToChannelMapper.class)
public class PlcNextGdsMeterDataToChannelMapper extends AbstractPlcNextGdsDataToChannelMapper {

	@Override
	public PlcNextGdsMeterDataVariableDefinition[] getVariableDefinitions() {
		return PlcNextGdsMeterDataVariableDefinition.values();
	}
}
