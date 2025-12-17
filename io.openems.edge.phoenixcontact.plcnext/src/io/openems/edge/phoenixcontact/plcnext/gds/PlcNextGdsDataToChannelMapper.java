package io.openems.edge.phoenixcontact.plcnext.gds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.phoenixcontact.plcnext.gds.enums.PlcNextGdsDataType;
import io.openems.edge.phoenixcontact.plcnext.gds.enums.PlcNextGdsDataVariableDefinition;

@Component(scope = ServiceScope.SINGLETON, service = PlcNextGdsDataToChannelMapper.class)
public class PlcNextGdsDataToChannelMapper {

	public static final String PLC_NEXT_VARIABLE_PATH = "path";
	public static final String PLC_NEXT_VARIABLE_VALUE = "value";

	private static final Logger log = LoggerFactory.getLogger(PlcNextGdsDataToChannelMapper.class);

	@Activate
	public PlcNextGdsDataToChannelMapper() {
	}

	/**
	 * Extracts single JSON element that can represent a primitive or an array
	 * value.
	 * 
	 * @param variable         JSON element to be parsed and mapped
	 * @param dataInstanceName the configured instance name
	 * @return list of mapped values, due to arrays including OpenEMS channel-ID
	 * @throws PlcNextGdsDataMappingException if anything goes wrong during variable
	 *                                        mapping
	 */
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
		PlcNextGdsDataVariableDefinition varDefinition = PlcNextGdsDataVariableDefinition.valueByIdentifier(varName)
				.orElseThrow(() -> new PlcNextGdsDataMappingException(
						"No variable definition found for identifier '" + varName + "'"));

