package io.openems.edge.goodwe.charger.twostring;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.goodwe.charger.GoodWeCharger;

public interface GoodWeChargerTwoString extends OpenemsComponent, EssDcCharger, GoodWeCharger {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Used PV port of the GoodWe inverter.
	 * 
	 * @return Used PV port
	 */
	public PvPort pvPort();

	/**
	 * Calculate a value by rule of three.
	 * 
	 * <p>
	 * Solves proportions and calculate the unknown value.
	 * 
	 * <p>
	 * Assure that the unit of the divisor and relatedValue are the same.
	 * 
	 * @param total   total optional of the required unit
	 * @param divisor divisor of the known unit
	 * @param related related optional with the known unit
	 * @return the calculated result. Return null for empty parameters or zero
	 *         divisor
	 */
	public static Optional<Integer> calculateByRuleOfThree(Optional<Integer> total, Optional<Integer> divisor,
			Optional<Integer> related) {

		var result = new AtomicReference<Integer>(null);
		total.ifPresent(totalValue -> {
			divisor.ifPresent(divisorValue -> {
				related.ifPresent(relatedValue -> {
					if (divisorValue == 0) {
						return;
					}

					/*
					 * As the total power of the charger is sometimes less than the power of an
					 * individual string, the minimum is taken.
					 * 
					 * TODO: Remove it if it has been fixed by GoodWe
					 */
					result.set(Math.round((totalValue * relatedValue) / divisorValue.floatValue()));
				});
			});
		});
		return Optional.ofNullable(result.get());
	}
}