package io.openems.edge.predictor.lstmmodel.preprocessing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Shuffle {

	private double[][] data;
	private double[] target;

	public Shuffle(double[][] data, double[] target) {
		this.data = this.copy2DArray(data);
		this.target = Arrays.copyOf(target, target.length);
		this.shuffleIt();
	}

	/**
	 * Shuffles the data and target arrays to randomize the order of elements. This
	 * method shuffles the data and target arrays simultaneously, ensuring that the
	 * corresponding data and target values remain aligned.
	 */
	public void shuffleIt() {
		List<Integer> indices = IntStream.range(0, this.data.length)//
				.boxed()//
				.collect(Collectors.toList());

		Collections.shuffle(indices, new Random(100));

		CompletableFuture<Void> dataFuture = CompletableFuture
				.runAsync(() -> this.shuffleData(new ArrayList<>(indices)));
		CompletableFuture<Void> targetFuture = CompletableFuture
				.runAsync(() -> this.shuffleTarget(new ArrayList<>(indices)));

		CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(dataFuture, targetFuture);
		combinedFuture.join();
	}

	private void shuffleData(List<Integer> indices) {
		this.data = indices.stream()//
				.map(i -> Arrays.copyOf(this.data[i], this.data[i].length))//
				.toArray(double[][]::new);
	}

	private void shuffleTarget(List<Integer> indices) {
		this.target = indices.stream()//
				.mapToDouble(i -> this.target[i])//
				.toArray();
	}

	public double[] getTarget() {
		return this.target;
	}

	public double[][] getData() {
		return this.data;
	}

	private double[][] copy2DArray(double[][] array) {
		return Arrays.stream(array)//
				.map(row -> Arrays.copyOf(row, row.length))//
				.toArray(double[][]::new);
	}
}
