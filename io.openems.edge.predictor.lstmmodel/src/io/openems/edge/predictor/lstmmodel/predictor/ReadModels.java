package io.openems.edge.predictor.lstmmodel.predictor;
import io.openems.edge.predictor.lstmmodel.model.SaveModel;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ReadModels {
	public ArrayList<ArrayList<ArrayList<Double>>> dataList = new ArrayList<ArrayList<ArrayList<Double>>>();
	public static ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allModel = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();

	public ReadModels() {

		try {
			String filename = "\\testResults\\model.txt";
			String path = new File(".").getCanonicalPath() + filename;
			String filePath = path;

			dataList = readDataFile(filePath);

			
			

			allModel = reshape();
			

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static ArrayList<ArrayList<ArrayList<Double>>> readDataFile(String filename) {
		ArrayList<ArrayList<ArrayList<Double>>> dataList = new ArrayList<>();

		try {
			Scanner scanner = new Scanner(new File(filename));
			ArrayList<ArrayList<Double>> outerList = new ArrayList<>();
			ArrayList<Double> innerList = new ArrayList<>();

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine().trim();

				if (line.isEmpty()) {
					if (!innerList.isEmpty()) {
						dataList.add(outerList);
						innerList = new ArrayList<>();
						outerList = new ArrayList<>();
					}

					if (!outerList.isEmpty()) {
						dataList.add(outerList);
						outerList = new ArrayList<>();
					}
				} else {
					String[] values = line.split(" ");
					for (String value : values) {
						innerList.add(Double.parseDouble(value));
					}
					outerList.add(innerList);
					innerList = new ArrayList<Double>();
				}
			}


			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return dataList;
	}

	public ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> reshape() {

		int m = 4 * 24;
		int n = dataList.size() / m;
		int o =0;
		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> temp2 = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
		for (int i = 0; i < n; i++) {
			ArrayList<ArrayList<ArrayList<Double>>> temp1 = new ArrayList<ArrayList<ArrayList<Double>>>();

			for (int j = 0; j < m; j++) {
				temp1.add(dataList.get(o));
				o=o+1;

			}
			temp2.add(temp1);

		}
		return temp2;
	}
public static void updateModel(List<List<Integer>>index) {
	ArrayList<ArrayList<ArrayList<Double>>> optimumWeight = new ArrayList<ArrayList<ArrayList<Double>>>();
	ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> finalWeight = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
	
	for(int i=0; i<index.size();i++) {
		
			ArrayList<ArrayList<Double>> temp1=allModel.get(index.get(i).get(0)).get(index.get(i).get(1));
			optimumWeight.add(temp1);
			
			
		
	
	}
	finalWeight.add(optimumWeight);
	SaveModel.saveModels(finalWeight);
	
	SaveModel.saveModels(finalWeight, "BestModels.txt");
	
}
public static void updateModel(Integer index) {
	ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> finalWeight = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
	ArrayList<ArrayList<ArrayList<Double>>> optimumWeight = new ArrayList<ArrayList<ArrayList<Double>>>();
	optimumWeight=allModel.get(index);
	finalWeight.add(optimumWeight);
	SaveModel.saveModels(finalWeight);

}
}
