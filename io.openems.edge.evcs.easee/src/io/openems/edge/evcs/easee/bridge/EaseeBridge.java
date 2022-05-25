package io.openems.edge.evcs.easee.bridge;

import io.openems.edge.common.timer.TimerByTime;
import io.openems.edge.common.timer.TimerType;
import io.openems.edge.evcs.api.GridVoltage;
import io.openems.edge.evcs.easee.EaseeImpl;
import io.openems.edge.core.timer.TimerHandler;
import io.openems.edge.core.timer.TimerHandlerImpl;
import org.osgi.service.component.annotations.Reference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
    @SuppressWarnings("unused")
/**
 * This Bridge will handle the communication with the Easee Cloud with its proprietary API.
 * https://developer.easee.cloud/docs/web-api-overview
 */

public class EaseeBridge {

    @Reference
    TimerByTime timerByTime;

    private String chargerSerial;
    private String chargerId;
    private String circuitId;
    private String siteId;
    private EaseeImpl parent;
    private String username;
    private String password;
    private TimerHandler timer;
    private static final String DOWN_TIME = "Ping";

	private boolean charging;

    private boolean connected;
    private String accessToken;
    private String refreshToken;
    private int expiration;
    private String authorizationHeader;

    private int lastChargingCurrent;


    private static final  String BASE_URL = "https://api.easee.cloud/api";
    private static final String AUTHORIZATION_URL = BASE_URL + "/accounts/token";
    private static final String REFRESH_URL = BASE_URL + "/accounts/refresh_token";
    private String chargeUrl;
    private String powerUrl;
    private final String resumeUrl;
    private final String pauseUrl;
    private boolean firstRun = true;
    private Boolean lastPing = true;
    private int counter;
    private float l1 = 0.f;
    private float l2 = 0.f;
    private float l3 = 0.f;

    public EaseeBridge(EaseeImpl parent, String chargerSerial, String chargerId, String circuitId, String siteId, String username, String password) {
        this.parent = parent;
        this.chargerId = chargerId;
        this.chargerSerial = chargerSerial;
        this.circuitId = circuitId;
        this.siteId = siteId;
        this.username = username;
        this.password = password;
        this.connected = this.getAccessToken();
        this.resumeUrl = BASE_URL + "/chargers/" + this.chargerId + "/commands/resume_charging";
        this.pauseUrl = BASE_URL + "/chargers/" + this.chargerId + "/commands/pause_charging";
        this.chargeUrl = BASE_URL + "/sites/" + this.siteId + "/circuits/" + this.circuitId + "/dynamicCurrent";
        this.powerUrl = BASE_URL + "/chargers/" + this.chargerSerial + "/state";
        try {
            this.timer = new TimerHandlerImpl(this.parent.getSuperId(), timerByTime);
            this.timer.addOneIdentifier(DOWN_TIME, TimerType.TIME, 10);
        } catch (Exception e) {
            //This should not happen
        }
    }

    /**
     * Executes the Bridge Routine. Should only be called by the EaseeImpl.
     *
     * @return true if connected to cloud
     */
    public boolean run() {
        if (!this.connected) {
            this.connected = this.getAccessToken();
            return this.connected;
        } else {

            if (this.timer.checkTimeIsUp(DOWN_TIME) || this.firstRun) {
                this.timer.resetTimer(DOWN_TIME);
                this.firstRun = false;
                boolean ping = this.getTotalPower();

                this.lastChargingCurrent = this.parent.getMaximumChargeCurrent();
                this.setChargeLimit(this.parent.getMaximumChargeCurrent());
                if (!ping) {
                    ping = this.getRefreshToken();
                    this.counter++;
                }
                if (ping && !this.lastPing) {
                    this.parent.reconnectSuccessful(this.counter);
                    this.counter = 0;
                }
                this.lastPing = ping;
                return ping;
            }
            return this.lastPing;
        }
    }