		// Map value
		if (varDefinition.getDataType().isArray()) {
			mappedValues = mapSingleJsonArrayVariable(varObject, varName, varDefinition);
		} else {
			mappedValues = mapSingleJsonPrimitiveVariable(varObject, varName, varDefinition);
		}
		return Collections.unmodifiableList(mappedValues);
	}

	/**
	 * Process simple data
	 * 
	 * @param mappedValue
	 * @param varObject
	 * @param varName
	 * @param varDefinition
	 * @return
	 * @throws PlcNextGdsDataMappingException
	 */
	List<PlcNextGdsDataMappedValue> mapSingleJsonPrimitiveVariable(JsonObject varObject, String varName, PlcNextGdsDataVariableDefinition varDefinition) {

		JsonPrimitive primitiveValue = varObject.get(PLC_NEXT_VARIABLE_VALUE).getAsJsonPrimitive();
		if (Objects.isNull(primitiveValue)) {
			log.warn("Got NULL value for variable '" + varName + "' from PLCnext API! Publishing to channel skipped.");
			return List.of();
		}

		ChannelId destinationChannelId = varDefinition.getOpenEmsChannelIds().get(0);
		PlcNextGdsDataType sourceDataType = varDefinition.getDataType();

		PlcNextGdsDataMappedValue mappedValue = mapValue(primitiveValue, sourceDataType, destinationChannelId);
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
		if (varDefinition.getDataType().getMemberCount() != arrayValue.size()) {
			throw new PlcNextGdsDataMappingException("Number of array members of " + arrayValue.size()
					+ " does not match the expected count of " + varDefinition.getDataType().getMemberCount());
		}

		List<PlcNextGdsDataMappedValue> mappedValues = new ArrayList<PlcNextGdsDataMappedValue>();
		for (int k = 0; k < arrayValue.size(); k++) {
			ChannelId destinationChannelId = varDefinition.getOpenEmsChannelIds().get(k);
			PlcNextGdsDataType sourceDataType = varDefinition.getDataType().getMemberType();

			mappedValues.add(mapValue(arrayValue.get(k), sourceDataType, destinationChannelId));
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
	PlcNextGdsDataMappedValue mapValue(JsonElement jsonElement, PlcNextGdsDataType sourceDataType,
			ChannelId destinationChannelId) {

		Object jsonValue = getJsonValue(jsonElement, sourceDataType);
		Object channelValue = getChannelValue(jsonValue, sourceDataType, destinationChannelId.doc());

		if (Objects.isNull(channelValue)) {
			throw new PlcNextGdsDataMappingException("Mapping from source to destination type failed!");
		}
		return new PlcNextGdsDataMappedValue(destinationChannelId, channelValue);
	}

	/**
	 * Conversion depending on channel type
	 * 
	 * @param jsonElement represents the JSON element to read
	 * @param type        represents the channel type
	 * @return proper value object
	 * @throws PlcNextGdsDataMappingException in case of unsupported data type
	 */
	Object getJsonValue(JsonElement jsonElement, PlcNextGdsDataType type) {
		if (type == PlcNextGdsDataType.FLOAT32) {
			return jsonElement.getAsFloat();
		} else if (type == PlcNextGdsDataType.FLOAT64) {
			return jsonElement.getAsDouble();
		} else {
			throw new PlcNextGdsDataMappingException("Extraction of type '" + type + "' is not supported.");
		}
	}

	/**
	 * Value conversion depending on PLCnext source and OpenEMS destination data
	 * type.
	 * 
	 * @param jsonValue   represents the value object
	 * @param plcNextType represents the source data type
	 * @param openEmsType represents the destination data type
	 * @return the value in destination data type
	 * @throws PlcNextGdsDataMappingException in any case of unsupported data type
	 *                                        combination
	 */
	Object getChannelValue(Object jsonValue, PlcNextGdsDataType plcNextType, Doc openEmsChannelDoc) {
		Object mappedValue = null;

		if (isPlcNextTypeFloat32(plcNextType) && isOpenEmsTypeFloat(openEmsChannelDoc)) {
			mappedValue = jsonValue;
		} else if (isPlcNextTypeFloat32(plcNextType) && isOpenEmsTypeDouble(openEmsChannelDoc)) {
			mappedValue = ((Float) jsonValue).doubleValue();
		} else if (isPlcNextTypeFloat32(plcNextType) && isOpenEmsTypeShort(openEmsChannelDoc)) {
			mappedValue = Integer.valueOf(((Float) jsonValue).intValue()).shortValue();
		} else if (isPlcNextTypeFloat32(plcNextType) && isOpenEmsTypeInteger(openEmsChannelDoc)) {
			mappedValue = ((Float) jsonValue).intValue();
		} else if (isPlcNextTypeFloat32(plcNextType) && isOpenEmsTypeLong(openEmsChannelDoc)) {
			mappedValue = ((Float) jsonValue).longValue();
		} else if (isPlcNextTypeFloat64(plcNextType) && isOpenEmsTypeFloat(openEmsChannelDoc)) {
			mappedValue = ((Double) jsonValue).floatValue();
		} else if (isPlcNextTypeFloat64(plcNextType) && isOpenEmsTypeDouble(openEmsChannelDoc)) {
			mappedValue = jsonValue;
		} else if (isPlcNextTypeFloat64(plcNextType) && isOpenEmsTypeShort(openEmsChannelDoc)) {
			mappedValue = Integer.valueOf(((Double) jsonValue).intValue()).shortValue();
		} else if (isPlcNextTypeFloat64(plcNextType) && isOpenEmsTypeInteger(openEmsChannelDoc)) {
			mappedValue = ((Double) jsonValue).intValue();
		} else if (isPlcNextTypeFloat64(plcNextType) && isOpenEmsTypeLong(openEmsChannelDoc)) {
			mappedValue = ((Double) jsonValue).longValue();
		} else {
			throw new PlcNextGdsDataMappingException("Mapping from source type '" + plcNextType
					+ "' to destination type '" + openEmsChannelDoc.getType() + "' is not supported.");
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

	private boolean isPlcNextTypeFloat64(PlcNextGdsDataType plcNextType) {
		return PlcNextGdsDataType.FLOAT64 == plcNextType;
	}

	private boolean isPlcNextTypeFloat32(PlcNextGdsDataType plcNextType) {
		return PlcNextGdsDataType.FLOAT32 == plcNextType;
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
				.substring(PlcNextGdsDataProviderConfig.PLC_NEXT_OPENEMS_COMPONENT_NAME.length() + 1);
		String varStrippedInstanceName = varStrippedPrefix.substring(instanceName.length() + 1);

		return varStrippedInstanceName.split("\\.")[1];
	}

	/**
	 * Extracts all incoming variables in JSON format.
	 * 
	 * @param variables        JSON array to be parsed and mapped
	 * @param dataInstanceName the configured instance name
	 * @return list of mapped values, due to arrays including OpenEMS channel-ID
	 * @throws PlcNextGdsDataMappingException if anything goes wrong during variable
	 *                                        mapping
	 */
	public List<PlcNextGdsDataMappedValue> mapAllValuesToChannels(JsonArray variables, String dataInstanceName) {
		List<PlcNextGdsDataMappedValue> mappedValues = new ArrayList<>();

		variables.forEach(variable -> mappedValues.addAll(mapSingleValueToChannel(variable, dataInstanceName)));
		log.debug("Mapped values: " + mappedValues);

		return Collections.unmodifiableList(mappedValues);
	}
}
