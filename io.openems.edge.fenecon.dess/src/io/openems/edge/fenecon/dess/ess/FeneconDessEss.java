package io.openems.edge.fenecon.dess.ess;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.fenecon.dess.charger.AbstractFeneconDessCharger;

public interface FeneconDessEss extends AsymmetricEss, SymmetricEss, OpenemsComponent {

	public static final int MAX_APPARENT_POWER = 9_000; // [VA]
	public static final int CAPACITY = 10_000; // [Wh]

	/**
	 * Gets the Modbus Unit-ID.
	 *
	 * @return the Unit-ID
	 */
	public Integer getUnitId();

	/**
	 * Gets the Modbus-Bridge Component-ID, i.e. "modbus0".
	 *
	 * @return the Component-ID
	 */
	public String getModbusBridgeId();

	/**
	 * Registers a Charger with this ESS.
	 *
	 * @param charger the Charger
	 */
	public void addCharger(AbstractFeneconDessCharger charger);

	/**
	 * Unregisters a Charger from this ESS.
	 *
	 * @param charger the Charger
	 */
	public void removeCharger(AbstractFeneconDessCharger charger);

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SYSTEM_STATE(Doc.of(SystemState.values())), //
		ORIGINAL_ACTIVE_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS)),
		ORIGINAL_ACTIVE_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS)),
		BSMU_WORK_STATE(Doc.of(BsmuWorkState.values()) //
				.<FeneconDessEss>onChannelChange((self, value) -> {
					// on each change set Grid-Mode channel
					BsmuWorkState state = value.asEnum();
					self._setGridMode(switch (state) {
					case ON_GRID -> GridMode.ON_GRID;
					case OFF_GRID -> GridMode.OFF_GRID;
					case FAULT, UNDEFINED, BEING_ON_GRID, BEING_PRE_CHARGE, BEING_STOP, DEBUG, INIT, LOW_CONSUMPTION,
							PRE_CHARGE ->
						GridMode.UNDEFINED;
					});
				})), //
		STACK_CHARGE_STATE(Doc.of(StackChargeState.values())); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}