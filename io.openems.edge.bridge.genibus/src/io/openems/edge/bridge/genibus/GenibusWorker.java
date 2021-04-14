package io.openems.edge.bridge.genibus;

import com.google.common.base.Stopwatch;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.bridge.genibus.api.PumpDevice;
import io.openems.edge.bridge.genibus.api.task.GenibusTask;
import io.openems.edge.bridge.genibus.api.task.GenibusWriteTask;
import io.openems.edge.bridge.genibus.protocol.ApplicationProgramDataUnit;
import io.openems.edge.bridge.genibus.protocol.Telegram;
import io.openems.edge.common.taskmanager.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

class GenibusWorker extends AbstractCycleWorker {

    private final Logger log = LoggerFactory.getLogger(GenibusWorker.class);
    private final Stopwatch cycleStopwatch = Stopwatch.createUnstarted();
    private final LinkedBlockingDeque<Telegram> telegramQueue = new LinkedBlockingDeque<>();
    private final ArrayList<PumpDevice> deviceList = new ArrayList<>();
    private int currentDeviceCounter = 0;
    private final GenibusImpl parent;
    private long cycleTimeMs = 1000;    // Start with 1000 ms, then measure the actual cycle time.
    private long lastExecutionMillis;


    protected GenibusWorker(GenibusImpl parent) {
        this.parent = parent;
    }

    @Override
    public void activate(String name) {
        super.activate(name);
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }


