package io.openems.edge.system.fenecon.masterbox2v0.ao;

import static io.openems.edge.common.channel.ChannelUtils.setWriteValueIfNotRead;
import static java.util.Collections.emptyList;

import java.util.List;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingConsumer;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.AnalogOutput;
import io.openems.edge.io.api.AnalogVoltageOutput;
import io.openems.edge.system.fenecon.masterbox2v0.MasterBox2v0;
import io.openems.edge.system.fenecon.masterbox2v0.utils.IocReadValueMapping;
import io.openems.edge.system.fenecon.masterbox2v0.utils.IocWriteValueMapping;
import io.openems.edge.system.fenecon.masterbox2v0.utils.MasterBoxReadWriteModbusComponent;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Fenecon.MasterBox2V0.AO", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
})
public class IoMasterBox2v0AoImpl extends AbstractOpenemsComponent implements IoMasterBox2v0Ao, OpenemsComponent,
		EventHandler, AnalogVoltageOutput, AnalogOutput, MasterBoxReadWriteModbusComponent {

	private static final int OFFSET = 0;
	private static final int PRECISION = 100;
	private static final int MAXIMUM = 10000;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private MasterBox2v0 ioc;

	private Config config;

	private List<IocWriteValueMapping<?>> writeValueMappings = emptyList();

	public IoMasterBox2v0AoImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				IoMasterBox2v0Ao.ChannelId.values(), //
				AnalogOutput.ChannelId.values(), //
				AnalogVoltageOutput.ChannelId.values());
	}

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsException {
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
	public Range range() {
		return new Range(OFFSET, PRECISION, MAXIMUM);
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
				new IocReadValueMapping<>(this.ioc::getAnalogOutVoltage,
						AnalogVoltageOutput.ChannelId.SET_OUTPUT_VOLTAGE),
				new IocReadValueMapping<>(this.ioc::getAnalogOutControl,
						IoMasterBox2v0Ao.ChannelId.ANALOG_OUT_CONTROL));
	}

	@Override
	public boolean hasIoc() {
		return this.ioc != null;
	}

	@Override
	public List<IocWriteValueMapping<?>> getWriteValueMappings() {

		ThrowingConsumer<Boolean, OpenemsNamedException> analogOutControlListener = value -> //
		setWriteValueIfNotRead(this.ioc.getAnalogOutControlChannel(), value);
		ThrowingConsumer<Integer, OpenemsNamedException> analogOutVoltageListener = value -> //
		setWriteValueIfNotRead(this.ioc.getAnalogOutVoltageChannel(), value);

		if (this.writeValueMappings.isEmpty()) {
			this.writeValueMappings = List.of(//
					new IocWriteValueMapping<>(analogOutControlListener, this::getAnalogOutControlChannel),
					new IocWriteValueMapping<>(analogOutVoltageListener, this::getSetOutputVoltageChannel) //
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
