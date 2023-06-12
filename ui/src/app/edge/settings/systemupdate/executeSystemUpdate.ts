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
    private ngUnsubscribe = new Subject<void>();

    public systemUpdateStateChange: (value: SystemUpdateState) => void;

    public constructor(
        private edge: Edge,
        private websocket: Websocket
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
            this.update()
                .then(updateState => {
                    if (updateState.running && updateState.running?.percentCompleted != 100) {
                        resolve(updateState);
                        return;
                    }
                    this.stopRefreshSystemUpdateState();
                    resolve(updateState);
                }).catch(error => {
                    reject(error);
                });
        });
    }

    private refreshSystemUpdateState(): Promise<SystemUpdateState> {
        return new Promise<SystemUpdateState>((resolve, reject) => {
            // if the version is a SNAPSHOT always set the udpate state
            // to updated with the current SNAPSHOT version
            if (this.edge.isSnapshot()) {
                let updateState = { updated: { version: this.edge.version } };
                this.setSystemUpdateState(updateState);
                this.stopRefreshSystemUpdateState();
                resolve(updateState);
            } else {
                this.edge.sendRequest(this.websocket,
                    new ComponentJsonApiRequest({
                        componentId: "_host",
                        payload: new GetSystemUpdateStateRequest()
                    })).then(response => {
                        let result = (response as GetSystemUpdateStateResponse).result;

                        this.setSystemUpdateState(result);
                        // Stop regular check if there is no Update available
                        if (result.updated) {
                            this.stopRefreshSystemUpdateState();
                        }
                        resolve(this.systemUpdateState);
                    }).catch(error => {
                        if (this.systemUpdateState.running) {
                            this.isEdgeRestarting = true;
                            return;
                        }
                        reject(error);
                    });
            }
        });
    }

    /**
     * Starts to execute the system update.
     * Gets resolved after the update is fully completed. 
     * @returns Promise<SystemUpdateState>
     */
    public executeSystemUpdate(): Promise<SystemUpdateState> {
        this.systemUpdateState = { running: { percentCompleted: 0, logs: [] } };
        return new Promise<SystemUpdateState>((resolve, reject) => {
            this.edge.sendRequest(this.websocket,
                new ComponentJsonApiRequest({
                    componentId: "_host",
                    payload: new ExecuteSystemUpdateRequest({ isDebug: environment.debugMode })
                })).then(response => {
                    // Finished System Update (without restart of OpenEMS Edge)
                    let systemUpdateState = (response as GetSystemUpdateStateResponse).result;
                    this.setSystemUpdateState(systemUpdateState);
                }).catch(reason => {
                    reject(reason);
                });

            this.update()
                .then(updateState => {
                    if (updateState.updated) {
                        this.stopRefreshSystemUpdateState();
                        resolve(this.systemUpdateState);
                    }
                }).catch(error => {
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
            source.pipe(
                takeUntil(this.ngUnsubscribe)
            ).subscribe(ignore => {
                if (!this.edge.isOnline) {
                    return;
                }
                this.refreshSystemUpdateState()
                    .then(updateState => {
                        resolve(updateState);
                    }).catch(error => {
                        if (!error["error"]) {
                            return;
                        }
                        let errorMessage = error["error"]["message"] as string;
                        if (!errorMessage) {
                            return;
                        }
                        if (errorMessage.includes("ExecuteSystemCommandRequest is not implemented for Windows")) {
                            this.canNotBeUpdated = true;
                            this.stopRefreshSystemUpdateState();
                            reject(error);
                        }
                    });
            });
        });
    }

    private stopRefreshSystemUpdateState() {
        this.ngUnsubscribe.next();
    }

    /**
     * Stops asking the status.
     */
    public stop() {
        this.stopRefreshSystemUpdateState();
        this.ngUnsubscribe.complete();
    }

    private setSystemUpdateState(systemUpdateState: SystemUpdateState) {
        this.systemUpdateState = systemUpdateState;
        this.systemUpdateStateChange(systemUpdateState);
    }
}
