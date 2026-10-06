package io.openems.backend.metadata.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.Test;

import io.openems.backend.common.test.DummyEventAdmin;

public class MetadataFileTest {

	@Test
	public void testGenerateUpdateMetadataCacheNotification() throws Exception {
		final var path = Files.createTempFile("metadata-file-test", ".json");
		Files.writeString(path, """
				{
				  "edges": {
				    "edge0": {
				      "apikey": "apikey0",
				      "comment": "Edge 0"
				    },
				    "edge1": {
				      "apikey": "apikey1",
				      "comment": "Edge 1"
				    },
				    "edgeWithoutApikey": {
				      "apikey": "",
				      "comment": "Edge without Apikey"
				    }
				  }
				}
				""");

		final var sut = createMetadataFile(path);
		final var notification = sut.generateUpdateMetadataCacheNotification();

		assertEquals(Map.of(//
				"apikey0", "edge0", //
				"apikey1", "edge1"), //
				notification.getApikeysToEdgeIds());
		assertFalse(notification.getApikeysToEdgeIds().containsKey(""));
	}

	private static MetadataFile createMetadataFile(Path path) throws Exception {
		final var sut = new MetadataFile();
		final Field pathField = MetadataFile.class.getDeclaredField("path");
		pathField.setAccessible(true);
		pathField.set(sut, path.toString());
		final Field eventAdminField = MetadataFile.class.getDeclaredField("eventAdmin");
		eventAdminField.setAccessible(true);
		eventAdminField.set(sut, new DummyEventAdmin(event -> {
		}));
		return sut;
	}
}
