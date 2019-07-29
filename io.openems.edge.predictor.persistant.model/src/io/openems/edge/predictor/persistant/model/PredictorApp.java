package io.openems.edge.predictor.persistant.model;


import io.openems.edge.predictor.persistant.consumption.ConsumptionPersistantModelPredictor;
import io.openems.edge.predictor.persistant.production.ProductionPersistantModelPredictor;


public class PredictorApp {

	



	public static void main(String[] args) throws InterruptedException {

		// Dummy
		ProductionPersistantModelPredictor productionPersistantModelPredictor = new ProductionPersistantModelPredictor();
		Integer[] val = productionPersistantModelPredictor.get24hPrediction().values;
		for (Integer integer : val) {
			System.out.println(integer.toString());
		}
		
		ConsumptionPersistantModelPredictor consumptionPersistantModelPredictor = new ConsumptionPersistantModelPredictor();
		Integer[] val1 = consumptionPersistantModelPredictor.get24hPrediction().values;
		for (Integer integer : val1) {
			System.out.println(integer.toString());
		}

	}
}
