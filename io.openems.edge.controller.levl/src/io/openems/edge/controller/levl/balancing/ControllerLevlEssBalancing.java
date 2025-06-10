package io.openems.edge.controller.levl.balancing;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.Unit.PERCENT;
import static io.openems.common.channel.Unit.WATT;
import static io.openems.common.channel.Unit.WATT_HOURS;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.DOUBLE;
import static io.openems.common.types.OpenemsType.LONG;
import static io.openems.common.types.OpenemsType.STRING;

import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerLevlEssBalancing extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		REMAINING_LEVL_ENERGY(Doc.of(LONG) //
				.persistencePriority(HIGH) //
				.text("Energy to be realized [Ws]")), //
		LEVL_STATE_OF_CHARGE(Doc.of(LONG) //
				.unit(WATT_HOURS) //
				.persistencePriority(HIGH) //
				.text("Levl state of charge [Wh]")), //
		SELL_TO_GRID_LIMIT(Doc.of(LONG) //
				.unit(WATT) //
				.persistencePriority(HIGH) //
				.text("Maximum power that may be sold to the grid [W]")), //
		BUY_FROM_GRID_LIMIT(Doc.of(LONG) //
				.unit(WATT) //
				.persistencePriority(HIGH) //
				.text("Maximum power that may be bought from the grid [W]")), //
		STATE_OF_CHARGE_LOWER_BOUND_LEVL(Doc.of(DOUBLE) //
				.unit(PERCENT) //
				.persistencePriority(HIGH) //
				.text("Lower soc bound levl has to respect [%]")), //
		STATE_OF_CHARGE_UPPER_BOUND_LEVL(Doc.of(DOUBLE) //
				.unit(PERCENT) //
				.persistencePriority(HIGH) //
				.text("Upper soc bound levl has to respect [%]")), //
		INFLUENCE_SELL_TO_GRID(Doc.of(BOOLEAN) //
				.persistencePriority(HIGH) //
				.text("Defines if levl is allowed to influence the sell to grid power [true/false]")), //
		ESS_EFFICIENCY(Doc.of(DOUBLE) //
				.unit(PERCENT) //
				.persistencePriority(HIGH) //
				.text("Ess efficiency defined by levl [%]")), //
		PRIMARY_USE_CASE_BATTERY_POWER(Doc.of(LONG) //
				.unit(WATT) //
				.persistencePriority(HIGH) //
				.text("Power that is applied for the ess primary use case")), //
		REALIZED_ENERGY_GRID(Doc.of(LONG) //
				.persistencePriority(HIGH) //
				.text("Energy realized for the current request on the grid [Ws])")), //
		REALIZED_ENERGY_BATTERY(Doc.of(LONG) //
				.persistencePriority(HIGH) //
				.text("Energy realized for the current request in the battery [Ws])")), //
		LAST_REQUEST_REALIZED_ENERGY_GRID(Doc.of(LONG) //
				.persistencePriority(HIGH) //
				.text("Energy that has been realized for the last request on the grid [Ws]")), //
		LAST_REQUEST_REALIZED_ENERGY_BATTERY(Doc.of(LONG) //
				.persistencePriority(HIGH) //
				.text("Energy that has been realized for the last request in the battery [Ws]")), //
		LAST_REQUEST_TIMESTAMP(Doc.of(STRING) //
				.persistencePriority(HIGH) //
				.text("The timestamp of the last levl control request")); //

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
	 * Returns the LongReadChannel for the remaining levl energy.
	 * 
	 * @return the LongReadChannel
	 */
	public default LongReadChannel getRemainingLevlEnergyChannel() {
		return this.channel(ChannelId.REMAINING_LEVL_ENERGY);
	}

	/**
	 * Returns the value of the remaining levl energy.
	 * 
	 * @return the value of the remaining levl energy
	 */
	public default Value<Long> getRemainingLevlEnergy() {
		return this.getRemainingLevlEnergyChannel().value();
	}

	/**
	 * Sets the next value of the remaining levl energy.
	 * 
	 * @param value the next value
	 */
	public default void _setRemainingLevlEnergy(Long value) {
		this.getRemainingLevlEnergyChannel().setNextValue(value);
	}

	/**
	 * Returns the LongReadChannel for the levl state of charge.
	 * 
	 * @return the LongReadChannel
	 */
	public default LongReadChannel getLevlSocChannel() {
		return this.channel(ChannelId.LEVL_STATE_OF_CHARGE);
	}

	/**
	 * Returns the value of the levl state of charge.
	 * 
	 * @return the value of the levl state of charge
	 */
	public default Value<Long> getLevlSoc() {
		return this.getLevlSocChannel().value();
	}

	/**
	 * Sets the next value of the levl state of charge.
	 * 
	 * @param value the next value
	 */
	public default void _setLevlSoc(Long value) {
		this.getLevlSocChannel().setNextValue(value);
	}

	/**
	 * Returns the LongReadChannel for the sell to grid limit.
	 * 
	 * @return the LongReadChannel
	 */
	public default LongReadChannel getSellToGridLimitChannel() {
		return this.channel(ChannelId.SELL_TO_GRID_LIMIT);
	}

	/**
	 * Returns the value of the sell to grid limit.
	 * 
	 * @return the value of the sell to grid limit
	 */
	public default Value<Long> getSellToGridLimit() {
		return this.getSellToGridLimitChannel().value();
	}

	/**
	 * Sets the next value of the sell to grid limit.
	 * 
	 * @param value the next value
	 */
	public default void _setSellToGridLimit(Long value) {
		this.getSellToGridLimitChannel().setNextValue(value);
	}

	/**
	 * Returns the LongReadChannel for the buy from grid limit.
	 * 
	 * @return the LongReadChannel
	 */
	public default LongReadChannel getBuyFromGridLimitChannel() {
		return this.channel(ChannelId.BUY_FROM_GRID_LIMIT);
	}

	/**
	 * Returns the value of the buy from grid limit.
	 * 
	 * @return the value of the buy from grid limit
	 */
	public default Value<Long> getBuyFromGridLimit() {
		return this.getBuyFromGridLimitChannel().value();
	}

	/**
	 * Sets the next value of the buy from grid limit.
	 * 
	 * @param value the next value
	 */
	public default void _setBuyFromGridLimit(Long value) {
		this.getBuyFromGridLimitChannel().setNextValue(value);
	}

	/**
	 * Returns the DoubleReadChannel for the lower soc bound.
	 * 
	 * @return the DoubleReadChannel
	 */
	public default DoubleReadChannel getSocLowerBoundLevlChannel() {
		return this.channel(ChannelId.STATE_OF_CHARGE_LOWER_BOUND_LEVL);
	}

	/**
	 * Returns the value of the lower soc bound.
	 * 
	 * @return the value of the lower soc bound
	 */
	public default Value<Double> getSocLowerBoundLevl() {
		return this.getSocLowerBoundLevlChannel().value();
	}

	/**
	 * Sets the next value of the lower soc bound.
	 * 
	 * @param value the next value
	 */
	public default void _setSocLowerBoundLevl(Double value) {
		this.getSocLowerBoundLevlChannel().setNextValue(value);
	}

	/**
	 * Returns the DoubleReadChannel for the upper soc bound.
	 * 
	 * @return the DoubleReadChannel
	 */
	public default DoubleReadChannel getSocUpperBoundLevlChannel() {
		return this.channel(ChannelId.STATE_OF_CHARGE_UPPER_BOUND_LEVL);
	}

	/**
	 * Returns the value of the upper soc bound.
	 * 
	 * @return the value of the upper soc bound
	 */
	public default Value<Double> getSocUpperBoundLevl() {
		return this.getSocUpperBoundLevlChannel().value();
	}

	/**
	 * Sets the next value of the upper soc bound.
	 * 
	 * @param value the next value
	 */
	public default void _setSocUpperBoundLevl(Double value) {
		this.getSocUpperBoundLevlChannel().setNextValue(value);
	}

	/**
	 * Returns the BooleanReadChannel for the influence sell to grid.
	 * 
	 * @return the BooleanReadChannel
	 */
	public default BooleanReadChannel getInfluenceSellToGridChannel() {
		return this.channel(ChannelId.INFLUENCE_SELL_TO_GRID);
	}

	/**
	 * Returns the value of the influence sell to grid.
	 * 
	 * @return the value of the influence sell to grid
	 */
	public default Value<Boolean> getInfluenceSellToGrid() {
		return this.getInfluenceSellToGridChannel().value();
	}

	/**
	 * Sets the next value of the influence sell to grid.
	 * 
	 * @param value the next value
	 */
	public default void _setInfluenceSellToGrid(Boolean value) {
		this.getInfluenceSellToGridChannel().setNextValue(value);
	}

	/**
	 * Returns the DoubleReadChannel for the ess efficiency.
	 * 
	 * @return the DoubleReadChannel
	 */
	public default DoubleReadChannel getEssEfficiencyChannel() {
		return this.channel(ChannelId.ESS_EFFICIENCY);
	}

	/**
	 * Returns the value of the ess efficiency.
	 * 
	 * @return the value of the ess efficiency
	 */
	public default Value<Double> getEssEfficiency() {
		return this.getEssEfficiencyChannel().value();
	}

	/**
	 * Sets the next value of the ess efficiency.
	 * 
	 * @param value the next value
	 */
	public default void _setEssEfficiency(Double value) {
		this.getEssEfficiencyChannel().setNextValue(value);
	}

	/**
	 * Returns the LongReadChannel for the puc (primary use case) battery power.
	 * 
	 * @return the LongReadChannel
	 */
	public default LongReadChannel getPucBatteryPowerChannel() {
		return this.channel(ChannelId.PRIMARY_USE_CASE_BATTERY_POWER);
	}

	/**
	 * Returns the value of the puc (primary use case) battery power.
	 * 
	 * @return the value of the puc battery power
	 */
	public default Value<Long> getPucBatteryPower() {
		return this.getPucBatteryPowerChannel().value();
	}

	/**
	 * Sets the next value of the puc (primary use case) battery power.
	 * 
	 * @param value the next value
	 */
	public default void _setPucBatteryPower(Long value) {
		this.getPucBatteryPowerChannel().setNextValue(value);
	}

	/**
	 * Returns the LongReadChannel for the realized energy on the grid (current
	 * request).
	 * 
	 * @return the LongReadChannel
	 */
	public default LongReadChannel getRealizedEnergyGridChannel() {
		return this.channel(ChannelId.REALIZED_ENERGY_GRID);
	}

	/**
	 * Returns the value of the realized energy on the grid (current request).
	 * 
	 * @return the value of the realized energy on the grid (current request)
	 */
	public default Value<Long> getRealizedEnergyGrid() {
		return this.getRealizedEnergyGridChannel().value();
	}

	/**
	 * Sets the next value of realized energy on the grid (current request).
	 * 
	 * @param value the next value
	 */
	public default void _setRealizedEnergyGrid(Long value) {
		this.getRealizedEnergyGridChannel().setNextValue(value);
	}

	/**
	 * Returns the LongReadChannel for the realized energy in the battery (current
	 * request).
	 * 
	 * @return the LongReadChannel
	 */
	public default LongReadChannel getRealizedEnergyBatteryChannel() {
		return this.channel(ChannelId.REALIZED_ENERGY_BATTERY);
	}

	/**
	 * Returns the value of the realized energy in the battery (current request).
	 * 
	 * @return the value of the realized energy in the battery (current request)
	 */
	public default Value<Long> getRealizedEnergyBattery() {
		return this.getRealizedEnergyBatteryChannel().value();
	}

	/**
	 * Sets the next value of realized energy in the battery (current request).
	 * 
	 * @param value the next value
	 */
	public default void _setRealizedEnergyBattery(Long value) {
		this.getRealizedEnergyBatteryChannel().setNextValue(value);
	}

	/**
	 * Returns the LongReadChannel for the realized energy on the grid (last
	 * request).
	 * 
	 * @return the LongReadChannel
	 */
	public default LongReadChannel getLastRequestRealizedEnergyGridChannel() {
		return this.channel(ChannelId.LAST_REQUEST_REALIZED_ENERGY_GRID);
	}

	/**
	 * Returns the value of the realized energy on the grid (last request).
	 * 
	 * @return the value of the realized energy on the grid (last request)
	 */
	public default Value<Long> getLastRequestRealizedEnergyGrid() {
		return this.getLastRequestRealizedEnergyGridChannel().value();
	}

	/**
	 * Sets the next value of the realized energy on the grid (last request).
	 * 
	 * @param value the next value
	 */
	public default void _setLastRequestRealizedEnergyGrid(Long value) {
		this.getLastRequestRealizedEnergyGridChannel().setNextValue(value);
	}

	/**
	 * Returns the LongReadChannel for the realized energy in the battery (last
	 * request).
	 * 
	 * @return the LongReadChannel
	 */
	public default LongReadChannel getLastRequestRealizedEnergyBatteryChannel() {
		return this.channel(ChannelId.LAST_REQUEST_REALIZED_ENERGY_BATTERY);
	}

	/**
	 * Returns the value of the realized energy in the battery (last request).
	 * 
	 * @return the value of the realized energy in the battery (last request)
	 */
	public default Value<Long> getLastRequestRealizedEnergyBattery() {
		return this.getLastRequestRealizedEnergyBatteryChannel().value();
	}

	/**
	 * Sets the next value of the realized energy in the battery (last request).
	 * 
	 * @param value the next value
	 */
	public default void _setLastRequestRealizedEnergyBattery(Long value) {
		this.getLastRequestRealizedEnergyBatteryChannel().setNextValue(value);
	}

	/**
	 * Returns the StringReadChannel for the request timestamp (last request).
	 * 
	 * @return the StringReadChannel
	 */
	public default StringReadChannel getLastRequestTimestampChannel() {
		return this.channel(ChannelId.LAST_REQUEST_TIMESTAMP);
	}

	/**
	 * Returns the value of the request timestamp (last request).
	 * 
	 * @return the value of the request timestamp (last request)
	 */
	public default Value<String> getLastRequestTimestamp() {
		return this.getLastRequestTimestampChannel().value();
	}

	/**
	 * Sets the next value of the request timestamp (last request).
	 * 
	 * @param value the next value
	 */
	public default void _setLastRequestTimestamp(String value) {
		this.getLastRequestTimestampChannel().setNextValue(value);
	}

}