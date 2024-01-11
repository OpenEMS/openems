package io.openems.edge.predictor.lstm.muliithread;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.train.MakeModel;

public class MulThrTrendTrain implements Runnable {
	private ArrayList<Double> trendData;
	private ArrayList<OffsetDateTime> trendDate;
	private HyperParameters hyp;

	public MulThrTrendTrain(ArrayList<Double> data, ArrayList<OffsetDateTime> date,
			HyperParameters hyperParameters) {
		this.trendData = data;
		this.trendDate = date;
		this.hyp = hyperParameters;

	}

	@Override
	public void run() {
		MakeModel obj = new MakeModel();
		obj.trainTrend(this.trendData,this.trendDate,this.hyp);

	}

}


