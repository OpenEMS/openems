package io.openems.edge.predictor.lstm.muliithread;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.validator.Validation;

public class MulThrTrendValidate implements Runnable {
	private ArrayList<Double> trendData;
	private ArrayList<OffsetDateTime> trendDate;
	private HyperParameters hyp;

	public MulThrTrendValidate(ArrayList<Double> data, ArrayList<OffsetDateTime> date,
			HyperParameters hyperParameters) {
		this.trendData = data;
		this.trendDate = date;
		this.hyp = hyperParameters;

	}

	@Override
	public void run() {
		Validation obj = new Validation(this.trendData,this.trendDate,this.hyp);
		obj.validateTrend(this.trendData,this.trendDate,this.hyp);
		// TODO Auto-generated method stub

	}

}
