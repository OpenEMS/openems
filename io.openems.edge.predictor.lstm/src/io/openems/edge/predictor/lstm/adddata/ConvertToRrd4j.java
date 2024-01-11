//package io.openems.edge.predictor.lstm.adddata;
//
//
//import java.time.OffsetDateTime;
//import java.util.ArrayList;
//
//import io.openems.edge.predictor.lstm.common.ReadCsv;
//
//public class ConvertToRrd4j {
//	private ArrayList<Double> data;
//	private ArrayList<OffsetDateTime> date;
//	private String path = "";
//
//	public ConvertToRrd4j() {
//
//		this.readCsv(null);
//
//	}
//
//	public void readCsv(String path) {
//		ReadCsv csv = new ReadCsv(path);
//		this.data = csv.getData();
//		this.date = csv.getDates();
//	}
//
//	public void setPath(String val) {
//		this.path = val;
//	}
//	
//}
