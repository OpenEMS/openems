package io.openems.edge.bridge.mqtt.manager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.bridge.mqtt.api.MqttTask;
import io.openems.edge.common.cycle.Cycle;
import org.joda.time.DateTimeZone;


abstract class AbstractMqttManager extends AbstractCycleWorker {
    //STRING = ID OF COMPONENT
    Map<String, List<MqttTask>> allTasks; //with ID
    List<MqttTask> toDoFuture = new ArrayList<>();
    List<MqttTask> currentToDo = new ArrayList<>();

    String mqttBroker;
    String mqttUsername;
    String mqttPassword;
    String mqttClientId;
    int keepAlive;
    DateTimeZone timeZone;
    int maxListLength = 30;
    //Counter for Qos --> e.g. QoS 0 has counter 10 --> FOR LIST FILL
    Map<Integer, AtomicInteger> counterForQos = new HashMap<>();
    //Time for QoS in mS
    Map<Integer, List<Long>> timeForQos;

    private long currentTime;

    private final List<Long> averageTime = new ArrayList<>();

    AbstractMqttManager(String mqttBroker, String mqttUsername, String mqttPassword,
                        String mqttClientId, int keepAlive, Map<String, List<MqttTask>> allTasks,
                        DateTimeZone timeZone) {

        this.mqttBroker = mqttBroker;
        this.mqttUsername = mqttUsername;
        this.mqttPassword = mqttPassword;
        this.mqttClientId = mqttClientId;
        this.keepAlive = keepAlive;
        this.allTasks = allTasks;
        this.timeForQos = new HashMap<>();
        for (int x = 0; x < 3; x++) {
            this.timeForQos.put(x, new ArrayList<>());
            this.counterForQos.put(x, new AtomicInteger(0));
            this.timeForQos.put(x, new ArrayList<>());
            this.timeForQos.get(x).add(0, (long) (x + 1) * 10);
        }
        this.timeZone = timeZone;
    }

    void foreverAbstract() {
        calculateAverageTimes();
        calculateCurrentTime();
        addToFutureAndCurrentToDo(sortTasks());
    }

    /**
     * Future Task List Publish Manager gets the CURRENT To do, while here's the list for future Tasks created.
     * <p>
     * Add all Sorted Tasks to the Future List.
     * While The Future Task List has an entry --> Get the first entry of FutureTasks and check if their QoS avg. Time is greater than the time left
     * If yes --> Don't add it and break the loop
     * otherwise add task to current To do and Remove from Future Task.
     * Repeat till there's no time left.
     *</p>
     * @param sortedTasks sorted Tasks by QoS 0 and the Priority (Bc QoS 0 will always be published) sorted Tasks are from method sortTasks()
     */
    private void addToFutureAndCurrentToDo(List<MqttTask> sortedTasks) {
        //Add at the End of Future
        if (sortedTasks != null) {
            this.toDoFuture.addAll(sortedTasks);
        }

        long timeLeft = Cycle.DEFAULT_CYCLE_TIME;
        while ((toDoFuture.size() > 0)) {
            MqttTask task = toDoFuture.get(0);
            timeLeft = timeLeft - averageTime.get(task.getQos());
            if (timeLeft < 0) {
                break;
            }
            currentToDo.add(task);
            toDoFuture.remove(0);
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
        collectionOfAllTasks.stream().filter(mqttTask -> mqttTask.getQos() == 0 && mqttTask.isReady(this.currentTime)).forEach(task -> this.currentToDo.add(task));
        //remove all added Tasks from all tasks that will be filtered now with the Priority
        this.currentToDo.forEach(collectionOfAllTasks::remove);
        if (collectionOfAllTasks.size() > 0) {
            return collectionOfAllTasks.stream().filter(mqttTask -> mqttTask.isReady(this.currentTime)).sorted(Comparator.comparing(MqttTask::getPriority)).collect(Collectors.toList());
        } else {
            return null;
        }
    }


    //EACH QoS has AverageTime except QoS 0

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
                addedTime /= value.size(); //either maxlength or <
                this.averageTime.add(key, addedTime);
                time.set(0);
            }
        });
    }


    void calculateCurrentTime() {
        this.currentTime = TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    long getCurrentTime() {
        return this.currentTime;
    }
}
