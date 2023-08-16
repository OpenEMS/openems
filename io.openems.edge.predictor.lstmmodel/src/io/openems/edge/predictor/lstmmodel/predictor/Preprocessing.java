package io.openems.edge.predictor.lstmmodel.predictor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;

public class Preprocessing {
	ArrayList<Double>data = new ArrayList<Double>();
	public ArrayList<Double> scaledData= new ArrayList<Double>();

	
	public Preprocessing(ArrayList<Double>data1) {
		data=data1;
		
		
	}
	public void scale(double min,double max) {
		
		double minScaled = 0.2;
		double maxScaled = 0.8;

		for (int i = 0; i < this.data.size(); i++) {
			double temp = ((this.data.get(i) - min) / (max-min)) * (maxScaled - minScaled);
			this.scaledData.add(minScaled + temp);
		}
		
		
}
}
