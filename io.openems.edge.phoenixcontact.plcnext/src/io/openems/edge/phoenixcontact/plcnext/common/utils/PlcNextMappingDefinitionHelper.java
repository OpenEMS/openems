package io.openems.edge.phoenixcontact.plcnext.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDynamicDefinition;

/**
 * Helper class for mapping definitions
 */
public final class PlcNextMappingDefinitionHelper {

	private PlcNextMappingDefinitionHelper() {
	}

	/**
	 * Joins a mapping definition to the base mapping using a prefix. If prefix is
	 * NULL or empty String the mapping will be joined without prefix.
	 * 
	 * @param baseMapping represents the base mapping
	 * @param joinSource  represents the mapping definition that will be joined to
	 *                    base mapping
	 * @param joinPrefix  the prefix to be used while joining, may be NULL or empty
	 *                    string
	 * @return joined mapping definitions
	 */
	public static PlcNextGdsDataMappingDefinition[] joinMappings(PlcNextGdsDataMappingDefinition[] baseMapping,
			PlcNextGdsDataMappingDefinition[] joinSource, String joinPrefix) {

		List<PlcNextGdsDataMappingDefinition> operationalList = new ArrayList<PlcNextGdsDataMappingDefinition>();

		if (Objects.nonNull(baseMapping) && baseMapping.length > 0) {
			operationalList.addAll(Stream.of(baseMapping) //
					.map(item -> new PlcNextGdsDataMappingDynamicDefinition(item.getIdentifier(), item.getChannelId())) //
					.toList());
		}
		if (Objects.nonNull(joinSource) && joinSource.length > 0) {
			operationalList.addAll(Stream.of(joinSource) //
					.map(item -> {
						String newVarIdentifier = item.getIdentifier();

						if (Objects.nonNull(joinPrefix) && !joinPrefix.isBlank()) {
							newVarIdentifier = joinPrefix + "." + newVarIdentifier;
						}
						return new PlcNextGdsDataMappingDynamicDefinition(newVarIdentifier, item.getChannelId());
					}) //
					.toList());
		}
		return operationalList.toArray(new PlcNextGdsDataMappingDynamicDefinition[operationalList.size()]);
	}

}
