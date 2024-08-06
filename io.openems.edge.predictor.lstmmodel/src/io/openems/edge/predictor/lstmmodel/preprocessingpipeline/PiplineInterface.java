package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

/**
 * Represents a pipeline that processes data through a series of stages.
 *
 * @param <O> The type of the output produced by the pipeline.
 * @param <I> The type of the input consumed by the pipeline.
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
