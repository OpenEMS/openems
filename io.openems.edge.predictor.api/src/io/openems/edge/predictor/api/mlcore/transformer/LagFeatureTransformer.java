package io.openems.edge.predictor.api.mlcore.transformer;

import java.util.ArrayList;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public class LagFeatureTransformer<I> extends AbstractDataFrameTransformer<I> {

	private final String columnName;
	private final int lag;
	private final String newFeatureName;

	public LagFeatureTransformer(String columnName, int lag, String newFeatureName) {
		this.columnName = columnName;
		this.lag = lag;
		this.newFeatureName = newFeatureName;
	}

	@Override
	protected DataFrame<I> safeTransform(DataFrame<I> dataframe) {
		var originalValues = dataframe.getColumn(this.columnName).getValues();

		var laggedValues = new ArrayList<Double>();

		for (int i = 0; i < originalValues.size(); i++) {
			if (i - this.lag < 0) {
				laggedValues.add(Double.NaN);
			} else {
				laggedValues.add(originalValues.get(i - this.lag));
			}
		}

		dataframe.setColumn(this.newFeatureName, new Series<>(dataframe.getIndex(), laggedValues));
		return dataframe;
	}
}
