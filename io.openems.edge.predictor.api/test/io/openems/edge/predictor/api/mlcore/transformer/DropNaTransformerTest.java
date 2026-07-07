package io.openems.edge.predictor.api.mlcore.transformer;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public class DropNaTransformerTest {

	@Test
	public void testTransformSeries_ShouldDropNaValues() {
		var index = List.of(1, 2, 3);
		var values = Arrays.asList(1.0, null, 3.0);
		var series = new Series<>(index, values);

		var transformer = new DropNaTransformer<Integer>();
		var result = transformer.transform(series);

		assertEquals(2, result.size());
		assertEquals(Arrays.asList(1, 3), result.getIndex());
		assertEquals(Arrays.asList(1.0, 3.0), result.getValues());
	}

	@Test
	public void testTransformDataFrame_ShouldDropNaRows() {
		var index = Arrays.asList(1, 2, 3);
		var columnNames = Arrays.asList("column1", "column2");

		var values = new ArrayList<List<Double>>();
		values.add(Arrays.asList(1.0, 2.0));
		values.add(Arrays.asList(null, 3.0));
		values.add(Arrays.asList(4.0, 5.0));

		var dataframe = new DataFrame<>(index, columnNames, values);

		var transformer = new DropNaTransformer<Integer>();
		var result = transformer.transform(dataframe);

		assertEquals(2, result.rowCount());
		assertEquals(Arrays.asList(1, 3), result.getIndex());
		assertEquals(Arrays.asList(1.0, 2.0), result.getValues().get(0));
		assertEquals(Arrays.asList(4.0, 5.0), result.getValues().get(1));
	}
}
