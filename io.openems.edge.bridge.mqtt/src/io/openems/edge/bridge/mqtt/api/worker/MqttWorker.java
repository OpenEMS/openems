package io.openems.edge.bridge.mqtt.api.worker;

import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_BROKER_UNAVAILABLE;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_CLOSED;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_DISCONNECTING;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_DISCONNECT_PROHIBITED;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_EXCEPTION;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_NOT_CONNECTED;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_TIMEOUT;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CONNECTION_LOST;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CONNECT_IN_PROGRESS;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_DISCONNECTED_BUFFER_FULL;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_FAILED_AUTHENTICATION;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_INVALID_CLIENT_ID;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_INVALID_MESSAGE;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_INVALID_PROTOCOL_VERSION;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_MAX_INFLIGHT;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_NOT_AUTHORIZED;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_NO_MESSAGE_IDS_AVAILABLE;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_SERVER_CONNECT_ERROR;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_SSL_CONFIG_ERROR;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_SUBSCRIBE_FAILED;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_TOKEN_INUSE;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_UNEXPECTED_ERROR;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_WRITE_TIMEOUT;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import io.openems.edge.bridge.mqtt.api.BridgeMqtt;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.api.MqttConnectionImpl;
import io.openems.edge.bridge.mqtt.api.MqttData;
import io.openems.edge.bridge.mqtt.api.MqttProtocol;
import io.openems.edge.bridge.mqtt.api.Topic;
import io.openems.edge.bridge.mqtt.api.task.MqttPublishTask;
import io.openems.edge.bridge.mqtt.api.task.MqttSubscribeTask;
import io.openems.edge.bridge.mqtt.api.task.MqttTask;
import io.openems.edge.bridge.mqtt.api.task.MqttWaitTask;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.worker.AbstractImmediateWorker;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.MetaTasksManager;
import io.openems.edge.common.taskmanager.TasksManager;
import io.openems.edge.common.timer.Timer;
import io.openems.edge.core.timer.TimerManager;

/**
 * The MqttWorker. This is the heart/logic part of the
 * {@link BridgeMqtt}. It manages the
 * MqttConnection, tasks, managing of tasks, updates the subscriptions, handles
 * subscribe/unsubscribe from the MqttBroker, Calculates the Time to handle all
 * MqttTasks etc. This Implementation orients itself to the ModbusWorker.
 * 
 */
public class MqttWorker extends AbstractImmediateWorker {
	private static final int AUTO_REVALIDATE_TIME_SECONDS = 300;
	private static final long TASK_DURATION_BUFFER = 50;
	private final Logger log = LoggerFactory.getLogger(MqttWorker.class);

	private final Stopwatch stopwatch = Stopwatch.createUnstarted();
	private final LinkedBlockingDeque<MqttTask> tasksQueue = new LinkedBlockingDeque<>();
	private final Deque<MqttTask> tempQueue = new LinkedList<>();

	private final MetaTasksManager<MqttSubscribeTask> subscribeTasksManager = new MetaTasksManager<>();
	private final MetaTasksManager<MqttPublishTask> publishTasksManager = new MetaTasksManager<>();
	private final List<Topic> subscribedTopics = new LinkedList<>();
	private final List<Topic> updatedTopics = new LinkedList<>();
	private final List<Topic> tempUnsubscribeTopics = new LinkedList<>();
	private final List<Topic> defectiveTopics = new LinkedList<>();
	private final BridgeMqtt parent;
	private final AtomicLong subDuration = new AtomicLong(0L);
	private final AtomicLong publishDuration = new AtomicLong(0L);
	protected MqttConnectionImpl mqttConnection = new MqttConnectionImpl(this);

	private boolean setInitialize = false;

	// The measured duration between BeforeProcessImage event and ExecuteWrite event
	private long durationBetweenBeforeProcessImageTillExecuteWrite = 0;
	private Timer timerRevalidate;

	public MqttWorker(BridgeMqtt parent) {
		this.parent = parent;
	}

