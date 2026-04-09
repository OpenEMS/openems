package io.openems.edge.ess.api;

import static io.openems.common.utils.FunctionUtils.doNothing;

import java.util.ArrayList;
import java.util.List;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.sum.GridMode;

/**
 * Helper class to find the effective Grid-Mode of multiple Ess.
 */
public class CalculateGridMode {

	private final List<GridMode> values = new ArrayList<>();

	/**
	 * Adds a Channel-Value.
	 *
	 * @param channel the {@link Channel}
	 */
	public void addValue(Channel<GridMode> channel) {
		GridMode gridMode = channel.getNextValue().asEnum();
		this.values.add(gridMode);
	}

	/**
	 * Finds the effective Grid-Mode.
	 *
	 * @return the {@link GridMode}
	 */
	public GridMode calculate() {
		return aggregateGridModes(this.values);
	}

	/**
	 * Aggregates {@link GridMode GridModes} to one {@link GridMode}.
	 * 
	 * @param gridModes the GridModes
	 * @return one {@link GridMode}
	 */
	public static GridMode aggregateGridModes(List<GridMode> gridModes) {
		if (gridModes.isEmpty()) {
			return GridMode.UNDEFINED;
		}

		var onGrids = 0;
		var offGrids = 0;
		var offGridGenset = 0;
		for (GridMode gridMode : gridModes) {
			switch (gridMode) {
			case OFF_GRID -> offGrids++;
			case ON_GRID -> onGrids++;
			case OFF_GRID_GENSET -> offGridGenset++;
			case UNDEFINED -> doNothing();
			}
		}

		var result = GridMode.UNDEFINED;

		if (gridModes.size() == onGrids) {
			result = GridMode.ON_GRID;
		}
		if (gridModes.size() == offGrids) {
			result = GridMode.OFF_GRID;
		}
		if (offGridGenset > 0 && onGrids == 0) {
			result = GridMode.OFF_GRID_GENSET;
		}

		return result;
	}
}
