package io.openems.edge.predictor.api;

import java.time.LocalDateTime;

import java.util.TreeMap;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface Predictor extends OpenemsComponent{

	/**
	 * The interface is a simple method, which return a Tree map of LocalDateTime
	 * and the (Long) predicted value
	 *
	 * @param Start, End time
	 * @return TreeMap of LocalDateTime and Long
	 */
	public TreeMap<LocalDateTime, Long> getPrediction(LocalDateTime start, LocalDateTime end);



}
