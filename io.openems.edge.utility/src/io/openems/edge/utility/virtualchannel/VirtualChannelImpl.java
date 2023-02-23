package io.openems.edge.utility.virtualchannel;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.utility.api.VirtualChannelType;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

/**
 * <p>
 * This OpenEMS Component allows to create "n" many VirtualChannel on the fly.
 * You can set via configuration how many Long, Boolean, String and Double
 * channel you want. Additionally it is possible to add a default value and
 * further description.
 * </p>
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Utility.Channel.Virtual", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)

public class VirtualChannelImpl extends AbstractOpenemsComponent implements OpenemsComponent {

	public VirtualChannelImpl() {
		super(OpenemsComponent.ChannelId.values());
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.activationOrModifiedRoutine(config);
	}

	@Modified
	void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.channels().stream().filter(channel -> !channel.channelId().id().contains("Property")) //
				.forEach(this::removeChannel);
		this.activationOrModifiedRoutine(config);
	}

	void activationOrModifiedRoutine(Config config) {

		generateChannel(this, VirtualChannelType.LONG, config.numberOfLongChannel(), config.optionalLongChannelIds(), //
				config.optionalLongChannelValues());
		generateChannel(this, VirtualChannelType.BOOLEAN, config.numberOfBooleanChannel(), //
				config.optionalBooleanChannelIds(), config.optionalBooleanChannelValues());

		generateChannel(this, VirtualChannelType.DOUBLE, config.numberOfDoubleChannel(), //
				config.optionalDoubleChannelIds(), config.optionalDoubleChannelValues());

		generateChannel(this, VirtualChannelType.STRING, config.numberOfStringChannel(), //
				config.optionalStringChannelIds(), config.optionalStringChannelValues());
	}

	private static void generateChannel(VirtualChannelImpl parent, VirtualChannelType type, int numberOfChannel,
			String[] optionalChannelIds, String[] optionalDefaultValues) {
		if (numberOfChannel > 0) {
			for (int x = 0; x < numberOfChannel; x++) {
				String channelIdSpecifier = Integer.toString(x);
				if (optionalChannelIds.length > x && !optionalChannelIds[x].trim().equals("")) {
					channelIdSpecifier = optionalChannelIds[x].toUpperCase();
				}
				io.openems.edge.common.channel.ChannelId createdChannel = new DynamicChannelId(//
						"VIRTUAL_" + type.name() + "_" + channelIdSpecifier, //
						Doc.of(getOpenEmsTypeByVirtualChannelType(type)).accessMode(AccessMode.READ_WRITE)//
								.onInit(channel -> {
									if (channel instanceof WriteChannel<?>) {
										((WriteChannel<?>) channel).onSetNextWrite(channel::setNextValue);
									}
								}));
				parent.addChannel(createdChannel);
				parent.channel(createdChannel).setNextValue(getOptionalValueFromConfig(x, optionalDefaultValues));
			}
		}
	}

	private static String getOptionalValueFromConfig(int x, String[] optionalDefaultValues) {
		return optionalDefaultValues.length > x ? optionalDefaultValues[x] : null;
	}

	private static OpenemsType getOpenEmsTypeByVirtualChannelType(VirtualChannelType type) {
		switch (type) {
			case STRING :
				return OpenemsType.STRING;
			case BOOLEAN :
				return OpenemsType.BOOLEAN;
			case DOUBLE :
				return OpenemsType.DOUBLE;
			case LONG :
			default :
				return OpenemsType.LONG;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return super.debugLog();
	}
}
