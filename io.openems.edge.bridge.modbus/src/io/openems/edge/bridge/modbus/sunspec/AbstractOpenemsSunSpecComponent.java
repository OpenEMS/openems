package io.openems.edge.bridge.modbus.sunspec;

import static com.ghgande.j2mod.modbus.Modbus.ILLEGAL_ADDRESS_EXCEPTION;
import static io.openems.edge.bridge.modbus.api.ModbusUtils.readElementOnce;
import static io.openems.edge.bridge.modbus.api.ModbusUtils.readElementsOnce;
import static io.openems.edge.bridge.modbus.api.ModbusUtils.FunctionCode.FC3;
import static io.openems.edge.bridge.modbus.sunspec.Utils.toUpperUnderscore;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusSlaveException;
import com.google.common.collect.Lists;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ElementToChannelScaleFactorConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.AbstractTask;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task.ExecuteState;
import io.openems.edge.bridge.modbus.sunspec.Point.BitFieldPoint;
import io.openems.edge.bridge.modbus.sunspec.Point.BitFieldPoint.SunSpecBitPoint;
import io.openems.edge.bridge.modbus.sunspec.Point.ChannelIdPoint;
import io.openems.edge.bridge.modbus.sunspec.Point.EnumFieldPoint;
import io.openems.edge.bridge.modbus.sunspec.Point.ModbusElementPoint;
import io.openems.edge.bridge.modbus.sunspec.Point.ScaleFactorPoint;
import io.openems.edge.bridge.modbus.sunspec.Point.ScaledValuePoint;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.taskmanager.Priority;

/**
 * This class provides a generic implementation of SunSpec ModBus protocols.
 */
public abstract class AbstractOpenemsSunSpecComponent extends AbstractOpenemsModbusComponent {

	/**
	 * Limit of a task length in j2mod.
	 */
	private static final int MAXIMUM_TASK_LENGTH = 126;

	private final Logger log = LoggerFactory.getLogger(AbstractOpenemsSunSpecComponent.class);

	public record SunSpecModelEntry(SunSpecModel sunSpecModel, Priority priority, boolean required) {

		public static class Builder {
			private final SunSpecModel sunSpecModel;
			private Priority priority = Priority.LOW;
			private boolean required = false;

			public Builder(SunSpecModel sunSpecModel) {
				this.sunSpecModel = sunSpecModel;
			}

			public Builder setPriority(Priority priority) {
				this.priority = priority;
				return this;
			}

			/**
			 * Sets if the model is required.
			 * 
			 * <p>
			 * Changing this value does not have any effect on reading or initialization.
			 * It's only used for {@link #areRequiredModelsRead()} which can be used to
			 * trigger a reinitialization, but this has to happen in the component
			 * implementation. The default just prints a log which states that the
			 * initialization finished without all required models.
			 * </p>
			 * 
			 * @param required true if required; else false; default false
			 * @return this
			 */
			public Builder setRequired(boolean required) {
				this.required = required;
				return this;
			}

			public SunSpecModelEntry build() {
				return new SunSpecModelEntry(this.sunSpecModel, this.priority, this.required);
			}

		}

		/**
		 * Creates a builder for a {@link SunSpecModelEntry}.
		 * 
		 * @param sunSpecModel the {@link SunSpecModel}
		 * @return the builder
		 */
		public static Builder create(SunSpecModel sunSpecModel) {
			return new Builder(sunSpecModel);
		}

	}

	// The active SunSpec-Models and their reading-priority
	private final List<SunSpecModelEntry> activeModels;
	private final List<SunSpecModel> readModels = new CopyOnWriteArrayList<>();
	private final ModbusProtocol modbusProtocol;

	private int readFromCommonBlockNo = 1;

	private final AtomicBoolean isSunSpecInitializationCompleted = new AtomicBoolean(false);
	private CompletableFuture<Void> runningInitialization;

