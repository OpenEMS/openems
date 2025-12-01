package io.openems.edge.predictor.api.mlcore.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.util.List;

import org.junit.Test;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;

public class DropColumnsTransformerTest {

	@Test
	public void testTransformDataFrame_ShouldDropSpecifiedColumns() {
		var dataframe = new DataFrame<>(//
				List.of(0), //
				List.of("column1", "column2"), //
				List.of(List.of(1.0, 2.0)));

		var transformer = new DropColumnsTransformer<Integer>(List.of("column1"));
		var transformed = transformer.transform(dataframe);

		assertEquals(1, transformed.columnCount());
		assertNotNull(transformed.getColumn("column2"));
		assertThrows(//
				IllegalArgumentException.class, //
				() -> transformed.getColumn("column1"));
	}
}
