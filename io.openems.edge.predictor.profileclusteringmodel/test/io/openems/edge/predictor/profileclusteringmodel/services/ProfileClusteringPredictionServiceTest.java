package io.openems.edge.predictor.profileclusteringmodel.services;

import static io.openems.edge.predictor.profileclusteringmodel.ColumnNames.LABEL;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import io.openems.edge.predictor.api.mlcore.clustering.Clusterer;
import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;

@RunWith(MockitoJUnitRunner.class)
public class ProfileClusteringPredictionServiceTest {

	@Test
	public void testPredictClusterLabels_ShouldReturnDataFrameWithPredictedLabels() {
		var dates = List.of(//
				LocalDate.of(2025, 7, 10), //
				LocalDate.of(2025, 7, 11));

		// Create a dummy DataFrame with 96 columns and dummy data
		var columns = new ArrayList<String>();
		for (int i = 1; i <= 96; i++) {
			columns.add(String.valueOf(i));
		}

		var data = List.of(Collections.nCopies(96, 5.0), Collections.nCopies(96, 6.0));

		var timeSeriesByDate = new DataFrame<>(dates, columns, data);

		// Mocked clusterer returns fixed cluster labels
		var clusterer = mock(Clusterer.class);
		when(clusterer.predict(any())).thenReturn(List.of(2, 3));
		var service = new ProfileClusteringPredictionService(clusterer);

		var result = service.predictClusterLabels(timeSeriesByDate);

		// Verify structure
		assertEquals(dates, result.getIndex());
		assertEquals(List.of(LABEL), result.getColumnNames());

		assertEquals(2, result.rowCount());
		assertEquals(2.0, result.getValueAt(0, 0), 0.0);
		assertEquals(3.0, result.getValueAt(1, 0), 0.0);
	}
}
