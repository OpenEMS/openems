package io.openems.edge.bridge.rest.communcation;

import io.openems.edge.bridge.rest.communcation.api.RestBridge;
import io.openems.edge.bridge.rest.communcation.api.RestReadRequest;
import io.openems.edge.bridge.rest.communcation.api.RestRequest;
import io.openems.edge.bridge.rest.communcation.api.RestWriteRequest;
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

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

@Designate(ocd = Config.class, factory = true)
@Component(name = "Bridge.Rest",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE})
public class RestBridgeImpl extends AbstractOpenemsComponent implements RestBridge, OpenemsComponent, EventHandler {

    private final Map<String, RestRequest> tasks = new ConcurrentHashMap<>();
    private final Map<String, RestRequest> readTasks = new ConcurrentHashMap<>();
    private final Map<String, RestRequest> writeTasks = new ConcurrentHashMap<>();
    private String loginData;
    private String ipAddressAndPort;
    private int keepAlive;
    AtomicBoolean connectionOk = new AtomicBoolean(true);
    DateTime initialDateTime;
    private boolean initialDateTimeSet = false;

    public RestBridgeImpl() {
        super(OpenemsComponent.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        if (config.enabled()) {
            this.loginData = "Basic " + Base64.getEncoder().encodeToString((config.username() + ":" + config.password()).getBytes());
            this.ipAddressAndPort = config.ipAddress() + ":" + config.port();
            this.keepAlive = config.keepAlive();
        }
    }


    @Deactivate
    public void deactivate() {
        super.deactivate();
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

    @Override
    public void removeRestRemoteDevice(String deviceId) {
        this.tasks.remove(deviceId);
        this.writeTasks.remove(deviceId);
        this.readTasks.remove(deviceId);
    }

    @Override
    public RestRequest getRemoteRequest(String id) {
        return this.tasks.get(id);
    }

    @Override
    public Map<String, RestRequest> getAllRequests() {
        return this.tasks;
    }

    @Override
    public boolean connectionOk() {
        return this.connectionOk.get();
    }

    /**
     * handles PostRequests called by the CycleWorker.
     *
     * @param entry the RestWriteRequest given by the CycleWorker. from this.tasks
     *              <p>
     *              Creates URL and if ReadyToWrite (can be changed via Interface) && isAudoadapt --> AutoAdaptRequest.
     *              AutoAdaptRequests is only necessary if Device is a Relays. --> IsCloser will be asked.
     *              Bc Opener and Closer have Inverse Logic. A Closer is Normally Open and an Opener is NormallyClosed,
     *              Therefor Changes in Relays needs to be Adapted. "ON" means true with closer but false with opener and
     *              vice versa.
     *              </p>
     * @throws IOException Bc of URL and connection.
     */

    private void handlePostRequest(RestWriteRequest entry) throws IOException {
        URL url = new URL("http://" + this.ipAddressAndPort + "/rest/channel/" + entry.getRequest());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", this.loginData);

        if (entry.readyToWrite()) {
            if (!entry.unitWasSet()) {
                handleUnitGet(entry, connection);
            }
            String msg = entry.getPostMessage();
            if (msg.equals("NoValueDefined") || msg.equals("NotReadyToWrite")) {
                return;
            }
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            OutputStream os = connection.getOutputStream();
            os.write(msg.getBytes());
            os.flush();
            os.close();
            //Task can check if everythings ok --> good for Controller etc; ---> Check Channel
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                entry.wasSuccess(true, entry.getDeviceId() + entry.getPostMessage());
            } else {
                entry.wasSuccess(false, "POST NOT WORKED");
            }
        }
    }

    /**
     * Handles UnitGet for entry.
     *
     * @param entry      the RestRequest from tasks. Usually called within forever Method --> handlePostRequest.
     * @param connection the Connection usually parsed by the handlePostRequest.
     * @throws IOException due to URL and response etc.
     *                     <p>
     *                     This gets the Unit for a POST Request by Setting Request to GET and split the answer to UNIT --> Auto unit setting.
     *                     </p>
     */

    private void handleUnitGet(RestWriteRequest entry, HttpURLConnection connection) throws IOException {
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
            entry.setUnit(true, response.toString());
            //---------------------//
        } else {
            entry.setUnit(false, "ERROR WITH CONNECTION");
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

    private void checkConnections() {
        AtomicBoolean connectionOkThisRun = new AtomicBoolean(true);
        this.tasks.forEach((key, value) -> {
            if (connectionOkThisRun.get()) {
                connectionOkThisRun.set(this.checkConnection(value));
            }
        });
        this.connectionOk.set(connectionOkThisRun.get());
    }

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


    @Override
    public void handleEvent(Event event) {
        switch (event.getTopic()) {
            case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
                if (this.initialDateTimeSet == false) {
                    this.initialDateTime = new DateTime();
                    this.initialDateTimeSet = true;
                } else {
                    DateTime now = new DateTime();
                    if (now.isAfter(initialDateTime.plusSeconds(this.keepAlive))) {
                        //only one connection read is necessary bc it was previously checked before.
                        //so check if connection ok
                        this.readTasks.keySet().stream().findAny().ifPresent(key -> this.connectionOk.set(this.checkConnection(this.readTasks.get(key))));
                    }
                }
                if (connectionOk.get()) {
                    taskRoutine(RestRoutineType.READ);
                }
                break;

            case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
                if (this.connectionOk.get()) {
                    taskRoutine(RestRoutineType.WRITE);
                }
                break;

        }
    }

    private void taskRoutine(RestRoutineType readOrWrite) {

        switch (readOrWrite) {
            case READ:
                this.readTasks.forEach((key, entry) -> {
                    try {
                        handleReadRequest((RestReadRequest) entry);
                    } catch (IOException e) {
                        this.connectionOk.set(false);
                    }
                });
                break;
            case WRITE:
                this.writeTasks.forEach((key, entry) -> {
                    try {
                        handlePostRequest((RestWriteRequest) entry);
                    } catch (IOException e) {
                        this.connectionOk.set(false);
                    }
                    ((RestWriteRequest) entry).nextValueSet();
                });
                break;
        }

    }

}