	/**
	 * Constructs a AbstractOpenemsSunSpecComponent.
	 *
	 * @param activeModels             the active SunSpec Models (i.e.
	 *                                 {@link SunSpecModel}) that should be
	 *                                 considered and their reading-priority
	 * @param firstInitialChannelIds   forwarded to
	 *                                 {@link AbstractOpenemsModbusComponent}
	 * @param furtherInitialChannelIds forwarded to
	 *                                 {@link AbstractOpenemsModbusComponent}
	 */
	public AbstractOpenemsSunSpecComponent(Map<SunSpecModel, Priority> activeModels,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		this(activeModels.entrySet().stream()
				.map(t -> SunSpecModelEntry.create(t.getKey()).setPriority(t.getValue()).build()).toList(),
				firstInitialChannelIds, furtherInitialChannelIds);
	}

	public AbstractOpenemsSunSpecComponent(List<SunSpecModelEntry> activeModels,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		this.activeModels = activeModels;
		this.modbusProtocol = new ModbusProtocol(this);
	}

	@Override
	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int unitId,
			ConfigurationAdmin cm, String modbusReference, String modbusId) {
		throw new IllegalArgumentException("Use the other activate() method.");
	}

	/**
	 * Make sure to call this method from the inheriting OSGi Component.
	 *
	 * @param context               ComponentContext of this component. Receive it
	 *                              from parameter for @Activate
	 * @param id                    ID of this component. Typically 'config.id()'
	 * @param alias                 Human-readable name of this Component. Typically
	 *                              'config.alias()'. Defaults to 'id' if empty
	 * @param enabled               Whether the component should be enabled.
	 *                              Typically 'config.enabled()'
	 * @param unitId                Unit-ID of the Modbus target
	 * @param cm                    An instance of ConfigurationAdmin. Receive it
	 *                              using @Reference
	 * @param modbusReference       The name of the @Reference setter method for the
	 *                              Modbus bridge - e.g. 'Modbus' if you have a
	 *                              setModbus()-method
	 * @param modbusId              The ID of the Modbus bridge. Typically
	 *                              'config.modbus_id()'
	 * @param readFromCommonBlockNo ignore all SunSpec blocks before
	 *                              'readFromCommonBlockNo' was passed
	 * @return true if the target filter was updated. You may use it to abort the
	 *         activate() method.
	 * @throws OpenemsException on error
	 */
	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int unitId,
			ConfigurationAdmin cm, String modbusReference, String modbusId, int readFromCommonBlockNo)
			throws OpenemsException {
		this.readFromCommonBlockNo = readFromCommonBlockNo;

		this.reinitializeSunSpecChannels(true);

		return super.activate(context, id, alias, enabled, unitId, cm, modbusReference, modbusId);
	}

	protected void reinitializeSunSpecChannels() {
		this.reinitializeSunSpecChannels(false);
	}

	private void reinitializeSunSpecChannels(boolean force) {
		final var success = this.isSunSpecInitializationCompleted.compareAndSet(true, false);
		if (!(success || force)) {
			this.log.debug("SunSpec initialization still running.");
			return;
		}

		final var runningInitialization = this.runningInitialization;
		if (runningInitialization != null) {
			runningInitialization.cancel(false);
		}

		this.logInfo(this.log, "Reinitialize SunSpec channels [force=" + force + "]");

		this.removeAllSunSpecChannels();
		this.readModels.clear();

		final var expectedBlocks = this.activeModels.stream() //
				.collect(toMap(t -> t.sunSpecModel().getBlockId(), Function.identity()));

		this.runningInitialization = this.isSunSpec().thenCompose(isSunSpec -> {
			if (!isSunSpec) {
				throw new IllegalArgumentException("This modbus device is not SunSpec!");
			}

			return this.readNextBlock(40_002, expectedBlocks, 0).whenComplete((result, error) -> {
				if (error != null) {
					this.log.error("Error during SunSpec initialization", error);
					return;
				}
				this.isSunSpecInitializationCompleted.set(true);
				this.onSunSpecInitializationCompleted();
			});
		});
	}

	private void removeAllSunSpecChannels() {
		for (var channel : new ArrayList<>(this.channels())) {
			if (channel.channelId() instanceof SunSChannelId) {
				if (this.removeChannel(channel)) {
					this.log.debug("Removed SunSpec channel {}", channel.channelId());
				}
			}
		}
	}

	protected boolean areRequiredModelsRead() {
		return this.activeModels.stream().allMatch(modelEntry -> {
			if (!modelEntry.required()) {
				return true;
			}

			return this.hasModelRead(modelEntry.sunSpecModel());
		});
	}

	protected boolean hasModelRead(SunSpecModel model) {
		return this.readModels.contains(model);
	}

	@Override
	protected final ModbusProtocol defineModbusProtocol() {
		return this.modbusProtocol;
	}

	/**
	 * Validates that this device complies to SunSpec specification.
	 *
	 * <p>
	 * Tests if first registers are 0x53756e53 ("SunS").
	 *
	 * @return a future true if it is SunSpec; otherwise false
	 */
	private CompletableFuture<Boolean> isSunSpec() {
		return readElementOnce(FC3, this.modbusProtocol, ModbusUtils::retryOnNull,
				new UnsignedDoublewordElement(40_000)) //
				.thenApply(v -> v == 0x53756e53);
	}

	/**
	 * Reads the next SunSpec block.
	 *
	 * @param startAddress       the startAddress
	 * @param remainingBlocks    the remaining blocks expected to read
	 * @param commonBlockCounter the number of already read common blocks
	 * @return a future that completes once reading the block finished
	 */
	private CompletableFuture<Void> readNextBlock(final int startAddress,
			final Map<Integer, SunSpecModelEntry> remainingBlocks, final int commonBlockCounter) {
		this.log.debug("Read next SunSpec value at " + startAddress + ", remainingBlocks=" + remainingBlocks);

		// Finish if all expected Blocks have been read
		if (remainingBlocks.isEmpty()) {
			return completedFuture(null);
		}

		/*
		 * Try to read block by block until all required blocks have been read or an
		 * END_OF_MAP register has been found or reading fails permanently.
		 *
		 * It may still happen that a device does not have a valid END_OF_MAP register
		 * and that some blocks are not read - especially when one component is used for
		 * multiple devices like single and three phase inverter.
		 */
		return readElementsOnce(FC3, this.modbusProtocol, //
				// Retry if value is null and error is not "Illegal Data Address".
				// Background: some SMA inverters do not provide an END_OF_MAP register.
				(executeState, value) -> {
					if (value != null) {
						return false; // do not retry
					}

					if (!this.areRequiredModelsRead()) {
						this.log.info("Could not read SunSpec value but required blocks are still missing.");
					}

					if (executeState instanceof ExecuteState.Error(Exception exception)) {
						if (exception instanceof ModbusSlaveException mse) {
							return !mse.isType(ILLEGAL_ADDRESS_EXCEPTION); // do not retry
						}
					}
					return true;
				}, //

				new UnsignedWordElement(startAddress), // Block-ID
				new UnsignedWordElement(startAddress + 1)) // Length of Block

				.thenCompose(rer -> {
					var blockCounter = commonBlockCounter;

					var values = rer.values();
					var blockId = values.get(0);

					this.log.debug("Read next SunSpec value at " + startAddress + ", blockId=" + blockId);
					// END_OF_MAP
					if (blockId == null || blockId == 0xFFFF) {
						if (!this.areRequiredModelsRead()) {
							this.log.warn(
									"Reached end of SunSpec but required blocks are still missing. Update Modbus protocol or remove required blocks.");
						}

						return completedFuture(null);
					}

					// Handle SunSpec Block
					if (blockId == 1 /* SunSpecModel.S_1 */) {
						blockCounter++;
					}

					if (blockCounter < this.readFromCommonBlockNo) {
						// ignore all SunSpec blocks before 'readFromCommonBlockNo' was passed

					} else {

						// Should this Block be considered?
						final var activeEntry = this.getActiveModelForId(blockId);
						if (activeEntry != null) {
							final var sunSpecModel = activeEntry.sunSpecModel();
							if (remainingBlocks.remove(sunSpecModel.getBlockId()) != null) {
								this.addBlock(startAddress, sunSpecModel, activeEntry.priority());
							} else {
								this.log.warn("Skip model {}; already added", sunSpecModel.getBlockId());
							}
						} else {
							// This block is not considered, because the Model is not active
							this.logInfo(this.log,
									"Ignoring SunSpec-Model [" + blockId + "] starting at [" + startAddress + "]");
						}

					}

					// Read next block recursively
					var nextBlockStartAddress = startAddress + 2 + values.get(1);

					// Announce finished when next block (recursively) is finished
					return this.readNextBlock(nextBlockStartAddress, remainingBlocks, blockCounter);
				});
	}

	/**
	 * Gets the Model and its reading priority; or null if the Model is not
	 * 'active', i.e. not used by this implementation.
	 *
	 * @param blockId the SunSpec Block-ID
	 * @return the entry with Model and priority
	 */
	private SunSpecModelEntry getActiveModelForId(int blockId) {
		return this.activeModels.stream() //
				.filter(t -> t.sunSpecModel().getBlockId() == blockId) //
				.findFirst().orElse(null);
	}

	/**
	 * Override to provide custom {@link SunSpecModel}.
	 *
	 * @param blockId the Block-Id
	 * @return the {@link SunSpecModel}
	 * @throws IllegalArgumentException on error
	 */
	protected SunSpecModel getSunSpecModel(int blockId) throws IllegalArgumentException {
		return null;
	}

	/**
	 * Override to provide custom {@link SunSpecBitPoint}s.
	 * 
	 * <p>
	 * Use this to create a different mapping for SunSpec BitFields, e.g. Model 103
	 * "Vendor defined events".
	 * 
	 * @param bfp the {@link BitFieldPoint}
	 * @return array of {@link SunSpecBitPoint}s; null or empty uses defaults from
	 *         {@link DefaultSunSpecModel}.
	 */
	protected SunSpecBitPoint[] getBitPoints(BitFieldPoint bfp) {
		return null;
	}

	/**
	 * Is the SunSpec initialization completed?.
	 *
	 * <p>
	 * If this returns true, all Channels are available.
	 *
	 * @return true if initialization is completed
	 */
	public final boolean isSunSpecInitializationCompleted() {
		return this.isSunSpecInitializationCompleted.get();
	}

	/**
	 * This method is called after the SunSpec initialization was completed.
	 *
	 * <p>
	 * The purpose of this method is to add mappings between SunSpec Channel-Points
	 * to OpenEMS Nature Channels.
	 */
	protected abstract void onSunSpecInitializationCompleted();

	/**
	 * Adds the block starting from startAddress.
	 *
	 * @param startAddress the address to start reading from
	 * @param model        the SunSpecModel
	 * @param priority     the reading priority
	 */
	protected void addBlock(int startAddress, SunSpecModel model, Priority priority) {
		this.logInfo(this.log, "Adding SunSpec-Model [" + model.getBlockId() + ":" + model.label() + "] starting at ["
				+ startAddress + "]");
		this.readModels.add(model);

		var readElements = new ArrayList<ModbusElement>();
		var writeElements = new ArrayList<ModbusElement>();
		startAddress += 2;
		for (var i = 0; i < model.points().length; i++) {
			var point = model.points()[i];
			var elements = this.addModbusElementAndChannels(startAddress, model, point);
			var length = elements.stream().mapToInt(e -> e.length).sum();

			// Handle AccessMode
			switch (point.get().accessMode) {
			case READ_ONLY -> {
				readElements.addAll(elements);
			}
			case READ_WRITE -> {
				readElements.addAll(elements);
				writeElements.addAll(elements);
			}
			case WRITE_ONLY -> {
				readElements.add(new DummyRegisterElement(startAddress, length));
				writeElements.addAll(elements);
			}
			}

			startAddress += length;
		}

		// Create Tasks and add them to the ModbusProtocol
		for (var elements : preprocessModbusElements(readElements)) {
			this.modbusProtocol.addTask(//
					new FC3ReadRegistersTask(//
							elements.get(0).startAddress, priority, elements.toArray(ModbusElement[]::new)));
		}
		for (var elements : preprocessModbusElements(writeElements)) {
			this.modbusProtocol.addTask(//
					new FC16WriteRegistersTask(//
							elements.get(0).startAddress, elements.toArray(ModbusElement[]::new)));
		}
	}

	/**
	 * Adds an Element of the block starting from startAddress and maps it to
	 * Channel(s).
	 *
	 * @param startAddress the address to start reading from
	 * @param model        the {@link SunSpecModel}
	 * @param ssp          the {@link SunSpecPoint}
	 * @return the added {@link ModbusElement}s
	 */
	protected List<ModbusElement> addModbusElementAndChannels(int startAddress, SunSpecModel model, SunSpecPoint ssp) {
		final var p = ssp.get();
		return switch (p) {
		case BitFieldPoint bfp -> {
			// Channels are added and mapped internally
			var alternativeBitPoints = this.getBitPoints(bfp);
			yield bfp.generateModbusElements(this, channelId -> this.addChannel(channelId), startAddress,
					alternativeBitPoints);
		}

		case EnumFieldPoint efp -> {
			final var points = efp.points;
			for (var point : points) {
				var channelId = point.getChannelId();
				this.addChannel(channelId);
			}
			final var uwe = efp.generateModbusElement(startAddress, t -> {
				for (var point : points) {
					try {
						final var channel = this.getSunSpecChannelOrError(point);
						channel.setNextValue(t.test(point));
					} catch (Exception e) {
						this.logWarn(this.log, "Missing SunSpec Channel for point [" + point + "]: " + e.getMessage());
					}
				}
			});
			yield List.of(uwe);
		}

		case ModbusElementPoint mep -> {
			final var element = mep.generateModbusElement(startAddress);
			if (p instanceof ChannelIdPoint cp) {
				var channelId = cp.channelId;
				this.addChannel(channelId);
				this.m(channelId, element, this.generateElementToChannelConverter(model, p));
			}
			yield List.of(element);
		}

		case ChannelIdPoint cip -> List.of();
		};
	}

	/**
	 * Converts a list of {@link ModbusElement}s to sublists, prepared for Modbus
	 * {@link AbstractTask}s.
	 * 
	 * <ul>
	 * <li>Sublists are without holes (i.e. nextStartAddress = currentStartAddress +
	 * Length + 1)
	 * <li>Length of sublist <= MAXIMUM_TASK_LENGTH
	 * </ul>
	 * 
	 * @param elements the source elements
	 * @return list of {@link ModbusElement} lists
	 */
	protected static List<List<ModbusElement>> preprocessModbusElements(List<ModbusElement> elements) {
		var result = Lists.<List<ModbusElement>>newArrayList(Lists.<ModbusElement>newArrayList());
		for (var element : elements) {
			// Get last sublist in result
			var l = result.get(result.size() - 1);
			// Get last element of sublist
			var e = l.isEmpty() ? null : l.get(l.size() - 1);
			if ((
			// Is first element of the sublist?
			e == null
					// Is element direct successor?
					|| e.startAddress + e.length == element.startAddress) //
					&& // Does element fit in task?
					l.stream().mapToInt(m -> m.length).sum() + element.length <= MAXIMUM_TASK_LENGTH //
			) {
				l.add(element); // Add to existing sublist

			} else {
				result.add(Lists.<ModbusElement>newArrayList(element)); // Create new sublist
			}
		}

		// Avoid length check for sublist
		if (result.get(0).isEmpty()) {
			return List.of();
		}
		return result;
	}

	/**
	 * Generates a {@link ElementToChannelConverter} for a Point.
	 * 
	 * <ul>
	 * <li>Check for UNDEFINED value as defined in SunSpec per Type specification
	 * <li>If a Scale-Factor is defined, try to add it - either as other point of
	 * model (e.g. "W_SF") or as static value converter
	 * </ul>
	 * 
	 * @param model the {@link SunSpecModel}
	 * @param point the {@link SunSpecPoint}
	 * @return an {@link ElementToChannelConverter}, never null
	 */
	protected ElementToChannelConverter generateElementToChannelConverter(SunSpecModel model, Point point) {
		// Create converter for 'defined' state
		final var valueIsDefinedConverter = new ElementToChannelConverter(//
				/* Element -> Channel */ value -> point.isDefined(value) ? value : null,
				/* Channel -> Element */ value -> value);

		// Generate Scale-Factor converter (possibly null)
		var scaleFactorConverter = switch (point) {
		case ScaledValuePoint svp -> {
			final var scaleFactorName = toUpperUnderscore(svp.scaleFactor);
			yield Stream.of(model.points()) //
					.filter(p -> p.name().equals(scaleFactorName)) //
					.map(SunSpecPoint::get) //
					.filter(ScaleFactorPoint.class::isInstance) //
					.map(ScaleFactorPoint.class::cast) //
					.map(sfp -> new ElementToChannelScaleFactorConverter(this, svp, sfp.channelId)) //
					// Found matching Scale-Factor Point in SunSpec Model
					.findFirst()

					// Else: try to parse constant Scale-Factor
					.orElseGet(() -> {
						try {
							return new ElementToChannelScaleFactorConverter(Integer.parseInt(svp.scaleFactor));
						} catch (NumberFormatException e) {
							// Unable to parse Scale-Factor to static value
							this.logError(this.log, "Unable to parse Scale-Factor [" + svp.scaleFactor + "] for Point ["
									+ point.name + "]");
							return null;
						}
					}); //
		}
		default -> null;
		};

		if (scaleFactorConverter != null) {
			return ElementToChannelConverter.chain(valueIsDefinedConverter, scaleFactorConverter);
		} else {
			return valueIsDefinedConverter;
		}
	}

	/**
	 * Get the Channel for the given Point.
	 *
	 * @param <T> the Channel type
	 * @param ssp the {@link SunSpecPoint}
	 * @return the optional {@link Channel}
	 */
	protected <T extends Channel<?>> Optional<T> getSunSpecChannel(SunSpecPoint ssp) {
		return switch (ssp.get()) {
		case ChannelIdPoint cp -> {
			try {
				yield Optional.ofNullable(this.channel(cp.channelId));
			} catch (IllegalArgumentException e) {
				yield Optional.empty();
			}
		}
		default -> Optional.empty();
		};
	}

	/**
	 * Get the Channel for the given Point or throw an error if it is not available.
	 *
	 * @param <T> the Channel type
	 * @param ssp the {@link SunSpecPoint}
	 * @return the optional {@link Channel}
	 * @throws OpenemsException if Channel is not available
	 */
	@SuppressWarnings("unchecked")
	protected <T extends Channel<?>> T getSunSpecChannelOrError(SunSpecPoint ssp) throws OpenemsException {
		Optional<Channel<?>> channelOpt = this.getSunSpecChannel(ssp);
		if (!channelOpt.isPresent()) {
			throw new OpenemsException("SunSpec Channel for Point [" + ssp.getClass().getSimpleName() + "." + ssp.name()
					+ "] is not available");
		}
		return (T) channelOpt.get();
	}

	/**
	 * Maps the first available SunSpec {@link SunSpecPoint} to the targetChannel.
	 *
	 * <p>
	 * The logic checks in order if a point is defined and uses that point.
	 *
	 * <p>
	 * Call this method only after all SunSpec models were completely read - i.e.
	 * onSunSpecInitializationCompleted()
	 *
	 * @param targetChannel the targetChannel
	 * @param converter     convert from Point value to the Unit of the Channel
	 * @param points        the points.
	 */
	protected void mapFirstPointToChannel(io.openems.edge.common.channel.ChannelId targetChannel,
			ElementToChannelConverter converter, SunSpecPoint... points) {
		for (SunSpecPoint point : points) {
			Optional<Channel<?>> c = this.getSunSpecChannel(point);
			if (c.isPresent()) {
				c.get().onSetNextValue(value -> {
					this.channel(targetChannel).setNextValue(converter.elementToChannel(value.get()));
				});
				return;
			}
		}
	}
}
