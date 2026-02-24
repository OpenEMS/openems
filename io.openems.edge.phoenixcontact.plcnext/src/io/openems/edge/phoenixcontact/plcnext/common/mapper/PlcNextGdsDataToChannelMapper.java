package io.openems.edge.phoenixcontact.plcnext.common.mapper;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;

/**
 * Data mapper used to read data from controller 
 */
public interface PlcNextGdsDataToChannelMapper {

	String PLC_NEXT_VARIABLE_PATH = "path";
	String PLC_NEXT_VARIABLE_VALUE = "value";

	/**
	 * Extracts single JSON element that can represent a primitive or an array
	 * value.
	 * 
	 * @param variable          JSON element to be parsed and mapped
	 * @param dataInstanceName  the configured instance name
	 * @param stationId			identifier of the component instance
	 * @param mappingDefinition represents the mapping definition
	 * @return mapped value
	 * @throws PlcNextGdsDataMappingException if anything goes wrong during variable
	 *                                        mapping
	 */
	PlcNextGdsDataMappedValue mapSingleValueToChannel(JsonElement variable, //
			String dataInstanceName, String stationId, //
			PlcNextGdsDataMappingDefinition[] mappingDefinition);

	/**
	 * Extracts all incoming variables in JSON format.
	 * 
	 * @param variables         JSON array to be parsed and mapped
	 * @param dataInstanceName  the configured instance name
	 * @param stationId			identifier of the component instance
	 * @param mappingDefinition represents the mapping definition
	 * @return list of mapped values
	 * @throws PlcNextGdsDataMappingException if anything goes wrong during variable
	 *                                        mapping
	 */
	List<PlcNextGdsDataMappedValue> mapAllValuesToChannels(JsonArray variables, //
			String dataInstanceName, String stationId, //
			PlcNextGdsDataMappingDefinition[] mappingDefinition);

}