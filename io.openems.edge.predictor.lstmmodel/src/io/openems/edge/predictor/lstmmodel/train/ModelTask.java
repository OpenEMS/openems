//package io.openems.edge.predictor.lstm.train;
//
//import java.time.OffsetDateTime;
//import java.util.ArrayList;
//
//import io.openems.edge.predictor.lstm.common.HyperParameters;
//
//abstract class ModelTask implements Runnable {
//	protected ArrayList<Double> data;
//	protected ArrayList<OffsetDateTime> date;
//	protected HyperParameters hyperParam;
//	protected MakeModel makeModel;
//
//	public ModelTask(ArrayList<Double> data, ArrayList<OffsetDateTime> date, HyperParameters hyperParameters) {
//		this.data = data;
//		this.date = date;
//		this.hyperParam = hyperParameters;
//		this.makeModel = new MakeModel(this.hyperParam);
//	}
//
//	protected abstract void performTask();
//
//	@Override
//	public void run() {
//		this.performTask();
//	}
//}
