package io.openems.edge.predictor.profileclusteringmodel.services;

import static io.openems.edge.predictor.profileclusteringmodel.ColumnNames.LABEL;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
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
import io.openems.edge.predictor.profileclusteringmodel.PredictorConfig.ClustererFitter;

@RunWith(MockitoJUnitRunner.class)
public class ProfileClusteringTrainingServiceTest {

	@Test
	public void testClusterTimeSeries_ShouldReturnDataFrameWithClusterLabels() {
		var dates = List.of(//
				LocalDate.of(2025, 7, 10), //
				LocalDate.of(2025, 7, 11), //
				LocalDate.of(2025, 7, 12));

		// Create a dummy DataFrame with 96 columns and dummy data
		var columnNames = new ArrayList<String>();
		for (int i = 1; i <= 96; i++) {
			columnNames.add(String.valueOf(i));
		}
		var data = List.of(//
				Collections.nCopies(96, 1.0), //
				Collections.nCopies(96, 2.0), //
				Collections.nCopies(96, 3.0));

		var timeSeriesByDate = new DataFrame<>(dates, columnNames, data);

		// Mocked clusterer returns fixed cluster labels
		var clustererFitter = mock(ClustererFitter.class);
		var clusterer = mock(Clusterer.class);

		when(clustererFitter.fit(eq(timeSeriesByDate))).thenReturn(clusterer);
		when(clusterer.predict(eq(timeSeriesByDate))).thenReturn(List.of(0, 1, 0));

		var service = new ProfileClusteringTrainingService(clustererFitter);
		var clusteringResult = service.clusterTimeSeries(timeSeriesByDate);

		// Verify that trained clusterer is returned
		assertEquals(clusterer, clusteringResult.clusterer());

		// Verify structure
		assertEquals(dates, clusteringResult.rawClusterLabelMatrix().getIndex());
		assertEquals(List.of(LABEL), clusteringResult.rawClusterLabelMatrix().getColumnNames());

		// Verify cluster labels converted to double
		assertEquals(3, clusteringResult.rawClusterLabelMatrix().rowCount());
		assertEquals(0.0, clusteringResult.rawClusterLabelMatrix().getValueAt(0, 0), 0.0);
		assertEquals(1.0, clusteringResult.rawClusterLabelMatrix().getValueAt(1, 0), 0.0);
		assertEquals(0.0, clusteringResult.rawClusterLabelMatrix().getValueAt(2, 0), 0.0);
	}
}
