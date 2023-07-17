package io.openems.edge.bridge.modbus.sunspec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.taskmanager.Priority;

/**
 * This class provides a generic implementation of SunSpec ModBus protocols.
 */
public abstract class AbstractOpenemsSunSpecComponent extends AbstractOpenemsModbusComponent {

	private final Logger log = LoggerFactory.getLogger(AbstractOpenemsSunSpecComponent.class);

	// The active SunSpec-Models and their reading-priority
	private final Map<SunSpecModel, Priority> activeModels;
	private final ModbusProtocol modbusProtocol;

	private int readFromCommonBlockNo = 1;
	private int commonBlockCounter = 0;

	private boolean isSunSpecInitializationCompleted = false;

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
	 * @throws OpenemsException on error
	 */
	public AbstractOpenemsSunSpecComponent(Map<SunSpecModel, Priority> activeModels,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) throws OpenemsException {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		this.activeModels = activeModels;
		this.modbusProtocol = new ModbusProtocol(this);
	}

	@Override
	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int unitId,
			ConfigurationAdmin cm, String modbusReference, String modbusId) {
		throw new IllegalArgumentException("Use the other activate() method.");
	}

	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int unitId,
			ConfigurationAdmin cm, String modbusReference, String modbusId, int readFromCommonBlockNo)
			throws OpenemsException {
		this.readFromCommonBlockNo = readFromCommonBlockNo;

		var expectedBlocks = this.activeModels.keySet().stream() //
				.map(SunSpecModel::getBlockId) //
				.collect(Collectors.toSet());

		// Start the SunSpec read procedure...
		this.isSunSpec().thenAccept(isSunSpec -> {
			if (!isSunSpec) {
				throw new IllegalArgumentException("This modbus device is not SunSpec!");
			}

			try {
				this.readNextBlock(40_002, expectedBlocks).thenRun(() -> {
					this.isSunSpecInitializationCompleted = true;
					this.onSunSpecInitializationCompleted();
				});

			} catch (OpenemsException e) {
				this.logWarn(this.log, "Error while reading SunSpec identifier block: " + e.getMessage());
				e.printStackTrace();
				this.isSunSpecInitializationCompleted = true;
				this.onSunSpecInitializationCompleted();
			}
		});
		return super.activate(context, id, alias, enabled, unitId, cm, modbusReference, modbusId);
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
	 * @throws OpenemsException on error
	 */
	private CompletableFuture<Boolean> isSunSpec() throws OpenemsException {
		final var result = new CompletableFuture<Boolean>();
		ModbusUtils.readELementOnce(this.modbusProtocol, new UnsignedDoublewordElement(40_000), true)
				.thenAccept(value -> {
					if (value == 0x53756e53) {
						result.complete(true);
					} else {
						result.complete(false);
					}
				});
		return result;
	}

	/**
	 * Reads the next SunSpec block.
	 *
	 * @param startAddress    the startAddress
	 * @param remainingBlocks the remaining blocks expected to read
	 * @return a future that completes once reading the block finished
	 * @throws OpenemsException on error
	 */
	private CompletableFuture<Void> readNextBlock(int startAddress, Set<Integer> remainingBlocks)
			throws OpenemsException {
		final var finished = new CompletableFuture<Void>();

		// Finish if all expected Blocks have been read
		if (remainingBlocks.isEmpty()) {
			finished.complete(null);
		}

		/*
		 * Try to read block by block until all required blocks have been read or an
		 * END_OF_MAP register has been found.
		 *
		 * It may still happen that a device does not have a valid END_OF_MAP register
		 * and that some blocks are not read - especially when one component is used for
		 * multiple devices like single and three phase inverter.
		 */
		this.readElementsOnceTyped(new UnsignedWordElement(startAddress), new UnsignedWordElement(startAddress + 1))
				.thenAccept(values -> {
					int blockId = values.get(0);

					// END_OF_MAP
					if (blockId == 0xFFFF) {
						finished.complete(null);
						return;
					}

					// Handle SunSpec Block
					if (blockId == 1 /* SunSpecModel.S_1 */) {
						this.commonBlockCounter++;
					}

					if (this.commonBlockCounter != this.readFromCommonBlockNo) {
						// ignore all SunSpec blocks before 'startFromCommonBlockNo' was passed

					} else {

						// Should this Block be considered?
						var activeEntry = this.getActiveModelForId(blockId);
						if (activeEntry != null) {
							var sunSpecModel = activeEntry.getKey();
							var priority = activeEntry.getValue();
							try {
								this.addBlock(startAddress, sunSpecModel, priority);
								remainingBlocks.remove(activeEntry.getKey().getBlockId());
							} catch (OpenemsException e) {
								this.logWarn(this.log, "Error while adding SunSpec-Model [" + blockId
										+ "] starting at [" + startAddress + "]: " + e.getMessage());
								e.printStackTrace();
							}

						} else {
							// This block is not considered, because the Model is not active
							this.logInfo(this.log,
									"Ignoring SunSpec-Model [" + blockId + "] starting at [" + startAddress + "]");
						}
					}

					// Stop reading if all expectedBlocks have been read
					if (remainingBlocks.isEmpty()) {
						finished.complete(null);
						return;
					}

					// Read next block recursively
					var nextBlockStartAddress = startAddress + 2 + values.get(1);
					try {

						final var readNextBlockFuture = this.readNextBlock(nextBlockStartAddress, remainingBlocks);
						// Announce finished when next block (recursively) is finished
						readNextBlockFuture.thenRun(() -> {
							finished.complete(null);
						});
					} catch (OpenemsException e) {
						this.logWarn(this.log, "Error while adding SunSpec-Model [" + blockId + "] starting at ["
								+ startAddress + "]: " + e.getMessage());
						e.printStackTrace();
						finished.complete(null); // announce finish immediately to not get stuck
					}

				});
		return finished;
	}

	/**
	 * Gets the Model and its reading priority; or null if the Model is not
	 * 'active', i.e. not used by this implementation.
	 *
	 * @param blockId the SunSpec Block-ID
	 * @return the entry with Model and priority
	 */
	private Entry<SunSpecModel, Priority> getActiveModelForId(int blockId) {
		for (Entry<SunSpecModel, Priority> entry : this.activeModels.entrySet()) {
			if (entry.getKey().getBlockId() == blockId) {
				return entry;
			}
		}
		return null;
	}

	/**
	 * Overwrite to provide custom SunSpecModel.
	 *
	 * @param blockId the Block-Id
	 * @return the {@link SunSpecModel}
	 * @throws IllegalArgumentException on error
	 */
	protected SunSpecModel getSunSpecModel(int blockId) throws IllegalArgumentException {
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
	public boolean isSunSpecInitializationCompleted() {
		return this.isSunSpecInitializationCompleted;
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
	 * @throws OpenemsException on error
	 */
	protected void addBlock(int startAddress, SunSpecModel model, Priority priority) throws OpenemsException {
		this.logInfo(this.log, "Adding SunSpec-Model [" + model.getBlockId() + ":" + model.label() + "] starting at ["
				+ startAddress + "]");
		var elements = new ModbusElement<?, ?>[model.points().length];
		startAddress += 2;
		for (var i = 0; i < model.points().length; i++) {
			var point = model.points()[i];
			var element = point.get().generateModbusElement(startAddress);
			startAddress += element.length;
			elements[i] = element;

			var channelId = point.getChannelId();
			this.addChannel(channelId);

			if (point.get().scaleFactor.isPresent()) {
				// This Point needs a ScaleFactor
				// - find the ScaleFactor-Point
				var scaleFactorName = SunSpecCodeGenerator.toUpperUnderscore(point.get().scaleFactor.get());
				SunSpecPoint scaleFactorPoint = null;
				for (var sfPoint : model.points()) {
					if (sfPoint.name().equals(scaleFactorName)) {
						scaleFactorPoint = sfPoint;
						break;
					}
				}
				if (scaleFactorPoint == null) {
					// Unable to find ScaleFactor-Point
					this.logError(this.log,
							"Unable to find ScaleFactor [" + scaleFactorName + "] for Point [" + point.name() + "]");
				}

				// Add a scale-factor mapping between Element and Channel
				element = this.m(channelId, element,
						new ElementToChannelScaleFactorConverter(this, point, scaleFactorPoint.getChannelId()));

			} else {
				// Add a direct mapping between Element and Channel
				element = this.m(channelId, element, new ElementToChannelConverter(
						// Element -> Channel
						value -> {
							if (!point.isDefined(value)) {
								// This value is set to be 'UNDEFINED' for the given type by SunSpec
								return null;
							}
							return value;
						},
						// Channel -> Element
						value -> value));

			}

			// Evaluate Access-Mode of the Channel
			switch (point.get().accessMode) {
			case READ_ONLY:
				// Read-Only -> replace element with dummy
				element = new DummyRegisterElement(element.getStartAddress(),
						element.getStartAddress() + point.get().type.length - 1);
				break;
			case READ_WRITE:
			case WRITE_ONLY:
				// Add a Write-Task
				// TODO create one FC16WriteRegistersTask for entire block
				final Task writeTask = new FC16WriteRegistersTask(element.getStartAddress(), element);
				this.modbusProtocol.addTask(writeTask);
				break;
			}
		}

		final Task readTask = new FC3ReadRegistersTask(elements[0].getStartAddress(), priority, elements);
		this.modbusProtocol.addTask(readTask);
	}

	/**
	 * Reads given Elements once from Modbus.
	 *
	 * @param <T>      the Type of the elements
	 * @param elements the elements
	 * @return a future list with the values, e.g. a list of integers
	 * @throws OpenemsException on error
	 */
	@SafeVarargs
	private final <T> CompletableFuture<List<T>> readElementsOnceTyped(ModbusElement<?, T>... elements)
			throws OpenemsException {
		// Register listeners for elements
		@SuppressWarnings("unchecked")
		final var subResults = (CompletableFuture<T>[]) new CompletableFuture<?>[elements.length];
		for (var i = 0; i < elements.length; i++) {
			var subResult = new CompletableFuture<T>();
			subResults[i] = subResult;

			var element = elements[i];
			element.onUpdateCallback(value -> {
				if (value == null) {
					// try again
					return;
				}
				subResult.complete(value);
			});
		}

		// Activate task
		final Task task = new FC3ReadRegistersTask(elements[0].getStartAddress(), Priority.HIGH, elements);
		this.modbusProtocol.addTask(task);

		// Prepare result
		final var result = new CompletableFuture<List<T>>();
		CompletableFuture.allOf(subResults).thenRun(() -> {
			// do not try again
			this.modbusProtocol.removeTask(task);

			// get all results and complete result
			List<T> values = Stream.of(subResults) //
					.map(CompletableFuture::join) //
					.collect(Collectors.toCollection(ArrayList::new));
			result.complete(values);
		});

		return result;
	}

	/**
	 * Get the Channel for the given Point.
	 *
	 * @param <T>   the Channel type
	 * @param point the SunSpec Point
	 * @return the optional Channel
	 */
	protected <T extends Channel<?>> Optional<T> getSunSpecChannel(SunSpecPoint point) {
		try {
			return Optional.ofNullable(this.channel(point.getChannelId()));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	/**
	 * Get the Channel for the given Point or throw an error if it is not available.
	 *
	 * @param <T>   the Channel type
	 * @param point the SunSpec Point
	 * @return the optional Channel
	 * @throws OpenemsException if Channel is not available
	 */
	@SuppressWarnings("unchecked")
	protected <T extends Channel<?>> T getSunSpecChannelOrError(SunSpecPoint point) throws OpenemsException {
		Optional<Channel<?>> channelOpt = this.getSunSpecChannel(point);
		if (!channelOpt.isPresent()) {
			throw new OpenemsException("SunSpec Channel for Point [" + point.getClass().getSimpleName() + "."
					+ point.name() + "] is not available");
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
				c.get().onUpdate(value -> {
					this.channel(targetChannel).setNextValue(converter.elementToChannel(value.get()));
				});
				return;
			}
		}
	}
}
