//package io.openems.edge.predictor.lstmmodel.interpolation;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.function.Supplier;
//import java.util.stream.Collectors;
//import java.util.stream.DoubleStream;
//import java.util.stream.IntStream;
//import java.util.stream.Stream;
//
//import org.junit.Test;
//
//import io.openems.edge.predictor.lstmmodel.Data;
//import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;
//
//public class InterpolationManagerTest {
//
//	@Test
//	public void test() {
//		Integer[] data = { 15085879, 9228392, 24124509, 6571137, 17642223, null, null, 22445357, 14785938, 9713856,
//				23915723 };
//		
//
//		UtilityConversion.convertListIntegerToListDouble(data);
//
//		InterpolationManager interpolation = new InterpolationManager(data1);
//		ArrayList<Double> interpolatedData = interpolation.interpolated;
//		System.out.println("Interpolated data:" + interpolatedData);
//
//	}
//	
//	public static ArrayList<Integer> convertDoubleArrayToArrayListDouble(int[] toBeConverted) {
//
//		return DoubleStream.of(toBeConverted) //
//				.boxed() //
//				.collect(Collectors.toCollection(ArrayList::new));
//	}
//
////	public ArrayList<Double> change(Integer[] data) {
////		ArrayList<Double> toReturn = new ArrayList<Double>();
////		for (int i = 0; i < data.length; i++) {
////
////			if (data[i] == null) {
////				toReturn.add(null);
////			}
////
////			else {
////				double temp = (double) data[i];
////				toReturn.add(temp);
////			}
////		}
////		return toReturn;
////	}
//
//}
