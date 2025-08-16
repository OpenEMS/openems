package io.openems.edge.io.comexio;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.meter.api.SymmetricMeter;

public interface comexio extends DigitalOutput, SymmetricMeter, OpenemsComponent, EventHandler {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		DEBUG_RELAY_1(Doc.of(OpenemsType.BOOLEAN)),
		RELAY_1(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(ChannelId.DEBUG_RELAY_1))),
		DEBUG_RELAY_2(Doc.of(OpenemsType.BOOLEAN)),
		RELAY_2(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(ChannelId.DEBUG_RELAY_2))),
		DEBUG_RELAY_3(Doc.of(OpenemsType.BOOLEAN)),
		RELAY_3(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(ChannelId.DEBUG_RELAY_3))),
		DEBUG_RELAY_4(Doc.of(OpenemsType.BOOLEAN)),
		RELAY_4(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(ChannelId.DEBUG_RELAY_4))),
		DEBUG_RELAY_5(Doc.of(OpenemsType.BOOLEAN)),
		RELAY_5(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(ChannelId.DEBUG_RELAY_5))),
		DEBUG_RELAY_6(Doc.of(OpenemsType.BOOLEAN)),
		RELAY_6(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(ChannelId.DEBUG_RELAY_6))),
		DEBUG_RELAY_7(Doc.of(OpenemsType.BOOLEAN)),
		RELAY_7(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(ChannelId.DEBUG_RELAY_7))),
		DEBUG_RELAY_8(Doc.of(OpenemsType.BOOLEAN)),
		RELAY_8(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(ChannelId.DEBUG_RELAY_8))),
		DEBUG_RELAY_9(Doc.of(OpenemsType.BOOLEAN)),
		RELAY_9(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(ChannelId.DEBUG_RELAY_9))),
		
		CURRENT_1(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)),
		CURRENT_2(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)),
		CURRENT_3(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)),
		CURRENT_4(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)),
		CURRENT_5(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)),
		CURRENT_6(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)),
		CURRENT_7(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)),
		CURRENT_8(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)),
		CURRENT_9(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)),
		
		
		POWER_1(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)),
		POWER_2(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)),
		POWER_3(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)),
		POWER_4(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)),
		POWER_5(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)),
		POWER_6(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)),
		POWER_7(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)),
		POWER_8(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)),
		POWER_9(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)),

		
		

		SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT)); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
	
	
	
	//Relay 1
	public default BooleanWriteChannel getRelay1Channel() {
		return this.channel(ChannelId.RELAY_1);
	}

	public default Value<Boolean> get1Relay() {
		return this.getRelay1Channel().value();
	}


	public default void _setRelay1(Boolean value) {
		this.getRelay1Channel().setNextValue(value);
	}

	public default void setRelay1(boolean value) throws OpenemsNamedException {
		this.getRelay1Channel().setNextWriteValue(value);
	}
	//Relay 2
	public default BooleanWriteChannel getRelay2Channel() {
		return this.channel(ChannelId.RELAY_2);
	}
	
	public default Value<Boolean> get2Relay() {
		return this.getRelay2Channel().value();
	}
	
	public default void _setRelay2(Boolean value) {
		this.getRelay2Channel().setNextValue(value);
	}

	public default void setRelay2(boolean value) throws OpenemsNamedException {
		this.getRelay2Channel().setNextWriteValue(value);
	}
	//Relay 3
	public default BooleanWriteChannel getRelay3Channel() {
		return this.channel(ChannelId.RELAY_3);
	}

	public default Value<Boolean> get3Relay() {
		return this.getRelay3Channel().value();
	}


	public default void _setRelay3(Boolean value) {
		this.getRelay3Channel().setNextValue(value);
	}

	public default void setRelay3(boolean value) throws OpenemsNamedException {
		this.getRelay3Channel().setNextWriteValue(value);
	}
	//Relay 4
	public default BooleanWriteChannel getRelay4Channel() {
		return this.channel(ChannelId.RELAY_4);
	}

	public default Value<Boolean> get4Relay() {
		return this.getRelay4Channel().value();
	}


	public default void _setRelay4(Boolean value) {
		this.getRelay4Channel().setNextValue(value);
	}

	public default void setRelay4(boolean value) throws OpenemsNamedException {
		this.getRelay4Channel().setNextWriteValue(value);
	}
	//Relay 5
	public default BooleanWriteChannel getRelay5Channel() {
		return this.channel(ChannelId.RELAY_5);
	}

	public default Value<Boolean> get5Relay() {
		return this.getRelay5Channel().value();
	}


	public default void _setRelay5(Boolean value) {
		this.getRelay5Channel().setNextValue(value);
	}

	public default void setRelay5(boolean value) throws OpenemsNamedException {
		this.getRelay5Channel().setNextWriteValue(value);
	}
	//Relay 6
	public default BooleanWriteChannel getRelay6Channel() {
		return this.channel(ChannelId.RELAY_6);
	}

	public default Value<Boolean> get6Relay() {
		return this.getRelay6Channel().value();
	}


	public default void _setRelay6(Boolean value) {
		this.getRelay6Channel().setNextValue(value);
	}

	public default void setRelay6(boolean value) throws OpenemsNamedException {
		this.getRelay6Channel().setNextWriteValue(value);
	}
	//Relay 7
	public default BooleanWriteChannel getRelay7Channel() {
		return this.channel(ChannelId.RELAY_7);
	}

	public default Value<Boolean> get7Relay() {
		return this.getRelay7Channel().value();
	}


	public default void _setRelay7(Boolean value) {
		this.getRelay7Channel().setNextValue(value);
	}

	public default void setRelay7(boolean value) throws OpenemsNamedException {
		this.getRelay7Channel().setNextWriteValue(value);
	}
	//Relay 8
	public default BooleanWriteChannel getRelay8Channel() {
		return this.channel(ChannelId.RELAY_8);
	}

	public default Value<Boolean> get8Relay() {
		return this.getRelay8Channel().value();
	}


	public default void _setRelay8(Boolean value) {
		this.getRelay8Channel().setNextValue(value);
	}

	public default void setRelay8(boolean value) throws OpenemsNamedException {
		this.getRelay8Channel().setNextWriteValue(value);
	}
	//Relay 9
	public default BooleanWriteChannel getRelay9Channel() {
		return this.channel(ChannelId.RELAY_9);
	}

	public default Value<Boolean> get9Relay() {
		return this.getRelay9Channel().value();
	}

	public default void _setRelay9(Boolean value) {
		this.getRelay9Channel().setNextValue(value);
	}

	public default void setRelay9(boolean value) throws OpenemsNamedException {
		this.getRelay9Channel().setNextWriteValue(value);
	}
	
	
	//Current 1
		public default FloatReadChannel getCurrent1Channel() {
			return this.channel(ChannelId.CURRENT_1);
		}

		public default Value<Float> get1Current() {
			return this.getCurrent1Channel().value();
		}


		public default void _setCurrent1(float value) {
			this.getCurrent1Channel().setNextValue(value);
		}
		//Current 2
		public default FloatReadChannel getCurrent2Channel() {
			return this.channel(ChannelId.CURRENT_2);
		}
		
		public default Value<Float> get2Current() {
			return this.getCurrent2Channel().value();
		}
		
		public default void _setCurrent2(float value) {
			this.getCurrent2Channel().setNextValue(value);
		}
		//Current 3
		public default FloatReadChannel getCurrent3Channel() {
			return this.channel(ChannelId.CURRENT_3);
		}

		public default Value<Float> get3Current() {
			return this.getCurrent3Channel().value();
		}

		public default void _setCurrent3(float value) {
			this.getCurrent3Channel().setNextValue(value);
		}
		//Current 4
		public default FloatReadChannel getCurrent4Channel() {
			return this.channel(ChannelId.CURRENT_4);
		}

		public default Value<Float> get4Current() {
			return this.getCurrent4Channel().value();
		}

		public default void _setCurrent4(float value) {
			this.getCurrent4Channel().setNextValue(value);
		}
		//Current 5
		public default FloatReadChannel getCurrent5Channel() {
			return this.channel(ChannelId.CURRENT_5);
		}

		public default Value<Float> get5Current() {
			return this.getCurrent5Channel().value();
		}

		public default void _setCurrent5(float value) {
			this.getCurrent5Channel().setNextValue(value);
		}
		//Current 6
		public default FloatReadChannel getCurrent6Channel() {
			return this.channel(ChannelId.CURRENT_6);
		}

		public default Value<Float> get6Current() {
			return this.getCurrent6Channel().value();
		}

		public default void _setCurrent6(float value) {
			this.getCurrent6Channel().setNextValue(value);
		}
		//Current 7
		public default FloatReadChannel getCurrent7Channel() {
			return this.channel(ChannelId.CURRENT_7);
		}

		public default Value<Float> get7Current() {
			return this.getCurrent7Channel().value();
		}
		
		public default void _setCurrent7(float value) {
			this.getCurrent7Channel().setNextValue(value);
		}
		//Current 8
		public default FloatReadChannel getCurrent8Channel() {
			return this.channel(ChannelId.CURRENT_8);
		}

		public default Value<Float> get8Current() {
			return this.getCurrent8Channel().value();
		}

		public default void _setCurrent8(float value) {
			this.getCurrent8Channel().setNextValue(value);
		}
		//Current 9
		public default FloatReadChannel getCurrent9Channel() {
			return this.channel(ChannelId.CURRENT_9);
		}

		public default Value<Float> get9Current() {
			return this.getCurrent9Channel().value();
		}

		public default void _setCurrent9(float value) {
			this.getCurrent9Channel().setNextValue(value);
		}
		
		
		//Power 1
		public default FloatReadChannel getPower1Channel() {
			return this.channel(ChannelId.POWER_1);
		}

		public default Value<Float> get1Power() {
			return this.getPower1Channel().value();
		}


		public default void _setPower1(float value) {
			this.getPower1Channel().setNextValue(value);
		}
		//Power 2
		public default FloatReadChannel getPower2Channel() {
			return this.channel(ChannelId.POWER_2);
		}
		
		public default Value<Float> get2Power() {
			return this.getPower2Channel().value();
		}
		
		public default void _setPower2(float value) {
			this.getPower2Channel().setNextValue(value);
		}
		//Power 3
		public default FloatReadChannel getPower3Channel() {
			return this.channel(ChannelId.POWER_3);
		}

		public default Value<Float> get3Power() {
			return this.getPower3Channel().value();
		}

		public default void _setPower3(float value) {
			this.getPower3Channel().setNextValue(value);
		}
		//Power 4
		public default FloatReadChannel getPower4Channel() {
			return this.channel(ChannelId.POWER_4);
		}

		public default Value<Float> get4Power() {
			return this.getPower4Channel().value();
		}

		public default void _setPower4(float value) {
			this.getPower4Channel().setNextValue(value);
		}
		//Power 5
		public default FloatReadChannel getPower5Channel() {
			return this.channel(ChannelId.POWER_5);
		}

		public default Value<Float> get5Power() {
			return this.getPower5Channel().value();
		}

		public default void _setPower5(float value) {
			this.getPower5Channel().setNextValue(value);
		}
		//Power 6
		public default FloatReadChannel getPower6Channel() {
			return this.channel(ChannelId.POWER_6);
		}

		public default Value<Float> get6Power() {
			return this.getPower6Channel().value();
		}

		public default void _setPower6(float value) {
			this.getPower6Channel().setNextValue(value);
		}
		//Power 7
		public default FloatReadChannel getPower7Channel() {
			return this.channel(ChannelId.POWER_7);
		}

		public default Value<Float> get7Power() {
			return this.getPower7Channel().value();
		}
		
		public default void _setPower7(float value) {
			this.getPower7Channel().setNextValue(value);
		}
		//Power 8
		public default FloatReadChannel getPower8Channel() {
			return this.channel(ChannelId.POWER_8);
		}

		public default Value<Float> get8Power() {
			return this.getPower8Channel().value();
		}

		public default void _setPower8(float value) {
			this.getPower8Channel().setNextValue(value);
		}
		//Power 9
		public default FloatReadChannel getPower9Channel() {
			return this.channel(ChannelId.POWER_9);
		}

		public default Value<Float> get9Power() {
			return this.getPower9Channel().value();
		}

		public default void _setPower9(float value) {
			this.getPower9Channel().setNextValue(value);
		}
		


	public default StateChannel getSlaveCommunicationFailedChannel() {
		return this.channel(ChannelId.SLAVE_COMMUNICATION_FAILED);
	}

	public default Value<Boolean> getSlaveCommunicationFailed() {
		return this.getSlaveCommunicationFailedChannel().value();
	}

	public default void _setSlaveCommunicationFailed(boolean value) {
		this.getSlaveCommunicationFailedChannel().setNextValue(value);
	}
}
