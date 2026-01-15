package io.openems.edge.phoenixcontact.plcnext.common.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataProvider;
import io.openems.edge.phoenixcontact.plcnext.common.utils.PlcNextChannelValueTypeHelper;

@Component(scope = ServiceScope.PROTOTYPE)
public class PlcNextChannelToGdsDataMapperImpl implements PlcNextChannelToGdsDataMapper {

	private static final Logger log = LoggerFactory.getLogger(PlcNextChannelToGdsDataMapperImpl.class);

	@Override
	public JsonElement mapSingleValueToGdsData(PlcNextGdsDataMappedValue channelValue, //
			String dataInstanceName, //
			PlcNextGdsDataMappingDefinition[] mappingDefinition) {

		PlcNextGdsDataMappingDefinition channelToVariableMappingDefinition = getMappingByChannelId(
				channelValue.getChannelId(), mappingDefinition)
				.orElseThrow(() -> new PlcNextGdsDataMappingException(
						"No mapping found for channelId '" + channelValue.getChannelId() + "'"));
		String variablePath = new StringBuilder(PlcNextGdsDataProvider.PLC_NEXT_OPENEMS_COMPONENT_NAME).append("/") //
				.append(dataInstanceName).append(".").append(PlcNextGdsDataProvider.PLC_NEXT_OUTPUT_CHANNEL).append(".")
				.append(channelToVariableMappingDefinition.getIdentifier()).toString();

		return PlcNextChannelValueTypeHelper.buildVariableToWrite(variablePath, channelValue.getValue(),
				channelValue.getChannelId().doc());
	}

	@Override
	public List<JsonElement> mapAllValuesToGdsData(List<PlcNextGdsDataMappedValue> channelValues, //
			String dataInstanceName, //
			PlcNextGdsDataMappingDefinition[] mappingDefinition) {

		List<JsonElement> mappedValues = new ArrayList<>();

		for (PlcNextGdsDataMappedValue channelValue : channelValues) {
			JsonElement mappedValue = mapSingleValueToGdsData(channelValue, dataInstanceName, mappingDefinition);

			if (Objects.nonNull(mappedValue)) {
				mappedValues.add(mappedValue);
			}
		}
		log.debug("Mapped values: {}", mappedValues);

		return Collections.unmodifiableList(mappedValues);
	}

	Optional<PlcNextGdsDataMappingDefinition> getMappingByChannelId(ChannelId channelId,
			PlcNextGdsDataMappingDefinition[] varMappingDefinitions) {
		return Stream.of(varMappingDefinitions).filter(item -> item.getChannelId().name().equals(channelId.name()))
				.findFirst();
	}

}
