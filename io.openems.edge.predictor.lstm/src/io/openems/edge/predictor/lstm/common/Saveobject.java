package io.openems.edge.predictor.lstm.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;

import io.openems.common.OpenemsConstants;

public class Saveobject {

	/**
	 * Saves an object to a file using Java serialization.
	 *
	 * @param hyperparameter The object to be serialized and saved.
	 * 
	 */
	public static void save(Object hyperparameter) {
		File file = Paths.get(OpenemsConstants.getOpenemsDataDir()).toFile();
		String path = file.getAbsolutePath() + File.separator + "models" + File.separator + "FenHp.fems";

		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
			oos.writeObject(hyperparameter);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
