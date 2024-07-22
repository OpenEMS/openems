package io.openems.edge.predictor.lstmmodel.preprocessingpipelineX;

import java.util.List;

interface Operation {
	List<Double> operate(List<Double> data);
}
