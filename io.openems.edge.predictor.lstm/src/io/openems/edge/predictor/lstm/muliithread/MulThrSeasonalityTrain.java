package io.openems.edge.predictor.lstm.muliithread;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.train.MakeModel;


public class MulThrSeasonalityTrain implements Runnable {
	private HyperParameters hyp;
	private ArrayList<Double> seasonalityData;
	private ArrayList<OffsetDateTime> seasonalityDate;

	public  MulThrSeasonalityTrain(ArrayList<Double> data, ArrayList<OffsetDateTime> date,
			HyperParameters hyperParameters) {

		this.seasonalityData = data;
		this.seasonalityDate = date;
		this.hyp = hyperParameters;

	}

	@Override
	public void run() {
		MakeModel obj = new MakeModel();
		obj.trainSeasonality(this.seasonalityData, this.seasonalityDate, this.hyp);

		// TODO Auto-generated method stub

	}

}
