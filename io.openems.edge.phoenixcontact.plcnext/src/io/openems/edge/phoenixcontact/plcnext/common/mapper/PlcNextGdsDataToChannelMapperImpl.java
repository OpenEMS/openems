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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataProvider;
import io.openems.edge.phoenixcontact.plcnext.common.utils.PlcNextChannelValueTypeHelper;

@Component(scope = ServiceScope.PROTOTYPE)
public class PlcNextGdsDataToChannelMapperImpl implements PlcNextGdsDataToChannelMapper {

	private static final Logger log = LoggerFactory.getLogger(PlcNextGdsDataToChannelMapperImpl.class);

	@Override
	public PlcNextGdsDataMappedValue mapSingleValueToChannel(JsonElement variable, //
			String dataInstanceName, //
			PlcNextGdsDataMappingDefinition[] mappingDefinition) {

		PlcNextGdsDataMappedValue mappedValue = null;

		if (!variable.isJsonObject()) {
			throw new PlcNextGdsDataMappingException("Passed JsonElement is not an object like expected! Aborting.");
		}
		// Determine variable name
		JsonObject varObject = variable.getAsJsonObject();
		String varName = getVariableName(varObject, dataInstanceName)
				.orElseThrow(() -> new PlcNextGdsDataMappingException("Cannot determine variable name! Aborting."));

		// Determine variable definition
		PlcNextGdsDataMappingDefinition variableToChannelMappingDefinition = getMappingByIdentifier(varName,
				mappingDefinition)
				.orElseThrow(() -> new PlcNextGdsDataMappingException(
						"No variable definition found for identifier '" + varName + "'"));

		// Map value
		if (isValueOfTypeArray(varObject)) {
			throw new PlcNextGdsDataMappingException("Processing of value arrays isn't supported! Mapping skipped.");
		} else {
			mappedValue = mapSingleJsonPrimitiveVariable(varObject, varName, variableToChannelMappingDefinition);
		}
		return mappedValue;
	}

	@Override
	public List<PlcNextGdsDataMappedValue> mapAllValuesToChannels(JsonArray variables, //
			String dataInstanceName, //
			PlcNextGdsDataMappingDefinition[] mappingDefinition) {

		List<PlcNextGdsDataMappedValue> mappedValues = new ArrayList<>();

		for (JsonElement variable : variables.asList()) {
			PlcNextGdsDataMappedValue mappedValue = mapSingleValueToChannel(variable, dataInstanceName,
					mappingDefinition);

			if (Objects.nonNull(mappedValue)) {
				mappedValues.add(mappedValue);
			}
		}
		log.debug("Mapped values: {}", mappedValues);

		return Collections.unmodifiableList(mappedValues);
	}

	private Optional<PlcNextGdsDataMappingDefinition> getMappingByIdentifier(String identifier,
			PlcNextGdsDataMappingDefinition[] variableDefinitions) {
		return Stream.of(variableDefinitions).filter(item -> item.getIdentifier().equals(identifier)).findFirst();
	}

	private boolean isValueOfTypeArray(JsonObject jsonObject) {
		return Objects.nonNull(jsonObject) //
				&& Objects.nonNull(jsonObject.get(PLC_NEXT_VARIABLE_VALUE)) //
				&& jsonObject.get(PLC_NEXT_VARIABLE_VALUE).isJsonArray();
	}

	/**
	 * Extracts variable name from JSON object
	 * 
	 * @param varObject    represents the JSON object returned by PLCnext REST-API
	 * @param instanceName the well defined instance name
	 * @return Optional with the variable name without path information or empty
	 *         when no path information have been found
	 */
	Optional<String> getVariableName(JsonObject varObject, String instanceName) {
		if (!varObject.has(PLC_NEXT_VARIABLE_PATH)) {
			log.error("Variable path not found in JsonObject! Aborting.");
			return Optional.empty();
		}
		String varPath = varObject.get(PLC_NEXT_VARIABLE_PATH).getAsString();

		return Optional.of(stripComponentAndInstanceNameAndPort(varPath, instanceName));
	}

	private String stripComponentAndInstanceNameAndPort(String varPath, String instanceName) {
		String varStrippedPrefix = varPath
				.substring(PlcNextGdsDataProvider.PLC_NEXT_OPENEMS_COMPONENT_NAME.length() + 1);
		String varStrippedInstanceName = varStrippedPrefix.substring(instanceName.length() + 1);

		return varStrippedInstanceName.substring(PlcNextGdsDataProvider.PLC_NEXT_INPUT_CHANNEL.length() + 1);
	}

	/**
	 * Fetches value field from given JSON object and maps it.
	 * 
	 * @param varObject            represents the JSON object to be processed
	 * @param varIdentifier        variable identifier to corresponding JSON object
	 * @param varMappingDefinition mapping definition
	 * @return mapped value including channelId
	 */
	PlcNextGdsDataMappedValue mapSingleJsonPrimitiveVariable(JsonObject varObject, String varIdentifier,
			PlcNextGdsDataMappingDefinition varMappingDefinition) {

		ChannelId destinationChannelId = varMappingDefinition.getChannelId();
		JsonPrimitive primitiveValue = null;

		try {
			primitiveValue = varObject.get(PLC_NEXT_VARIABLE_VALUE).getAsJsonPrimitive();
		} catch (IllegalStateException e) {
			log.warn("Fetching field 'value' from object {} failed!", varObject, e);
		}

		if (Objects.isNull(primitiveValue)) {
			log.warn("Got NULL value for variable '{}' from PLCnext API! Publishing to channel skipped.",
					varIdentifier);
			return null;
		}

		Object mappedValue = mapValue(primitiveValue, destinationChannelId.doc());
		log.debug("PLCnext variable named '{}' and value '{}' mapped to value '{}'",
				varMappingDefinition.getIdentifier(), primitiveValue, mappedValue);

		return new PlcNextGdsDataMappedValue(varMappingDefinition.getChannelId(), mappedValue);
	}

	/**
	 * Extracts value from JSON object using channel data type.
	 * 
	 * @param jsonPrimitive                represents the JSON primitive
	 * @param dataTypeOfDestinationChannel represents the channelId the value should
	 *                                     be mapped for
	 * @return mapped value
	 * @throws PlcNextGdsDataMappingException
	 */
	Object mapValue(JsonPrimitive jsonPrimitive, Doc dataTypeOfDestinationChannel) {

		Object channelValue = PlcNextChannelValueTypeHelper.getChannelValue(jsonPrimitive,
				dataTypeOfDestinationChannel);

		if (Objects.isNull(channelValue)) {
			throw new PlcNextGdsDataMappingException("Mapping from source to destination type failed!");
		}	
		return channelValue;
	}

}