	/**
	 * Setup for the MqttConnection and Setup for the Timer.
	 * 
	 * @param mqttData the wrapper class of the {@link MqttData}.
	 * @param tm       the {@link TimerManager} of the Parent.
	 */
	public void initialize(MqttData mqttData, TimerManager tm) {
		this.timerRevalidate = tm.getTimerByTime(AUTO_REVALIDATE_TIME_SECONDS);
		try {
			this.mqttConnection.createMqttConnection(mqttData.url(), //
					"OpenEMS-" + this.parent.alias() + "-" + this.parent.id(), //
					mqttData.user(), //
					mqttData.password(), //
					mqttData.userRequired(), //
					mqttData.mqttVersion()//
			);
			this.setInitialize = true;
		} catch (MqttException e) {
			this.analyzeReason(e);
		}
	}

	private void analyzeReason(MqttException e) {
		switch (e.getReasonCode()) {
		case REASON_CODE_FAILED_AUTHENTICATION -> this.parent._setAuthenticationFailed(true);
		case REASON_CODE_NOT_AUTHORIZED -> this.parent._setAccessDenied(true);
		default -> this.parent._setCommunicationFailed(true, "MQTT Error: " //
				+ "Code: " + e.getReasonCode() //
				+ " Reason: " + this.reasonCodeToString(e.getReasonCode()));
		}
	}

	/**
	 * Converts the {@link MqttException#getReasonCode()} to a readable text.
	 * 
	 * @param reasonCode the reasoncode.
	 * @return the Reason as a String.
	 */
	private String reasonCodeToString(int reasonCode) {
		return switch (reasonCode) {
		case REASON_CODE_CLIENT_EXCEPTION -> "Client Exception";
		case REASON_CODE_INVALID_PROTOCOL_VERSION -> "BrokerVersion Mismatch. Only Version 3.1.1 is supported";
		case REASON_CODE_INVALID_CLIENT_ID -> "Invalid ClientId -> please reactivated this component";
		case REASON_CODE_BROKER_UNAVAILABLE, REASON_CODE_SERVER_CONNECT_ERROR ->
			"Broker is unavailable, check URL, or try again later/Call Customer support of the Broker provider.";
		case REASON_CODE_FAILED_AUTHENTICATION ->
			"Authentication failed, please check Username and Password, check if \"userRequired\" is correctly set.";
		case REASON_CODE_NOT_AUTHORIZED ->
			"Not authorized. A Configuration tries to access a topic, that your client is not allowed to.";
		case REASON_CODE_SUBSCRIBE_FAILED ->
			"Subscription to a topic failed, check your Configuration or call the support of the Broker provider.";
		case REASON_CODE_CLIENT_TIMEOUT, REASON_CODE_WRITE_TIMEOUT, REASON_CODE_CLIENT_NOT_CONNECTED ->
			"Client Timeout, Please check your Connection and reactivate this component.";
		case REASON_CODE_NO_MESSAGE_IDS_AVAILABLE, REASON_CODE_TOKEN_INUSE ->
			"Internal Error, Please wait or reactivate this component.";
		// case REASON_CODE_CLIENT_CONNECTED -> ; shouldn't occur/ignore
		// case REASON_CODE_CLIENT_ALREADY_DISCONNECTED -> 32101; ignore already
		// disconnected
		case REASON_CODE_CLIENT_DISCONNECTING ->
			"Couldn't handle tasks, client is disconnected. Please reactivate the Bridge.";
		case REASON_CODE_SOCKET_FACTORY_MISMATCH -> "The provided Port is wrong, please check the Config.";
		case REASON_CODE_SSL_CONFIG_ERROR -> "SSL Error, please Update certificates of the System.";
		case REASON_CODE_CLIENT_DISCONNECT_PROHIBITED, REASON_CODE_CLIENT_CLOSED ->
			"Internal Error, restart the System.";
		case REASON_CODE_INVALID_MESSAGE -> "Invalid MQTT Package, please contact the Broker Provider";
		case REASON_CODE_CONNECTION_LOST -> "Unexpected disconnect";
		case REASON_CODE_CONNECT_IN_PROGRESS -> "Unexpected Connection attempt, please wait.";
		case REASON_CODE_MAX_INFLIGHT -> "Maximum parallel Requests, reduce MqttTasks, or reduce the PublishInterval";
		case REASON_CODE_DISCONNECTED_BUFFER_FULL ->
			"Full Buffer, please Reconnect the Bridge to the Broker or delete the Component.";
		default -> "Unknown reason. " + BridgeMqtt.DEFAULT_COMMUNICATION_FAILED_MESSAGE;
		};
	}