    // Creates a telegram for the current device (if possible) and increments the currentDeviceCounter (with a few exceptions).
    // The telegram is built from a task list of priority high, low and once tasks. This list is created by the module
    // that implements the pump device.
    // Picking tasks for the telegram works as follows: from the list of tasks, all high tasks are added to taskQueue.
    // Then a number of low tasks is added to tasksQueue. How many low tasks are added is defined by the pump device
    // module. Then as many tasks as possible are taken from tasksQueue and added to the telegram. Tasks added to the
    // telegram are removed from tasksQueue. If there are still tasks in taskQueue once the telegram is full, they
    // remain there and will be processed next time a telegram is created for this device. The queue is refilled as
    // needed and checks are in place to prevent the queue from getting too big.
    // All priority once tasks are added to the queue on the first run after the high tasks.
    //
    // If this method is called multiple times in a cycle for the same device, all possible tasks are only executed once.
    // Once the regularly scheduled tasks have been executed (high tasks + defined number of low tasks), a telegram with
    // the remaining (unscheduled) low tasks is created. Further calls of this method in the same cycle for the same
    // device will then not create a telegram but only switch to the next device (increase currentDeviceCounter).
    protected void createTelegram() {
        if (deviceList.isEmpty()) {
            if (parent.getDebug()) {
                this.parent.logInfo(this.log, "No devices registered for the GENIbus bridge.");
            }
            return;
        }
        PumpDevice currentDevice = deviceList.get(0);   // Only done so Intellij does not complain about "variable might not be initialized".

        // This handles the switching to the next pump. Selects the next pump in line, unless that pump is offline.
        for (int i = 0; i < deviceList.size(); i++) {
            if (deviceList.size() <= currentDeviceCounter) {
                currentDeviceCounter = 0;
            }
            currentDevice = deviceList.get(currentDeviceCounter);

            // The timeoutCounter is 0 when the device is responding. If the device has not responded 3 times in a row,
            // pump is most likely offline or wrong address. Wait 5 seconds before trying again, otherwise skip the pump.
            if (currentDevice.getTimeoutCounter() > 2) {
                long timeSinceLastTry = System.currentTimeMillis() - currentDevice.getTimestamp();
                if (timeSinceLastTry > 5000 || timeSinceLastTry < 0) {  // < 0 for overflow protection.
                    break;
                }
                currentDeviceCounter++;
            } else {
                break;
            }
        }

        // Remaining bytes that can be put in this telegram. More bytes will result in a buffer overflow in the device
        // and there will be no answer.
        // Calculated by getting the buffer length of the device and subtracting the telegram header (4 bytes) and the
        // crc (2 bytes).
        int telegramSendRemainingBytes = currentDevice.getDeviceReadBufferLengthBytes() - 6;

        // Remaining bytes that the answer telegram can have. You can cause an output buffer overflow in the device too.
        // This is achieved by sending lots of tasks that can have more bytes in the answer than in the request such as
        // INFO and ASCII. An INFO answer can be 1 or 4 bytes (for 8 bit tasks), an ASCII answer any amount of bytes.
        // Because of that, ASCII tasks are put in a telegram all by themselves. INFO tasks are assumed to have the
        // maximum byte count.
        // Calculated by getting the buffer length of the device and subtracting the telegram header (4 bytes) and the
        // crc (2 bytes).
        int telegramAnswerRemainingBytes = currentDevice.getDeviceSendBufferLengthBytes() - 6;

        // A timestamp is done on the first execution in a cycle (per device). So this is the time since the last first
        // execution. Used to track if more than one telegram is sent to the same device in a cycle.
        lastExecutionMillis = System.currentTimeMillis() - currentDevice.getTimestamp();
        // Rollover backup. Just in case this software actually runs that long...
        if (lastExecutionMillis < 0) {
            lastExecutionMillis = cycleTimeMs;
        }

        // If connection to pump is lost (pump turned off for example), send an empty telegram to test if the pump is
        // back online. If an answer is received, isConnectionOk() returns true next time and normal execution will resume.
        if (currentDevice.isConnectionOk() == false) {
            if (lastExecutionMillis > cycleTimeMs - 50) {
                currentDevice.setTimestamp();
                currentDevice.setFirstTelegram(true);
                if (parent.getDebug()) {
                    this.parent.logInfo(this.log, "Sending empty telegram to test if device " + currentDevice.getPumpDeviceId() + " is online.");
                }
                sendEmptyTelegram(currentDevice);
            }
            currentDeviceCounter++;
            return;
        }

        // Estimate how big the telegram can be to still fit in this cycle.
        long remainingCycleTime = cycleTimeMs - 60 - cycleStopwatch.elapsed(TimeUnit.MILLISECONDS);  // Include 60 ms buffer.
        if (remainingCycleTime < 0) {
            remainingCycleTime = 0;
        }
        // Each byte adds a certain time to the telegram execution length. The remaining time in the cycle can then be
        // expressed as "cycleRemainingBytes".
        // A telegram with no tasks takes ~33 ms to send and receive. Each additional byte in the telegrams (request and
        // answer) adds ~1 ms to that. We only calculate for the request bytes here, so divide by 2 as a good estimate.
        // A buffer of 60 ms was used in the time calculation, the 33 ms base time for a telegram is included in that buffer.
        int cycleRemainingBytes = (int) (remainingCycleTime / (currentDevice.getMillisecondsPerByte() * 2));
        if (cycleRemainingBytes < 5) {
            // Not enough time left. The telegram would be so small that it is not worth sending.
            // Exit the method without changing the device.
            if (parent.getDebug()) {
                this.parent.logInfo(this.log, "Not enough time left this cycle to send a telegram.");
            }
            return;
        }
        // Reduce telegram byte count if time is too short
        if (cycleRemainingBytes < telegramSendRemainingBytes) {
            // If this is the first telegram to that device this cycle and the telegram has a greatly reduced byte count
            // because of time constraints, don't switch to the next device. So the device is guaranteed to have a full
            // size telegram in the next cycle. This avoids one device being stuck at the end of the cycle and thus only
            // getting small telegram sizes.
            if (lastExecutionMillis > cycleTimeMs - 50 && cycleRemainingBytes / (telegramSendRemainingBytes * 1.0) < 0.3) {
                currentDeviceCounter--;
            }
            double divisor = cycleRemainingBytes / telegramSendRemainingBytes;
            telegramSendRemainingBytes = cycleRemainingBytes;
            telegramAnswerRemainingBytes = (int) Math.round(telegramAnswerRemainingBytes * divisor);
        }
        currentDeviceCounter++;

        fillTaskQueue(currentDevice, telegramSendRemainingBytes);

        // Get taskQueue from the device. Tasks that couldn't fit in the last telegram stayed in the queue.
        List<GenibusTask> tasksQueue = currentDevice.getTaskQueue();

        if (tasksQueue.isEmpty()) {
            // Don't send when it's broadcast.
            if (currentDevice.isEmptyTelegramSent() == false && currentDevice.getGenibusAddress() != 254) {
                currentDevice.setEmptyTelegramSent(true);
                sendEmptyTelegram(currentDevice);
                return;
            }

            // Nothing useful left to do. No tasks could be found that were not already executed this cycle.
            if (parent.getDebug()) {
                this.parent.logInfo(this.log, "No tasks left this cycle for pump number "
                        + currentDeviceCounter + ". Time since last timestamp: " + lastExecutionMillis + " ms.");
                // currentDeviceCounter has already been incremented at this point, but that is fine. First pump in the
                // list is now displayed as "pump number 1", while the deviceCounter number would be 0.
            }
            return;
        }

        if (parent.getDebug()) {
            this.parent.logInfo(this.log, "--Telegram Builder--");
            this.parent.logInfo(this.log, "Number of pumps in list: " + deviceList.size()
                    + ", current pump number: " + (currentDeviceCounter) + ", current pump id: " + currentDevice.getPumpDeviceId()
                    + ", GENIbus address: " + currentDevice.getGenibusAddress()
                    + ", Time since last timestamp: " + lastExecutionMillis + " ms.");
        }

        // This list contains all tasks for the current telegram. The key is a 3 digit decimal number called the
        // apduIdentifier. The 100 digit is the HeadClass of the apdu, the 10 digit is the operation
        // (0=get, 2=set, 3=info), the 1 digit is a counter starting at 0. The counter allows to have more than one apdu
        // of a given type. Since an apu (request and answer) is limited to 63 bytes, several apdu of the same type
        // might be needed to fit all tasks.
        Map<Integer, ArrayList<GenibusTask>> telegramTaskList = new HashMap<>();

        // Same as telegramTaskList, except this list contains the apdus with the tasks of telegramTaskList.
        Map<Integer, ApplicationProgramDataUnit> telegramApduList = new HashMap<>();

        if (parent.getDebug()) {
            this.parent.logInfo(this.log, "Bytes allowed: " + telegramSendRemainingBytes + ", task queue size: "
                    + tasksQueue.size() + ".");
            this.parent.logInfo(this.log, "Tasks are listed with \"apduIdentifier, address\". The apduIdentifier "
                    + "is a three digit number where the 100 digit is the HeadClass of the apdu, the 10 digit is the "
                    + "operation (0=get, 2=set, 3=info) and the 1 digit is a counter starting at 0 to track if more than "
                    + "one apdu of this type exists.");
        }

        // Need separate counter as start count of telegramAnswerRemainingBytes is dynamic.
        int answerByteCounter = 0;

        // This loop fills the telegramTaskList and telegramApduList with tasks and bytes. Here it is decided if a task is
        // executed as INFO, GET or SET. SET is not executed if there is no value in the write channel. When a SET task
        // is added to the telegram, the write channel is reset. Something needs to be written in the channel again
        // before SET is executed once more for this task.
        while (tasksQueue.isEmpty() == false) {
            // Get task from position 0.
            GenibusTask currentTask = tasksQueue.get(0);

            // Check how many bytes this task would add to the telegram. This check also decides if the task is INFO, GET
            // or SET and saves that decision as the apduIdentifier. It is also checked if the task needs to be executed
            // at all. If no, a 0 is returned.
            int sendByteSize = checkTaskByteSize(currentTask, telegramApduList);

            // Calculate how many bytes this task would add to the answer telegram.
            int answerBytes = checkAnswerByteSize(currentTask, sendByteSize);

            // When sendByteSize is 0, this task does nothing (for example a command task with no command to send). Skip this task.
            if (sendByteSize == 0) {
                // Remove this task from the queue.
                checkGetOrRemove(tasksQueue, 0);
                if (parent.getDebug()) {
                    this.parent.logInfo(this.log, "Skipping task: " + currentTask.getApduIdentifier() + ", "
                            + Byte.toUnsignedInt(currentTask.getAddress()) + ". Task queue size: " + tasksQueue.size());
                }
                continue;
            }

            // Check if there are enough bytes left in the telegram for this task.
            if (telegramSendRemainingBytes - sendByteSize >= 0
                    && telegramAnswerRemainingBytes - answerBytes >= 0) {
                // The task can be added. Update byte counter.
                telegramSendRemainingBytes = telegramSendRemainingBytes - sendByteSize;
                telegramAnswerRemainingBytes = telegramAnswerRemainingBytes - answerBytes;
                answerByteCounter += answerBytes;

                // Add the task to telegramTaskList and telegramApduList. This also creates the apdu if necessary and
                // writes the bytes into the apdu.
                addTaskToApdu(currentTask, telegramTaskList, telegramApduList);

                // Check if this task has SET and GET. SET is done first, then it is decided if this task is also GET.
                // If yes, the task is left in the queue and flagged for GET. In the next loop iteration it will be
                // added as a GET, removed from queue and the GET-flag reset.
                checkGetOrRemove(tasksQueue, 0);
                if (parent.getDebug()) {
                    this.parent.logInfo(this.log, "Adding task: " + currentTask.getApduIdentifier() + ", "
                            + Byte.toUnsignedInt(currentTask.getAddress()) + " - bytes added: " + sendByteSize + " - bytes remaining: "
                            + telegramSendRemainingBytes + " - Task queue size: " + tasksQueue.size());
                }
                if (telegramSendRemainingBytes == 0) {
                    // Telegramm is full. In case tasksQueue is not yet empty, need break to escape the loop.
                    break;
                }
                continue;
            }

            // You land here if the sendByteSize or answerBytes of the task is too big to fit. Leave that task in the queue.
            // If there is still room in the telegram, check the sendByteSize of the other tasks in the queue to find one
            // that might still fit. After that, exit the loop.
            if (telegramSendRemainingBytes >= 1 && telegramAnswerRemainingBytes >= 1) {
                for (int i = 1; i < tasksQueue.size(); i++) {
                    currentTask = tasksQueue.get(i);
                    sendByteSize = checkTaskByteSize(currentTask, telegramApduList);
                    answerBytes = checkAnswerByteSize(currentTask, sendByteSize);
                    if (sendByteSize != 0) {
                        if (telegramSendRemainingBytes - sendByteSize >= 0
                                && telegramAnswerRemainingBytes - answerBytes >= 0) {
                            telegramSendRemainingBytes = telegramSendRemainingBytes - sendByteSize;
                            telegramAnswerRemainingBytes = telegramAnswerRemainingBytes - answerBytes;
                            answerByteCounter += answerBytes;
                            addTaskToApdu(currentTask, telegramTaskList, telegramApduList);
                            checkGetOrRemove(tasksQueue, i);
                            if (parent.getDebug()) {
                                this.parent.logInfo(this.log, "Adding last small task: " + currentTask.getApduIdentifier() + ", "
                                        + Byte.toUnsignedInt(currentTask.getAddress()) + " - bytes added: " + sendByteSize + " - bytes remaining: "
                                        + telegramSendRemainingBytes + " - Task queue size: " + tasksQueue.size());
                            }
                            // Telegramm is full. In case tasksQueue is not yet empty, need break to escape the loop.
                            break;
                        }
                    }
                }
            }
            // Telegramm is full. In case tasksQueue is not yet empty, need break to escape the loop.
            break;
        }

        // Create the telegram and add it to the queue.
        Telegram telegram = new Telegram();
        telegram.setStartDelimiterDataRequest();
        telegram.setDestinationAddress(currentDevice.getGenibusAddress());
        telegram.setSourceAddress(0x01);
        telegram.setPumpDevice(currentDevice);

        // Upper limit estimate. Not the actual length. That is set once the answer telegram has been received. +2 for crc.
        telegram.setAnswerTelegramLength(answerByteCounter + 2);

        telegramApduList.forEach((key, apdu) -> {
            telegram.getProtocolDataUnit().putAPDU(apdu);
        });
        telegram.setTelegramTaskList(telegramTaskList);

        telegramQueue.add(telegram);
    }

