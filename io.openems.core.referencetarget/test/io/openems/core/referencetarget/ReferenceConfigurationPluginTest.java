package io.openems.core.referencetarget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;

@ExtendWith(MockitoExtension.class)
class ReferenceConfigurationPluginTest {

	@Mock
	private ServiceComponentRuntime scr;

	@Mock
	private ServiceReference<?> reference;

	@Mock
	private Bundle bundle;

	@InjectMocks
	private ReferenceConfigurationPlugin plugin;

	private Hashtable<String, Object> properties;
	private ComponentDescriptionDTO description;

	@BeforeEach
	void beforeEach() {
		this.properties = new Hashtable<>();
		this.properties.put(Constants.SERVICE_PID, "test.pid");
		this.properties.put("id", "controller0");

		this.description = new ComponentDescriptionDTO();
		this.description.properties = Map.of(
				"generate.targets.from.references", new String[] {});

		when(this.reference.getBundle()).thenReturn(this.bundle);
		when(this.scr.getComponentDescriptionDTO(this.bundle, "test.pid"))
				.thenReturn(this.description);
	}

	@Test
	void testExistingArrayExpression() {
		final var devices = new ReferenceDTO();
		devices.name = "devices";
		devices.target = "(&(id=${config.device_ids})(enabled=true))";
		this.description.references = new ReferenceDTO[] { devices };
		this.properties.put("device.ids", new String[] { "device0", "device1" });

		this.plugin.modifyConfiguration(this.reference, this.properties);

		assertEquals("(&(|(id=device0)(id=device1))(enabled=true))",
				this.properties.get("devices.target"));
	}

	@Test
	void testTransformedAndExistingExpressionsTogether() {
		final var thermometer = new ReferenceDTO();
		thermometer.name = "thermometer";
		thermometer.target = "(&(id=${config.thermometer_id})(enabled=true))";

		final var relays = new ReferenceDTO();
		relays.name = "relays";
		relays.target = "(&(id=${config.floorRelays;componentIdFromChannel})(enabled=true))";

		this.description.references = new ReferenceDTO[] { thermometer, relays };
		this.properties.put("thermometer.id", "thermometer0");
		this.properties.put("floorRelays",
				new String[] { "io0/Relay1", "io0/Relay2", "io1/Relay1" });

		this.plugin.modifyConfiguration(this.reference, this.properties);

		assertEquals("(&(id=thermometer0)(enabled=true))",
				this.properties.get("thermometer.target"));
		assertEquals("(&(|(id=io0)(id=io1))(enabled=true))",
				this.properties.get("relays.target"));
	}

	@Test
	void testUpdatedChannelAddressChangesTarget() {
		final var relays = new ReferenceDTO();
		relays.name = "relays";
		relays.target = "(&(id=${config.floorRelays;componentIdFromChannel})(enabled=true))";
		this.description.references = new ReferenceDTO[] { relays };

		this.properties.put("floorRelays", "io0/Relay1");

		this.plugin.modifyConfiguration(this.reference, this.properties);

		assertEquals("(&(id=io0)(enabled=true))",
				this.properties.get("relays.target"));
		assertEquals("io0/Relay1", this.properties.get("floorRelays"));

		this.properties.put("floorRelays", "io1/Relay1");

		this.plugin.modifyConfiguration(this.reference, this.properties);

		assertEquals("(&(id=io1)(enabled=true))",
				this.properties.get("relays.target"));
		assertEquals("io1/Relay1", this.properties.get("floorRelays"));
	}

	@ParameterizedTest
	@CsvSource({
			"componentIdFromChannel, invalid-address",
			"unknownTransformer, io0/Relay1"
	})
	void testTransformationFailureDoesNotStopOtherReferences(String transformerName, String address) {
		final var relays = new ReferenceDTO();
		relays.name = "relays";
		relays.target = "(&(id=${config.floorRelays;" + transformerName + "})(enabled=true))";

		final var thermometer = new ReferenceDTO();
		thermometer.name = "thermometer";
		thermometer.target = "(&(id=${config.thermometer_id})(enabled=true))";

		this.description.references = new ReferenceDTO[] { relays, thermometer };
		this.properties.put("relays.target", "(&(id=io0)(enabled=true))");
		this.properties.put("floorRelays", address);
		this.properties.put("thermometer.id", "thermometer0");

		this.plugin.modifyConfiguration(this.reference, this.properties);

		assertEquals("(&(objectClass=*)(!(objectClass=*)))",
				this.properties.get("relays.target"));
		assertEquals("(&(id=thermometer0)(enabled=true))",
				this.properties.get("thermometer.target"));
		assertEquals(address, this.properties.get("floorRelays"));
	}

	@Test
	void testChannelAddressCollection() {
		final var relays = new ReferenceDTO();
		relays.name = "relays";
		relays.target = "(&(id=${config.floorRelays;componentIdFromChannel})(enabled=true))";
		this.description.references = new ReferenceDTO[] { relays };

		final var addresses = List.of("io0/Relay1", "io0/Relay2", "io1/Relay1");
		this.properties.put("floorRelays", addresses);

		this.plugin.modifyConfiguration(this.reference, this.properties);

		assertEquals("(&(|(id=io0)(id=io1))(enabled=true))",
				this.properties.get("relays.target"));
		assertEquals(addresses, this.properties.get("floorRelays"));
	}

	@Test
	void testCollectionWithNonStringElementMatchesNothing() {
		final var relays = new ReferenceDTO();
		relays.name = "relays";
		relays.target = "(&(id=${config.floorRelays;componentIdFromChannel})(enabled=true))";
		this.description.references = new ReferenceDTO[] { relays };

		this.properties.put("relays.target", "(&(id=io0)(enabled=true))");
		this.properties.put("floorRelays", List.of("io0/Relay1", Integer.valueOf(42)));

		this.plugin.modifyConfiguration(this.reference, this.properties);

		assertEquals("(&(objectClass=*)(!(objectClass=*)))",
				this.properties.get("relays.target"));
	}
}