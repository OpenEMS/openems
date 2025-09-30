// @ts-strict-ignore
import { Subject, timer } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { Edge, Websocket } from "src/app/shared/shared";
import { environment } from "src/environments";
import { ExecuteSystemUpdateRequest } from "./executeSystemUpdateRequest";
import { GetSystemUpdateStateRequest } from "./getSystemUpdateStateRequest";
import { GetSystemUpdateStateResponse, SystemUpdateState } from "./getSystemUpdateStateResponse";

export class ExecuteSystemUpdate {

    /**
     * Can not execute update on Windows.
     */
    public canNotBeUpdated: boolean = false;
    public isEdgeRestarting: boolean = false;
    public systemUpdateState: SystemUpdateState = { unknown: {} };
    public systemUpdateStateChange: (value: SystemUpdateState) => void;
    private ngUnsubscribe = new Subject<void>();

    public constructor(
        private edge: Edge,
        private websocket: Websocket,
    ) {
    }

    /**
     * Starts asking the status of the device.
     *
     * If an update is running the status gets updated till the update is completed.
     * @returns the first received Promise<SystemUpdateState>
     */
    public start(): Promise<SystemUpdateState> {
        return new Promise<SystemUpdateState>((resolve, reject) => {
            THIS.UPDATE()
                .then(updateState => {
                    if (UPDATE_STATE.RUNNING && UPDATE_STATE.RUNNING?.percentCompleted != 100) {
                        resolve(updateState);
                        return;
                    }
                    THIS.STOP_REFRESH_SYSTEM_UPDATE_STATE();
                    resolve(updateState);
                }).catch(error => {
                    reject(error);
                });
        });
    }

    /**
     * Starts to execute the system update.
     * Gets resolved after the update is fully completed.
     * @returns Promise<SystemUpdateState>
     */
    public executeSystemUpdate(): Promise<SystemUpdateState> {
        THIS.SYSTEM_UPDATE_STATE = { running: { percentCompleted: 0, logs: [] } };
        return new Promise<SystemUpdateState>((resolve, reject) => {
            THIS.EDGE.SEND_REQUEST(THIS.WEBSOCKET,
                new ComponentJsonApiRequest({
                    componentId: "_host",
                    payload: new ExecuteSystemUpdateRequest({ isDebug: ENVIRONMENT.DEBUG_MODE }),
                })).then(response => {
                    // Finished System Update (without restart of OpenEMS Edge)
                    const systemUpdateState = (response as GetSystemUpdateStateResponse).result;
                    THIS.SET_SYSTEM_UPDATE_STATE(systemUpdateState);
                }).catch(reason => {
                    reject(reason);
                });

            THIS.UPDATE()
                .then(updateState => {
                    if (UPDATE_STATE.UPDATED) {
                        THIS.STOP_REFRESH_SYSTEM_UPDATE_STATE();
                        resolve(THIS.SYSTEM_UPDATE_STATE);
                    }
                }).catch(error => {
                    reject(error);
                });
        });
    }

    /**
 * Stops asking the status.
 */
    public stop() {
        THIS.STOP_REFRESH_SYSTEM_UPDATE_STATE();
        THIS.NG_UNSUBSCRIBE.COMPLETE();
    }

    private refreshSystemUpdateState(): Promise<SystemUpdateState> {
        return new Promise<SystemUpdateState>((resolve, reject) => {
            THIS.EDGE.SEND_REQUEST(THIS.WEBSOCKET,
                new ComponentJsonApiRequest({
                    componentId: "_host",
                    payload: new GetSystemUpdateStateRequest(),
                })).then(response => {
                    const result = (response as GetSystemUpdateStateResponse).result;

                    THIS.SET_SYSTEM_UPDATE_STATE(result);
                    // Stop regular check if there is no Update available
                    if (RESULT.UPDATED) {
                        THIS.STOP_REFRESH_SYSTEM_UPDATE_STATE();
                    }
                    resolve(THIS.SYSTEM_UPDATE_STATE);
                }).catch(error => {
                    if (THIS.SYSTEM_UPDATE_STATE.RUNNING) {
                        THIS.IS_EDGE_RESTARTING = true;
                        return;
                    }
                    reject(error);
                });
        });
    }

    /**
     * Tries to get the status update every 15 seconds until its finished.
     *
     * @returns Promise<SystemUpdateState>
     */
    private update(): Promise<SystemUpdateState> {
        return new Promise<SystemUpdateState>((resolve, reject) => {
            const source = timer(0, 15000);
            SOURCE.PIPE(
                takeUntil(THIS.NG_UNSUBSCRIBE),
            ).subscribe(ignore => {
                if (!THIS.EDGE.IS_ONLINE) {
                    return;
                }
                THIS.REFRESH_SYSTEM_UPDATE_STATE()
                    .then(updateState => {
                        resolve(updateState);
                    }).catch(error => {
                        if (!error["error"]) {
                            return;
                        }
                        const errorMessage = error["error"]["message"] as string;
                        if (!errorMessage) {
                            return;
                        }
                        if (ERROR_MESSAGE.INCLUDES("ExecuteSystemCommandRequest is not implemented for Windows")) {
                            THIS.CAN_NOT_BE_UPDATED = true;
                            THIS.STOP_REFRESH_SYSTEM_UPDATE_STATE();
                            reject(error);
                        }
                    });
            });
        });
    }

    private stopRefreshSystemUpdateState() {
        THIS.NG_UNSUBSCRIBE.NEXT();
    }

    private setSystemUpdateState(systemUpdateState: SystemUpdateState) {
        THIS.SYSTEM_UPDATE_STATE = systemUpdateState;
        THIS.SYSTEM_UPDATE_STATE_CHANGE(systemUpdateState);
    }
}
