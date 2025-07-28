package io.openems.edge.predictor.production.linearmodel;

import java.nio.file.Path;

import io.openems.edge.predictor.production.linearmodel.PredictorProductionLinearModelImpl.LocalConfig;

public record DummyLocalConfig(//
		Path modelsDirectoryPath, //
		int trainingIntervalInDays, //
		int trainingWindowInQuarters, //
		int maxAgeOfModelInDays, //
		int halfLifeInQuarters, //
		double minTrainingDataRatio//
) implements LocalConfig {

	public DummyLocalConfig(Path modelsDirectoryPath) {
		this(modelsDirectoryPath, //
				30 * 24 * 4, // trainingWindowInQuarters: 30 days
				3 * 24 * 4 // halfLifeInQuarters: 3 days
		);
	}

	public DummyLocalConfig(Path modelsDirectoryPath, int trainingWindowInQuarters, int halfLifeInQuarters) {
		this(modelsDirectoryPath, //
				1, // trainingIntervalInDays
				trainingWindowInQuarters, //
				7, // maxAgeOfModelInDays
				halfLifeInQuarters, //
				0.9 // minTrainingDataRatio
		);
	}
}
