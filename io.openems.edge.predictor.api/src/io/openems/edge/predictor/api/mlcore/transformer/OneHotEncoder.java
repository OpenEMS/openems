package io.openems.edge.predictor.api.mlcore.transformer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public class OneHotEncoder<I> extends AbstractStatefulDataFrameTransformer<I> {

	private final List<String> columnNames;
	private final boolean fitAtTransform;

	private final Map<String, List<Double>> uniqueValuesPerColumn = new HashMap<>();

	public OneHotEncoder(List<String> columnNames) {
		this(columnNames, false);
	}

	public OneHotEncoder(List<String> columnNames, boolean fitAtTransform) {
		this.columnNames = columnNames;
		this.fitAtTransform = fitAtTransform;
	}

	@Override
	protected void safeFit(DataFrame<I> dataframe) {
		this.uniqueValuesPerColumn.clear();
		for (var columnName : this.columnNames) {
			var values = dataframe.getColumn(columnName).getValues();
			var uniqueValues = values.stream()//
					.distinct()//
					.toList();
			this.uniqueValuesPerColumn.put(columnName, uniqueValues);
		}
	}

	@Override
	protected DataFrame<I> safeTransform(DataFrame<I> dataframe) {
		if (!this.isFitted()) {
			if (!this.fitAtTransform) {
				throw new IllegalStateException("OneHotEncoder is not fitted yet");
			}
			this.fit(dataframe);
		}

		for (var columnName : this.columnNames) {
			var originalValues = dataframe.getColumn(columnName).getValues();

			var uniqueValues = this.uniqueValuesPerColumn.get(columnName);

			for (var value : uniqueValues) {
				var encoded = originalValues.stream()//
						.map(v -> value.equals(v) ? 1.0 : 0.0)//
						.toList();
				dataframe.setColumn(columnName + "_" + value, new Series<>(dataframe.getIndex(), encoded));
			}
			dataframe.removeColumn(columnName);
		}

		return dataframe;
	}
}
