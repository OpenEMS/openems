package io.openems.edge.predictor.api.mlcore.transformer;

import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;

public class LagValidationTransformer<I extends Temporal> extends AbstractDataFrameTransformer<I> {

	private final String columnName;
	private final int lag;
	private final ChronoUnit chronoUnit;

	public LagValidationTransformer(String columnName, int lag, ChronoUnit chronoUnit) {
		this.columnName = columnName;
		this.lag = lag;
		this.chronoUnit = chronoUnit;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected DataFrame<I> safeTransform(DataFrame<I> dataframe) {
		var index = dataframe.getIndex();
		int lagColIndex = dataframe.getColumnNames().indexOf(this.columnName);

		for (int i = this.lag; i < dataframe.rowCount(); i++) {
			I laggedIndex = index.get(i - this.lag);
			I expectedIndex = (I) index.get(i).minus(this.lag, this.chronoUnit);

			if (!laggedIndex.equals(expectedIndex)) {
				dataframe.setValueAt(i, lagColIndex, Double.NaN);
			}
		}

		return dataframe;
	}
}
