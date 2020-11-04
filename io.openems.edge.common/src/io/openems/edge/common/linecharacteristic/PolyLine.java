package io.openems.edge.common.linecharacteristic;

import java.util.TreeMap;

import com.google.gson.JsonArray;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public interface PolyLine {

	public TreeMap<Float, Float> parseLine(JsonArray lineConfig) throws OpenemsNamedException;
	public Float getLineValue(JsonArray lineConfig, float referencePoint) throws OpenemsNamedException;
}
