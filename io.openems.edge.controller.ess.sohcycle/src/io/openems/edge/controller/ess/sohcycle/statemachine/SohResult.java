package io.openems.edge.controller.ess.sohcycle.statemachine;

/**
 * Result of a SoH measurement cycle.
 *
 * @param soh    the calculated State of Health (SoH) in percentage (0-100)
 * @param sohRaw the raw SoH value before any adjustments. It may exceed 100%.
 */
public record SohResult(int soh, float sohRaw) {
}
