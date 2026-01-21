package io.openems.edge.phoenixcontact.plcnext.common.mapper;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;
import io.openems.edge.phoenixcontact.plcnext.ess.PlcNextEssGdsDataWriteMappingDefinition;

public class PlcNextChannelToGdsDataMapperImplTest {

	private PlcNextChannelToGdsDataMapperImpl dataMapper;

	@Before
	public void setupBefore() {
		dataMapper = new PlcNextChannelToGdsDataMapperImpl();
	}

	@Test
	public void testFindChannelMapping() {
		// test
		Optional<PlcNextGdsDataMappingDefinition> result = dataMapper.getMappingByChannelId(
				PlcNextEssGdsDataWriteMappingDefinition.SET_ACTIVE_POWER_EQUALS.getChannelId(),
				PlcNextEssGdsDataWriteMappingDefinition.values());

		// check
		assertNotNull(result);
		assertTrue(result.isPresent());
	}

}
