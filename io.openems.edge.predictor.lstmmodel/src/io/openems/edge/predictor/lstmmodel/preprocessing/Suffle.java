package io.openems.edge.predictor.lstmmodel.preprocessing;

import java.util.ArrayList;
import java.util.Collections;

public class Suffle {
	public double[][] data;
	 public double[] target;
	public Suffle(double data1[][], double[] target1) {
		
		data = new double [data1.length][data1[0].length];
		target = new double [target1.length];
		
		for(int i=0; i< data1.length; i++) {
			for (int j=0; j<data1[0].length;j++) {
				
				data[i][j] = data1[i][j];
				//System.out.print(data[i][j]);
				target[i]=target1[i];
			}
		
		}
		suffelIt();
		
		
	}
	public void suffelIt() {
		ArrayList<Integer> temp = new ArrayList<Integer>();
		double[][]suffledData = new double [data.length][data[0].length];
		
		double[] suffledTarget = new double[this.data.length];
		for (int i = 0; i < this.data.length; i++) {
			temp.add(i);
		}
		Collections.shuffle(temp);
		for (int i = 0; i < this.data.length; i++) {
			suffledData[i]=(this.data[temp.get(i)]);

			suffledTarget[i]=this.target[temp.get(i)];

		}
		this.data = suffledData;
		this.target = suffledTarget;

	}
	
	

}
