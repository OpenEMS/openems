package io.openems.edge.aithil;

import java.util.Map;

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
import org.osgi.service.metatype.annotations.Designate;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.sunspec.AbstractOpenemsSunSpecComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "AIT.HIL", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class AitHilImpl extends AbstractOpenemsSunSpecComponent implements AitHil, OpenemsComponent {

	private static final int UNIT_ID = 1;
	private static final int READ_FROM_MODBUS_BLOCK = 1;

	/**
	 * Active SunSpec models for AIT HIL Controller.
	 */
	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) //
			.put(DefaultSunSpecModel.S_103, Priority.LOW) //
			.put(DefaultSunSpecModel.S_120, Priority.LOW) //
			.put(DefaultSunSpecModel.S_121, Priority.LOW) //
			.put(DefaultSunSpecModel.S_122, Priority.LOW) //
			.put(DefaultSunSpecModel.S_123, Priority.LOW) //
			.put(AitSunSpecModel.S_134, Priority.LOW) //

//			.put(DefaultSunSpecModel.S_126, Priority.LOW) //
//			.put(DefaultSunSpecModel.S_132, Priority.LOW) //
//			.put(DefaultSunSpecModel.S_134, Priority.LOW) //
			.build();

	public AitHilImpl() throws OpenemsException {
		super(//
				ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				AitHil.ChannelId.values() //
		);
	}

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), UNIT_ID, this.cm, "Modbus",
				config.modbus_id(), READ_FROM_MODBUS_BLOCK)) {
			return;
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		if (super.modified(context, config.id(), config.alias(), config.enabled(), UNIT_ID, this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected void addBlock(int startAddress, SunSpecModel model, int length, Priority priority)
			throws OpenemsException {
		super.addBlock(startAddress, model, length, priority);
	}

	private boolean sunSpecInitializationCompleted = false;

	@Override
	protected synchronized void onSunSpecInitializationCompleted() {
		this.sunSpecInitializationCompleted = true;
	}

	private FreqWattCrv activeFreqWattCrv = null;

	@Override
	public void applyFreqWattCrv(FreqWattCrv freqWattCrv) throws OpenemsNamedException {
		if (!this.sunSpecInitializationCompleted) {
			// Wait for initialization
			return;
		}

		if (Objects.equal(this.activeFreqWattCrv, freqWattCrv)) {
			// FreqWattCrv is already active
			return;
		}
		this.activeFreqWattCrv = freqWattCrv;

		this.writeRegister(AitSunSpecModel.S134.ACT_CRV, 1);
		this.writeRegister(AitSunSpecModel.S134.MOD_ENA, freqWattCrv.enabled ? 0 : 1);

		// Number of active points in array.
		this.writeRegister(AitSunSpecModel.S134.ACT_PT, freqWattCrv.curve.length);

		for (int i = 0; i < freqWattCrv.curve.length; i++) {
			PByF pByF = freqWattCrv.curve[i];
			this.writeRegister(AitSunSpecModel.S134.valueOf("HZ" + (i + 1)), pByF.f);
			this.writeRegister(AitSunSpecModel.S134.valueOf("W" + (i + 1)), pByF.p);
		}
	}

	private void writeRegister(SunSpecPoint point, int value) throws OpenemsNamedException {
		IntegerWriteChannel channel = this.getSunSpecChannelOrError(point);
		channel.setNextWriteValue(value);
	}

	private void writeRegister(SunSpecPoint point, float value) throws OpenemsNamedException {
		FloatWriteChannel channel = this.getSunSpecChannelOrError(point);
		channel.setNextWriteValue(value);
	}

}
