package io.openems.edge.system.fenecon.masterbox2v0.relay;

import static io.openems.edge.common.channel.ChannelUtils.setWriteValueIfNotRead;
import static java.util.Collections.emptyList;

import java.util.List;
import java.util.stream.Stream;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingConsumer;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.system.fenecon.masterbox2v0.MasterBox2v0;
import io.openems.edge.system.fenecon.masterbox2v0.utils.IocReadValueMapping;
import io.openems.edge.system.fenecon.masterbox2v0.utils.IocWriteValueMapping;
import io.openems.edge.system.fenecon.masterbox2v0.utils.MasterBoxReadWriteModbusComponent;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Fenecon.MasterBox2V0.Relay", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
})
public class IoMasterBox2v0RelayImpl extends AbstractOpenemsComponent implements IoMasterBox2v0Relay, DigitalOutput,
		OpenemsComponent, EventHandler, MasterBoxReadWriteModbusComponent {

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private MasterBox2v0 ioc;

	private Config config;

	private BooleanWriteChannel[] digitalOutputChannels;

	private List<IocWriteValueMapping<?>> writeValueMappings = emptyList();

	public IoMasterBox2v0RelayImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				IoMasterBox2v0Relay.ChannelId.values() //
		);

		this.initializeChannels();
	}

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
		if (this.ioc != null) {
			this.addListenerToChannels();
		}
	}

	@Modified
	protected void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		if (this.ioc != null) {
			this.removeListenerFromChannels();
		}
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.digitalOutputChannels;
	}

	@Override
	public String debugLog() {
		var b = new StringBuilder();
		var i = 1;
		for (BooleanReadChannel channel : this.digitalOutputChannels) {
			String valueText;
			var valueOpt = channel.value().asOptional();
			valueText = valueOpt.map(aBoolean -> aBoolean ? "x" : "-").orElse("?");
			b.append(i).append(valueText);

			// add space for all but the last
			if (++i <= this.digitalOutputChannels.length) {
				b.append(" ");
			}
		}
		return b.toString();
	}

	private void initializeChannels() {
		this.digitalOutputChannels = Stream.of(IoMasterBox2v0Relay.ChannelId.values()) //
				.filter(channelId -> channelId.doc().getAccessMode() == AccessMode.READ_WRITE) //
				.map(this::channel) //
				.toArray(BooleanWriteChannel[]::new);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> this.setChannels();
		}
	}

	@Override
	public List<IocReadValueMapping<?>> getReadValueMappings() {
		return List.of(//
				new IocReadValueMapping<>(this.ioc::getRelay1, IoMasterBox2v0Relay.ChannelId.RELAY_1),
				new IocReadValueMapping<>(this.ioc::getRelay2, IoMasterBox2v0Relay.ChannelId.RELAY_2),
				new IocReadValueMapping<>(this.ioc::getRelay3, IoMasterBox2v0Relay.ChannelId.RELAY_3),
				new IocReadValueMapping<>(this.ioc::getRelay4, IoMasterBox2v0Relay.ChannelId.RELAY_4),
				new IocReadValueMapping<>(this.ioc::getRelay5, IoMasterBox2v0Relay.ChannelId.RELAY_5),
				new IocReadValueMapping<>(this.ioc::getRelay6, IoMasterBox2v0Relay.ChannelId.RELAY_6));
	}

	@Override
	public boolean hasIoc() {
		return this.ioc != null;
	}

	@Override
	public List<IocWriteValueMapping<?>> getWriteValueMappings() {

		ThrowingConsumer<Boolean, OpenemsNamedException> relay1Listener = value -> //
		setWriteValueIfNotRead(this.ioc.getRelay1Channel(), value);
		ThrowingConsumer<Boolean, OpenemsNamedException> relay2Listener = value -> //
		setWriteValueIfNotRead(this.ioc.getRelay2Channel(), value);
		ThrowingConsumer<Boolean, OpenemsNamedException> relay3Listener = value -> //
		setWriteValueIfNotRead(this.ioc.getRelay3Channel(), value);
		ThrowingConsumer<Boolean, OpenemsNamedException> relay4Listener = value -> //
		setWriteValueIfNotRead(this.ioc.getRelay4Channel(), value);
		ThrowingConsumer<Boolean, OpenemsNamedException> relay5Listener = value -> //
		setWriteValueIfNotRead(this.ioc.getRelay5Channel(), value);
		ThrowingConsumer<Boolean, OpenemsNamedException> relay6Listener = value -> //
		setWriteValueIfNotRead(this.ioc.getRelay6Channel(), value);

		if (this.writeValueMappings.isEmpty()) {
			this.writeValueMappings = List.of(//
					new IocWriteValueMapping<>(relay1Listener, this::getRelay1Channel),
					new IocWriteValueMapping<>(relay2Listener, this::getRelay2Channel),
					new IocWriteValueMapping<>(relay3Listener, this::getRelay3Channel),
					new IocWriteValueMapping<>(relay4Listener, this::getRelay4Channel),
					new IocWriteValueMapping<>(relay5Listener, this::getRelay5Channel),
					new IocWriteValueMapping<>(relay6Listener, this::getRelay6Channel) //
			);
		}
		return this.writeValueMappings;
	}

	private void applyConfig(Config config) {
		this.config = config;
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ioc", this.config.ioc_id())) {
			return;
		}
	}
}
