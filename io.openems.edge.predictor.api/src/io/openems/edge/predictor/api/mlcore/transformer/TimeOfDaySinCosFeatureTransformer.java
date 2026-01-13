package io.openems.edge.predictor.api.mlcore.transformer;

import java.util.function.Function;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public class TimeOfDaySinCosFeatureTransformer<I> extends AbstractDataFrameTransformer<I> {

	private static final int MINUTES_PER_DAY = 1_440;

	private final String sinColumnName;
	private final String cosColumnName;
	private final Function<I, Integer> minuteOfDayExtractor;

	public TimeOfDaySinCosFeatureTransformer(//
			String sinColumnName, //
			String cosColumnName, //
			Function<I, Integer> minuteOfDayExtractor) {
		this.sinColumnName = sinColumnName;
		this.cosColumnName = cosColumnName;
		this.minuteOfDayExtractor = minuteOfDayExtractor;
	}

	@Override
	protected DataFrame<I> safeTransform(DataFrame<I> dataframe) {
		var index = dataframe.getIndex();

		var sinValues = index.stream()//
				.map(idx -> {
					int minuteOfDay = this.minuteOfDayExtractor.apply(idx);
					double angle = 2 * Math.PI * ((double) minuteOfDay / MINUTES_PER_DAY);
					return Math.sin(angle);
				})//
				.toList();

		var cosValues = index.stream()//
				.map(idx -> {
					int minuteOfDay = this.minuteOfDayExtractor.apply(idx);
					double angle = 2 * Math.PI * ((double) minuteOfDay / MINUTES_PER_DAY);
					return Math.cos(angle);
				})//
				.toList();

		dataframe.setColumn(this.sinColumnName, new Series<>(index, sinValues));
		dataframe.setColumn(this.cosColumnName, new Series<>(index, cosValues));

		return dataframe;
	}
}
