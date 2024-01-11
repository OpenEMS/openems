//package io.openems.edge.predictor.lstm.muliithread;
//
//import java.time.OffsetDateTime;
//import java.util.ArrayList;
//
//import io.openems.edge.predictor.lstm.common.HyperParameters;
//import io.openems.edge.predictor.lstm.train.MakeModel;
//import io.openems.edge.predictor.lstm.validator.Validation;
//
//class MulThrSeasonalityTrain1 implements Runnable {
//
//	private ArrayList<Double> seasonalityData;
//	private ArrayList<OffsetDateTime> seasonalityDate;
//	private HyperParameters hyp;
//
//	public MulThrSeasonalityTrain1(ArrayList<Double> data, ArrayList<OffsetDateTime> date,
//			HyperParameters hyperParameters) {
//
//		this.seasonalityData = data;
//		this.seasonalityDate = date;
//		this.hyp = hyperParameters;
//
//	}
//
//	@Override
//	public void run() {
//		MakeModel obj = new MakeModel();
//		obj.trainSeasonality(this.seasonalityData, this.seasonalityDate, this.hyp);
//
//		// TODO Auto-generated method stub
//
//	}
//}
//
//class MulThrTrendTrain1 implements Runnable {
//
//	private ArrayList<Double> trendData;
//	private ArrayList<OffsetDateTime> trendDate;
//	private HyperParameters hyp;
//
//	public MulThrTrendTrain1(ArrayList<Double> data, ArrayList<OffsetDateTime> date, HyperParameters hyperParameters) {
//		this.trendData = data;
//		this.trendDate = date;
//		this.hyp = hyperParameters;
//
//	}
//
//	@Override
//	public void run() {
//		MakeModel obj = new MakeModel();
//		obj.trainTrend(this.trendData, this.trendDate, this.hyp);
//
//	}
//
//}
//
//class MulThrSeasonalityValidate1 implements Runnable {
//
//	private ArrayList<Double> seasonalityData;
//	private ArrayList<OffsetDateTime> seasonalityDate;
//	private HyperParameters hyp;
//
//	public MulThrSeasonalityValidate1(ArrayList<Double> data, ArrayList<OffsetDateTime> date,
//			HyperParameters hyperParameters) {
//
//		this.seasonalityData = data;
//		this.seasonalityDate = date;
//		this.hyp = hyperParameters;
//
//	}
//
//	@Override
//	public void run() {
//		Validation obj = new Validation();
//		obj.validateSeasonality(this.seasonalityData, this.seasonalityDate, this.hyp);
//
//		// TODO Auto-generated method stub
//
//	}
//
//	class MulThrTrendValidate implements Runnable {
//
//		private ArrayList<Double> trendData;
//		private ArrayList<OffsetDateTime> trendDate;
//		private HyperParameters hyp;
//
//		public MulThrTrendValidate(ArrayList<Double> data, ArrayList<OffsetDateTime> date,
//				HyperParameters hyperParameters) {
//			this.trendData = data;
//			this.trendDate = date;
//			this.hyp = hyperParameters;
//
//		}
//
//		@Override
//		public void run() {
//			Validation obj = new Validation();
//			obj.validateTrend(this.trendData, this.trendDate, this.hyp);
//			// TODO Auto-generated method stub
//
//		}
//
//	}
//}
