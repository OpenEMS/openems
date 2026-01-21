package io.openems.edge.phoenixcontact.plcnext.common.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;

import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDynamicDefinition;
import io.openems.edge.phoenixcontact.plcnext.ess.PlcNextEssGdsDataReadMappingDefinition;
import io.openems.edge.phoenixcontact.plcnext.meter.PlcNextMeterGdsDataReadMappingDefinition;

public class PlcNextMappingDefinitionHelperTest {

	@Test
	public void testJoinTwoMappingsWithPrefix() {
		// prep
		int expectedMappingCount = PlcNextEssGdsDataReadMappingDefinition.values().length
				+ PlcNextMeterGdsDataReadMappingDefinition.values().length;
		String mappingPrefix = "electricityMeter";

		// test
		PlcNextGdsDataMappingDefinition[] result = PlcNextMappingDefinitionHelper.joinMappings(
				PlcNextEssGdsDataReadMappingDefinition.values(), PlcNextMeterGdsDataReadMappingDefinition.values(),
				mappingPrefix);

		// check
		assertNotNull(result);
		assertEquals(expectedMappingCount, result.length);

		List<String> itemsWithMappingPrefix = Stream.of(result).map(PlcNextGdsDataMappingDefinition::getIdentifier)
				.filter(item -> item.startsWith(mappingPrefix)).toList();
		assertEquals(PlcNextMeterGdsDataReadMappingDefinition.values().length, itemsWithMappingPrefix.size());
	}

	@Test
	public void testJoinTwoMappingsWithEmptyPrefix() {
		// prep
		int expectedMappingCount = PlcNextEssGdsDataReadMappingDefinition.values().length
				+ PlcNextMeterGdsDataReadMappingDefinition.values().length;
		String mappingPrefix = "";

		// test
		PlcNextGdsDataMappingDefinition[] result = PlcNextMappingDefinitionHelper.joinMappings(
				PlcNextEssGdsDataReadMappingDefinition.values(), PlcNextMeterGdsDataReadMappingDefinition.values(),
				mappingPrefix);

		// check
		assertNotNull(result);
		assertEquals(expectedMappingCount, result.length);

		List<String> itemsWithMappingPrefix = Stream.of(result).map(PlcNextGdsDataMappingDefinition::getIdentifier)
				.filter(item -> item.startsWith(mappingPrefix)).toList();
		assertEquals(expectedMappingCount, itemsWithMappingPrefix.size());
	}

	@Test
	public void testJoinTwoMappingsWithPrefixNull() {
		// prep
		int expectedMappingCount = PlcNextEssGdsDataReadMappingDefinition.values().length
				+ PlcNextMeterGdsDataReadMappingDefinition.values().length;
		String mappingPrefix = null;

		// test
		PlcNextGdsDataMappingDefinition[] result = PlcNextMappingDefinitionHelper.joinMappings(
				PlcNextEssGdsDataReadMappingDefinition.values(), PlcNextMeterGdsDataReadMappingDefinition.values(),
				mappingPrefix);

		// check
		assertNotNull(result);
		assertEquals(expectedMappingCount, result.length);
	}

	@Test
	public void testJoinTwoMappingsWithEmptyBaseMapping() {
		// prep
		int expectedMappingCount = PlcNextMeterGdsDataReadMappingDefinition.values().length;
		String mappingPrefix = "electricityMeter";

		// test
		PlcNextGdsDataMappingDefinition[] result = PlcNextMappingDefinitionHelper.joinMappings(
				new PlcNextGdsDataMappingDynamicDefinition[0], PlcNextMeterGdsDataReadMappingDefinition.values(),
				mappingPrefix);

		// check
		assertNotNull(result);
		assertEquals(expectedMappingCount, result.length);

		List<String> itemsWithMappingPrefix = Stream.of(result).map(PlcNextGdsDataMappingDefinition::getIdentifier)
				.filter(item -> item.startsWith(mappingPrefix)).toList();
		assertEquals(PlcNextMeterGdsDataReadMappingDefinition.values().length, itemsWithMappingPrefix.size());
	}

	@Test
	public void testJoinTwoMappingsWithBaseMappingNull() {
		// prep
		int expectedMappingCount = PlcNextMeterGdsDataReadMappingDefinition.values().length;
		String mappingPrefix = "electricityMeter";

		// test
		PlcNextGdsDataMappingDefinition[] result = PlcNextMappingDefinitionHelper.joinMappings(null,
				PlcNextMeterGdsDataReadMappingDefinition.values(), mappingPrefix);

		// check
		assertNotNull(result);
		assertEquals(expectedMappingCount, result.length);

		List<String> itemsWithMappingPrefix = Stream.of(result).map(PlcNextGdsDataMappingDefinition::getIdentifier)
				.filter(item -> item.startsWith(mappingPrefix)).toList();
		assertEquals(PlcNextMeterGdsDataReadMappingDefinition.values().length, itemsWithMappingPrefix.size());
	}

	@Test
	public void testJoinTwoMappingsWithEmptyJoinSourcMapping() {
		// prep
		int expectedMappingCount = PlcNextEssGdsDataReadMappingDefinition.values().length;
		String mappingPrefix = "electricityMeter";

		// test
		PlcNextGdsDataMappingDefinition[] result = PlcNextMappingDefinitionHelper.joinMappings(
				PlcNextEssGdsDataReadMappingDefinition.values(), new PlcNextGdsDataMappingDynamicDefinition[0],
				mappingPrefix);

		// check
		assertNotNull(result);
		assertEquals(expectedMappingCount, result.length);

		List<String> itemsWithMappingPrefix = Stream.of(result).map(PlcNextGdsDataMappingDefinition::getIdentifier)
				.filter(item -> item.startsWith(mappingPrefix)).toList();
		assertTrue(itemsWithMappingPrefix.isEmpty());
	}

	public void testJoinTwoMappingsWithJoinSourceMappingNull() {
		// prep
		int expectedMappingCount = PlcNextEssGdsDataReadMappingDefinition.values().length;
		String mappingPrefix = "electricityMeter";

		// test
		PlcNextGdsDataMappingDefinition[] result = PlcNextMappingDefinitionHelper
				.joinMappings(PlcNextEssGdsDataReadMappingDefinition.values(), null, mappingPrefix);

		// check
		assertNotNull(result);
		assertEquals(expectedMappingCount, result.length);

		List<String> itemsWithMappingPrefix = Stream.of(result).map(PlcNextGdsDataMappingDefinition::getIdentifier)
				.filter(item -> item.startsWith(mappingPrefix)).toList();
		assertTrue(itemsWithMappingPrefix.isEmpty());
	}
}
