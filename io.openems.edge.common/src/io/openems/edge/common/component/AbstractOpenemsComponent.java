package io.openems.edge.common.component;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Factory.Property;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.internal.AbstractDoc;
import io.openems.edge.common.type.TypeUtils;

/**
 * This is the default implementation of the {@link OpenemsComponent} interface.
 *
 * {@link #activate(ComponentContext, String, boolean)} and
 * {@link #deactivate()} methods should be called by the corresponding methods
 * in the OSGi component.
 */
public abstract class AbstractOpenemsComponent implements OpenemsComponent {

	private static final String PROPERTY_CHANNEL_ID_PREFIX = "_PROPERTY_";

	private final Logger log = LoggerFactory.getLogger(AbstractOpenemsComponent.class);

	/**
	 * Holds all Channels by their Channel-ID String representation (in
	 * CaseFormat.UPPER_CAMEL).
	 */
	private final Map<String, Channel<?>> channels = new ConcurrentHashMap<>();

	private String id = null;
	private String alias = null;
	private ComponentContext componentContext = null;
	private boolean enabled = true;

	private ServiceTracker<MetaTypeService, MetaTypeService> metaTypeServiceTracker = null;
	private final AtomicReference<MetaTypeService> metaTypeService = new AtomicReference<>();

	/**
	 * Default constructor for AbstractOpenemsComponent.
	 *
	 * <p>
	 * Automatically initializes (i.e. creates {@link Channel} instances for each
	 * given {@link ChannelId} using the Channel-{@link Doc}.
	 *
	 * <p>
	 * It is important to list all Channel-ID enums of all inherited
	 * OpenEMS-Natures, i.e. for every OpenEMS Java interface you are implementing,
	 * you need to list the interface' ChannelID-enum here like
	 * Interface.ChannelId.values().
	 *
	 * <p>
	 * Use as follows:
	 *
	 * <pre>
	 * public YourPhantasticOpenemsComponent() {
	 * 	super(//
	 * 			OpenemsComponent.ChannelId.values(), //
	 * 			YourPhantasticOpenemsComponent.ChannelId.values());
	 * }
	 * </pre>
	 *
	 * <p>
	 * Note: the separation in firstInitialChannelIds and furtherInitialChannelIds
	 * is only there to enforce that calling the constructor cannot be forgotten.
	 * This way it needs to be called with at least one parameter - which is always
	 * at least "OpenemsComponent.ChannelId.values()". Just use it as if it was:
	 *
	 * <pre>
	 * AbstractOpenemsComponent(ChannelId[]... channelIds)
	 * </pre>
	 *
	 * @param firstInitialChannelIds   the Channel-IDs to initialize.
	 * @param furtherInitialChannelIds the Channel-IDs to initialize.
	 */
	protected AbstractOpenemsComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		this.addChannels(firstInitialChannelIds);
		this.addChannels(furtherInitialChannelIds);
	}

	protected void activate(String id) {
		throw new IllegalArgumentException("Use the other activate() method.");
	}

	/**
	 * Handles @Activate of implementations. Prints log output.
	 *
	 * @param context the OSGi ComponentContext
	 * @param id      the unique OpenEMS Component ID
	 * @param alias   Human-readable name of this Component. Typically
	 *                'config.alias()'. Defaults to 'id' if empty
	 * @param enabled is the Component enabled?
	 * @throws IllegalArgumentException if 'id' is null
	 */
	protected void activate(ComponentContext context, String id, String alias, boolean enabled)
			throws IllegalArgumentException {
		// Get the MetaTypeService via ServiceTracker
		// If we wouldn't do this here, each inheriting Component would have to get an
		// @Reference to MetaTypeService, which would be cumbersome.
		if (context != null && context.getBundleContext() != null) {
			this.metaTypeServiceTracker = new ServiceTracker<>(context.getBundleContext(), MetaTypeService.class,
					null) {

				@Override
				public MetaTypeService addingService(ServiceReference<MetaTypeService> serviceReference) {
					var metaTypeService = super.addingService(serviceReference);
					AbstractOpenemsComponent.this.metaTypeService.set(metaTypeService);
					AbstractOpenemsComponent.this.addChannelsForProperties();
					return metaTypeService;
				}

				@Override
				public void removedService(ServiceReference<MetaTypeService> serviceReference,
						MetaTypeService service) {
					AbstractOpenemsComponent.this.metaTypeService.set(null);
					super.removedService(serviceReference, service);
				}
			};
			this.metaTypeServiceTracker.open(true);
		}

		this.updateContext(context, id, alias, enabled);

		if (this.isEnabled()) {
			this.logMessage("Activate");
		} else {
			this.logMessage("Activate DISABLED");
		}
	}

	/**
	 * Handles @Modified of implementations.
	 *
	 * @param context the OSGi ComponentContext
	 * @param id      the unique OpenEMS Component ID
	 * @param alias   Human-readable name of this Component. Typically
	 *                'config.alias()'. Defaults to 'id' if empty
	 * @param enabled is the Component enabled?
	 * @throws IllegalArgumentException if 'id' is null
	 */
	protected void modified(ComponentContext context, String id, String alias, boolean enabled)
			throws IllegalArgumentException {
		this.updateContext(context, id, alias, enabled);

		if (this.isEnabled()) {
			this.logMessage("Modified");
		} else {
			this.logMessage("Modified DISABLED");
		}
	}

	/**
	 * Handles @Deactivate of implementations. Prints log output.
	 */
	protected void deactivate() {
		this.logMessage("Deactivate");
		// disable the ServiceTracker
		if (this.metaTypeServiceTracker != null) {
			this.metaTypeServiceTracker.close();
		}

		// deactivate all Channels
		for (Channel<?> channel : this.channels.values()) {
			channel.deactivate();
		}
	}

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}

	@Override
	public ComponentContext getComponentContext() {
		if (this.componentContext == null) {
			this.logWarn(this.log,
					"ComponentContext is null. Please make sure to call AbstractOpenemsComponent.activate()-method early!");
		}
		return this.componentContext;
	}

	/**
	 * Helper method to update the Context on @Activate and @Modified.
	 *
	 * @param context the OSGi ComponentContext
	 * @param id      the unique OpenEMS Component ID
	 * @param alias   Human-readable name of this Component. Typically
	 *                'config.alias()'. Defaults to 'id' if empty
	 * @param enabled is the Component enabled?
	 * @throws IllegalArgumentException if 'id' is null
	 */
	private void updateContext(ComponentContext context, String id, String alias, boolean enabled)
			throws IllegalArgumentException {
		TypeUtils.assertNull("Component-ID is not allowed to be null", id);
		this.id = id;

		if (alias == null || alias.trim().isEmpty()) {
			this.alias = this.id;
		} else {
			this.alias = alias;
		}

		this.enabled = enabled;
		this.componentContext = context;

		this.addChannelsForProperties();
	}

	/**
	 * Add a Channel for each Property and set the configured value.
	 *
	 * <p>
	 * If the Property key is "enabled" then a Channel with the ID
	 * "_PropertyEnabled" is generated.
	 */
	private synchronized void addChannelsForProperties() {
		// Make sure ComponentContext, MetaTypeService, Bundle and MetaTypeInformation
		// are available
		final var context = this.componentContext;
		final var metaTypeService = this.metaTypeService.get();
		if (context == null || metaTypeService == null) {
			return;
		}
		final var bundle = context.getUsingBundle();
		if (bundle == null) {
			return;
		}
		final var mti = metaTypeService.getMetaTypeInformation(bundle);
		if (mti == null) {
			return;
		}
		final var properties = context.getProperties();
		if (properties == null) {
			return;
		}

		// get Factory-PIDs in this Bundle
		var factoryPids = mti.getFactoryPids();
		for (String factoryPid : factoryPids) {
			var ocd = mti.getObjectClassDefinition(factoryPid, null);
			this.addChannelsForProperties(ocd, properties);
		}

		// get Singleton PIDs in this Bundle
		for (String pid : mti.getPids()) {
			switch (pid) {
			default:
				var ocd = mti.getObjectClassDefinition(pid, null);
				this.addChannelsForProperties(ocd, properties);
			}
		}
	}

	/**
	 * Adds Channels for Properties defined by {@link ObjectClassDefinition}..
	 *
	 * @param ocd        The {@link ObjectClassDefinition}, i.e. the main annotation
	 *                   on the Config class
	 * @param properties the configuration properties {@link Dictionary}
	 */
	private void addChannelsForProperties(ObjectClassDefinition ocd, Dictionary<String, Object> properties) {
		for (Property property : EdgeConfig.Factory.toProperties(ocd)) {
			if (property.isPassword()) {
				// Do not add 'Password' properties as Channels
				continue;
			}

			// Evaluate Channel-Type
			final OpenemsType channelType;
			var propertyValue = properties.get(property.getId());
			if (propertyValue != null && propertyValue.getClass().isArray() && Array.getLength(propertyValue) > 1) {
				// Arrays with more than one value can only be stored as string
				channelType = OpenemsType.STRING;
			} else {
				channelType = property.getType();
			}

			// Create Channel
			var channelName = PROPERTY_CHANNEL_ID_PREFIX
					+ CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, property.getId().replace(".", "_"));
			Channel<?> channel = this.channels
					.get(io.openems.edge.common.channel.ChannelId.channelIdUpperToCamel(channelName));
			if (channel == null) {
				// Channel does not already exist -> create new Channel
				AbstractDoc<?> doc = Doc.of(channelType);
				doc.persistencePriority(PersistencePriority.MEDIUM);
				io.openems.edge.common.channel.ChannelId channelId = new io.openems.edge.common.channel.ChannelId() {

					@Override
					public String name() {
						return channelName;
					}

					@Override
					public Doc doc() {
						return doc;
					}
				};
				channel = this.addChannel(channelId);
			}

			// Set the Value
			Object value = null;
			try {
				value = TypeUtils.getAsType(channelType, properties.get(property.getId()));
			} catch (IllegalArgumentException e) {
				// can be ignored
			}
			if (value == null) {
				try {
					value = JsonUtils.getAsType(channelType, property.getDefaultValue());
				} catch (OpenemsNamedException | IllegalArgumentException e) {
					this.logError(this.log, "Unable to parse Property [" + property.getId() + "] value ["
							+ property.getDefaultValue() + "] to [" + property.getType() + "]: " + e.getMessage());
				}
			}
			channel.setNextValue(value);
		}
	}

	/**
	 * Initializes the given Channel-ID.
	 *
	 * <ul>
	 * <li>Creates an object instance from Channel-Doc
	 * <li>Registers the Channel
	 * </ul>
	 *
	 * @param channelId the given Channel-ID
	 * @return the newly created Channel
	 */
	protected Channel<?> addChannel(io.openems.edge.common.channel.ChannelId channelId) {
		var doc = channelId.doc();
		Channel<?> channel = doc.createChannelInstance(this, channelId);
		this.addChannel(channel);
		return channel;
	}

	/**
	 * Adds a Channel to this Component.
	 *
	 * @param channel the Channel
	 * @throws NullPointerException     if the Channel was not initialized.
	 * @throws IllegalArgumentException if the Channel-ID had already been added.
	 */
	private void addChannel(Channel<?> channel) throws NullPointerException, IllegalArgumentException {
		if (channel == null) {
			throw new NullPointerException(
					"Trying to add 'null' Channel. Hint: Check for missing handling of Enum value.");
		}
		if (this.channels.containsKey(channel.channelId().id())) {
			throw new IllegalArgumentException(
					"Duplicated Channel-ID [" + channel.channelId().id() + "] for Component [" + this.id + "]");
		}
		// Add Channel to channels list
		this.channels.put(channel.channelId().id(), channel);
		// Handle StateChannels
		if (channel instanceof StateChannel) {
			this.getStateChannel().addChannel((StateChannel) channel);
		}
	}

	/**
	 * Initializes the given Channel-IDs.
	 *
	 * <ul>
	 * <li>Creates object instances from Channel-Doc
	 * <li>Registers the Channels
	 * </ul>
	 *
	 * @param initialChannelIds the given Channel-IDs
	 */
	protected void addChannels(io.openems.edge.common.channel.ChannelId[] initialChannelIds) {
		for (io.openems.edge.common.channel.ChannelId channelId : initialChannelIds) {
			this.addChannel(channelId);
		}
	}

	/**
	 * Initializes the given Channel-IDs.
	 *
	 * <ul>
	 * <li>Creates object instances from Channel-Doc
	 * <li>Registers the Channels
	 * </ul>
	 *
	 * @param initialChannelIds the given Channel-IDs
	 */
	protected void addChannels(io.openems.edge.common.channel.ChannelId[][] initialChannelIds) {
		for (io.openems.edge.common.channel.ChannelId[] channelIds : initialChannelIds) {
			for (io.openems.edge.common.channel.ChannelId channelId : channelIds) {
				this.addChannel(channelId);
			}
		}
	}

	/**
	 * Nicely writes a log message on activate/deactivate/modified events.
	 *
	 * @param message the message
	 */
	private void logMessage(String message) {
		// by default: use the class name
		var name = this.getClass().getSimpleName();

		// try to find the component name
		var context = this.componentContext;
		if (context != null) {
			var properties = context.getProperties();
			if (properties != null) {
				var obj = properties.get(ComponentConstants.COMPONENT_NAME);
				if (obj != null) {
					name = obj.toString();
				}
			}
		}
		this.logInfo(this.log, message + " " + name);
	}

	@Override
	public String id() {
		return this.id;
	}

	@Override
	public String alias() {
		return this.alias;
	}

	@Deprecated()
	@Override
	public Channel<?> _channel(String channelName) {
		return this.channels.get(channelName);
	}

	/**
	 * Removes a Channel from this Component.
	 *
	 * @param channel the Channel
	 */
	// TODO remove Channel(s) using Channel-ID; see addChannels()-method above.
	protected void removeChannel(Channel<?> channel) {
		// Add Channel to channels list
		this.channels.remove(channel.channelId().id(), channel);
		// Handle StateChannels
		if (channel instanceof StateChannel) {
			this.getStateChannel().removeChannel((StateChannel) channel);
		}
	}

	@Override
	public Collection<Channel<?>> channels() {
		return this.channels.values();
	}

	/**
	 * Log a debug message including the Component ID.
	 *
	 * @param log     the Logger instance
	 * @param message the message
	 */
	protected void logDebug(Logger log, String message) {
		OpenemsComponent.logDebug(this, log, message);
	}

	/**
	 * Log an info message including the Component ID.
	 *
	 * @param log     the Logger instance
	 * @param message the message
	 */
	protected void logInfo(Logger log, String message) {
		OpenemsComponent.logInfo(this, log, message);
	}

	/**
	 * Log a warn message including the Component ID.
	 *
	 * @param log     the Logger instance
	 * @param message the message
	 */
	protected void logWarn(Logger log, String message) {
		OpenemsComponent.logWarn(this, log, message);
	}

	/**
	 * Log an error message including the Component ID.
	 *
	 * @param log     the Logger instance
	 * @param message the message
	 */
	protected void logError(Logger log, String message) {
		OpenemsComponent.logError(this, log, message);
	}

}
