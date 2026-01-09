package io.openems.edge.phoenixcontact.plcnext.common.mapper;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public interface PlcNextGdsDataToChannelMapper {

	String PLC_NEXT_VARIABLE_PATH = "path";
	String PLC_NEXT_VARIABLE_VALUE = "value";

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
	List<PlcNextGdsDataMappedValue> mapSingleValueToChannel(JsonElement variable, String dataInstanceName);

	/**
	 * Extracts all incoming variables in JSON format.
	 * 
	 * @param variables        JSON array to be parsed and mapped
	 * @param dataInstanceName the configured instance name
	 * @return list of mapped values, due to arrays including OpenEMS channel-ID
	 * @throws PlcNextGdsDataMappingException if anything goes wrong during variable
	 *                                        mapping
	 */
	List<PlcNextGdsDataMappedValue> mapAllValuesToChannels(JsonArray variables, String dataInstanceName);

}