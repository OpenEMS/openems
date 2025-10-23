package io.openems.edge.predictor.api.mlcore.transformer;

import java.time.DayOfWeek;
import java.util.function.Function;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public class DayOfWeekFeatureTransformer<I> extends AbstractDataFrameTransformer<I> {

	private final String columnName;
	private final Function<I, DayOfWeek> dayOfWeekExtractor;

	public DayOfWeekFeatureTransformer(//
			String columnName, //
			Function<I, DayOfWeek> dayOfWeekExtractor) {
		this.columnName = columnName;
		this.dayOfWeekExtractor = dayOfWeekExtractor;
	}

	@Override
	protected DataFrame<I> safeTransform(DataFrame<I> dataframe) {
		var index = dataframe.getIndex();
		var dayOfWeekValues = index.stream()//
				.map(i -> (double) this.dayOfWeekExtractor.apply(i).getValue())//
				.toList();

		dataframe.setColumn(this.columnName, new Series<>(index, dayOfWeekValues));
		return dataframe;
	}
}
