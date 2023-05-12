package io.openems.edge.predictor.lstmmodel.interpolation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Test;

import io.openems.edge.predictor.lstmmodel.Data;

public class CubicalInterpolationTest {

	@Test
	public void test() {
		Integer[] data=Data.data;
		ArrayList<Double>data1=new ArrayList();
		data1=change(data);
		
		
		
		System.out.println(" data:" + data1);

		interpolationManager interpolation = new interpolationManager(data1);
		ArrayList<Double> interpolatedData = interpolation.interpolated;
		System.out.println("Interpolated data:" + interpolatedData);

	}
	public ArrayList<Double> change(Integer[] data)
	{
		 ArrayList<Double> toReturn=new  ArrayList<Double> ();
		 for(int i=0;i<data.length;i++)
		 {
			 
			 if (data[i]==null) {
				 toReturn.add(null);
			 }
//			 if(Double.isNaN(data[i])) {
//				 toReturn.add(Double.NaN);
//			 }
			 else{
				 double temp=(double)data[i];
				 toReturn.add(temp);
			 }
		 }
		 return toReturn;
	}

}
