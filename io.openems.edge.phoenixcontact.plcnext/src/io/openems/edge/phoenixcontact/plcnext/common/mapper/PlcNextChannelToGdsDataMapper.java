package io.openems.edge.phoenixcontact.plcnext.common.mapper;

import java.util.List;

import com.google.gson.JsonElement;

import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;

/**
 * Data mapper used to write data to controller 
 */
public interface PlcNextChannelToGdsDataMapper {

	String PLC_NEXT_VARIABLE_PATH = "path";
	String PLC_NEXT_VARIABLE_VALUE = "value";
	String PLC_NEXT_VARIABLE_VALUE_TYPE = "valueType";

	/**
	 * Extracts single JSON element that can represent a primitive or an array
	 * value.
	 * 
	 * @param channelValue      represents the channel and value to be mapped
	 * @param dataInstanceName  the configured instance name
	 * @param stationId			identifier of the component instance
	 * @param mappingDefinition represents the mapping definition
	 * @return mapped value
	 * @throws PlcNextGdsDataMappingException if anything goes wrong during variable
	 *                                        mapping
	 */
	JsonElement mapSingleValueToGdsData(PlcNextGdsDataMappedValue channelValue, //
			String dataInstanceName, String stationId, //
			PlcNextGdsDataMappingDefinition[] mappingDefinition)
					throws PlcNextGdsDataMappingException;

	/**
	 * Extracts all incoming variables in JSON format.
	 * 
	 * @param channelValues     represents a list of channel and values to be mapped
	 * @param dataInstanceName  the configured instance name
	 * @param stationId			identifier of the component instance
	 * @param mappingDefinition represents the mapping definition
	 * @return list of mapped values
	 * @throws PlcNextGdsDataMappingException if anything goes wrong during variable
	 *                                        mapping
	 */
	List<JsonElement> mapAllValuesToGdsData(List<PlcNextGdsDataMappedValue> channelValues, //
			String dataInstanceName, String stationId, //
			PlcNextGdsDataMappingDefinition[] mappingDefinition)
					throws PlcNextGdsDataMappingException;

}