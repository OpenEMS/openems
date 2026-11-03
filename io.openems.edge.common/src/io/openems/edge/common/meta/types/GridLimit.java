package io.openems.edge.common.meta.types;

import static java.lang.Math.max;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;

import java.time.Clock;
import java.util.Optional;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.types.Tuple2;
import io.openems.common.utils.IntUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.meta.GridFeedInLimitationType;

/**
 * Holds Grid-Limits in both directions for Grid-Buy and Grid-Sell.
 * 
 * <p>
 * The order is: Hard-Limit >= Hard-Limit With Buffer >= Soft-Limit
 */
public sealed interface GridLimit {

	// Common

	/**
	 * Gets the maximum current allowed at the Grid Connection Point (GCP) in [A].
	 *
	 * @return the rating of the fuse in [A]
	 */
	int fuseCurrent();

	// Buy

	/**
	 * Gets the hard limit for power bought from the grid in [W]; derived from the
	 * fuse rating.
	 *
	 * @return the Grid-Buy-Hard-Limit in [W]
	 */
	int gridBuyHardLimit();

	/**
	 * Gets the hard limit for power bought from the grid in [W], reduced by a
	 * buffer.
	 *
	 * @return the Grid-Buy-Hard-Limit With Buffer in [W]
	 */
	int gridBuyHardLimitWithBuffer();

	/**
	 * Gets the currently active soft limit tasks for power bought from the grid in
	 * [W].
	 *
	 * @return the Grid-Buy-Soft-Limit as {@link JSCalendar.Tasks}
	 */
	JSCalendar.Tasks<Power> gridBuySoftLimit();

	/**
	 * Gets the currently active Grid-Buy-Soft-Limit in [W]; limited by the
	 * Grid-Buy-Hard-Limit.
	 *
	 * @return the active Grid-Buy-Soft-Limit in [W]
	 */
	public default int activeGridBuySoftLimit() {
		final var hardLimit = this.gridBuyHardLimit();
		return Optional.ofNullable(this.gridBuySoftLimit().getActiveOneTask()) //
				.map(JSCalendar.Tasks.OneTask::payload) //
				.map(Power::power) //
				.map(softLimit -> Math.min(softLimit, hardLimit)) //
				.orElse(hardLimit);
	}

	/**
	 * Checks whether charging the ESS from the grid is allowed.
	 *
	 * @return true if allowed
	 */
	boolean isEssChargeFromGridAllowed();

	// Sell

	/**
	 * Gets the hard limit for power fed into the grid in [W]; derived from the fuse
	 * rating and the dynamic Grid-Sell-Limit.
	 *
	 * @return the Grid-Sell-Hard-Limit in [W]
	 */
	int gridSellHardLimit();

	/**
	 * Gets the hard limit for power fed into the grid in [W], reduced by a buffer.
	 *
	 * @return the Grid-Sell-Hard-Limit With Buffer in [W]
	 */
	int gridSellHardLimitWithBuffer();

	/**
	 * Checks whether discharging the ESS to the grid is allowed.
	 *
	 * @return true if allowed
	 */
	boolean isEssDischargeToGridAllowed();

	/**
	 * Gets the maximum power for discharging the ESS to the grid in [W]; 0 if not
	 * allowed.
	 *
	 * @return the ESS-Discharge-To-Grid limit in [W]
	 */
	int essDischargeToGridLimit();

