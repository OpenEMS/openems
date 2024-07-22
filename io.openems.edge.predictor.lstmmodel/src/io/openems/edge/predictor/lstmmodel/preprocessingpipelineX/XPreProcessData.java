package io.openems.edge.predictor.lstmmodel.preprocessingpipelineX;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;

public class XPreProcessData {

	public List<Double> data;
	public List<OffsetDateTime> date;
	public HyperParameters hyperParameters;

	public XPreProcessData(HyperParameters hyperParameters, //
			ArrayList<Double> data, //
			ArrayList<OffsetDateTime> date) {
		this.hyperParameters = hyperParameters;
		this.data = data;
		this.date = date;
	}

}
