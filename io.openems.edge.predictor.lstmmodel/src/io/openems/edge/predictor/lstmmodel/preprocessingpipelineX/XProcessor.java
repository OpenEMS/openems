package io.openems.edge.predictor.lstmmodel.preprocessingpipelineX;

import java.util.ArrayList;
import java.util.List;

public class XProcessor {

	private List<Operation> operations;

	private XProcessor(Builder builder) {
		this.operations = builder.operations;
	}

	public List<Double> process(List<Double> data) {
		for (Operation operation : operations) {
			data = operation.operate(data);
		}
		return data;
	}

	public static class Builder {
		private List<Operation> operations;

		public Builder() {
			this.operations = new ArrayList<>();
		}

		public Builder addOperation(Operation operation) {
			this.operations.add(operation);
			return this;
		}

		public XProcessor build() {
			return new XProcessor(this);
		}
	}

}
