package io.openems.edge.phoenixcontact.plcnext.common.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;
import io.openems.edge.phoenixcontact.plcnext.ess.PlcNextEssGdsDataWriteMappingDefinition;

public class PlcNextChannelToGdsDataMapperImplTest {

	private PlcNextChannelToGdsDataMapperImpl dataMapper;

	@BeforeEach
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