	/**
	 * Resets parent State Channel (when connect to the broker was successful).
	 */
	private void resetFailedStates() {
		this.parent._setAccessDenied(false);
		this.parent._setCommunicationFailed(false);
		this.parent._setAuthenticationFailed(false);
	}

	/**
	 * When connected to the Broker -> handle the {@link MqttTask}s. Otherwise, try
	 * to connect to the broker again.
	 * 
	 * @throws Throwable on unexpected/unhandled error.
	 */
	@Override
	protected void forever() throws Throwable {
		try {
			if (this.setInitialize) {
				this.setInitialize = false;
				this.mqttConnection.connect();
				// mqttConnection.
			}
		} catch (MqttException e) {
			this.analyzeReason(e);
		}
		if (this.mqttConnection.isConnected()) {
			if (this.timerRevalidate.checkAndReset()) {
				this.autoValidateTopics();

			}
			var task = this.tasksQueue.takeLast();

			// If there are no tasks in the bridge, there will always be only one
			// 'WaitTask'.
			if (task instanceof MqttWaitTask && !this.hasTasks()) {
				return;
			}
			var mqttComponent = task.getParent();
			try {
				var clock = this.parent.getComponentManager().getClock();
				var noOfExecutedSubTasks = task.execute(this.subscribedTopics, this.mqttConnection, clock);
				if (noOfExecutedSubTasks > 0) {
					if (mqttComponent != null) {
						mqttComponent._setMqttCommunicationFailed(false);
					}
				}
			} catch (OpenemsException e) {
				OpenemsComponent.logWarn(this.parent, this.log, task + " execution failed: " + e.getMessage());

				// mark this component as erroneous
				if (mqttComponent != null) {
					mqttComponent._setMqttCommunicationFailed(true);
				}

				// invalidate topic of this task
				var topic = task.getTopic();
				this.defectiveTopics.add(topic);
			}
		} else {
			// on reconnect (callback from mqttConnection) -> set to false
			//this.parent._setCommunicationFailed(true);
			this.subscribedTopics.forEach(topic -> topic.setSubscribed(false)); // mark every topic as not subscribed
			this.defectiveTopics.addAll(this.subscribedTopics);
			this.subscribedTopics.clear();
			if (this.timerRevalidate.checkAndReset()) {
				try {
					this.mqttConnection.disconnect();
				} catch (MqttException e) { // when already disconnected
				} finally { // try to reconnect when already disconnected
					try {
						this.mqttConnection.connect();
						this.autoValidateTopics();
					} catch (MqttException e) {
						this.analyzeReason(e);
					}
				}
			}
		}

	}

	/**
	 * After a certain amount of time, try to auto Validate Topics. E.g. if a topic
	 * needs to be subscribed but is not subscribed -> subscribe. If a topic needs
	 * to be unsubscribed -> unsubscribe.
	 */
	private void autoValidateTopics() {
		this.defectiveTopics.stream().filter(topic -> //
		topic.needsToBeSubscribed() //
				&& !topic.isSubscribed() //
		) //
				.forEach(this::subscribeToTopic);
		this.defectiveTopics.stream().filter(topic -> //
		!topic.needsToBeSubscribed() //
				&& topic.isSubscribed()) //
				.forEach(this::unsubscribe);
		this.defectiveTopics.removeIf(topic -> !topic.isSubscribed() && topic.needsToBeSubscribed());
	}

	/**
	 * Does this {@link MqttWorker} have any Tasks?.
	 *
	 * @return true if there are Tasks
	 */
	private boolean hasTasks() {
		return this.publishTasksManager.hasTasks() && this.subscribeTasksManager.hasTasks();
	}

	/**
	 * Adds the MqttProtocol and containing tasks to the publish/subscribeTask
	 * manager.
	 * 
	 * @param sourceId     the parent
	 *                     {@link MqttComponent}
	 * @param mqttProtocol the {@link MqttProtocol}.
	 */
	public void addProtocol(String sourceId, MqttProtocol mqttProtocol) {
		this.subscribeTasksManager.addTasksManager(sourceId, mqttProtocol.getSubscribeTaskManager());
		this.publishTasksManager.addTasksManager(sourceId, mqttProtocol.getPublishTaskManager());
		this.subscribeToTopic(mqttProtocol.getSubscribeTaskManager());
	}

