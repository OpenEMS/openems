package io.openems.edge.predictor.api.mlcore.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public class OneHotEncoderTest {

	private DataFrame<Integer> testDataFrame;

	@Before
	public void setUp() {
		var index = Arrays.asList(0, 1, 2);
		var continuousFeature = new Series<>(index, List.of(1.1, 2.2, 3.3));
		var categoricalFeature1 = new Series<>(index, List.of(1.0, 2.0, 1.0));
		var categoricalFeature2 = new Series<>(index, List.of(4.0, 5.0, Double.NaN));

		this.testDataFrame = DataFrame.fromSeriesMap(Map.of(//
				"continuous_feature", continuousFeature, //
				"categorical_feature1", categoricalFeature1, //
				"categorical_feature2", categoricalFeature2));
	}

	@Test
	public void testTransformDataFrame_ShouldEncodeSpecifiedColumnsCorrectly() {
		var columnsToEncode = List.of("categorical_feature1", "categorical_feature2");
		var encoder = new OneHotEncoder<Integer>(columnsToEncode, true);
		var encodedDataFrame = encoder.transform(this.testDataFrame);

		// Verify column names
		var expectedColumnNames = List.of(//
				"continuous_feature", //
				"categorical_feature1_1.0", //
				"categorical_feature1_2.0", //
				"categorical_feature2_4.0", //
				"categorical_feature2_5.0", //
				"categorical_feature2_NaN");
		assertEquals(expectedColumnNames, encodedDataFrame.getColumnNames());
		assertFalse(encodedDataFrame.getColumnNames().contains("categorical_feature1"));
		assertFalse(encodedDataFrame.getColumnNames().contains("categorical_feature2"));

		// Verify column values
		assertEquals(List.of(1.1, 2.2, 3.3), encodedDataFrame.getColumn("continuous_feature").getValues());
		assertEquals(List.of(1.0, 0.0, 1.0), encodedDataFrame.getColumn("categorical_feature1_1.0").getValues());
		assertEquals(List.of(0.0, 1.0, 0.0), encodedDataFrame.getColumn("categorical_feature1_2.0").getValues());
		assertEquals(List.of(1.0, 0.0, 0.0), encodedDataFrame.getColumn("categorical_feature2_4.0").getValues());
		assertEquals(List.of(0.0, 1.0, 0.0), encodedDataFrame.getColumn("categorical_feature2_5.0").getValues());
		assertEquals(List.of(0.0, 0.0, 1.0), encodedDataFrame.getColumn("categorical_feature2_NaN").getValues());
	}

	@Test
	public void testTransformDataFrame_ShouldThrow_WhenNotFitted() {
		var encoder = new OneHotEncoder<Integer>(//
				List.of("categorical_feature1", "categorical_feature2"), //
				false);
		assertThrows(//
				IllegalStateException.class, //
				() -> encoder.transform(this.testDataFrame));
	}

	@Test
	public void testTransformDataFrame_ShouldThrow_WhenColumnNotFound() {
		var encoder = new OneHotEncoder<Integer>(//
				List.of("non_existent_column"), //
				true);
		assertThrows(//
				IllegalArgumentException.class, //
				() -> encoder.transform(this.testDataFrame));
	}
}
