package io.openems.edge.predictor.production.linearmodel;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonParser;

public class ModelSerializer {

	private final Path modelPath;

	private long lastModified = -1;
	private ModelConfigState cachedState;

	public ModelSerializer(Path modelDirectory) {
		this.modelPath = modelDirectory.resolve("linearmodel.json");
	}

	/**
	 * Saves the given {@link ModelConfigState} to a JSON file.
	 * 
	 * <p>
	 * Ensures the parent directory of the model file exists, creating it if
	 * necessary, and then serializes the {@link ModelConfigState} to the file. The
	 * cache is updated after saving.
	 * </p>
	 *
	 * @param modelConfigState The {@link ModelConfigState} to be saved.
	 * @throws IOException If an I/O error occurs while writing the file or creating
	 *                     the directory.
	 */
	public synchronized void saveModelConfigState(ModelConfigState modelConfigState) throws IOException {
		var modelDirectory = this.modelPath.getParent();
		if (!Files.exists(modelDirectory)) {
			Files.createDirectories(modelDirectory);
		}

		var file = this.modelPath.toFile();

		final var serializer = ModelConfigState.serializer();

		try (var writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(serializer.serialize(modelConfigState).toString());
		}

		this.cachedState = modelConfigState;
		this.lastModified = file.lastModified();
	}

	/**
	 * Reads the model configuration state from a JSON file.
	 * 
	 * <p>
	 * If the file has been modified since the last read, it deserializes its
	 * content into a {@link ModelConfigState} object and updates the cache.
	 * </p>
	 *
	 * @return The deserialized {@link ModelConfigState} object.
	 * @throws IOException If an error occurs while reading the file.
	 */
	public synchronized ModelConfigState readModelConfigState() throws IOException {
		final var file = this.modelPath.toFile();
		final var currentModified = file.lastModified();

		final var serializer = ModelConfigState.serializer();

		if (this.cachedState == null || currentModified != this.lastModified) {
			try (var reader = new FileReader(file)) {
				var jsonElement = JsonParser.parseReader(reader);
				var jsonObject = jsonElement.getAsJsonObject();
				this.cachedState = serializer.deserialize(jsonObject);
				this.lastModified = currentModified;
			}
		}

		return this.cachedState;
	}
}
