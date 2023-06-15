package io.openems.edge.predictor.lstmmodel.predictor;
//import java.util.ArrayList;
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
//import java.io.IOException;
//
//
//public class ReadModels {
//	
//	public static final String FILENAME = "\\testResults\\model.txt";
//	ArrayList<ArrayList<Double>> data = new ArrayList<ArrayList<Double>>();
//	
//	public ReadModels() {
//		try {
//			String filename = "\\testResults\\model.txt";
//			String path = new File(".").getCanonicalPath() + filename;
//			String filePath = path;
//	         data = parseFile(filePath);
//		}
//
//
//	 catch (IOException e) {
//		e.printStackTrace();
//	}
//		
//		
//		
//		
//
//       
//    }
//
//		
//	
//	
//	public static ArrayList<ArrayList<Double>> parseFile(String filePath) {
//		
//        ArrayList<ArrayList<Double>> result = new ArrayList<>();
//
//        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
//            String line;
//            
//            while ((line = reader.readLine()) != null) {
//                String[] numbers = line.split(" ");
//                ArrayList<Double> row = new ArrayList<>();
//                for (String number : numbers) {
//                    double value = Double.parseDouble(number);
//                    row.add(value);
//                }
//                result.add(row);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return result;
//    }
//
//   
//}

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class ReadModels {
	ArrayList<ArrayList<ArrayList<Double>>> dataList = new ArrayList<ArrayList<ArrayList<Double>>> ();
 

	public ReadModels() {
    	
    	
    	try {
			String filename = "\\testResults\\model.txt";
			String path = new File(".").getCanonicalPath() + filename;
			String filePath = path;
//        String filename = "data.txt"; // Replace with your file path
      dataList = readDataFile(filePath);
        
//        for (ArrayList<ArrayList<Double>> outerList : dataList) {
//            for (ArrayList<Double> innerList : outerList) {
//                for (Double value : innerList) {
//                    System.out.print(value + " ");
//                }
//                System.out.println();
//            }
//            System.out.println();
//    	}
    	
    	
    	}catch (IOException e) {
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
                    innerList=new ArrayList();
                }
            }

//            if (!innerList.isEmpty()) {
//                outerList.add(innerList);
//            }
//
//            if (!outerList.isEmpty()) {
//                dataList.add(outerList);
//            }

            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return dataList;
    }
}

