package io.openems.edge.phoenixcontact.plcnext.common.mapper;

import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;
import io.openems.edge.phoenixcontact.plcnext.ess.PlcNextEssGdsDataWriteMappingDefinition;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PlcNextChannelToGdsDataMapperImplTest {

	private PlcNextChannelToGdsDataMapperImpl dataMapper;

	@Before
	public void setupBefore() {
		this.dataMapper = new PlcNextChannelToGdsDataMapperImpl();
	}

	@Test
	public void testFindChannelMapping() {
		// test
		Optional<PlcNextGdsDataMappingDefinition> result = this.dataMapper.getMappingByChannelId(
				PlcNextEssGdsDataWriteMappingDefinition.SET_ACTIVE_POWER_EQUALS.getChannelId(),
				PlcNextEssGdsDataWriteMappingDefinition.values());

		// check
		assertNotNull(result);
		assertTrue(result.isPresent());
	}

}