    private void sendEmptyTelegram(PumpDevice currentDevice) {
        Telegram telegram = new Telegram();
        telegram.setStartDelimiterDataRequest();
        telegram.setDestinationAddress(currentDevice.getGenibusAddress());
        telegram.setSourceAddress(0x01);
        telegram.setPumpDevice(currentDevice);
        telegramQueue.add(telegram);
    }

    /**
     * This method fills the taskQueue of the pump device with tasks. Once per cycle, all high tasks are added (if the
     * queue does not already contain them). Then a configurable amount of low tasks are added as well. If it is the
     * first telegram sent to this device (true after a connection loss), then all priority once tasks are added too.
     * Using a timestamp, the method recognizes if it was already called this cycle for this device. If it is not the
     * first call, no tasks are added to the queue if the queue is not empty. If the queue is empty, the remaining low
     * tasks are added. The logic of the method should prevent a task from being executed more than once per cycle.
     * @param currentDevice
     * @param telegramRemainingBytes
     */
    private void fillTaskQueue(PumpDevice currentDevice, int telegramRemainingBytes) {
        List<GenibusTask> tasksQueue = currentDevice.getTaskQueue();

        // Priority low tasks are added with .getOneTask(), which starts from 0 when the end of the list is reached.
        // Make sure the number of low tasks added per telegram is not longer than the list to prevent adding a task twice.
        int lowTasksToAdd = currentDevice.getLowPrioTasksPerCycle();
        int numberOfLowTasks = currentDevice.getTaskManager().getAllTasks(Priority.LOW).size();
        if (lowTasksToAdd <= 0) {
            lowTasksToAdd = 1;
        }
        if (lowTasksToAdd > numberOfLowTasks) {
            lowTasksToAdd = numberOfLowTasks;
        }

        if (lastExecutionMillis > cycleTimeMs - 50 || currentDevice.isFirstTelegram()) {
            // Check if this was already executed this cycle. This part should execute once per cycle only.
            // isFirstTelegram is true when connection was lost and has just been reestablished.

            currentDevice.setTimestamp();
            currentDevice.setAllLowPrioTasksAdded(false);
            currentDevice.setFirstTelegram(false);
            currentDevice.setEmptyTelegramSent(false);

            // Check content of taskQueue. If length is longer than numberOfHighTasks + lowTasksToAdd (=number of tasks
            // this method would add), all high tasks are already in the queue and don't need to be added again.
            int numberOfHighTasks = currentDevice.getTaskManager().getAllTasks(Priority.HIGH).size();
            if (tasksQueue.size() <= numberOfHighTasks + lowTasksToAdd) {
                // Add all high tasks
                tasksQueue.addAll(currentDevice.getTaskManager().getAllTasks(Priority.HIGH));

                // Add all once tasks that need a second execution because the first execution was only INFO. This list
                // should be empty after the second or third cycle and won't be refilled.
                List<GenibusTask> onceTasksWithInfo = currentDevice.getOnceTasksWithInfo();
                for (int i = 0; i < onceTasksWithInfo.size(); i++) {
                    GenibusTask currentTask = onceTasksWithInfo.get(i);
                    if (currentTask.informationDataAvailable()) {
                        tasksQueue.add(currentTask);
                        onceTasksWithInfo.remove(i);
                        i--;
                    }
                }

                // Add all once Tasks. Only done on the first run or after a device reset.
                if (currentDevice.isAddAllOnceTasks()) {
                    currentDevice.getTaskManager().getAllTasks(Priority.ONCE).forEach(onceTask -> {
                        tasksQueue.add(onceTask);
                        // Tasks with INFO need two executions. First to get INFO, second for GET and/or SET.
                        switch (onceTask.getHeader()) {
                            case 2:
                            case 4:
                            case 5:
                                currentDevice.getOnceTasksWithInfo().add(onceTask);
                        }
                    });
                    currentDevice.setAddAllOnceTasks(false);
                }

                // Add a number of low tasks.
                for (int i = 0; i < lowTasksToAdd; i++) {
                    GenibusTask currentTask = currentDevice.getTaskManager().getOneTask(Priority.LOW);
                    if (currentTask == null) {
                        break;
                    } else {
                        tasksQueue.add(currentTask);
                    }
                }
            }
        } else {
            // This executes if a telegram was already sent to this pumpDevice in this cycle.
            // If the taskQueue is empty, fill the telegram with any remaining low priority tasks. If that was already
            // done this cycle, exit the method.
            if (tasksQueue.isEmpty()) {
                if (currentDevice.isAllLowPrioTasksAdded() == false) {
                    currentDevice.setAllLowPrioTasksAdded(true);
                    // The amount lowTasksToAdd was already added in the first telegram this cycle. The number of tasks
                    // before they repeat is then numberOfLowTasks - lowTasksToAdd.
                    int lowTaskFill = numberOfLowTasks - lowTasksToAdd;
                    // Compare that number with the bytes allowed in this telegram. We want to fill this telegram up
                    // with low tasks, but we don't want to add more tasks than can be sent with this telegram. Anything
                    // left in taskQueue is executed in the next telegram and reduces the space there for high tasks.
                    // Assume one task = one byte.
                    // The math is not exact but a good enough estimate. 1-2 low tasks remaining in taskQueue is not a big deal.
                    if ((telegramRemainingBytes - 2) < lowTaskFill) {    // -2 to account for at least one apdu header.
                        lowTaskFill = (telegramRemainingBytes - 2);
                    }

                    for (int i = 0; i < lowTaskFill; i++) {
                        GenibusTask currentTask = currentDevice.getTaskManager().getOneTask(Priority.LOW);
                        if (currentTask == null) {
                            // This should not happen, but checked just in case to prevent null pointer exception.
                            break;
                        } else {
                            tasksQueue.add(currentTask);
                        }
                    }
                }
            }
        }
    }

