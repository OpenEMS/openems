package io.openems.edge.bridge.communication.remote.rest;

import io.openems.edge.bridge.communication.remote.rest.api.RestBridge;
import io.openems.edge.bridge.communication.remote.rest.api.RestReadRequest;
import io.openems.edge.bridge.communication.remote.rest.api.RestRequest;
import io.openems.edge.bridge.communication.remote.rest.api.RestWriteRequest;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import org.joda.time.DateTime;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides a RestBridge. This Bridge Communicates with another System running a different OpenEMS Edge / it's components via the Rest Protocol.
 * The reason why it has only the 'Basic' Authentication method, is bc it was designed to communicate only with different OpenEMS Edges.
 * Rest Remote Devices need to be configured and mapped to the remote OpenEMS.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Bridge.Rest",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE})
public class RestBridgeImpl extends AbstractOpenemsComponent implements RestBridge, OpenemsComponent, EventHandler {
    private final Logger log = LoggerFactory.getLogger(RestBridgeImpl.class);

    private final Map<String, RestRequest> tasks = new ConcurrentHashMap<>();
    private final Map<String, RestRequest> readTasks = new ConcurrentHashMap<>();
    private final Map<String, RestRequest> writeTasks = new ConcurrentHashMap<>();
    private String loginData;
    private String ipAddressAndPort;
    private int keepAlive;
    private int dutyTime;
    private DateTime initialDutyTime;
    private boolean initialDutyTimeSet;
    private boolean useDutyTime;
    AtomicBoolean connectionOk = new AtomicBoolean(true);
    DateTime initialDateTime;
    private boolean initialDateTimeSet = false;

