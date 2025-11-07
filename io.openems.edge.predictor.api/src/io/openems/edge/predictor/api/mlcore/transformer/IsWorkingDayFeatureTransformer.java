package io.openems.edge.predictor.api.mlcore.transformer;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.function.Function;

import io.openems.edge.common.meta.types.SubdivisionCode;
import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;

/**
 * Currently this implementation only marks weekends as non-working days. Public
 * holidays are not yet taken into account and will always be treated as working
 * days. The actual logic for considering public holidays still needs to be
 * implemented.
 */
public class IsWorkingDayFeatureTransformer<I> extends AbstractDataFrameTransformer<I> {

	private final String columnName;
	@SuppressWarnings("unused")
	private final SubdivisionCode subdivisionCode;
	private final Function<I, LocalDate> localDateExtractor;

	public IsWorkingDayFeatureTransformer(//
			String columnName, //
			SubdivisionCode subdivisionCode, //
			Function<I, LocalDate> localDateExtractor) {
		this.columnName = columnName;
		this.subdivisionCode = subdivisionCode;
		this.localDateExtractor = localDateExtractor;
	}

	@Override
	protected DataFrame<I> safeTransform(DataFrame<I> dataframe) {
		var values = dataframe.getIndex().stream()//
				.map(i -> { //
					var date = this.localDateExtractor.apply(i);
					var dow = date.getDayOfWeek();
					boolean isNonWorkingDay //
							= dow == DayOfWeek.SATURDAY //
									|| dow == DayOfWeek.SUNDAY;
					return !isNonWorkingDay ? 1.0 : 0.0;
				})//
				.toList();

		dataframe.setColumn(this.columnName, new Series<>(dataframe.getIndex(), values));
		return dataframe;
	}
}
