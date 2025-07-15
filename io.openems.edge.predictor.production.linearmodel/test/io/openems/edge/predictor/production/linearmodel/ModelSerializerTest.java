package io.openems.edge.predictor.production.linearmodel;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.time.ZonedDateTime;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ModelSerializerTest {

	private static final double DELTA = 1e-6;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void readModelConfigState_ShouldReturnCachedInstance_IfFileNotModified() throws Exception {
		var modelConfigState = new ModelConfigState(//
				ZonedDateTime.now(), //
				new double[] { 1.0, 2.0, 3.0 });

		var tempDirectory = this.tempFolder.newFolder("models").toPath();
		var modelSerializer = new ModelSerializer(tempDirectory);

		// Save once
		modelSerializer.saveModelConfigState(modelConfigState);

		// First read - loads from file
		final var firstRead = modelSerializer.readModelConfigState();
		// Second read - should return cached instance
		final var secondRead = modelSerializer.readModelConfigState();

		// Same object reference => cache was used
		assertSame(firstRead, secondRead);
	}

	@Test
	public void readModelConfigState_ShouldReload_IfFileIsModified() throws Exception {
		var tempDirectory = this.tempFolder.newFolder("models").toPath();
		var modelSerializer = new ModelSerializer(tempDirectory);

		// Save initial state
		var initialState = new ModelConfigState(//
				ZonedDateTime.now(), //
				new double[] { 1.0, 2.0, 3.0 });
		modelSerializer.saveModelConfigState(initialState);

		final var firstRead = modelSerializer.readModelConfigState();

		// Modify and re-save a new state
		Thread.sleep(1000); // Ensure file timestamp changes
		var newState = new ModelConfigState(//
				ZonedDateTime.now().plusDays(1), //
				new double[] { 4.0, 5.0, 6.0 });
		modelSerializer.saveModelConfigState(newState);

		final var secondRead = modelSerializer.readModelConfigState();

		assertNotSame(firstRead, secondRead);
		assertArrayEquals(newState.betas(), secondRead.betas(), DELTA);
		assertEquals(newState.lastTrainedDate(), secondRead.lastTrainedDate());
	}

	@Test
	public void testReadModelConfigState_ShouldThrowException_IfFileDoesNotExist() throws Exception {
		var tempDirectory = this.tempFolder.newFolder("models").toPath();
		var modelSerializer = new ModelSerializer(tempDirectory);

		assertThrows(IOException.class, () -> {
			modelSerializer.readModelConfigState();
		});
	}
}
