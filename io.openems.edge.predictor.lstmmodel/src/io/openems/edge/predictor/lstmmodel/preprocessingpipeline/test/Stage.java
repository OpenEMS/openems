package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test;

/**
 * The {@code Stage} interface represents a processing stage in a data pipeline.
 * 
 * @param <I> the type of input to the stage
 * @param <O> the type of output from the stage
 */
public interface Stage<I, O> {

	/**
	 * Processes the given input and returns the output.
	 * 
	 * @param input the input to be processed
	 * @return the result of processing the input
	 */
	O execute(I input);

}
