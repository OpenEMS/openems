package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test;

public interface Stage<I, O> {

	O execute(I input);

}
