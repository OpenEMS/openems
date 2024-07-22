package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

/**
 * Defines an interface for a pipeline that processes data through stages.
 *
 * @param <Out> The output type of the pipeline.
 * @param <In>  The input type of the pipeline.
 */

public interface PiplineInterface<O, I> {
	/**
	 * Adds a stage to the pipeline.
	 *
	 * @param stage The stage to be added to the pipeline.
	 */
	void add(Stage<O, I> stage);

	/**
	 * Executes the pipeline, processing the data through the added stages.
	 *
	 * @return The result of executing the pipeline.
	 */

	Object execute();

}
