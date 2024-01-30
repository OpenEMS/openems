import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.junit.Test;

import io.openems.edge.predictor.lstm.common.DataModification;
import io.openems.edge.predictor.lstm.common.ReadCsv;

public class BatchDataAndDateTest {
	/**
	 * Generates batched data from a CSV file and prints the size of each batch.
	 * It reads data from a CSV file, processes it in batches using DataModification.getDataInBatch,
	 * and prints the size of each batch to the console.
	 * 
	 * <p>
	 * The method performs the following steps:
	 * 1. Reads data from a CSV file specified by the filename "1.csv" using ReadCsv.
	 * 2. Processes the data in batches using DataModification.getDataInBatch with a batch size of 9.
	 * 3. Prints the size of each batch to the console.
	 * </p>
	 * 
	 * <p>
	 * Note: Ensure that the file "1.csv" exists and contains valid data.
	 * </p>
	 */
	
	public static void batchDataDate() {
		ReadCsv obj1 = new ReadCsv("1.csv");
		ArrayList<ArrayList<Double>> batchDate = DataModification.getDataInBatch(obj1.getData(), 9);

		for (int i = 0; i < batchDate.size(); i++) {

			System.out.println(batchDate.get(i).size());
		}

	}

	@Test
	public void test() {
		BatchDataAndDateTest.batchDataDate();
		fail("Not yet implemented");
	}

}