    // This is how GET for a writeTask (HeadClass 4 or 5) is handled. Checks if the task is a writeTask and if not the
    // task is removed. If it is a writeTask, it will set the sendGet boolean to true and leaves the task in the queue
    // so that it is added to a GET apdu. If sendGet is true that is reset to false and the task is removed from the
    // queue. That means every writeTask will perform a SET and a GET.
    private void checkGetOrRemove(List<GenibusTask> tasksQueue, int position) {
        GenibusTask currentTask = tasksQueue.get(position);
        if (currentTask instanceof GenibusWriteTask && currentTask.informationDataAvailable()) {

            // Do GET next cycle. sendGet is set to 2 by getRequest() in PumpWriteTask16bitOrMore. When SET is done for
            // a write task, do a GET the next cycle to update the channel by getting the value from the device.
            if (((GenibusWriteTask) currentTask).getSendGet() == 2) {
                ((GenibusWriteTask) currentTask).setSendGet(1);
                tasksQueue.remove(position);
                return;
            }
            // This task was executed as GET. Set GET to false (=0) and leave task in queue to see if SET needs to be done.
            if (((GenibusWriteTask) currentTask).getSendGet() == 1) {
                ((GenibusWriteTask) currentTask).setSendGet(0);
                return;
            }

            // This was code to do GET every time. Need to remove "sendGet = 2" in getRequest() in PumpWriteTask16bitOrMore
            // for this code to work as intended.
            /*
            if (((GenibusWriteTask) currentTask).getSendGet() == 0) {
                ((GenibusWriteTask) currentTask).setSendGet(1);
                return;
            } else {
                ((GenibusWriteTask) currentTask).setSendGet(0);
            }
            */
        }
        tasksQueue.remove(position);
    }

