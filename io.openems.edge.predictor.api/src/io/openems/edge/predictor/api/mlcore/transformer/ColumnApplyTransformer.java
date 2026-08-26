package io.openems.edge.predictor.api.mlcore.transformer;

import java.util.List;
import java.util.function.Function;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;

public class ColumnApplyTransformer<I> extends AbstractDataFrameTransformer<I> {

	private final Function<Double, Double> function;
	private final List<String> columnNames;

	public ColumnApplyTransformer(//
			Function<Double, Double> function, //
			List<String> columnNames) {
		this.function = function;
		this.columnNames = columnNames;
	}

	@Override
	public DataFrame<I> safeTransform(DataFrame<I> dataframe) {
		for (var columnName : this.columnNames) {
			var columnSeries = dataframe.getColumn(columnName);
			columnSeries.apply(this.function);
			dataframe.setColumn(columnName, columnSeries);
		}
		return dataframe;
	}
}
