package io.openems.edge.bridge.mqtt.manager;

import io.openems.edge.bridge.mqtt.api.MqttTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.cycle.Cycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * This class provides the AbstractMqttManager. This class stores most important data of the Subscribe-/PublishManager,
 * as well as providing methods to tell the extending Manager, what tasks to handle in the current OpenEmsCycle.
 */
abstract class AbstractMqttManager {

    protected static final Logger log = LoggerFactory.getLogger(AbstractMqttManager.class);
    ComponentManager cpm;
    Cycle cycle;
    private boolean useCoreCycle = false;


    //STRING = ID OF COMPONENT
    Map<String, List<MqttTask>> allTasks; //with ID
    List<MqttTask> toDoFuture = new ArrayList<>();
    List<MqttTask> currentToDo = new ArrayList<>();

    String mqttBroker;
    String mqttUsername;
    String mqttPassword;
    String mqttClientId;
    int keepAlive;
    protected static final int MAX_LIST_LENGTH = 30;
    //Counter for Qos --> e.g. QoS 0 has counter 10 --> FOR LIST FILL
    Map<Integer, AtomicInteger> counterForQos = new HashMap<>();
    //Time for QoS in mS
    Map<Integer, List<Long>> timeForQos;

    private long currentTime;

    private final List<Long> averageTime = new ArrayList<>();

    AbstractMqttManager(String mqttBroker, String mqttUsername, String mqttPassword,
                        String mqttClientId, int keepAlive, Map<String, List<MqttTask>> allTasks) {

        this.mqttBroker = mqttBroker;
        this.mqttUsername = mqttUsername;
        this.mqttPassword = mqttPassword;
        this.mqttClientId = mqttClientId;
        this.keepAlive = keepAlive;
        this.allTasks = allTasks;
        this.timeForQos = new HashMap<>();
        //one List Entry for each QoS
        for (int x = 0; x < 3; x++) {
            this.timeForQos.put(x, new ArrayList<>());
            this.counterForQos.put(x, new AtomicInteger(0));
            this.timeForQos.put(x, new ArrayList<>());
            //just default timestamps that will be overwritten
            this.timeForQos.get(x).add(0, (long) (x + 1) * 10);
        }
    }

    void foreverAbstract() {
        this.calculateAverageTimes();
        this.calculateCurrentTime();
        this.addToFutureAndCurrentToDo(this.sortTasks());
    }

    /**
     * Future Task List Publish Manager gets the CURRENT To do, while here's the list for future Tasks created.
     * <p>
     * Add all Sorted Tasks to the Future List.
     * While The Future Task List has an entry --> Get the first entry of FutureTasks and check if their QoS avg. Time is greater than the time left
     * If yes --> Don't add it and break the loop
     * otherwise add task to current To do and Remove from Future Task.
     * Repeat till there's no time left.
     * </p>
     *
     * @param sortedTasks sorted Tasks by QoS 0 and the Priority (Bc QoS 0 will always be published) sorted Tasks are from method sortTasks()
     */
    private void addToFutureAndCurrentToDo(List<MqttTask> sortedTasks) {
        //Add at the End of Future
        if (sortedTasks != null) {
            this.toDoFuture.addAll(sortedTasks);
        }
        AtomicLong maxTime = new AtomicLong(Cycle.DEFAULT_CYCLE_TIME);

        if (this.useCoreCycle) {
            if (this.cycle == null) {
                this.cpm.getAllComponents().stream().filter(component -> component instanceof Cycle).findAny().ifPresent(component -> {
                            this.cycle = (Cycle) component;
                            maxTime.set(this.cycle.getCycleTime());
                        }
                );
            } else {
                maxTime.set(this.cycle.getCycleTime());
            }
        }
        long timeLeft = maxTime.get();
        while ((this.toDoFuture.size() > 0)) {
            MqttTask task = this.toDoFuture.get(0);
            timeLeft = timeLeft - this.averageTime.get(task.getQos());
            if (timeLeft < 0) {
                break;
            }
            this.currentToDo.add(task);
            this.toDoFuture.remove(0);
        }
    }

    /**
     * Sort The all Tasks this class has.
     * First get all QoS 0 Tasks (and if it's ready --> Cool Down time will be set by user for Each task in the Configuration)
     * After that all the other Tasks will be handled --> get all remaining "ready" tasks (depending on cool down time)
     * Sort them afterwards with the Priority (Configuration for each Task done by User)
     * Return the remaining collection as a List
     *
     * @return The sorted List or null of no QoS 1/2 is available.
     */

    private List<MqttTask> sortTasks() {
        List<MqttTask> collectionOfAllTasks = new ArrayList<>();
        this.allTasks.forEach((key, value) -> collectionOfAllTasks.addAll(value));
        //Add QoS 0 to CurrentToDo --> No Time Required
        collectionOfAllTasks.stream().filter(mqttTask -> mqttTask.getQos() == 0 && mqttTask.isReady(this.currentTime)).forEach(task ->
                this.currentToDo.add(task)
        );
        //remove all added Tasks from all tasks that will be filtered now with the Priority
        this.currentToDo.forEach(collectionOfAllTasks::remove);
        if (collectionOfAllTasks.size() > 0) {
            return collectionOfAllTasks.stream().filter(mqttTask -> mqttTask.isReady(this.currentTime)).sorted(Comparator.comparing(MqttTask::getPriority)).collect(Collectors.toList());
        } else {
            return null;
        }
    }


    //EACH QoS has AverageTime except QoS 0

    /**
     * Each QoS has an AverageTime except for QoS 0.
     * Calculate the avg. Time a Message needs to be completed. Used by the PublishManager.
     */
    private void calculateAverageTimes() {
        //for each Time of each QoS --> add and create Average
        AtomicLong time = new AtomicLong(0);
        this.timeForQos.forEach((key, value) -> {
            //Qos = 0 takes almost no time due to no ACK etc
            if (key == 0) {
                this.averageTime.add(key, (long) 0);
            } else {
                value.forEach(time::getAndAdd);
                long addedTime = time.get();
                addedTime /= value.size(); //either max length or <
                this.averageTime.add(key, addedTime);
                time.set(0);
            }
        });
    }

    /**
     * Gets the current Time an save it for 1 Cycle. (Used by extending manager).
     */
    void calculateCurrentTime() {
        this.currentTime = TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    /**
     * Get the current Time.
     *
     * @return this.currentTime
     */
    long getCurrentTime() {
        return this.currentTime;
    }

    /**
     * Sets the ComponentManager, usually called by MqttBridge.
     *
     * @param cpm the ComponentManager.
     */
    public void setComponentManager(ComponentManager cpm) {
        this.cpm = cpm;
    }

    /**
     * Sets the CoreCycle if configured. Usually called by the MqttBridge.
     *
     * @param setCoreCycle should the CoreCycle be set.
     */
    public void setCoreCycle(boolean setCoreCycle) {
        this.useCoreCycle = setCoreCycle;
    }

}