    // Returns how many bytes this task would need if it were added to the telegram. This means checking if a new apdu
    // is required or not, since a new apdu adds it's header of two bytes to the byte count. Since this method checks
    // in which apdu a task can be placed, the resulting key/identifier is saved to the task object so it can be used later
    // when the task is actually placed in an apdu.
    private int checkTaskByteSize(GenibusTask task, Map<Integer, ApplicationProgramDataUnit> telegramApduList) {
        int headClass = task.getHeader();
        int dataByteSize = task.getDataByteSize();
        switch (headClass) {
            case 0:
                task.setApduIdentifier(0);
                // HeadClass 0 only has three commands. No more than one HeadClass 0 apdu will exist, so don't need to
                // check if there is more than one or if apdu is full. Key is 0 since GET is 0.
                if (telegramApduList.containsKey(0)) {
                    return dataByteSize;
                }
                return 2 + dataByteSize;
            case 2:
                // Check if INFO has already been received. If yes this task is GET, if not this task is INFO.
                if (task.informationDataAvailable()) {
                    // See if an apdu exists and if yes check remaining space.
                    // Key is 2*100 for HeadClass 2, 0*10 for GET and 0 for first apdu.
                    return checkAvailableApdu(task, 200, 63, telegramApduList);
                } else {

                    // Task is INFO. Search for apdu with key 2*100 for HeadClass 2, 3*10 for INFO and 0 for first apdu.
                    // For INFO, return message can be 4 byte per task and max is 63. So 15 is the maximum number of
                    // tasks where overflow is guaranteed to be avoided. Assume one byte per task, so 15 bytes maximum
                    // in a request INFO apdu.
                    return checkAvailableApdu(task, 230, 15, telegramApduList);
                }
            case 3:
                // HeadClass 3 is commands. Commands are boolean where true = send and false = no send. For true,
                // getRequest() returns 1. HeadClass 3 does allow INFO, but it is not needed.
                if (task.getRequest(0, false) != 0) {
                    // Task is SET. Search for apdu with key 3*100 for HeadClass 3, 2*10 for SET and 0 for first apdu.
                    return checkAvailableApdu(task, 320, 63, telegramApduList);
                } else {
                    // Set apduIdentifier for debug info.
                    task.setApduIdentifier(320);
                }
                // False in channel, so no send = no bytes.
                return 0;
            case 4:
            case 5:
                // Check if INFO has already been received. INFO is needed for both GET and SET.
                if (task.informationDataAvailable()) {
                    if (task instanceof GenibusWriteTask) {
                        // Check if this is GET or SET.
                        if (((GenibusWriteTask) task).getSendGet() == 1) {
                            // Check apdu for GET. Key is HeadClass*100, 0*10 for GET and 0 for first apdu.
                            return checkAvailableApdu(task, headClass * 100, 63, telegramApduList);
                        } else {
                            // Task is SET. Check if there is a value to set. If not don't send anything.
                            int valueRequest = task.getRequest(0, false);  // This also works for 16bit. Will return something else than -256 in byte[0] if there is a value to set.
                            int setBytes = 0;
                            if (valueRequest > -256) {
                                // Check apdu for SET. Key is HeadClass*100, 2*10 for SET and 0 for first apdu.
                                setBytes = checkAvailableApdu(task, (headClass * 100) + 20, 63, telegramApduList);
                            } else {
                                // Set apduIdentifier for debug readout.
                                task.setApduIdentifier((headClass * 100) + 20);
                            }
                            return  setBytes;
                        }
                    } else {
                        this.parent.logError(this.log, "GENIbus error. Wrong headclass for task "
                                + task.getHeader() + ", " + task.getAddress() + ". Can't execute.");
                        return 0;
                    }
                } else {
                    // Task is INFO. Search for apdu with key 4*100 for HeadClass 4, 3*10 for INFO and 0 for first apdu.
                    // For INFO, return message can be 4 byte per task and max is 63. So 15 is the maximum number of
                    // tasks where overflow is guaranteed to be avoided. Assume one byte per task, so 15 bytes maximum
                    // in a request INFO apdu.
                    return checkAvailableApdu(task, (headClass * 100) + 30, 15, telegramApduList);
                }
            case 7:
                // HeadClass 7 is ASCII, which has only GET. Each ASCII task needs it's own apdu.
                // Check apdu for GET. Key is 700, 0*10 for GET and 0 for first apdu.
                // Further, the answer to an ASCII task can be so long that the send buffer of the device overflows.
                // So an ASCII should be sent in a telegram with nothing else in it. Add 61 to return byte count to make
                // enough space
                return checkAvailableApdu(task, 700, 1, telegramApduList) + 61;
        }

        return 0;
    }

