package io.openems.edge.phoenixcontact.plcnext.common.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataAccessConfig;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataProvider;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataVariableDefinition;


public abstract class AbstractPlcNextGdsDataToChannelMapper implements PlcNextGdsDataToChannelMapper {
	
	private static final Logger log = LoggerFactory.getLogger(AbstractPlcNextGdsDataToChannelMapper.class);
	
	protected abstract PlcNextGdsDataVariableDefinition[] getVariableDefinitions();

	@Override
	public List<PlcNextGdsDataMappedValue> mapSingleValueToChannel(JsonElement variable, String dataInstanceName) {

		List<PlcNextGdsDataMappedValue> mappedValues = List.of();

		if (!variable.isJsonObject()) {
			throw new PlcNextGdsDataMappingException("Passed JsonElement is not an object like expected! Aborting.");
		}
		// Determine variable name
		JsonObject varObject = variable.getAsJsonObject();
		String varName = getVariableName(varObject, dataInstanceName)
				.orElseThrow(() -> new PlcNextGdsDataMappingException("Cannot determine variable name! Aborting."));

		// Determine variable definition
		PlcNextGdsDataVariableDefinition varDefinition = valueByIdentifier(varName, getVariableDefinitions())
				.orElseThrow(() -> new PlcNextGdsDataMappingException(
						"No variable definition found for identifier '" + varName + "'"));

		// Map value
		if (isValueOfTypeArray(varObject)) {
			mappedValues = mapSingleJsonArrayVariable(varObject, varName, varDefinition);
		} else {
			mappedValues = mapSingleJsonPrimitiveVariable(varObject, varName, varDefinition);
		}
		return Collections.unmodifiableList(mappedValues);
	}
	
	public List<PlcNextGdsDataMappedValue> mapAllValuesToChannels(JsonArray variables, String dataInstanceName) {
		List<PlcNextGdsDataMappedValue> mappedValues = new ArrayList<>();

		variables.forEach(variable -> mappedValues.addAll(mapSingleValueToChannel(variable, dataInstanceName)));
		log.debug("Mapped values: " + mappedValues);

		return Collections.unmodifiableList(mappedValues);		
	}
	
	// Helper
	
	private Optional<PlcNextGdsDataVariableDefinition> valueByIdentifier(String identifier, PlcNextGdsDataVariableDefinition[] variableDefinitions) {
		return Stream.of(variableDefinitions)
				.filter(item -> item.getIdentifier().equals(identifier)).findFirst();
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
				.substring(PlcNextGdsDataAccessConfig.PLC_NEXT_OPENEMS_COMPONENT_NAME.length() + 1);
		String varStrippedInstanceName = varStrippedPrefix.substring(instanceName.length() + 1);

		return varStrippedInstanceName.substring(PlcNextGdsDataProvider.PLC_NEXT_INPUT_CHANNEL.length() + 1);
	}

	/**
	 * TODO
	 * 
	 * @param varObject
	 * @param varName
	 * @param varDefinition
	 * @return
	 */
	List<PlcNextGdsDataMappedValue> mapSingleJsonPrimitiveVariable(JsonObject varObject, String varName, PlcNextGdsDataVariableDefinition varDefinition) {

		JsonPrimitive primitiveValue = varObject.get(PLC_NEXT_VARIABLE_VALUE).getAsJsonPrimitive();
		if (Objects.isNull(primitiveValue)) {
			log.warn("Got NULL value for variable '" + varName + "' from PLCnext API! Publishing to channel skipped.");
			return List.of();
		}

		ChannelId destinationChannelId = varDefinition.getOpenEmsChannelIds().get(0);

		PlcNextGdsDataMappedValue mappedValue = mapValue(primitiveValue, destinationChannelId);
		log.info("PLCnext variable [ name=" + varDefinition.getIdentifier() + ", value="+primitiveValue+"] mapped to " + mappedValue);
		
		return List.of(mappedValue);
	}

