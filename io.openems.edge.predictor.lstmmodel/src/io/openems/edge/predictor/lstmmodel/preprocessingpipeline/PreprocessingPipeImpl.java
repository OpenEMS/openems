package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;

public class PreprocessingPipeImpl implements PiplineInterface<Object, Object> {
	private Object inputData;
	private Object outputData;
	private Object mean;
	private Object standardDeviation;
	private HyperParameters hyperParameter;
	private double scalingFactor = 0.001;
	private ArrayList<OffsetDateTime> dates;
	private List<Stage<Object, Object>> stages = new ArrayList<>();

	public PreprocessingPipeImpl(ArrayList<Double> data, ArrayList<OffsetDateTime> dates,
			HyperParameters hyperParameters) {
		this.inputData = data;
		this.dates = dates;
		this.hyperParameter = hyperParameters;
	}

	public PreprocessingPipeImpl(HyperParameters hyperParameters) {
		this.hyperParameter = hyperParameters;
	}

	private PreprocessingPipeImpl addStage(Stage<Object, Object> stage) {
		this.stages.add(stage);
		return this;
	}

	/**
	 * Add moving average stage.
	 * 
	 * @return this
	 */
	public PreprocessingPipeImpl movingAverage() {
		return this.addStage(new MovingAveragePipe());
	}

	/**
	 * Add moving scale stage.
	 * 
	 * @return this
	 */
	public PreprocessingPipeImpl scale() {
		return this.addStage(new ScalingPipe(this.hyperParameter));
	}

	/**
	 * Add moving constantscale stage.
	 * 
	 * @return this
	 */
	public PreprocessingPipeImpl constantscale() {
		return this.addStage(new ConstantScalingPipe(this.scalingFactor));
	}

	/**
	 * Add moving trainTestSplit stage.
	 * 
	 * @return this
	 */
	public PreprocessingPipeImpl trainTestSplit() {
		return this.addStage(new TrainandTestSplitPipe(this.hyperParameter));
	}

	/**
	 * Add moving filterOutliers stage.
	 * 
	 * @return this
	 */
	public PreprocessingPipeImpl filterOutliers() {
		return this.addStage(new FilterOutliersPipe());
	}

	/**
	 * Add moving normalize stage.
	 * 
	 * @return this
	 */
	public PreprocessingPipeImpl normalize() {
		return this.addStage(new NormalizePipe(this.hyperParameter));
	}

	/**
	 * Add moving groupToWIndowTrend stage.
	 * 
	 * @return this
	 */
	public PreprocessingPipeImpl groupToWIndowTrend() {
		return this.addStage(new GrouptoWindowpipe(this.hyperParameter.getWindowSizeTrend()));
	}

	/**
	 * Add moving groupToWIndowSeasonality stage.
	 * 
	 * @return this
	 */
	public PreprocessingPipeImpl groupToWIndowSeasonality() {
		return this.addStage(new GrouptoWindowpipe(this.hyperParameter.getWindowSizeSeasonality()));
	}

	/**
	 * Add moving shuffel stage.
	 * 
	 * @return this
	 */
	public PreprocessingPipeImpl shuffel() {
		return this.addStage(new ShufflePipe());
	}

	/**
	 * Add moving groupByHoursAndMinutes stage.
	 * 
	 * @return this
	 */
	public PreprocessingPipeImpl groupByHoursAndMinutes() {
		return this.addStage(new GroupbyPipe(this.hyperParameter, this.dates));
	}

	/**
	 * Add moving Remove Negatives
	 * 
	 * @return this
	 */

	public PreprocessingPipeImpl removeNegatives() {
		return this.addStage(new RemoveNegativesPipe());
	}

	/**
	 * Add moving groupToStiffedWindow stage.
	 * 
	 * @return this
	 */
	public PreprocessingPipeImpl groupToStiffedWindow() {
		return this.addStage(new GroupToStiffWindowPipe(this.hyperParameter.getWindowSizeTrend()));
	}

	/**
	 * Add moving interpolate stage.
	 * 
	 * @return this
	 */
	public PreprocessingPipeImpl interpolate() {
		return this.addStage(new InterpolationPipe(this.hyperParameter, this.dates));
	}

	/**
	 * Add moving modifyForShortTermPrediction stage.
	 * 
	 * @return this
	 */
	public PreprocessingPipeImpl modifyForTrendPrediction() {
		return this.addStage(new ModifyDataForTrend(this.dates, this.hyperParameter));
	}

	/**
	 * Add moving differencing stage.
	 * 
	 * @return this
	 */
	public PreprocessingPipeImpl differencing() {
		return this.addStage(new DifferencingPipe());
	}

	/**
	 * Add moving reverseScale stage.
	 * 
	 * @return this
	 */
	public PreprocessingPipeImpl reverseScale() {
		return this.addStage(new ReverseScalingPipe(this.hyperParameter));
	}

	/**
	 * Add moving reverseNormalize stage.
	 * 
	 * @return this
	 */
	public PreprocessingPipeImpl reverseNormalize() {
		return this.addStage(new ReverseNormalizationPipe(this.mean, this.standardDeviation, this.hyperParameter));
	}

	@Override
	public Object execute() {
		Object preprocessingInput = this.inputData;
		for (Stage<Object, Object> i : this.stages) {
			preprocessingInput = i.execute(preprocessingInput);
		}
		this.outputData = preprocessingInput;
		this.stages = new ArrayList<Stage<Object, Object>>();
		return this.outputData;
	}

	@Override
	public void add(Stage<Object, Object> stage) {
		this.stages.add(stage);
	}

	public PreprocessingPipeImpl setData(Object val) {
		this.inputData = val;
		return this;
	}

	public PreprocessingPipeImpl setMean(Object val) {
		this.mean = val;
		return this;
	}

	public PreprocessingPipeImpl setStandardDeviation(Object val) {
		this.standardDeviation = val;
		return this;
	}

	public PreprocessingPipeImpl setDates(ArrayList<OffsetDateTime> date) {
		this.dates = date;
		return this;
	}

	public PreprocessingPipeImpl setScalingFactor(double val) {
		this.scalingFactor = val;
		return this;
	}
}
