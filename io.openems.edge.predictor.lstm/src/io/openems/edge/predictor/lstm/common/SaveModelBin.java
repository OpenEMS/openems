package io.openems.edge.predictor.lstm.common;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class SaveModelBin {

	/**
	 * Saves an object to a file using Java serialization.
	 *
	 * @param weightMatrix The object to be serialized and saved.
	 * @param fileName     The name of the file to which the object will be saved.
	 */
	public static void saveModels(Object weightMatrix, String fileName) {

		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
			oos.writeObject(weightMatrix);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
}