	/**
	 * Subscribes to the topics given by the TaskManager (if not already
	 * subscribed).
	 * 
	 * @param subscribeTasks the subscribeTasks.
	 */
	private void subscribeToTopic(TasksManager<MqttSubscribeTask> subscribeTasks) {
		if (subscribeTasks != null) {
			subscribeTasks.getTasks().forEach(task -> this.subscribeToTopic(task.getTopic()));
		}
	}

	/**
	 * Subscribes to a Topic of the Broker, if not already subscribed.
	 * 
	 * @param topic the topic you want to subscribe to.
	 */
	private void subscribeToTopic(Topic topic) {
		if (!this.subscribedTopics.contains(topic)) {
			try {
				this.mqttConnection.subscribeToTopic(topic.getTopicName());
				topic.setSubscribed(true);
				this.subscribedTopics.add(topic);
				this.defectiveTopics.remove(topic);
			} catch (MqttException e) {
				if (e.getReasonCode() == REASON_CODE_SUBSCRIBE_FAILED) {
					this.defectiveTopics.add(topic);
				}
			}
		}
	}

	/**
	 * Removes MqttTasks and unsubscribes Topics if necessary.
	 * 
	 * @param sourceId the openems-componentId(source-Id)
	 */
	public void removeMqttProtocol(String sourceId) {
		this.publishTasksManager.removeTasksManager(sourceId);
		var tasksOfSource = this.subscribeTasksManager.getAllTasksBySourceId(sourceId);
		tasksOfSource.values().forEach(task -> this.tempUnsubscribeTopics.add(task.getTopic()));
		this.subscribeTasksManager.removeTasksManager(sourceId);
		var allSubTasks = this.subscribeTasksManager.getAllTasksBySourceId();
		allSubTasks.values().forEach(task -> this.tempUnsubscribeTopics.remove(task.getTopic()));
		this.tempUnsubscribeTopics.forEach(this::unsubscribe);
		this.tempUnsubscribeTopics.clear();
	}

	/**
	 * Calculate the duration between BeforeProcessImage event and ExecuteWrite
	 * event. This duration is used for planning the queue in
	 * onBeforeProcessImage().
	 */
	public synchronized void onExecuteWrite() {

		if (this.stopwatch.isRunning()) {
			this.durationBetweenBeforeProcessImageTillExecuteWrite = this.stopwatch.elapsed(TimeUnit.MILLISECONDS);
		} else {
			this.durationBetweenBeforeProcessImageTillExecuteWrite = 0;
		}
	}

	/**
	 * Prepare the Tasks Queue for this Cycle if empty.
	 */
	public void onBeforeProcessImage() {
		this.stopwatch.reset();
		this.stopwatch.start();
		// If the current tasks queue spans multiple cycles and we are in-between ->
		// stop here
		if (!this.tasksQueue.isEmpty()) {
			return;
		}

		List<MqttSubscribeTask> nextSubTasks = this.getAllSubscribeTasks();
		this.subDuration.set(0L);
		nextSubTasks.forEach(task -> this.subDuration.getAndAdd(task.getExecuteDuration()));
		this.publishDuration.set(0L);
		var nextPublishTasks = this.getAllPublishTasks();
		nextPublishTasks.forEach(task -> this.publishDuration.getAndAdd(task.getExecuteDuration()));

		var totalDuration = this.publishDuration.get() + this.subDuration.get();
		var totalDurationWithBuffer = totalDuration + TASK_DURATION_BUFFER;
		var cycleTime = this.parent.getCycle().getCycleTime();
		var noOfRequiredCycles = MqttTask.ceilDiv(totalDurationWithBuffer, cycleTime);
		// Set EXECUTION_DURATION channel
		this.parent._setExecutionDuration(totalDuration);

		// Set CYCLE_TIME_IS_TOO_SHORT state-channel
		this.parent._setCycleTimeIsTooShort(noOfRequiredCycles > 1);

		var durationOfTasksBeforeExecuteWriteEvent = 0L;
		var noOfTasksBeforeExecuteWriteEvent = 0;
		for (MqttSubscribeTask task : nextSubTasks) {
			if (durationOfTasksBeforeExecuteWriteEvent < this.durationBetweenBeforeProcessImageTillExecuteWrite) {
				noOfTasksBeforeExecuteWriteEvent++;
				durationOfTasksBeforeExecuteWriteEvent += task.getExecuteDuration();
			} else {
				break;
			}
		}
		this.tempQueue.clear();
		this.tempQueue.addAll(nextPublishTasks);

		for (var i = 0; i < nextSubTasks.size(); i++) {
			var task = nextSubTasks.get(i);
			if (i < noOfTasksBeforeExecuteWriteEvent) {
				// this Task will be executed before ExecuteWrite event -> add it to the end of
				this.tempQueue.addLast(task);
			} else {
				// this Task will be executed after ExecuteWrite event -> add it to the
				this.tempQueue.addFirst(task);
			}
		}
		// Add a waiting-task to the end of the queue
		var waitTillStart = noOfRequiredCycles * cycleTime - totalDurationWithBuffer;
		this.tempQueue.addLast(new MqttWaitTask(waitTillStart));
		this.tasksQueue.clear();
		this.tasksQueue.addAll(this.tempQueue);
	}

