package io.openems.edge.predictor.lstmmodel.model;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;

public class SaveModelTest {

	@Test
	public void test() {
		
		double [] x1 = {-4.159999999999725, -2.0787424932497, -1.5346969548349207, -1.2840575119809934, -1.1360025610056006, -1.0289980048031109, -0.9366542352134836};		
		double [] x2 = {28.89400000001233, 21.023630406147834, 17.43182752032293, 15.298624767948953, 13.93064189506067, 12.842830275791867, 11.812384684133779};
		double [] x3 = {3.4999999999997247, 2.9717034841926786, 2.3896281523150855, 2.0152936079411243, 1.7861969786567873, 1.6310379075525092, 1.5161190943982106};
		double [] x4 = {-1.0819999999999932, -0.3912183966214912, -0.22666584340198584, -0.15570830933433455, -0.11442651855777597, -0.09179929497718438, -0.07699970641798926};
		double [] x5 = {27.842000000011044, 20.053270982762385, 16.507747294331992, 14.439096139334179, 13.1627335479337, 12.15370727288556, 11.137676592931223};
		double [] x6 = {-5.384000000000134, -2.921804530033897, -2.353768001796486, -2.039832431465908, -1.804974580538017, -1.6329419650294479, -1.509180813685398};
		double [] x7 = {0.175297602143143, 0.26784409938186843, 0.271030711399907, 0.2691831782626957, 0.27055778210119746, 0.2712667329383346, 0.2703567571549838};
		double [] x8 = {1.0025012116388066E-5};
		
		ArrayList<Double>  x11 = UtilityConversion.convertDoubleArrayToArrayListDouble(x1);
		ArrayList<Double>  x12 = UtilityConversion.convertDoubleArrayToArrayListDouble(x2);
		ArrayList<Double>  x13 = UtilityConversion.convertDoubleArrayToArrayListDouble(x3);
		ArrayList<Double>  x14 = UtilityConversion.convertDoubleArrayToArrayListDouble(x4);
		ArrayList<Double>  x15 = UtilityConversion.convertDoubleArrayToArrayListDouble(x5);
		ArrayList<Double>  x16 = UtilityConversion.convertDoubleArrayToArrayListDouble(x6);
		ArrayList<Double>  x17 = UtilityConversion.convertDoubleArrayToArrayListDouble(x7);
		ArrayList<Double>  x18 = UtilityConversion.convertDoubleArrayToArrayListDouble(x8);
		
		ArrayList<ArrayList<Double>> finalWeight  = new ArrayList<ArrayList<Double>>();
		finalWeight.add(x11);
		finalWeight.add(x12);
		finalWeight.add(x13);
		finalWeight.add(x14);
		finalWeight.add(x15);
		finalWeight.add(x16);
		finalWeight.add(x17);
		finalWeight.add(x18);
		
		System.out.println(finalWeight);
		
		SaveModel sM = new SaveModel();
		
		//sM.saveModel(finalWeight, "modelFromTest.txt");
		

		
	}

}
