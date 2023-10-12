package io.openems.edge.predictor.lstm.train;

public class LstmTrain implements LstmTrainCallBack {

	@Override
	public void onCallback() {
		// Put your code here to be executed every 15 days
		System.out.println("Training every 15 days");
	}
}
