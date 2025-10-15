package io.openems.edge.predictor.profileclusteringmodel.services;

import static io.openems.edge.predictor.profileclusteringmodel.ColumnNames.DAY_OF_WEEK;
import static io.openems.edge.predictor.profileclusteringmodel.ColumnNames.IS_WORKING_DAY;
import static io.openems.edge.predictor.profileclusteringmodel.ColumnNames.LABEL;
import static io.openems.edge.predictor.profileclusteringmodel.ColumnNames.LABEL_LAG_1_DAY;
import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.common.meta.types.SubdivisionCode;
import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.transformer.OneHotEncoder;

public class FeatureEngineeringServiceTest {

	private DataFrame<LocalDate> dataframe;

	@Before
	public void setUp() {
		var index = List.of(//
				LocalDate.of(2025, 7, 7), // Monday
				LocalDate.of(2025, 7, 8), // Tuesday
				LocalDate.of(2025, 7, 9), // Wednesday
				LocalDate.of(2025, 7, 11), // Friday
				LocalDate.of(2025, 7, 12), // Saturday
				LocalDate.of(2025, 7, 13) // Sunday
		);
		var columnNames = List.of(LABEL);
		var values = List.of(//
				List.of(1.0), // Cluster label on Monday
				List.of(0.0), // Cluster label on Tuesday
				List.of(3.0), // Cluster label on Wednesday
				List.of(1.0), // Cluster label on Friday
				List.of(2.0), // Cluster label on Saturday
				List.of(3.0) // Cluster label on Sunday
		);

		this.dataframe = new DataFrame<>(index, columnNames, values);
	}

	@Test
	public void testTransformForTraining_ShouldProduceExpectedFeatureMatrix() {
		var service = new FeatureEngineeringService(() -> SubdivisionCode.DE_BY);

		var result = service.transformForTraining(this.dataframe);
		var featureMatrix = result.featureLabelMatrix();

		// Assert basic shape
		// There should be 4 rows left due to missing lag values and lag validation
		assertEquals(4, featureMatrix.rowCount());

		var expectedRowIndex = List.of(//
				LocalDate.of(2025, 7, 8), //
				LocalDate.of(2025, 7, 9), //
				LocalDate.of(2025, 7, 12), //
				LocalDate.of(2025, 7, 13));
		assertEquals(expectedRowIndex, featureMatrix.getIndex());

		var expectedColumnNames = List.of(//
				LABEL, //
				IS_WORKING_DAY, //
				DAY_OF_WEEK + "_2.0", // Tuesday
				DAY_OF_WEEK + "_3.0", // Wednesday
				DAY_OF_WEEK + "_6.0", // Saturday
				DAY_OF_WEEK + "_7.0", // Sunday
				LABEL_LAG_1_DAY + "_1.0", //
				LABEL_LAG_1_DAY + "_0.0", //
				LABEL_LAG_1_DAY + "_2.0");
		assertEquals(expectedColumnNames, featureMatrix.getColumnNames());

		// Assert encoded values
		assertEquals(List.of(0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0),
				featureMatrix.getRow(LocalDate.of(2025, 7, 8)));
		assertEquals(List.of(3.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0),
				featureMatrix.getRow(LocalDate.of(2025, 7, 9)));
		assertEquals(List.of(2.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0),
				featureMatrix.getRow(LocalDate.of(2025, 7, 12)));
		assertEquals(List.of(3.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0),
				featureMatrix.getRow(LocalDate.of(2025, 7, 13)));
	}

	@Test
	public void testTransformBaseFeatureMatrixForPrediction_ShouldProduceExpectedFeatureMatrix() {
		var service = new FeatureEngineeringService(() -> SubdivisionCode.DE_BY);

		var featureMatrix = service.transformBaseFeatureMatrixForPrediction(this.dataframe);

		// Assert basic shape
		assertEquals(6, featureMatrix.rowCount());

		assertEquals(this.dataframe.getIndex(), featureMatrix.getIndex());

		var expectedColumnNames = List.of(//
				LABEL, //
				DAY_OF_WEEK, //
				IS_WORKING_DAY, //
				LABEL_LAG_1_DAY);
		assertEquals(expectedColumnNames, featureMatrix.getColumnNames());

		// Assert values
		var expectedLabelsColumn = List.of(1.0, 0.0, 3.0, 1.0, 2.0, 3.0);
		assertEquals(expectedLabelsColumn, featureMatrix.getColumn(LABEL).getValues());

		var expectedWeekdayColumn = List.of(1.0, 2.0, 3.0, 5.0, 6.0, 7.0);
		assertEquals(expectedWeekdayColumn, featureMatrix.getColumn(DAY_OF_WEEK).getValues());

		var expectedIsWorkingDayColumn = List.of(1.0, 1.0, 1.0, 1.0, 0.0, 0.0);
		assertEquals(expectedIsWorkingDayColumn, featureMatrix.getColumn(IS_WORKING_DAY).getValues());

		var expectedLag1DayColumn = List.of(Double.NaN, 1.0, 0.0, Double.NaN, 1.0, 2.0);
		assertEquals(expectedLag1DayColumn, featureMatrix.getColumn(LABEL_LAG_1_DAY).getValues());
	}

	@Test
	public void testTransformForPrediction_ShouldProduceExpectedFeatureMatrix() {
		var service = new FeatureEngineeringService(() -> SubdivisionCode.DE_BY);

		var oneHotEncoder = new OneHotEncoder<LocalDate>(//
				List.of(DAY_OF_WEEK, LABEL_LAG_1_DAY), //
				true);
		var baseFeatureMatrix = service.transformBaseFeatureMatrixForPrediction(this.dataframe);
		var featureMatrix = service.transformForPrediction(baseFeatureMatrix, oneHotEncoder);

		// Assert basic shape
		// There should be 4 rows left due to missing lag values and lag validation
		assertEquals(4, featureMatrix.rowCount());

		var expectedRowIndex = List.of(//
				LocalDate.of(2025, 7, 8), //
				LocalDate.of(2025, 7, 9), //
				LocalDate.of(2025, 7, 12), //
				LocalDate.of(2025, 7, 13));
		assertEquals(expectedRowIndex, featureMatrix.getIndex());

		var expectedColumnNames = List.of(//
				IS_WORKING_DAY, //
				DAY_OF_WEEK + "_2.0", // Tuesday
				DAY_OF_WEEK + "_3.0", // Wednesday
				DAY_OF_WEEK + "_6.0", // Saturday
				DAY_OF_WEEK + "_7.0", // Sunday
				LABEL_LAG_1_DAY + "_1.0", //
				LABEL_LAG_1_DAY + "_0.0", //
				LABEL_LAG_1_DAY + "_2.0");
		assertEquals(expectedColumnNames, featureMatrix.getColumnNames());

		// Assert encoded values
		assertEquals(List.of(1.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0), featureMatrix.getRow(LocalDate.of(2025, 7, 8)));
		assertEquals(List.of(1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0), featureMatrix.getRow(LocalDate.of(2025, 7, 9)));
		assertEquals(List.of(0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0), featureMatrix.getRow(LocalDate.of(2025, 7, 12)));
		assertEquals(List.of(0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0), featureMatrix.getRow(LocalDate.of(2025, 7, 13)));
	}
}