    /**
     * Retrieves the AccessToken from the Easeecloud with the configured username and password.
     *
     * @return true if connection could be established
     */
    private boolean getAccessToken() {
        try {
            HttpURLConnection connection = (HttpURLConnection) (new URL(AUTHORIZATION_URL)).openConnection();
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/*+json");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            OutputStream os = connection.getOutputStream();
            String curlRequest = "{\"userName\":\"" + this.username + "\",\"password\":\"" + this.password + "\"}";
            os.write(curlRequest.getBytes());
            os.flush();
            os.close();
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                this.solveTokenResponse(response.toString());
                this.authorizationHeader = "Bearer " + this.accessToken;
                this.chargeUrl = BASE_URL + "/sites/" + this.siteId + "/circuits/" + this.circuitId + "/dynamicCurrent";
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Retrieves the RefreshToken from the Easeecloud with the configured username and password.
     *
     * @return true if connection could be established
     */
    private boolean getRefreshToken() {
        try {
            HttpURLConnection connection = (HttpURLConnection) (new URL(REFRESH_URL)).openConnection();
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/*+json");
            connection.setRequestProperty("Authorization", this.authorizationHeader);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            OutputStream os = connection.getOutputStream();
            String curlRequest = "{\"accessToken\":\"" + this.accessToken + "\",\"refreshToken\":\"" + this.refreshToken + "\"}";
            os.write(curlRequest.getBytes());
            os.flush();
            os.close();
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                this.solveTokenResponse(response.toString());
                this.authorizationHeader = "Bearer " + this.accessToken;
                this.chargeUrl = BASE_URL + "/sites/" + this.siteId + "/circuits/" + this.circuitId + "/dynamicCurrent";
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Searches the response for the important information and puts it in the correct variables.
     *
     * @param tokens response from the Authorization request
     */
    private void solveTokenResponse(String tokens) {
        String[] responseArray = tokens.split(",");
        String workString;
        for (int n = 0; n < responseArray.length; n++) {
            if (responseArray[n].contains("accessToken")) {
                workString = responseArray[n].replace("accessToken", "");
                workString = this.trimReturnString(workString);
                this.accessToken = workString;
            } else if (responseArray[n].contains("expires")) {
                workString = responseArray[n].replace("expiresIn", "");
                workString = this.trimReturnString(workString);
                this.expiration = Integer.parseInt(workString);
            } else if (responseArray[n].contains("refreshToken")) {
                workString = responseArray[n].replace("refreshToken", "");
                workString = this.trimReturnString(workString);

                this.refreshToken = workString;
            }
        }

    }

    /**
     * Sends the current limit to the easee Cloud.
     *
     * @param limit The current limit from the EvcsLimiter
     * @return true if successful
     */
    private boolean setChargeLimit(int limit) {
        try {
            HttpURLConnection connection = (HttpURLConnection) (new URL(this.chargeUrl)).openConnection();
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/*+json");
            connection.setRequestProperty("Authorization", this.authorizationHeader);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            OutputStream os = connection.getOutputStream();
            //TODO possibly the phases that aren't in use have to be set to 0 but idk
            String curlRequest = "{\"phase1\":" + limit + ",\"phase2\":" + limit + ",\"phase3\":" + limit + "}";
            os.write(curlRequest.getBytes());
            os.flush();
            os.close();
            int responseCode = connection.getResponseCode();
            return responseCode == 200;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Retrieves the current Information from the evcs through the cloud.
     *
     * @return true if successful
     */
    private boolean getLimit() {
        try {
            HttpURLConnection connection = (HttpURLConnection) (new URL(this.chargeUrl)).openConnection();
            connection.setRequestProperty("Authorization", this.authorizationHeader);
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                this.solveCurrentResponse(response.toString());
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Puts the response from the cloud in the correct variables.
     *
     * @param currents response from the cloud
     */
    private void solveCurrentResponse(String currents) {
        String[] responseArray = currents.split(",");
        String workString;
        for (int n = 0; n < responseArray.length; n++) {
            if (responseArray[n].contains("phase1")) {
                workString = responseArray[n].replace("phase1", "");
                workString = this.trimReturnString(workString);
                this.parent.getCurrentL1Channel().setNextValue(Float.parseFloat(workString));
            } else if (responseArray[n].contains("phase2")) {
                workString = responseArray[n].replace("phase2", "");
                workString = this.trimReturnString(workString);
                this.parent.getCurrentL2Channel().setNextValue(Float.parseFloat(workString));
            } else if (responseArray[n].contains("phase3")) {
                workString = responseArray[n].replace("phase3", "");
                workString = this.trimReturnString(workString);
                this.parent.getCurrentL3Channel().setNextValue(Float.parseFloat(workString));
            }
        }
        //this.parent.getApparentPowerChannel().setNextValue(this.parent.getCurrentL1() + this.parent.getCurrentL2() + this.parent.getCurrentL3());
    }

    /**
     * Retrieves the Total Power draw of the Evcs from the Cloud.
     *
     * @return true if successful
     */
    private boolean getTotalPower() {
        try {
            HttpURLConnection connection = (HttpURLConnection) (new URL(this.powerUrl)).openConnection();
            connection.setRequestProperty("Authorization", this.authorizationHeader);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                this.solvePowerResponse(response.toString());
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Helper Method for the getTotalPower that reads the response from the Cloud and puts it in a way that the Bridge understands it.
     *
     * @param response Response from the Easee Cloud
     */
    private void solvePowerResponse(String response) {
        String[] responseArray = response.split(",");
        String workString;
        int counter = 0;
        for (int n = 0; n < responseArray.length; n++) {
            if (responseArray[n].contains("circuitTotalPhaseConductorCurrentL1")) {
                workString = responseArray[n].replace("circuitTotalPhaseConductorCurrentL1", "");
                workString = this.trimReturnString(workString);
                if (!workString.equals("null")) {
                    this.parent.getCurrentL1Channel().setNextValue(Float.parseFloat(workString));
                    this.l1 = Float.parseFloat(workString);
                }
                counter++;
            }
            if (responseArray[n].contains("circuitTotalPhaseConductorCurrentL2")) {
                workString = responseArray[n].replace("circuitTotalPhaseConductorCurrentL2", "");
                workString = this.trimReturnString(workString);
                if (!workString.equals("null")) {
                    this.parent.getCurrentL2Channel().setNextValue(Float.parseFloat(workString));
                    this.l2 = Float.parseFloat(workString);
                }
                counter++;
            }
            if (responseArray[n].contains("circuitTotalPhaseConductorCurrentL3")) {
                workString = responseArray[n].replace("circuitTotalPhaseConductorCurrentL3", "");
                workString = this.trimReturnString(workString);
                if (!workString.equals("null")) {
                    this.parent.getCurrentL3Channel().setNextValue(Float.parseFloat(workString));
                    this.l3 = Float.parseFloat(workString);
                }
                counter++;
            }
            if (counter >= 3) {
                this.parent._setChargePower(Math.round(this.l1 + this.l2 + this.l3) * GridVoltage.V_230_HZ_50.getValue());
                break;
            }

        }
    }

    /**
     * Trims common unnecessary Parts out of the response String from the Cloud.
     *
     * @param string Part of the response from the cloud.
     * @return trimmed response from the cloud
     */
    private String trimReturnString(String string) {
        string = string.replace("{", "");
        string = string.replace(" ", "");
        string = string.replace(":", "");
        string = string.replace("\"", "");
        string = string.replace("}", "");
        string = string.replace("\"", "");
        return string;
    }

    /**
     * Executes the resume Charge command.
     *
     * @return true if successful
     */
    private boolean resumeCharging() {
        try {
            HttpURLConnection connection = (HttpURLConnection) (new URL(this.resumeUrl)).openConnection();
            connection.setRequestProperty("Authorization", this.authorizationHeader);
            connection.setRequestMethod("POST");
            int responseCode = connection.getResponseCode();
            if (responseCode == 200 || responseCode == 202) {
                String currents = connection.getResponseMessage();
                this.solveCurrentResponse(currents);
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Executes the pause Charge command.
     *
     * @return true if successful
     */
    private boolean pauseCharging() {
        try {
            HttpURLConnection connection = (HttpURLConnection) (new URL(this.pauseUrl)).openConnection();
            connection.setRequestProperty("Authorization", this.authorizationHeader);
            connection.setRequestMethod("POST");
            int responseCode = connection.getResponseCode();
            if (responseCode == 200 || responseCode == 202) {
                String currents = connection.getResponseMessage();
                this.solveCurrentResponse(currents);
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

}
