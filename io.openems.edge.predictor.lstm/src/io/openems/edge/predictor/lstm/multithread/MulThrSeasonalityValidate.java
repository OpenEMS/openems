package io.openems.edge.predictor.lstm.multithread;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.validator.Validation;

public class MulThrSeasonalityValidate implements Runnable {

	private HyperParameters hyp;
	private ArrayList<Double> seasonalityData;
	private ArrayList<OffsetDateTime> seasonalityDate;

	public MulThrSeasonalityValidate(ArrayList<Double> data, ArrayList<OffsetDateTime> date,
			HyperParameters hyperParameters) {

		this.seasonalityData = data;
		this.seasonalityDate = date;
		this.hyp = hyperParameters;

	}

	@Override
	public void run() {
		Validation obj = new Validation(this.seasonalityData, this.seasonalityDate, this.hyp);
		obj.validateSeasonality(this.seasonalityData, this.seasonalityDate, this.hyp);


	}

}