	/**
	 * Process array data
	 * 
	 * @param varObject
	 * @param varName
	 * @param varDefinition
	 * @return
	 * @throws PlcNextGdsDataMappingException
	 */
	List<PlcNextGdsDataMappedValue> mapSingleJsonArrayVariable(JsonObject varObject, String varName,
			PlcNextGdsDataVariableDefinition varDefinition) {

		JsonArray arrayValue = varObject.get(PLC_NEXT_VARIABLE_VALUE).getAsJsonArray();

		if (Objects.isNull(arrayValue)) {
			throw new PlcNextGdsDataMappingException(
					"Got NULL value for variable '" + varName + "' from PLCnext API! Publishing to channel skipped.");
		}
		if (arrayValue.size() > 3) {
			throw new PlcNextGdsDataMappingException("Number of array members of " + arrayValue.size()
					+ " does not match the expected count of 3");
		}

		List<PlcNextGdsDataMappedValue> mappedValues = new ArrayList<PlcNextGdsDataMappedValue>();
		for (int k = 0; k < arrayValue.size(); k++) {
			ChannelId destinationChannelId = varDefinition.getOpenEmsChannelIds().get(k);

			mappedValues.add(mapValue(arrayValue.get(k), destinationChannelId));
		}
		log.info("PLCnext variable [name=" + varDefinition.getIdentifier() + ", values=[" + arrayValue + "]] mapped to " + mappedValues);
		return Collections.unmodifiableList(mappedValues);
	}

	/**
	 * TODO
	 * 
	 * @param jsonElement
	 * @param sourceDataType
	 * @param destinationChannelId
	 * @return
	 * @throws PlcNextGdsDataMappingException
	 */
	PlcNextGdsDataMappedValue mapValue(JsonElement jsonElement, ChannelId destinationChannelId) {

		Object channelValue = getChannelValue(jsonElement, destinationChannelId.doc());

		if (Objects.isNull(channelValue)) {
			throw new PlcNextGdsDataMappingException("Mapping from source to destination type failed!");
		}
		return new PlcNextGdsDataMappedValue(destinationChannelId, channelValue);
	}

	private Object getChannelValue(JsonElement jsonElement, Doc openEmsChannelDoc) {
		Object mappedValue = null;

		if (isOpenEmsTypeFloat(openEmsChannelDoc)) {
			mappedValue = jsonElement.getAsFloat();
		} else if (isOpenEmsTypeDouble(openEmsChannelDoc)) {
			mappedValue = jsonElement.getAsDouble();
		} else if (isOpenEmsTypeShort(openEmsChannelDoc)) {
			mappedValue = jsonElement.getAsShort();
		} else if (isOpenEmsTypeInteger(openEmsChannelDoc)) {
			mappedValue = jsonElement.getAsInt();
		} else if (isOpenEmsTypeLong(openEmsChannelDoc)) {
			mappedValue = jsonElement.getAsLong();
		} else {
			throw new PlcNextGdsDataMappingException("Fetching value by type '" + openEmsChannelDoc.getType() + "' is not supported.");
		}
		return mappedValue;
	}

	private boolean isOpenEmsTypeLong(Doc openEmsChannelDoc) {
		return OpenemsType.LONG == openEmsChannelDoc.getType();
	}

	private boolean isOpenEmsTypeInteger(Doc openEmsChannelDoc) {
		return OpenemsType.INTEGER == openEmsChannelDoc.getType();
	}

	private boolean isOpenEmsTypeShort(Doc openEmsChannelDoc) {
		return OpenemsType.SHORT == openEmsChannelDoc.getType();
	}

	private boolean isOpenEmsTypeDouble(Doc openEmsChannelDoc) {
		return OpenemsType.DOUBLE == openEmsChannelDoc.getType();
	}

	private boolean isOpenEmsTypeFloat(Doc openEmsChannelDoc) {
		return OpenemsType.FLOAT == openEmsChannelDoc.getType();
	}
}