    // Helper method for checkTaskByteSize()
    private int checkAvailableApdu(GenibusTask task, int key, int apduMaxBytes, Map<Integer, ApplicationProgramDataUnit> telegramApduList) {
        int dataByteSize = task.getDataByteSize();
        int nextFreeApdu = key;
        // nextFreeApdu is the key/identifier of the last existing apdu +1.
        while (telegramApduList.containsKey(nextFreeApdu)) {
            nextFreeApdu++;
        }
        // No apdu yet.
        if (nextFreeApdu == key) {
            task.setApduIdentifier(nextFreeApdu);
            return 2 + dataByteSize;
        }
        // Check remaining space in last apdu.
        int remainingBytes = apduMaxBytes - telegramApduList.get(nextFreeApdu - 1).getLength();
        if (remainingBytes >= dataByteSize) {
            task.setApduIdentifier(nextFreeApdu - 1);
            return dataByteSize;
        }
        task.setApduIdentifier(nextFreeApdu);
        return 2 + dataByteSize;
    }

    // Returns how many bytes this task would need in the answer telegram if it were added to the send telegram. This is
    // an upper estimate and not an accurate value. For example, INFO can be a 1 or 4 byte answer -> upper limit is 4 bytes.
    // Additional bytes needed for an apdu header are taken from "sendByteSize". If the request is in a new apdu, so is
    // the answer.
    private int checkAnswerByteSize(GenibusTask task, int sendByteSize) {
        int operationSpecifier = (task.getApduIdentifier() % 100) / 10;
        if (task.getHeader() == 7) {    // ASCII. Don't know how long that answer will be, 30 is a conservative guess.
            return 30;
        }
        switch (operationSpecifier) {
            case 0: // GET
                return sendByteSize;
            case 2: // SET
                return sendByteSize - task.getDataByteSize();   // >0 if it is a new apdu. Otherwise 0.
            case 3: // INFO
                return sendByteSize + 3;
        }
        return 0;
    }


