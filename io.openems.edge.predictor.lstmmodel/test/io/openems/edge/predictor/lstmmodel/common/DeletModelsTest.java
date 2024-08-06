//package io.openems.edge.predictor.lstm.common;
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//
//import org.junit.Test;
//
//import io.openems.common.OpenemsConstants;
//
//public class DeletModelsTest {
//
//	@Test
//	public void deletModels() {
//		HyperParameters hyperParameters = HyperParameters.getInstance();
//
//		hyperParameters.setCount(5);
//
//		hyperParameters.setModelName("TestModels");
//		hyperParameters.setRmsErrorSeasonality(0.1);
//		hyperParameters.setRmsErrorSeasonality(0.1);
//		hyperParameters.setRmsErrorSeasonality(0.01); // Index of least error if 2
//		hyperParameters.setRmsErrorSeasonality(0.1);
//
//		hyperParameters.setRmsErrorTrend(0.1);
//		hyperParameters.setRmsErrorTrend(0.1);
//		hyperParameters.setRmsErrorTrend(0.01); // Index of least error if 2
//		hyperParameters.setRmsErrorTrend(0.1);
//
//		File file = Paths.get(OpenemsConstants.getOpenemsDataDir()).toFile();
//		String allModelpath = file.getAbsolutePath() + File.separator + "models" + File.separator;
//
//		try {
//			this.createTemporaryFiles(hyperParameters, allModelpath);
//			DeletModels.delet(hyperParameters);
//			try {
//				HyperParameters hyp = (HyperParameters) GetObject.get(hyperParameters.getModelName());
//
//				boolean check = 0.01 == hyp.getRmsErrorSeasonality().get(0)
//						&& 0.01 == hyp.getRmsErrorSeasonality().get(0);
//				assert (check);
//
//			} catch (ClassNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			// deleting the created files
//
//			Files.delete(Paths.get(allModelpath + "0" + hyperParameters.getModelName() + "trend.txt"));
//			Files.delete(Paths.get(allModelpath + "0" + hyperParameters.getModelName() + "seasonality.txt"));
//			Files.delete(Paths.get(allModelpath + hyperParameters.getModelName() + "fenHp.fems"));
//
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//	
//
//	}
//
//	/**
//	 * Creates temporary files for a given model based on specified hyperparameters.
//	 *
//	 * @param hyperParameters The hyperparameters for the model.
//	 * @param modelsPath      The path where temporary files will be created.
//	 * @throws IOException If an I/O error occurs during file operations.
//	 */
//
//	public void createTemporaryFiles(HyperParameters hyperParameters, String modelsPath) throws IOException {
//		String modelName = hyperParameters.getModelName();
//
//		for (int i = 0; i < hyperParameters.getCount(); i++) {
//			String modelIndex = String.valueOf(i);
//			String trendFileName = modelsPath + modelIndex + modelName + "trend.txt";
//			String seasonalityFileName = modelsPath + modelIndex + modelName + "seasonality.txt";
//
//			FileWriter fw = new FileWriter(trendFileName);
//			BufferedWriter bw = new BufferedWriter(fw);
//			bw.newLine();
//			bw.close();
//
//			FileWriter fw1 = new FileWriter(seasonalityFileName);
//			BufferedWriter bw1 = new BufferedWriter(fw1);
//			bw1.newLine();
//			bw1.close();
//
//		}
//
//		for (int i = 0; i < hyperParameters.getCount(); i++) {
//
//			String modelIndex = String.valueOf(i);
//			String trendFileName = modelsPath + modelIndex + modelName + "trend.txt";
//			String seasonalityFileName = modelsPath + modelIndex + modelName + "seasonality.txt";
//			File tempTrend = new File(trendFileName);
//			File tempSeasonality = new File(seasonalityFileName);
//			assert (tempTrend.exists());
//			assert (tempSeasonality.exists());
//
//		}
//
//	}
//
//}
