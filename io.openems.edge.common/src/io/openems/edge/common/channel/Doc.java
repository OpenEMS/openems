package io.openems.edge.common.channel;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.ChannelCategory;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.internal.AbstractDoc;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Provides static meta information for a {@link Channel}.
 * 
 * Possible meta information include:
 * <ul>
 * <li>access-mode (read-only/read-write/write-only) flag
 * {@link #accessMode(AccessMode)}. Defaults to Read-Only.
 * <li>expected OpenemsType via {@link #getType()}
 * <li>descriptive text via {@link #getText()}
 * <li>is debug mode activated via {@link #isDebug()}
 * <li>callback on initialization of a Channel via {@link #getOnInitCallback()}
 * </ul>
 * 
 */
public interface Doc {

	/**
	 * Create a Channel-Doc with a specific OpenemsType.
	 * 
	 * <p>
	 * use like this:
	 * 
	 * <pre>
	 * Doc.of(OpenemsType.INTEGER)
	 * </pre>
	 * 
	 * 
	 * @param type the OpenemsType
	 * @return an instance of {@link OpenemsTypeDoc}
	 */
	public static OpenemsTypeDoc<?> of(OpenemsType type) {
		return OpenemsTypeDoc.of(type);
	}

	/**
	 * Create a Channel-Doc with specific options defined by an {@link OptionsEnum}.
	 * 
	 * <p>
	 * use like this:
	 * 
	 * <pre>
	 * Doc.of([YourOptionsEnum].values())
	 * </pre>
	 * 
	 * @param options the possible options as an OptionsEnum
	 * @return an instance of {@link EnumDoc}
	 */
	public static EnumDoc of(OptionsEnum[] options) {
		return new EnumDoc(options);
	}

	/**
	 * Create a Channel-Doc for a {@link StateChannel} with a given {@link Level}.
	 * 
	 * <p>
	 * use like this:
	 * 
	 * <pre>
	 * Doc.of(Level.FAULT)
	 * </pre>
	 * 
	 * @param level the Level
	 * @return an instance of {@link StateChannelDoc}
	 */
	public static StateChannelDoc of(Level level) {
		return new StateChannelDoc(level);
	}

	/**
	 * Gets the {@link ChannelCategory} of the Channel of this Doc.
	 * 
	 * @return the ChannelCategory
	 */
	public ChannelCategory getChannelCategory();

	/**
	 * Gets the OpenemsType.
	 * 
	 * @return the OpenemsType
	 */
	public OpenemsType getType();

	/**
	 * Gets the 'Access-Mode' information
	 * 
	 * @return
	 */
	public AccessMode getAccessMode();

	/**
	 * Gets the Unit. Defaults to NONE.
	 * 
	 * @return the unit
	 */
	public Unit getUnit();

	/**
	 * Gets the descriptive text. Defaults to empty String.
	 * 
	 * @return the text
	 */
	public String getText();

	/**
	 * Is the more verbose debug mode activated?
	 * 
	 * @return true for debug mode
	 */
	public boolean isDebug();

	/**
	 * Creates an instance of {@link Channel} for the given Channel-ID using its
	 * Channel-{@link AbstractDoc}.
	 * 
	 * @param channelId the Channel-ID
	 * @return the Channel
	 */
	public <C extends Channel<?>> C createChannelInstance(OpenemsComponent component,
			io.openems.edge.common.channel.ChannelId channelId);

}
