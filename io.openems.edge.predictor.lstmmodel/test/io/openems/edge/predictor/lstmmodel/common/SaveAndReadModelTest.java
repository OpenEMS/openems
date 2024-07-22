//package io.openems.edge.predictor.lstm.common;
//
//import static org.junit.Assert.assertEquals;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//
//import org.junit.Test;
//
//import io.openems.common.OpenemsConstants;
//import io.openems.edge.predictor.lstm.train.MakeModel;
//
//public class SaveAndReadModelTest {
//
//	//@Test
//	public void saveModels() {
//
//		HyperParameters hyperParameter = HyperParameters.getInstance();
//		hyperParameter.setModelName("Test");
//
//		// generate models
//
//		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> generatedModelTrend = this
//				.generateWeight(hyperParameter.getWindowSizeTrend(), hyperParameter);
//		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> generatedModelSeasonality = this
//				.generateWeight(hyperParameter.getWindowSizeSeasonality(), hyperParameter);
//
//		// Saving model as Trend
//		SaveModel.saveModels(generatedModelTrend, "TestTrendModel.txt");
//		// Saving model as seasonality
//		SaveModel.saveModels(generatedModelSeasonality, "TestSeasonalityModel.txt");
//
//		File trend = new File(this.getModelPath(hyperParameter.getModelName() + "TrendModel.txt"));
//		File seasonality = new File(this.getModelPath(hyperParameter.getModelName() + "SeasonalityModel.txt"));
//		assert (trend.exists());
//		assert (seasonality.exists());
//		
//		try {
//			Files.delete(Paths.get(this.getModelPath(hyperParameter.getModelName() + "SeasonalityModel.txt")));
//			Files.delete(Paths.get(this.getModelPath(hyperParameter.getModelName() + "TrendModel.txt")));
//
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//	}
//
//	@Test
//	public void readModels() {
//		HyperParameters hyperParameter = HyperParameters.getInstance();
//		hyperParameter.setModelName("Test");
//		
//		
//
//		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> generatedModelTrend = this
//				.generateWeight(hyperParameter.getWindowSizeTrend(), hyperParameter);
//		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> generatedModelSeasonality = this
//				.generateWeight(hyperParameter.getWindowSizeSeasonality(), hyperParameter);
//		
//		// Saving model as Trend
//		SaveModel.saveModels(generatedModelTrend, "TestTrendModel.txt");
//		// Saving model as seasonality
//		SaveModel.saveModels(generatedModelSeasonality, "TestSeasonalityModel.txt");
//
//		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> modelSeasonality = ReadModels.getModelForSeasonality(
//				this.getModelPath(hyperParameter.getModelName() + "SeasonalityModel.txt"), hyperParameter);
//		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> modelTrend = ReadModels.getModelForSeasonality(
//				this.getModelPath(hyperParameter.getModelName() + "TrendModel.txt"), hyperParameter);
//
//		for (int i = 0; i < (60 / hyperParameter.getInterval()) * 24; i++) {
//			// for seasonality
//
//			assertEquals(modelSeasonality.get(0).get(i).get(0), generatedModelSeasonality.get(i).get(0).get(0));
//			assertEquals(modelSeasonality.get(0).get(i).get(1), generatedModelSeasonality.get(i).get(0).get(1));
//			assertEquals(modelSeasonality.get(0).get(i).get(2), generatedModelSeasonality.get(i).get(0).get(2));
//			assertEquals(modelSeasonality.get(0).get(i).get(3), generatedModelSeasonality.get(i).get(0).get(3));
//			assertEquals(modelSeasonality.get(0).get(i).get(4), generatedModelSeasonality.get(i).get(0).get(4));
//			assertEquals(modelSeasonality.get(0).get(i).get(5), generatedModelSeasonality.get(i).get(0).get(5));
//			assertEquals(modelSeasonality.get(0).get(i).get(6), generatedModelSeasonality.get(i).get(0).get(6));
//			assertEquals(modelSeasonality.get(0).get(i).get(7), generatedModelSeasonality.get(i).get(0).get(7));
//
//			// for trend
//
//			assertEquals(modelTrend.get(0).get(i).get(0), generatedModelTrend.get(i).get(0).get(0));
//			assertEquals(modelTrend.get(0).get(i).get(1), generatedModelTrend.get(i).get(0).get(1));
//			assertEquals(modelTrend.get(0).get(i).get(2), generatedModelTrend.get(i).get(0).get(2));
//			assertEquals(modelTrend.get(0).get(i).get(3), generatedModelTrend.get(i).get(0).get(3));
//			assertEquals(modelTrend.get(0).get(i).get(4), generatedModelTrend.get(i).get(0).get(4));
//			assertEquals(modelTrend.get(0).get(i).get(5), generatedModelTrend.get(i).get(0).get(5));
//			assertEquals(modelTrend.get(0).get(i).get(6), generatedModelTrend.get(i).get(0).get(6));
//			assertEquals(modelTrend.get(0).get(i).get(7), generatedModelTrend.get(i).get(0).get(7));
//
//		}
//
//		// Deleting the saved file
//		try {
//			Files.delete(Paths.get(this.getModelPath(hyperParameter.getModelName() + "SeasonalityModel.txt")));
//			Files.delete(Paths.get(this.getModelPath(hyperParameter.getModelName() + "TrendModel.txt")));
//
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//	}
//
//	/**
//	 * Generates a 4D ArrayList representing weights for a neural network over a
//	 * specified time window. The weights are initialized based on the provided
//	 * hyperparameters.
//	 *
//	 * @param windowSize      The size of the time window for which weights are
//	 *                        generated.
//	 * @param hyperParameters The hyperparameters used for weight initialization.
//	 * @return A 4D ArrayList representing weights for each parameter type and time
//	 *         step. The structure is:
//	 *         [layer][parameterType][timeStep][weightValue].
//	 * @throws IllegalArgumentException If an invalid parameter type is encountered
//	 *                                  during weight initialization.
//	 */
//	public ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> generateWeight(int windowSize,
//			HyperParameters hyperParameters) {
//
//		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> finalWeight = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
//
//		for (int i = 0; i < 24 * 60 / hyperParameters.getInterval(); i++) {
//			ArrayList<ArrayList<ArrayList<Double>>> temp = new ArrayList<ArrayList<ArrayList<Double>>>();
//			temp.add(MakeModel.generateInitialWeightMatrix(windowSize, hyperParameters));
//			finalWeight.add(temp);
//		}
//
//		return finalWeight;
//	}
//
//	/**
//	 * Constructs the absolute path for a model file based on the provided suffix.
//	 * The constructed path is within the OpenEMS data directory under the "models"
//	 * subdirectory.
//	 *
//	 * @param suffix The suffix to be appended to the model file path.
//	 * @return The absolute path for the model file with the specified suffix.
//	 */
//
//	public String getModelPath(String suffix) {
//		File file = Paths.get(OpenemsConstants.getOpenemsDataDir()).toFile();
//		return file.getAbsolutePath() + File.separator + "models" + File.separator + suffix;
//	}
//
//}
