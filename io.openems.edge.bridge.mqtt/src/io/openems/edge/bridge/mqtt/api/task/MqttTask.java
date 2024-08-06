package io.openems.edge.bridge.mqtt.api.task;

import java.time.Clock;
import java.util.Collection;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.api.MqttConnectionImpl;
import io.openems.edge.bridge.mqtt.api.MqttProtocol;
import io.openems.edge.bridge.mqtt.api.Topic;
import io.openems.edge.bridge.mqtt.api.worker.MqttWorker;
import io.openems.edge.common.taskmanager.ManagedTask;

public interface MqttTask extends ManagedTask {

	/**
	 * Getter for receiving the {@link Topic} object.
	 * 
	 * @return the {@link Topic}.
	 */

	Topic getTopic();

	/**
	 * Gets the execution duration of the last execution (successful or not
	 * successful) in [ms].
	 *
	 * @return the duration in [ms]
	 */
	long getExecuteDuration();

	/**
	 * This is a helper function. It calculates the opposite of Math.floorDiv().
	 *
	 * <p>
	 * Source: <a href=
	 * "https://stackoverflow.com/questions/27643616/ceil-conterpart-for-math-floordiv-in-java">StackOverflow</a>
	 *
	 * @param x the dividend
	 * @param y the divisor
	 * @return the result of the division, rounded up
	 */
	static long ceilDiv(long x, long y) {
		return -Math.floorDiv(-x, y);
	}

	/**
	 * This is the Execute-Method, used by the {@link MqttWorker}. This tells the
	 * {@link MqttPublishTask} to build its current Payload and send it to the
	 * broker. Where as {@link MqttSubscribeTask}s update the corresponding
	 * OpenEMSComponent Channel with the received values.
	 * 
	 * @param topics         the Topics that are updated by the Broker.
	 * @param mqttConnection the {@link MqttConnectionImpl} to allow publish of
	 *                       tasks.
	 * @param clock          the Clock used by {@link MqttPublishTask} to set a new
	 *                       publish time.
	 * @return the number of executed Tasks (usually 1)
	 * @throws OpenemsException if a fail occurs.
	 */
	int execute(Collection<Topic> topics, MqttConnectionImpl mqttConnection, Clock clock) throws OpenemsException;

	/**
	 * Internal Method, usually used by {@link MqttPublishTask} and
	 * {@link MqttWaitTask}, since they don't need updated Topics to handle their
	 * tasks.
	 * 
	 * @param mqttConnection the {@link MqttConnectionImpl} to allow publish of
	 *                       tasks.
	 * @param clock          the Clock used by {@link MqttPublishTask} to set a new
	 *                       publish time.
	 * @return the number of executed Tasks (usually 1)
	 * @throws OpenemsException if a fail occurs.
	 */
	int _execute(MqttConnectionImpl mqttConnection, Clock clock) throws OpenemsException;

	/**
	 * The {@link MqttComponent} Parent of this Tasks. This Parent holds a reference
	 * to the other OpenEmsComponent, that is published/updated via subscription.
	 * 
	 * @return the {@link MqttComponent} parent.
	 */

	MqttComponent getParent();

	/**
	 * Internal Method to set the Parent component. This is usually called by the
	 * {@link MqttProtocol}.
	 * 
	 * @param parent the {@link MqttComponent} parent.
	 */
	void _setParent(MqttComponent parent);

	/**
	 * Method that is called when the Parent is deactivated.
	 */
	void deactivate();
}
