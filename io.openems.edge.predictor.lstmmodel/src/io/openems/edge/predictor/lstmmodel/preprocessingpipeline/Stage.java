package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

public interface Stage<O, I> {
	/**
	 * Executes the stage's processing logic on the provided input.
	 *
	 * @param input The input data to be processed by the stage.
	 * @return The result of the processing, typically of type O.
	 */
	O execute(final I input);

}