	/** Mutable variant of {@link GridLimit}. */
	public non-sealed class Mutable implements GridLimit {

		private GridLimit.Immutable immutable = null;

		// Common
		private int fuseCurrent = 0;

		// Buy
		private JSCalendar.Tasks<Power> gridBuySoft = JSCalendar.Tasks.empty();
		private boolean isEssChargeFromGridAllowed = false;

		// Sell
		private Tuple2<GridFeedInLimitationType, Integer> gridSellLimit = new Tuple2<>(
				GridFeedInLimitationType.UNDEFINED, null);
		private boolean isEssDischargeToGridAllowed = false;

		/** Invalidates the immutable variant. Should be called on every setter. */
		protected synchronized void invalidateImmutable() {
			this.immutable = null;
		}

		public synchronized GridLimit.Mutable setFuseCurrent(int fuseCurrent) {
			if (this.fuseCurrent != fuseCurrent) {
				this.fuseCurrent = fuseCurrent;
				this.invalidateImmutable();
			}
			return this;
		}

		public synchronized GridLimit.Mutable setGridBuySoftLimit(JSCalendar.Tasks<Power> buySoft) {
			if (this.gridBuySoft != buySoft) {
				this.gridBuySoft = buySoft;
				this.invalidateImmutable();
			}
			return this;
		}

		public synchronized GridLimit.Mutable setGridSellLimit(GridFeedInLimitationType type, Integer power) {
			if (power != null && power < 0) {
				power = null;
			}
			if (this.gridSellLimit.a() != type || this.gridSellLimit.b() != power) {
				this.gridSellLimit = new Tuple2<>(type, power);
				this.invalidateImmutable();
			}
			return this;
		}

		public synchronized GridLimit.Mutable setEssDischargeToGridAllowed(boolean isEssDischargeToGridAllowed) {
			if (this.isEssDischargeToGridAllowed != isEssDischargeToGridAllowed) {
				this.isEssDischargeToGridAllowed = isEssDischargeToGridAllowed;
				this.invalidateImmutable();
			}
			return this;
		}

		public synchronized GridLimit.Mutable setEssChargeFromGridAllowed(boolean isEssChargeFromGridAllowed) {
			if (this.isEssChargeFromGridAllowed != isEssChargeFromGridAllowed) {
				this.isEssChargeFromGridAllowed = isEssChargeFromGridAllowed;
				this.invalidateImmutable();
			}
			return this;
		}

		@Override
		public synchronized int fuseCurrent() {
			return this.fuseCurrent;
		}

		@Override
		public synchronized int gridBuyHardLimit() {
			final var physicalLimitInWatt = this.getFuseWatt();
			return physicalLimitInWatt;
		}

		@Override
		public synchronized int gridBuyHardLimitWithBuffer() {
			final var gridBuyHardLimit = this.gridBuyHardLimit();
			final var buffer = max(Math.round(gridBuyHardLimit * 0.10F), 6000);
			return Math.max(0, gridBuyHardLimit - buffer);
		}

		@Override
		public synchronized JSCalendar.Tasks<Power> gridBuySoftLimit() {
			final var hardLimitWithBuffer = this.gridBuyHardLimitWithBuffer();
			final var src = this.gridBuySoft;
			final var result = JSCalendar.Tasks.<Power>create(src.clock);

			// Make sure each value is <= Grid-Buy-Hard-Limit
			src.tasks.stream() //
					.map(t -> {
						return t.payload().power() > hardLimitWithBuffer //
								? JSCalendar.Task.createFrom(t) //
										.setPayload(new Power(hardLimitWithBuffer)) //
										.build()
								: t;
					}) //
					.forEach(result::add);

			// Add fallback of Grid-Buy-Hard-Limit to make sure each timestamp has a value
			result.add(t -> t //
					.setPayload(new Power(hardLimitWithBuffer)));

			return result.build();
		}

		@Override
		public synchronized int gridSellHardLimit() {
			final var physicalLimitInWatt = this.getFuseWatt();
			// A dynamic Grid-Sell-Limit is only considered if it is actually configured
			// (DYNAMIC_LIMITATION); negative values were already normalized to null by
			// the setter.
			final var dynamicLimit = switch (this.gridSellLimit.a()) {
			case DYNAMIC_LIMITATION -> this.gridSellLimit.b();
			case NO_LIMITATION, UNDEFINED -> null;
			};
			return IntUtils.minInt(physicalLimitInWatt, dynamicLimit);
		}

		@Override
		public synchronized int gridSellHardLimitWithBuffer() {
			final var gridSellHardLimit = this.gridSellHardLimit();
			// Reduce limit by 5% with a minimum buffer of 150 W
			final var buffer = round(max(gridSellHardLimit * 0.05F, 150));
			return max(0, gridSellHardLimit - buffer);
		}

		@Override
		public synchronized boolean isEssChargeFromGridAllowed() {
			return this.isEssChargeFromGridAllowed;
		}

		@Override
		public synchronized boolean isEssDischargeToGridAllowed() {
			return this.isEssDischargeToGridAllowed;
		}

		@Override
		public synchronized int essDischargeToGridLimit() {
			if (this.isEssDischargeToGridAllowed) {
				return this.gridSellHardLimitWithBuffer();
			}
			return 0;
		}

		/**
		 * Gets the maximum power allowed at the Grid Connection Point (GCP) in [W].
		 * 
		 * <p>
		 * Converts the {@link #fuseCurrent} to power using a three-phase grid with 400
		 * V and cos(phi) = 1.
		 *
		 * @return the fuse limit in [W]
		 */
		private int getFuseWatt() {
			final var voltage = 400.0; // [V]
			final var cosPhi = 1;
			final var current = this.fuseCurrent; // [A]
			return Double.valueOf(sqrt(3) * voltage * current * cosPhi).intValue();
		}

		/**
		 * Gets the immutable variant of this object; creates and caches it if required.
		 *
		 * @return the {@link GridLimit.Immutable}
		 */
		public synchronized GridLimit.Immutable toImmutable() {
			if (this.immutable != null) {
				return this.immutable;
			}
			this.immutable = new GridLimit.Immutable(//
					// Common
					this.fuseCurrent(),
					// Buy
					this.gridBuyHardLimit(), this.gridBuyHardLimitWithBuffer(), this.gridBuySoftLimit(), //
					this.isEssChargeFromGridAllowed(), //
					// Sell
					this.gridSellHardLimit(), this.gridSellHardLimitWithBuffer(), //
					this.isEssDischargeToGridAllowed(), this.essDischargeToGridLimit() //
			);
			return this.immutable;
		}
	}

	/** Immutable variant of {@link GridLimit}. */
	public record Immutable(//
			// Common
			int fuseCurrent,
			// Buy
			int gridBuyHardLimit, int gridBuyHardLimitWithBuffer, JSCalendar.Tasks<Power> gridBuySoftLimit, //
			boolean isEssChargeFromGridAllowed, //
			// Sell
			int gridSellHardLimit, int gridSellHardLimitWithBuffer, //
			boolean isEssDischargeToGridAllowed, int essDischargeToGridLimit //
	) implements GridLimit {
	}

	public record Power(int power) {
		/**
		 * Returns a {@link JsonSerializer} for {@link Power}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Power> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(Power.class, json -> {
				return new Power(json.getInt("power"));
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("power", obj.power()).build();
			});
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link JSCalendar.Tasks} of
		 * {@link Power}.
		 *
		 * @param clock the {@link Clock}
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<JSCalendar.Tasks<Power>> tasksSerializer(Clock clock) {
			return JSCalendar.Tasks.serializer(clock, Power.serializer());
		}
	}
}
