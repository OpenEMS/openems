package io.openems.edge.predictor.persistant.model;

public class PredictorApp {



	public static void main(String[] args) throws InterruptedException {

		// Dummy
		ProductionPersistantModelPredictor productionPersistantModelPredictor = new ProductionPersistantModelPredictor();
		Integer[] val = productionPersistantModelPredictor.get24hPrediction().values;
		for (Integer integer : val) {
			System.out.println(integer.toString());
		}

	}
}