    // Adds a task to the telegramTaskList and the telegramApduList.
    private void addTaskToApdu(GenibusTask currentTask, Map<Integer, ArrayList<GenibusTask>> telegramTaskList, Map<Integer, ApplicationProgramDataUnit> telegramApduList) {
        int apduIdentifier = currentTask.getApduIdentifier();

        // telegramApduList should have the same keys as telegramTaskList, so only need to check one.
        if (telegramTaskList.containsKey(apduIdentifier) == false) {
            // Create task list and apdu if they don't exist yet.
            ArrayList<GenibusTask> apduTaskList = new ArrayList<GenibusTask>();
            telegramTaskList.put(apduIdentifier, apduTaskList);
            ApplicationProgramDataUnit newApdu = new ApplicationProgramDataUnit();
            newApdu.setHeadClass(apduIdentifier / 100);
            newApdu.setHeadOSACK((apduIdentifier % 100) / 10);
            telegramApduList.put(apduIdentifier, newApdu);
        }

        // Add task to list.
        telegramTaskList.get(apduIdentifier).add(currentTask);

        // Write bytes in apdu. For tasks with more than 8 bit, put more than one byte in the apdu.
        for (int i = 0; i < currentTask.getDataByteSize(); i++) {
            telegramApduList.get(apduIdentifier).putDataField(currentTask.getAddress() + i);
            // Add write value for write task.
            switch (currentTask.getHeader()) {
                case 3:
                    // Reset channel write value to send command just once.
                    currentTask.getRequest(i, true);
                    break;
                case 4:
                case 5:
                    if (((apduIdentifier % 100) / 10) == 2) {
                        int valueRequest = currentTask.getRequest(i, true);
                        telegramApduList.get(apduIdentifier).putDataField((byte) valueRequest);
                    }
                    break;
            }
        }
    }