    public RestBridgeImpl() {
        super(OpenemsComponent.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        if (config.enabled()) {
            this.loginData = "Basic " + Base64.getEncoder().encodeToString((config.username() + ":" + config.password()).getBytes());
            this.ipAddressAndPort = config.ipAddress() + ":" + config.port();
            this.keepAlive = config.keepAlive();
            this.dutyTime = config.dutyTime();
            this.useDutyTime = config.useDutyTime();
        }
    }

    @Modified
    void modified(ComponentContext context, Config config) {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.loginData = "Basic " + Base64.getEncoder().encodeToString((config.username() + ":" + config.password()).getBytes());
        this.ipAddressAndPort = config.ipAddress() + ":" + config.port();
        this.keepAlive = config.keepAlive();
        this.dutyTime = config.dutyTime() >= 0 ? config.dutyTime() : 0;
        this.useDutyTime = config.useDutyTime();
    }

    /**
     * Check the Connection. If it's ok, read / get Data in Before Process Image,
     * otherwise write into Channel in Execute Write.
     *
     * @param event the Event, either BeforeProcessImage or Execute Write
     */
    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled() == false || (this.useDutyTime && this.timeToHandleEvent() == false)) {
            return;
        }
        this.initialDutyTimeSet = false;
        switch (event.getTopic()) {
            case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
                if (this.initialDateTimeSet == false) {
                    this.initialDateTime = new DateTime();
                    this.initialDateTimeSet = true;
                } else {
                    DateTime now = new DateTime();
                    if (now.isAfter(this.initialDateTime.plusSeconds(this.keepAlive))) {
                        //only one connection read is necessary bc it was previously checked before.
                        //so check if connection ok
                        this.readTasks.keySet().stream().findAny().ifPresent(key -> this.connectionOk.set(this.checkConnection(this.readTasks.get(key))));
                    }
                }
                if (this.connectionOk.get()) {
                    this.taskRoutine(RestRoutineType.READ);
                }
                break;

            case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
                if (this.connectionOk.get()) {
                    this.taskRoutine(RestRoutineType.WRITE);
                }
                break;

        }

    }

    /**
     * Checks if it's time to POST/GET REST Request.
     *
     * @return true if configured Time is up.
     */

    private boolean timeToHandleEvent() {
        if (this.initialDutyTimeSet == false) {
            this.initialDutyTime = new DateTime();
            this.initialDutyTimeSet = true;
            return false;
        }
        return DateTime.now().isAfter(this.initialDutyTime.plusMillis(this.dutyTime));
    }

    /**
     * Check if the Connection is ok with given Id and Channel.
     *
     * @param value any Task.
     * @return responseCode == HTTP_OK
     */
    private boolean checkConnection(RestRequest value) {
        try {
            URL url = new URL("http://" + this.ipAddressAndPort + "/rest/channel/" + value.getRequest());

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Authorization", this.loginData);

            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * This method reads/writes from the RestRequests depending on the Event.
     *
     * @param readOrWrite RestRoutineType, usually defined by RestBridge and the handleEvent.
     */
    private void taskRoutine(RestRoutineType readOrWrite) {

        switch (readOrWrite) {
            case READ:
                this.readTasks.forEach((key, entry) -> {
                    try {
                        this.handleReadRequest((RestReadRequest) entry);
                    } catch (IOException e) {
                        this.connectionOk.set(false);
                    }
                });
                break;
            case WRITE:
                this.writeTasks.forEach((key, entry) -> {
                    try {
                        this.handlePostRequest((RestWriteRequest) entry);
                    } catch (IOException e) {
                        this.connectionOk.set(false);
                    }
                });
                break;
        }

    }

    /**
     * Handles PostRequests called by the CycleWorker.
     *
     * @param entry the RestWriteRequest given by the CycleWorker. from this.tasks
     *              <p>
     *              Creates URL and if ReadyToWrite (can be changed via Interface) && isAudoadapt --> AutoAdaptRequest.
     *              AutoAdaptRequests is only necessary if Device is a Relays. --> IsCloser will be asked.
     *              Bc Opener and Closer have Inverse Logic. A Closer is Normally Open and an Opener is NormallyClosed,
     *              Therefore Changes in Relays needs to be Adapted. "ON" means true with closer but false with opener and
     *              vice versa.
     *              </p>
     * @throws IOException Bc of URL and connection.
     */

    private void handlePostRequest(RestWriteRequest entry) throws IOException {
        URL url = new URL("http://" + this.ipAddressAndPort + "/rest/channel/" + entry.getRequest());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", this.loginData);

        if (entry.allowedToWrite()) {
            String msg = entry.getPostMessage();
            if (msg.equals("NoValueDefined") || msg.equals("NotReadyToWrite")) {
                return;
            }
            if (msg.equals("ChannelNotAvailable")) {
                this.log.warn("Channel for: " + entry.getDeviceId() + " is not available");
            }
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            OutputStream os = connection.getOutputStream();
            os.write(msg.getBytes());
            os.flush();
            os.close();
            //Task can check if everything's ok --> good for Controller etc; ---> Check Channel
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                entry.wasSuccess(true, entry.getDeviceId() + entry.getPostMessage());
            } else {
                entry.wasSuccess(false, "REST POST DID NOT WORK FOR ENTRY: " + entry.getDeviceId());
            }
        }
    }

    /**
     * Gets a RestRequest and creates the GET Rest Method.
     *
     * @param entry entry the RestWriteRequest given by the CycleWorker. from this.tasks
     * @throws IOException bc of URL requests etc.
     *                     <p>
     *                     Gets a Request via Cycleworker. Creates the URL and reacts if HTTP_OK is true
     *                     If that's the case, the response will be set to entry.
     *                     </p>
     */
    private void handleReadRequest(RestReadRequest entry) throws IOException {
        URL url = new URL("http://" + this.ipAddressAndPort + "/rest/channel/" + entry.getRequest());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", this.loginData);

        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            String readLine;
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));

            StringBuilder response = new StringBuilder();
            while ((readLine = in.readLine()) != null) {
                response.append(readLine);
            }
            in.close();
            //---------------------//
            entry.setResponse(true, response.toString());
            //---------------------//
        } else {
            entry.setResponse(false, "ERROR WITH CONNECTION");
        }
    }

    /**
     * Is the Connection OK (Test Get request) Not ideal but it works.
     *
     * @return a boolean if connection is Ok.
     */

    @Override
    public boolean connectionOk() {
        return this.connectionOk.get();
    }

    /**
     * Adds the RestRequest to the tasks map.
     *
     * @param id      identifier == remote device Id usually from Remote Device config
     * @param request the RestRequest created by the Remote Device.
     * @throws ConfigurationException if the id is already in the Map.
     */
    @Override
    public void addRestRequest(String id, RestRequest request) throws ConfigurationException {

        if (this.tasks.containsKey(id)) {
            throw new ConfigurationException(id, "Already in RemoteTasks Check your UniqueId please.");
        }
        if (request instanceof RestWriteRequest) {
            this.writeTasks.put(id, request);
        } else if (request instanceof RestReadRequest) {
            this.readTasks.put(id, request);
        }
        this.tasks.put(id, request);

        boolean connectOk = this.checkConnection(request);
        if (connectOk == false) {
            throw new ConfigurationException("Internet Or Path Wrong for RemoteDevice", id);
        }
    }

    /**
     * Removes a Remote device from the Bridge.
     * Usually called by RestRemote Component on deactivation or when the Bridge itself deactivates.
     *
     * @param deviceId the deviceId to Remove.
     */
    @Override
    public void removeRestRemoteDevice(String deviceId) {
        this.tasks.remove(deviceId);
        this.writeTasks.remove(deviceId);
        this.readTasks.remove(deviceId);
    }


    /**
     * Deactivates the Component.
     */
    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

}
