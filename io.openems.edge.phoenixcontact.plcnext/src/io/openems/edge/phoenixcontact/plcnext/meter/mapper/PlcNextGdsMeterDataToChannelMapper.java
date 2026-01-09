package io.openems.edge.phoenixcontact.plcnext.meter.mapper;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.edge.phoenixcontact.plcnext.common.mapper.AbstractPlcNextGdsDataToChannelMapper;
import io.openems.edge.phoenixcontact.plcnext.meter.data.PlcNextGdsMeterDataVariableDefinition;

@Component(scope = ServiceScope.SINGLETON, service = PlcNextGdsMeterDataToChannelMapper.class)
public class PlcNextGdsMeterDataToChannelMapper extends AbstractPlcNextGdsDataToChannelMapper {

	@Override
	protected PlcNextGdsMeterDataVariableDefinition[] getVariableDefinitions() {
		return PlcNextGdsMeterDataVariableDefinition.values();
	}
}
