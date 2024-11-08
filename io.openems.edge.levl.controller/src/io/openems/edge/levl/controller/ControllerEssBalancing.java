package io.openems.edge.levl.controller;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerEssBalancing extends Controller, OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId { 
    	REMAINING_LEVL_ENERGY(Doc.of(OpenemsType.LONG)
        		.persistencePriority(PersistencePriority.HIGH)
        		.text("energy to be realized [Ws])")),
        LEVL_SOC(Doc.of(OpenemsType.LONG)
        		.unit(Unit.WATT_HOURS)
        		.persistencePriority(PersistencePriority.HIGH)
        		.text("levl state of charge [Wh]")),
        SELL_TO_GRID_LIMIT(Doc.of(OpenemsType.LONG)
        		.unit(Unit.WATT)
        		.persistencePriority(PersistencePriority.HIGH)
        		.text("maximum power that may be sold to the grid [W]")),
        BUY_FROM_GRID_LIMIT(Doc.of(OpenemsType.LONG)
        		.unit(Unit.WATT)
        		.persistencePriority(PersistencePriority.HIGH)
        		.text("maximum power that may be bought from the grid [W]")),
        SOC_LOWER_BOUND_LEVL(Doc.of(OpenemsType.DOUBLE)
        		.unit(Unit.PERCENT)
        		.persistencePriority(PersistencePriority.HIGH)
        		.text("lower soc bound limit levl has to respect [%]")),
        SOC_UPPER_BOUND_LEVL(Doc.of(OpenemsType.DOUBLE)
        		.unit(Unit.PERCENT)
        		.persistencePriority(PersistencePriority.HIGH)
        		.text("upper soc bound limit levl has to respect [%]")),
        INFLUENCE_SELL_TO_GRID(Doc.of(OpenemsType.BOOLEAN)
        		.persistencePriority(PersistencePriority.HIGH)
        		.text("defines if levl is allowed to influence the sell to grid power [true/false]")),
        EFFICIENCY(Doc.of(OpenemsType.DOUBLE)
                .unit(Unit.PERCENT)
                .persistencePriority(PersistencePriority.HIGH)
                .text("ess efficiency defined by levl [%]")),
        PUC_BATTERY_POWER(Doc.of(OpenemsType.LONG)
        		.unit(Unit.WATT)
        		.persistencePriority(PersistencePriority.HIGH)
        		.text("power that is applied for the ess primary use case")),
        REALIZED_ENERGY_GRID(Doc.of(OpenemsType.LONG)
        		.persistencePriority(PersistencePriority.HIGH)
        		.text("energy realized for the current request on the grid [Ws])")),
        REALIZED_ENERGY_BATTERY(Doc.of(OpenemsType.LONG)
        		.persistencePriority(PersistencePriority.HIGH)
        		.text("energy realized for the current request in the battery [Ws])")),
        LAST_REQUEST_REALIZED_ENERGY_GRID(Doc.of(OpenemsType.LONG)
                .persistencePriority(PersistencePriority.HIGH)
                .text("cumulated amount of discharge energy that has been realized since the last discharge request on the grid [Ws]")),
        LAST_REQUEST_REALIZED_ENERGY_BATTERY(Doc.of(OpenemsType.LONG)
                .persistencePriority(PersistencePriority.HIGH)
                .text("cumulated amount of discharge energy that has been realized since the last discharge request in the battery [Ws]")),
        LAST_REQUEST_TIMESTAMP(Doc.of(OpenemsType.STRING)
                .persistencePriority(PersistencePriority.HIGH)
                .text("the timestamp of the last levl control request"));

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
	 * @return the LongReadChannel
	 */
	public default LongReadChannel getRemainingLevlEnergyChannel() {
	    return this.channel(ChannelId.REMAINING_LEVL_ENERGY);
	}
	
	/**
	 * Returns the value of the remaining levl energy.
	 * @return the value of the remaining levl energy
	 */
	public default Value<Long> getRemainingLevlEnergy() {
	    return this.getRemainingLevlEnergyChannel().value();
	}
	
	/**
	 * Sets the next value of the remaining levl energy.
	 * @param value the next value
	 */
	public default void _setRemainingLevlEnergy(Long value) {
	    this.getRemainingLevlEnergyChannel().setNextValue(value);
	}
	
	/**
	 * Returns the LongReadChannel for the levl state of charge.
	 * @return the LongReadChannel
	 */
	public default LongReadChannel getLevlSocChannel() {
	    return this.channel(ChannelId.LEVL_SOC);
	}
	
	/**
	 * Returns the value of the levl state of charge.
	 * @return the value of the levl state of charge
	 */
	public default Value<Long> getLevlSoc() {
	    return this.getLevlSocChannel().value();
	}
	
	/**
	 * Sets the next value of the levl state of charge.
	 * @param value the next value
	 */
	public default void _setLevlSoc(Long value) {
	    this.getLevlSocChannel().setNextValue(value);
	}

	/**
	 * Returns the LongReadChannel for the sell to grid limit.
	 * @return the LongReadChannel
	 */
	public default LongReadChannel getSellToGridLimitChannel() {
	    return this.channel(ChannelId.SELL_TO_GRID_LIMIT);
	}
	
	/**
	 * Returns the value of the sell to grid limit.
	 * @return the value of the sell to grid limit
	 */
	public default Value<Long> getSellToGridLimit() {
	    return this.getSellToGridLimitChannel().value();
	}
	
	/**
	 * Sets the next value of the sell to grid limit.
	 * @param value the next value
	 */
	public default void _setSellToGridLimit(Long value) {
	    this.getSellToGridLimitChannel().setNextValue(value);
	}

	/**
	 * Returns the LongReadChannel for the buy from grid limit.
	 * @return the LongReadChannel
	 */
	public default LongReadChannel getBuyFromGridLimitChannel() {
	    return this.channel(ChannelId.BUY_FROM_GRID_LIMIT);
	}
	
	/**
	 * Returns the value of the buy from grid limit.
	 * @return the value of the buy from grid limit
	 */
	public default Value<Long> getBuyFromGridLimit() {
	    return this.getBuyFromGridLimitChannel().value();
	}
	
	/**
	 * Sets the next value of the buy from grid limit.
	 * @param value the next value
	 */
	public default void _setBuyFromGridLimit(Long value) {
	    this.getBuyFromGridLimitChannel().setNextValue(value);
	}

	/**
	 * Returns the DoubleReadChannel for the lower soc bound limit.
	 * @return the DoubleReadChannel
	 */
	public default DoubleReadChannel getSocLowerBoundLevlChannel() {
	    return this.channel(ChannelId.SOC_LOWER_BOUND_LEVL);
	}
	
	/**
	 * Returns the value of the lower soc bound limit.
	 * @return the value of the lower soc bound limit
	 */
	public default Value<Double> getSocLowerBoundLevl() {
	    return this.getSocLowerBoundLevlChannel().value();
	}
	
	/**
	 * Sets the next value of the lower soc bound limit.
	 * @param value the next value
	 */
	public default void _setSocLowerBoundLevl(Double value) {
	    this.getSocLowerBoundLevlChannel().setNextValue(value);
	}

	/**
	 * Returns the DoubleReadChannel for the upper soc bound limit.
	 * @return the DoubleReadChannel
	 */
	public default DoubleReadChannel getSocUpperBoundLevlChannel() {
	    return this.channel(ChannelId.SOC_UPPER_BOUND_LEVL);
	}
	
	/**
	 * Returns the value of the upper soc bound limit.
	 * @return the value of the upper soc bound limit
	 */
	public default Value<Double> getSocUpperBoundLevl() {
	    return this.getSocUpperBoundLevlChannel().value();
	}
	
	/**
	 * Sets the next value of the upper soc bound limit.
	 * @param value the next value
	 */
	public default void _setSocUpperBoundLevl(Double value) {
	    this.getSocUpperBoundLevlChannel().setNextValue(value);
	}

	/**
	 * Returns the BooleanReadChannel for the influence sell to grid.
	 * @return the BooleanReadChannel
	 */
	public default BooleanReadChannel getInfluenceSellToGridChannel() {
	    return this.channel(ChannelId.INFLUENCE_SELL_TO_GRID);
	}
	
	/**
	 * Returns the value of the influence sell to grid.
	 * @return the value of the influence sell to grid
	 */
	public default Value<Boolean> getInfluenceSellToGrid() {
	    return this.getInfluenceSellToGridChannel().value();
	}
	
	/**
	 * Sets the next value of the influence sell to grid.
	 * @param value the next value
	 */
	public default void _setInfluenceSellToGrid(Boolean value) {
	    this.getInfluenceSellToGridChannel().setNextValue(value);
	}

    /**
     * Returns the DoubleReadChannel for the efficiency.
     * @return the DoubleReadChannel
     */
    public default DoubleReadChannel getEfficiencyChannel() {
        return this.channel(ChannelId.EFFICIENCY);
    }

    /**
     * Returns the value of the efficiency.
     * @return the value of the efficiency
     */
    public default Value<Double> getEfficiency() {
        return this.getEfficiencyChannel().value();
    }

    /**
     * Sets the next value of the efficiency.
     * @param value the next value
     */
    public default void _setEfficiency(Double value) {
        this.getEfficiencyChannel().setNextValue(value);
    }
    
	/**
	 * Returns the LongReadChannel for the PUC battery power.
	 * @return the LongReadChannel
	 */
	public default LongReadChannel getPucBatteryPowerChannel() {
	    return this.channel(ChannelId.PUC_BATTERY_POWER);
	}
	
	/**
	 * Returns the value of the PUC battery power.
	 * @return the value of the PUC battery power
	 */
	public default Value<Long> getPucBatteryPower() {
	    return this.getPucBatteryPowerChannel().value();
	}
		
	/**
	 * Sets the next value of the PUC battery power.
	 * @param value the next value
	 */
	public default void _setPucBatteryPower(Long value) {
	    this.getPucBatteryPowerChannel().setNextValue(value);
	}
	
	/**
	 * Returns the LongReadChannel for the realized energy on the grid (current request).
	 * @return the LongReadChannel
	 */
	public default LongReadChannel getRealizedEnergyGridChannel() {
	    return this.channel(ChannelId.REALIZED_ENERGY_GRID);
	}
	
	/**
	 * Returns the value of the realized energy on the grid (current request).
	 * @return the value of the realized energy on the grid (current request)
	 */
	public default Value<Long> getRealizedEnergyGrid() {
	    return this.getRealizedEnergyGridChannel().value();
	}
	
	/**
	 * Sets the next value of realized energy on the grid (current request).
	 * @param value the next value
	 */
	public default void _setRealizedEnergyGrid(Long value) {
	    this.getRealizedEnergyGridChannel().setNextValue(value);
	}
	
	/**
	 * Returns the LongReadChannel for the realized energy in the battery (current request).
	 * @return the LongReadChannel
	 */
	public default LongReadChannel getRealizedEnergyBatteryChannel() {
	    return this.channel(ChannelId.REALIZED_ENERGY_BATTERY);
	}
	
	/**
	 * Returns the value of the realized energy in the battery (current request).
	 * @return the value of the realized energy in the battery (current request)
	 */
	public default Value<Long> getRealizedEnergyBattery() {
	    return this.getRealizedEnergyBatteryChannel().value();
	}
	
	/**
	 * Sets the next value of realized energy in the battery (current request).
	 * @param value the next value
	 */
	public default void _setRealizedEnergyBattery(Long value) {
	    this.getRealizedEnergyBatteryChannel().setNextValue(value);
	}
	
	/**
	 * Returns the LongReadChannel for the realized energy on the grid (last request).
	 * @return the LongReadChannel
	 */
	public default LongReadChannel getLastRequestRealizedEnergyGridChannel() {
	    return this.channel(ChannelId.LAST_REQUEST_REALIZED_ENERGY_GRID);
	}
	
	/**
	 * Returns the value of the realized energy on the grid (last request).
	 * @return the value of the realized energy on the grid (last request)
	 */
	public default Value<Long> getLastRequestRealizedEnergyGrid() {
	    return this.getLastRequestRealizedEnergyGridChannel().value();
	}
	
	/**
	 * Sets the next value of the realized energy on the grid (last request).
	 * @param value the next value
	 */
	public default void _setLastRequestRealizedEnergyGrid(Long value) {
	    this.getLastRequestRealizedEnergyGridChannel().setNextValue(value);
	}
	
	/**
	 * Returns the LongReadChannel for the realized energy in the battery (last request).
	 * @return the LongReadChannel
	 */
	public default LongReadChannel getLastRequestRealizedEnergyBatteryChannel() {
	    return this.channel(ChannelId.LAST_REQUEST_REALIZED_ENERGY_BATTERY);
	}
	
	/**
	 * Returns the value of the realized energy in the battery (last request).
	 * @return the value of the realized energy in the battery (last request)
	 */
	public default Value<Long> getLastRequestRealizedEnergyBattery() {
	    return this.getLastRequestRealizedEnergyBatteryChannel().value();
	}
	
	/**
	 * Sets the next value of the realized energy in the battery (last request).
	 * @param value the next value
	 */
	public default void _setLastRequestRealizedEnergyBattery(Long value) {
	    this.getLastRequestRealizedEnergyBatteryChannel().setNextValue(value);
	}

	/**
	 * Returns the StringReadChannel for the request timestamp (last request).
	 * @return the StringReadChannel
	 */
	public default StringReadChannel getLastRequestTimestampChannel() {
	    return this.channel(ChannelId.LAST_REQUEST_TIMESTAMP);
	}
	
	/**
	 * Returns the value of the request timestamp (last request).
	 * @return the value of the request timestamp (last request)
	 */
	public default Value<String> getLastRequestTimestamp() {
	    return this.getLastRequestTimestampChannel().value();
	}
	
	/**
	 * Sets the next value of the request timestamp (last request).
	 * @param value the next value
	 */
	public default void _setLastRequestTimestamp(String value) {
	    this.getLastRequestTimestampChannel().setNextValue(value);
	}

}