	private List<MqttSubscribeTask> getAllSubscribeTasks() {
		var tasks = this.subscribeTasksManager.getAllTasksBySourceId();
		this.updatedTopics.removeAll(this.defectiveTopics);
		return tasks.values().stream().filter(entry -> entry.hasTopic(this.updatedTopics)).collect(Collectors.toList());
	}

	private List<MqttPublishTask> getAllPublishTasks() {
		var tasks = this.publishTasksManager.getAllTasksBySourceId();
		return tasks.values().stream().filter(task -> !this.defectiveTopics.contains(task.getTopic())
				&& task.shouldBePublished(this.parent.getComponentManager().getClock())).toList();

	}

	/**
	 * Called by {@link #mqttConnection} to tell that the connection to broker was a
	 * success.
	 */
	public void notifySuccessConnection() {
		this.connectSuccess();
	}

	/**
	 * Sets State Channel of the {@link BridgeMqtt} parent.
	 */
	private void connectSuccess() {
		this.parent._setConnected(true);
		this.resetFailedStates();
	}

	/**
	 * Called by the {@link MqttConnectionImpl} when the connection to the broker
	 * was lost.
	 * 
	 * @param throwable the cause of the connection loss.
	 */
	public void connectionLost(Throwable throwable) {
		if (throwable instanceof MqttException) {
			this.analyzeReason((MqttException) throwable);
			this.defectiveTopics.addAll(this.subscribedTopics);
		}
	}

	/**
	 * Called by {@link #mqttConnection}. Is a Callback and updates Payloads of a
	 * Topic Object.
	 * 
	 * @param topic   the subscribed topic.
	 * @param payload the Mqtt Payload.
	 */
	public void updateTopicPayload(String topic, String payload) {
		var topicToGet = this.subscribedTopics.stream().filter(entry -> entry.getTopicName().equals(topic)).findFirst();
		topicToGet.ifPresent(topicObj -> {
			topicObj.getPayload().updatePayloadAfterCallback(payload);
			this.updatedTopics.add(topicObj);
			this.defectiveTopics.remove(topicObj);
		});

	}

	/**
	 * Unsubscribes the Topic.
	 * 
	 * @param unsubscribe the topic to unsubscribe.
	 */
	private void unsubscribe(Topic unsubscribe) {
		try {
			this.mqttConnection.unsubscribeFromTopic(unsubscribe.getTopicName());
		} catch (MqttException e) {
			if (e.getReasonCode() == REASON_CODE_UNEXPECTED_ERROR) {
				this.defectiveTopics.add(unsubscribe);
				unsubscribe.setNeedsToBeSubscribed(false);
			}
		}
		;
	}

	@Override
	public void deactivate() {
		try {
			this.mqttConnection.disconnect();
		} catch (MqttException e) {
			// ignore
		} finally {
			super.deactivate();

		}
	}
}
