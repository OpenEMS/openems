package io.openems.edge.predictor.api.mlcore.transformer;

import java.util.List;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;

public class DropColumnsTransformer<I> extends AbstractDataFrameTransformer<I> {

	private final List<String> columnNames;

	public DropColumnsTransformer(List<String> columnNames) {
		this.columnNames = columnNames;
	}

	@Override
	public DataFrame<I> safeTransform(DataFrame<I> dataframe) {
		for (var columnName : this.columnNames) {
			dataframe.removeColumn(columnName);
		}
		return dataframe;
	}
}