    /**
     * Checks if the telegram queue is empty and if yes, creates a telegram (if possible) and puts it in the queue.
     * If a telegram is in the queue, the connection is checked and the telegram is sent. If the connection is not ok,
     * the telegram will stay in the waiting queue until it can be sent. This is important to ensure priority once tasks
     * are actually executed, since they are only added to one telegram. So far there is no check to see if a priority
     * once task has actually been executed.
     *
     */
    @Override
    protected void forever() {
        if (this.cycleStopwatch.isRunning()) {
            cycleTimeMs = cycleStopwatch.elapsed(TimeUnit.MILLISECONDS);
            if (parent.getDebug()) {
                this.parent.logInfo(this.log, "Stopwatch 3: " + cycleStopwatch.elapsed(TimeUnit.MILLISECONDS));
            }
        }
        cycleStopwatch.reset();
        cycleStopwatch.start();

        if (this.telegramQueue.isEmpty()) {
            createTelegram();
        }

        while (this.telegramQueue.isEmpty() == false) {
            // Check connection.
            if (!parent.handler.checkStatus()) {
                // If checkStatus() returns false, the connection is lost. Try to reconnect
                parent.connectionOk = parent.handler.start(parent.portName);
                deviceList.forEach(pumpDevice -> {
                    if (parent.connectionOk == false) {
                        // Reset device in case pump was changed or restarted.
                        pumpDevice.setConnectionOk(false);
                        pumpDevice.resetDevice();
                    }
                });
            }
            if (parent.connectionOk) {
                try {
                    Telegram telegram = telegramQueue.takeLast();
                    long timeCounterTimestamp = cycleStopwatch.elapsed(TimeUnit.MILLISECONDS);
                    if (parent.getDebug()) {
                        this.parent.logInfo(this.log, "Stopwatch 1: " + cycleStopwatch.elapsed(TimeUnit.MILLISECONDS));
                    }
                    long cycletimeLeft = cycleTimeMs - timeCounterTimestamp;
                    parent.handleTelegram(telegram, cycletimeLeft);
                    if (parent.getDebug()) {
                        this.parent.logInfo(this.log, "Stopwatch 2: " + cycleStopwatch.elapsed(TimeUnit.MILLISECONDS));
                    }

                    // If the telegram was executed successfully, measure how long it took. Calculate time per byte and
                    // store it in the pump device. This value is later retrieved to check if a telegram for this pump
                    // could still fit into the remaining time of the cycle.
                    if (telegram.getPumpDevice().isConnectionOk()) {
                        long executionDuration = cycleStopwatch.elapsed(TimeUnit.MILLISECONDS) - timeCounterTimestamp;
                        int telegramByteLength = Byte.toUnsignedInt(telegram.getLength()) - 2 // Subtract crc
                                + telegram.getAnswerTelegramLength() - 2;
                        int emptyTelegramTime = telegram.getPumpDevice().getEmptyTelegramTime();
                        if (parent.getDebug()) {
                            this.parent.logInfo(this.log, "Estimated telegram execution time was: "
                                    + (emptyTelegramTime + telegramByteLength * telegram.getPumpDevice().getMillisecondsPerByte())
                                    + " ms. Actual time: " + executionDuration + " ms. Ms/byte = "
                                    + telegram.getPumpDevice().getMillisecondsPerByte());
                        }
                        telegram.getPumpDevice().setExecutionDuration(executionDuration);

                        // Check if the telegram is suitable for timing calculation. Calculation error gets bigger the
                        // smaller the telegram, so exclude tiny telegrams.
                        if (telegramByteLength > 10 && executionDuration > emptyTelegramTime) {
                            // Calculate "millisecondsPerByte", then store it in the pump device.
                            // Calculation: An empty telegram with no tasks takes ~33 ms to send and receive. When tasks
                            // are added to the telegram, the additional time is then proportional to the amount of bytes
                            // the tasks added (request and answer). "millisecondsPerByte" is the average amount of time
                            // each byte of a task adds to the telegram.
                            telegram.getPumpDevice().setMillisecondsPerByte((executionDuration - emptyTelegramTime) / (telegramByteLength * 1.0));
                        }
                        if (telegramByteLength == 0) {
                            telegram.getPumpDevice().setEmptyTelegramTime((int)executionDuration);
                            if (parent.getDebug()) {
                                this.parent.logInfo(this.log, "Empty Telegram detected. Updating emptyTelegramTime (currently "
                                        + emptyTelegramTime + " ms).");
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    this.parent.logWarn(this.log, "Couldn't get telegram. " + e);
                }

                // Check if there is enough time for another telegram. The telegram length is dynamic and adjusts depending
                // on time left in the cycle. A short telegram can be sent and received in ~50 ms.
                if (cycleStopwatch.elapsed(TimeUnit.MILLISECONDS) < cycleTimeMs - 100) {
                    // There should be enough time. Create the telegram. The createTelegram() method checks if a telegram
                    // has already been sent this cycle and will then only fill it with tasks that have not been executed
                    // this cycle. If all tasks were already executed, no telegram is created.
                    createTelegram();

                    // If no telegram was created and put in the queue (the device had nothing to send), check if the other
                    // devices still have tasks.
                    if (this.telegramQueue.isEmpty()) {
                        for (int i = 0; i < deviceList.size() - 1; i++) {   // "deviceList.size() - 1" because we already checked one device.
                            createTelegram();
                            if (this.telegramQueue.isEmpty() == false) {
                                // Exit for-loop if a telegram was created.
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public void addDevice(PumpDevice pumpDevice) {
        deviceList.add(pumpDevice);
    }

    public void removeDevice(String deviceId) {
        for (int counter = 0; counter < deviceList.size(); counter++) {
            if (deviceList.get(counter).getPumpDeviceId().equals(deviceId)) {
                deviceList.remove(counter);
                // decrease counter to not skip an entry.
                counter--;
            }
        }
    }
}
