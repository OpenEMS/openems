package io.openems.edge.predictor.lstm.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import io.openems.common.OpenemsConstants;

public class DeletModels {
	
	
	/**
	 * Deletes models based on minimum error criteria, saves the best models as '0trend.txt'
	 * and '0seasonality.txt', and updates the hyperparameter object accordingly.
	 *
	 * @param hyper An instance of the HyperParameters class containing model information.	
	 */

	public static void delet(HyperParameters hyper) {

		// get the path of the all the models
		File file = Paths.get(OpenemsConstants.getOpenemsDataDir()).toFile();
		String allModelpath = file.getAbsolutePath() + File.separator + "models" + File.separator;
		// get the the filename of minimum error models
		String toSaveSeas = Integer.toString(hyper.getMinimumErrorModelSeasonality()) + "seasonality.txt";
		String toSaveTrend = Integer.toString(hyper.getMinimumErrorModelTrend()) + "trend.txt";
		// read minimum error model seasonality
		final var modelSeasonality = ReadModels.getModelForSeasonality(allModelpath + toSaveSeas, hyper);
		// read minimum error model
		var modelTrend = ReadModels.getModelForSeasonality(allModelpath + toSaveTrend, hyper);
		// deleting all models
		filterDelet(toSaveTrend, "trend", hyper);
		filterDelet(toSaveSeas, "seasonality", hyper);

		// savve the best model as 0Seasonality, 0trend.txt

		SaveModel.saveModels(modelTrend, "0trend.txt");
		SaveModel.saveModels(modelSeasonality, "0seasonality.txt");
		// read model seasonalityS

		final double minErrorTrend = Collections.min(hyper.getRmsErrorTrend());
		double minErrorSeasonality = Collections.min(hyper.getRmsErrorSeasonality());
		hyper.getRmsErrorSeasonality().clear();
		hyper.getRmsErrorTrend().clear();
		hyper.setRmsErrorSeasonality(minErrorSeasonality);
		hyper.setRmsErrorTrend(minErrorTrend);
		hyper.setCount(1);
		// saving the hyperparameter object
		Saveobject.save(hyper);
	}

	private static void filterDelet(String toFilter, String modelType, HyperParameters hype) {
		File file = Paths.get(OpenemsConstants.getOpenemsDataDir()).toFile();
		String allModelpath = file.getAbsolutePath() + File.separator + "models" + File.separator;
		for (int i = 0; i < hype.getCount(); i++) {
			String fileName = Integer.toString(i) + modelType + ".txt";
			String path = allModelpath + fileName;
			Path fpath = Paths.get(path);
			try {
				Files.delete(fpath);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

}
