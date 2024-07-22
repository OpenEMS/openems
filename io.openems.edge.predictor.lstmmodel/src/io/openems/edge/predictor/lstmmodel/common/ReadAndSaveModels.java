package io.openems.edge.predictor.lstmmodel.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.openems.common.OpenemsConstants;
import io.openems.edge.predictor.lstmmodel.validator.ValidationSeasonalityModel;
import io.openems.edge.predictor.lstmmodel.validator.ValidationTrendModel;

public class ReadAndSaveModels {

	private static final String MODEL_DIRECTORY = Paths.get(OpenemsConstants.getOpenemsDataDir())//
			.toFile()//
			.getAbsolutePath();

	private static final String MODEL_FOLDER = File.separator + "models" + File.separator;

	/**
	 * Saves the {@link HyperParameters} object to a file in JSON format. This
	 * method serializes the provided {@link HyperParameters} object into JSON
	 * format and saves it to a file with the specified name in the "models"
	 * directory. The serialization process utilizes a custom Gson instance
	 * configured to handle the serialization of OffsetDateTime objects. The file is
	 * saved in the directory specified by the OpenEMS data directory.
	 * 
	 * @param hyperParameters The {@link HyperParameters} object to be saved.
	 */
	public static void save(HyperParameters hyperParameters) {
		String modelName = hyperParameters.getModelName();
		String filePath = Paths.get(MODEL_DIRECTORY, MODEL_FOLDER, modelName)//
				.toString();

		Gson gson = new GsonBuilder()//
				.registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())//
				.create();

		try {
			var compressedData = compress(hyperParameters);
			var compressedDataString = Base64.getEncoder().encodeToString(compressedData);
			var json = gson.toJson(compressedDataString);

			try (FileWriter writer = new FileWriter(filePath)) {
				writer.write(json);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads and de-serializes a {@link HyperParameters} object from a JSON file.
	 * This method reads a HyperParameters object from the specified JSON file,
	 * de-serializing it into a {@link HyperParameters} instance. The
	 * de-serialization process utilizes a custom Gson instance configured to handle
	 * the de-serialization of {@link OffsetDateTime} objects. The file is expected
	 * to be located in the "models" directory within the OpenEMS data directory.
	 * 
	 * @param the fileName The name of the JSON file to read the HyperParameters
	 *            from.
	 * @return The {@link HyperParameters} object read from the file.
	 * @throws FileNotFoundException If the specified file is not found.
	 * @throws IOException           If an I/O error occurs while reading the file.
	 */
	public static HyperParameters read(String fileName) {

		String filePath = Paths.get(MODEL_DIRECTORY, MODEL_FOLDER, fileName)//
				.toString();

		try (Reader reader = new FileReader(filePath)) {
			Gson gson = new GsonBuilder()//
					.registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())//
					.create();
			var json = gson.fromJson(reader, String.class);
			var deserializedData = Base64.getDecoder().decode(json);
			return decompress(deserializedData);
		} catch (IOException e) {
			var hyperParameters = new HyperParameters();
			hyperParameters.setModelName(fileName);
			return hyperParameters;
		}
	}

	/**
	 * Compress the data.
	 * 
	 * @param hyp the Hyper parameter object
	 * @return compressend byte array
	 */
	public static byte[] compress(HyperParameters hyp) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DeflaterOutputStream dos = new DeflaterOutputStream(baos);
				ObjectOutputStream oos = new ObjectOutputStream(dos)) {

			oos.writeObject(hyp);
			dos.finish();
			return baos.toByteArray();

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * DeCompress the data.
	 * 
	 * @param value the value array to decompress
	 * @return Hyper parameter
	 */
	public static HyperParameters decompress(byte[] value) {
		HyperParameters hyperParameters = null;
		try (ByteArrayInputStream bais = new ByteArrayInputStream(value);
				InflaterInputStream iis = new InflaterInputStream(bais);
				ObjectInputStream ois = new ObjectInputStream(iis)) {
			hyperParameters = (HyperParameters) ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return hyperParameters;
	}

	/**
	 * Adapt it.
	 * 
	 * @param hyperParameters the Hyperparameter
	 * @param data            the data
	 * @param dates           the dates
	 */
	public static void adapt(HyperParameters hyperParameters, ArrayList<Double> data, ArrayList<OffsetDateTime> dates) {
		if (hyperParameters.getCount() == 0) {
			return;
		}

		var valSeas = new ValidationSeasonalityModel();
		var valTrend = new ValidationTrendModel();

		hyperParameters.resetModelErrorValue();

		valSeas.validateSeasonality(data, dates, hyperParameters.getAllModelSeasonality(), hyperParameters);
		valTrend.validateTrend(data, dates, hyperParameters.getAllModelsTrend(), hyperParameters);
	}
}
