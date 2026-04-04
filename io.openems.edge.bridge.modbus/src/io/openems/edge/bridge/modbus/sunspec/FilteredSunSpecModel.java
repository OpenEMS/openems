package io.openems.edge.bridge.modbus.sunspec;

import java.util.Arrays;

/**
 * Represents a Filtered SunSpec Model.
 */
public class FilteredSunSpecModel implements SunSpecModel {

	private final SunSpecModel delegate;
	private final SunSpecPoint[] points;

	public FilteredSunSpecModel(SunSpecModel delegate, SunSpecPoint[] points) {
		this.delegate = delegate;
		this.points = points;
	}

	@Override
	public String name() {
		return this.delegate.name();
	}

	@Override
	public String label() {
		return this.delegate.label();
	}

	@Override
	public SunSpecPoint[] points() {
		return this.points;
	}

	/**
	 * Creates a {@link FilteredSunSpecModel} that delegates to the given
	 * {@link SunSpecModel} but excludes the specified {@link SunSpecPoint}s
	 * from the returned {@link SunSpecModel#points()}.
	 *
	 * <p>
	 * This can be used if a device implements a standard SunSpec model but
	 * does not support some points (e.g. vendor-specific event registers).
	 * The returned model behaves like the original model except that the
	 * excluded points are not exposed and therefore not mapped to Modbus
	 * registers.
	 *
	 * @param delegate the original {@link SunSpecModel}
	 * @param excluded the {@link SunSpecPoint}s that should be removed
	 * @return a {@link FilteredSunSpecModel} without the specified points
	 */
	public static FilteredSunSpecModel withoutPoints(SunSpecModel delegate, SunSpecPoint... excluded) {
		var excludedSet = Arrays.asList(excluded);
		var filtered = Arrays.stream(delegate.points())
				.filter(p -> !excludedSet.contains(p))
				.toArray(SunSpecPoint[]::new);
		return new FilteredSunSpecModel(delegate, filtered);
	}
}