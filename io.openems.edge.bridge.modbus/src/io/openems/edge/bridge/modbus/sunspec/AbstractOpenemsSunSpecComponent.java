package io.openems.edge.bridge.modbus.sunspec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModelUtils.Point;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModelUtils.SunSChannelId;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.taskmanager.Priority;

/**
 * This class provides a generic implementation of SunSpec ModBus protocols.
 */
public abstract class AbstractOpenemsSunSpecComponent extends AbstractOpenemsModbusComponent {

	private final ModbusProtocol modbusProtocol;

	public AbstractOpenemsSunSpecComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		this.modbusProtocol = new ModbusProtocol(this);
	}

	@Override
	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int unitId,
			ConfigurationAdmin cm, String modbusReference, String modbusId) {
		// Start the SunSpec read procedure...
		this.isSunSpec().thenAccept(isSunSpec -> {
			if (!isSunSpec) {
				throw new IllegalArgumentException("This modbus device is not SunSpec!");
			}

			this.readNextBlock(40_002).thenRun(() -> {
				this.onSunSpecInitializationCompleted();
			});
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
	 */
	private CompletableFuture<Boolean> isSunSpec() {
		final CompletableFuture<Boolean> result = new CompletableFuture<Boolean>();
		this.readELementOnce(new UnsignedDoublewordElement(40_000)).thenAccept(value -> {
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
	 * @param startAddress the startAddress
	 * @return a future that completes once reading the block finished
	 */
	private CompletableFuture<Void> readNextBlock(int startAddress) {
		final CompletableFuture<Void> finished = new CompletableFuture<Void>();
		this.readElementsOnceTyped(new UnsignedWordElement(startAddress), new UnsignedWordElement(startAddress + 1))
				.thenAccept(values -> {
					int blockId = values.get(0);

					// END_OF_MAP
					if (blockId == 0xFFFF) {
						finished.complete(null);
						return;
					}

					// Handle SunSpec Block
					int length = values.get(1);

					// Is this SunSpecModel block supported?
					SunSpecModel sunSpecModel = null;
					try {
						sunSpecModel = SunSpecModel.valueOf("S_" + blockId);
					} catch (IllegalArgumentException e) {
						// checked later
					}

					// Read block
					final CompletableFuture<Void> readBlockFuture;
					if (sunSpecModel != null) {
						readBlockFuture = this.addBlock(startAddress, sunSpecModel);
					} else {
						this.addUnknownBlock(startAddress, blockId);
						readBlockFuture = CompletableFuture.completedFuture(null);
					}

					// Read next block recursively
					int nextBlockStartAddress = startAddress + 2 + length;
					final CompletableFuture<Void> readNextBlockFuture = this.readNextBlock(nextBlockStartAddress);

					// Announce finished when this block and next block (recursively) are finished
					CompletableFuture.allOf(readBlockFuture, readNextBlockFuture).thenRun(() -> {
						finished.complete(null);
					});
				});
		return finished;
	}

	/**
	 * Adds a SunSpec block/model that cannot be handled automatically.
	 * 
	 * <p>
	 * The purpose of this method is to add one or more tasks (using
	 * this.modbusProtocol.addTask(task)) and map the elements to Channels.
	 * 
	 * @param startAddress   the startAddress of the block
	 * @param sunSpecBlockId the SunSpec block/model-ID
	 */
	protected abstract void addUnknownBlock(int startAddress, int sunSpecBlockId);

	/**
	 * This method is called after the SunSpec initialization was completed.
	 * 
	 * <p>
	 * The purpose of this method is to add mappings between SunSpec Channel-Points
	 * to OpenEMS Nature Channels.
	 */
	protected abstract void onSunSpecInitializationCompleted();

	/**
	 * Reads the block starting from startAddress.
	 * 
	 * @param startAddress the address to start reading from
	 * @param model        the SunSpecModel
	 */
	private CompletableFuture<Void> addBlock(int startAddress, SunSpecModel model) {
		final CompletableFuture<Void> finished = new CompletableFuture<Void>();
		AbstractModbusElement<?>[] elements = new AbstractModbusElement[model.points.length];
		startAddress += 2;
		for (int i = 0; i < model.points.length; i++) {
			Point point = model.points[i];
			AbstractModbusElement<?> element = point.get().generateModbusElement(startAddress);
			startAddress += element.getLength();
			elements[i] = element;
		}

		this.readElementsOnce(elements).thenAccept(values -> {
			/*
			 * Use results to prepare final Modbus Task
			 * 
			 * -> register Channels to defined SunSpec points
			 * 
			 * -> ignore non-defined SunSpec points with DummyElement
			 */
			for (int i = 0; i < values.size(); i++) {
				Point point = model.points[i];
				Object value = values.get(i);
				AbstractModbusElement<?> element = elements[i];
				if (point.isDefined(value)) {
					// Point is available -> create Channel
					SunSChannelId<?> channelId = point.getChannelId();
					this.addChannel(channelId);
					element = m(channelId, element);

				} else {
					// Point is not available -> replace element with dummy
					element = new DummyRegisterElement(element.getStartAddress(),
							element.getStartAddress() + point.get().type.length - 1);
				}
			}
			final Task task = new FC3ReadRegistersTask(elements[0].getStartAddress(), Priority.HIGH, elements);
			// TODO add WriteRegisterTask
			this.modbusProtocol.addTask(task);
			finished.complete(null);
		});
		return finished;
	}

	/**
	 * Reads given Element once from Modbus.
	 * 
	 * @param <T>      the Type of the element
	 * @param elements the elements
	 * @return a future value, e.g. a integer
	 */
	private <T> CompletableFuture<T> readELementOnce(AbstractModbusElement<T> element) {
		// Prepare result
		final CompletableFuture<T> result = new CompletableFuture<T>();

		// Activate task
		final Task task = new FC3ReadRegistersTask(element.getStartAddress(), Priority.HIGH, element);
		this.modbusProtocol.addTask(task);

		// Register listener for element
		element.onUpdateCallback(value -> {
			if (value == null) {
				// try again
				return;
			}
			// do not try again
			this.modbusProtocol.removeTask(task);
			result.complete(value);
		});

		return result;
	}

	/**
	 * Reads given Elements once from Modbus.
	 * 
	 * @param <T>      the Type of the elements
	 * @param elements the elements
	 * @return a future list with the values, e.g. a list of integers
	 */
	@SafeVarargs
	private final <T> CompletableFuture<List<T>> readElementsOnceTyped(AbstractModbusElement<T>... elements) {
		// Register listeners for elements
		@SuppressWarnings("unchecked")
		final CompletableFuture<T>[] subResults = (CompletableFuture<T>[]) new CompletableFuture<?>[elements.length];
		for (int i = 0; i < elements.length; i++) {
			CompletableFuture<T> subResult = new CompletableFuture<T>();
			subResults[i] = subResult;

			AbstractModbusElement<T> element = elements[i];
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
		final CompletableFuture<List<T>> result = new CompletableFuture<List<T>>();
		CompletableFuture.allOf(subResults).thenRun(() -> {
			// do not try again
			this.modbusProtocol.removeTask(task);

			// get all results and complete result
			List<T> values = Stream.of(subResults) //
					.map(future -> future.join()) //
					.collect(Collectors.toCollection(ArrayList::new));
			result.complete(values);
		});

		return result;
	}

	/**
	 * Reads given Elements once from Modbus. This is a non-generic version.
	 * 
	 * @param elements the elements
	 * @return a future list with the values as Object
	 */
	@SafeVarargs
	private final CompletableFuture<List<?>> readElementsOnce(AbstractModbusElement<?>... elements) {
		// Register listeners for elements
		final CompletableFuture<?>[] subResults = new CompletableFuture<?>[elements.length];
		for (int i = 0; i < elements.length; i++) {
			CompletableFuture<Object> subResult = new CompletableFuture<>();
			subResults[i] = subResult;

			AbstractModbusElement<?> element = elements[i];
			if (element instanceof DummyRegisterElement) {
				subResult.complete(null);
			} else {
				element.onUpdateCallback(value -> {
					if (value == null) {
						// try again
						return;
					}
					subResult.complete(value);
				});
			}
		}

		// Activate task
		final Task task = new FC3ReadRegistersTask(elements[0].getStartAddress(), Priority.HIGH, elements);
		this.modbusProtocol.addTask(task);

		// Prepare result
		final CompletableFuture<List<?>> result = new CompletableFuture<List<?>>();
		CompletableFuture.allOf(subResults).thenRun(() -> {
			// do not try again
			this.modbusProtocol.removeTask(task);

			// get all results and complete result
			List<?> values = Stream.of(subResults) //
					.map(future -> future.join()) //
					.collect(Collectors.toCollection(ArrayList::new));
			result.complete(values);
		});

		return result;
	}

	/**
	 * Get the Channel for the given Point.
	 * 
	 * @param point the SunSpec Point
	 * @return the optional Channel
	 */
	protected Optional<Channel<?>> getSunSpecChannel(Point point) {
		try {
			return Optional.ofNullable(this.channel(point.getChannelId()));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
