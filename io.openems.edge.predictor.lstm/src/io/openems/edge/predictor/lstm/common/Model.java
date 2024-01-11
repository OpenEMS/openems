package io.openems.edge.predictor.lstm.common;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class Model implements Serializable {

	private static final long serialVersionUID = 1L;
	private int batchTrack = 0;
	private int epochTrack = 0;
	private double rmsErrorTrend;
	private double rmsErrorSeasonality;
	private double accuracyTrend;
	private double accuracySeasonality;
	private ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> trendModle;
	private ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> seasonalityModle;
	private ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allTrendModle;
	private ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allSeasonalityModle;
	private ArrayList<Double> allSeasonalityModleRmsError;
	private ArrayList<Double> allTrendModleRmsError;

	public Model() {

	}

	public void setBatchCount(int val) {
		this.batchTrack = val;

	}

	public int getBatchCount() {
		return this.batchTrack;

	}

	public void setEpochTrack(int val) {
		this.epochTrack = val;

	}

	public int getEpochTrack() {
		return this.epochTrack;

	}

	public void setRmsErrorTrend(double val) {
		this.rmsErrorTrend = val;
	}

	public double getRmsErrorTrend() {
		return this.rmsErrorTrend;
	}

	public void setRmsErrorSeasonality(double val) {
		this.rmsErrorSeasonality = val;
	}

	public double getRmsErrorSeasonality() {
		return this.rmsErrorSeasonality;
	}

	public void setAccuracyTrend(double val) {
		this.accuracyTrend = val;
	}

	public double getAccuracyTrend() {
		return this.accuracyTrend;
	}

	public void setAccuracySeasonality(double val) {
		this.accuracySeasonality = val;
	}

	public double getAccuracySeasonality() {
		return this.accuracySeasonality;
	}

	public void setTrendModle(ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> val) {
		this.trendModle = val;

	}

	public ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> getTrendModle() {
		return this.trendModle;
	}

	public void setSeasonalityModel(ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> val) {
		this.seasonalityModle = val;
	}

	public ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> getSeasonalityModle() {
		return this.seasonalityModle;
	}

	public void setTrendAllModle(ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> val) {
		this.allTrendModle = val;

	}

	public ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> getTrendAllModle() {
		return this.allTrendModle;
	}

	public void setSeasonalityAllModel(ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> val) {
		this.allSeasonalityModle = val;
	}

	public ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> getSeasonalityAllModle() {
		return this.allSeasonalityModle;
	}

	public void setAllSeasonalityModleRmsError(ArrayList<Double> val) {
		this.allSeasonalityModleRmsError = val;

	}

	public ArrayList<Double> getAllSeasonalityModleRmsError() {
		return this.allSeasonalityModleRmsError;
	}

	public void setAllTrendModleRmsError(ArrayList<Double> val) {
		this.allTrendModleRmsError = val;

	}

	public ArrayList<Double> getAllTrendModleRmsError() {
		return this.allTrendModleRmsError;
	}

	/**
	 * Deserializes an object from a file and prints its value.
	 *
	 * @param path The name of the file from which the object will be deserialized.
	 * @return temp the modle
	 * @throws IOException            If an I/O error occurs during deserialization.
	 * @throws ClassNotFoundException If the class of the serialized object cannot
	 *                                be found.
	 */

	public static Object read(String path) throws ClassNotFoundException, IOException {
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {

			return ois.readObject();

		}

	}

	/**
	 * Deserializes an object from a file and prints its value.
	 *
	 * @return temp the modle
	 * @throws IOException            If an I/O error occurs during deserialization.
	 * @throws ClassNotFoundException If the class of the serialized object cannot
	 *                                be found.
	 */

	public static Object read() throws ClassNotFoundException, IOException {

		// String openemsDirectory = OpenemsConstants.getOpenemsDataDir();
		// String path = openemsDirectory + "/models/fems.fen";

		String path = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\fems.fe";
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {

			return ois.readObject();
		}

	}

	/**
	 * Saves an object to a file using Java serialization.
	 *
	 * @param val  The object to be serialized and saved.
	 * @param path is the path to save the model
	 */

	public static void save(Object val, String path) {

		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
			oos.writeObject(val);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Saves an object to a file using Java serialization.
	 *
	 * @param val The object to be serialized and saved.
	 */

	public static void save(Object val) {
		// String openemsDirectory = OpenemsConstants.getOpenemsDataDir();
		// System.out.println(openemsDirectory);
		// String path = openemsDirectory + "/models/fems.fen";
		String path = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\fems.fe";

		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
			oos.writeObject(val);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
