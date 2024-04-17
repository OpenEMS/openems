import { Subscription } from "rxjs";
import { filter } from "rxjs/operators";
import { ChannelAddress, Edge, EdgeConfig, Websocket } from "src/app/shared/shared";
import { SchedulerId, SchedulerIdBehaviour } from "../../installation-systems/abstract-ibn";
import { AppCenterUtil } from "../../shared/appcenterutil";

export enum BaseMode {
    UI = "UI", // This component will be created by the UI
    AppManager = "created-by-app-manager", // The component will be created by the AppManager
}

export enum ConfigurationMode {
    RemoveAndConfigure = "remove-and-configure",    // The component will be removed and then configured as specified
    RemoveOnly = "remove-only",                     // The component will only be removed
    UpdateOnly = "update-only",                     // The component gets updated can be used for core components like _power, ...
}

export enum ConfigurationState {
    Missing = "missing",                // Component is not configured
    PreConfigured = "pre-configured",   // Component is already configured

    Configuring = "configuring",        // Component is being configured

    Configured = "configured",          // Configuration of component was successful
    Error = "error"                     // Configuration of component was not successful
}

export enum FunctionState {
    Undefined = "undefined",

    Testing = "testing",

    Ok = "ok",
    Warning = "warning",
    Fault = "fault"
}

export type ConfigurationObject = {
    factoryId: string,
    componentId: string,
    alias: string,
    properties?: { name: string, value: any }[],
    mode: ConfigurationMode,
    configState?: ConfigurationState,
    functionState?: FunctionState,
    baseMode?: BaseMode
}

const DELAY_CLEAR = 5000;          // Time between the clear and the start of the configurations
const DELAY_CONFIG = 15000;         // Time between the configuration of every component
const DELAY_FUNCTION_TEST = 120000;  // Time between the configuration and the function test of every component

export class ComponentConfigurator {

    private configurationObjects: ConfigurationObject[] = [];
    private channelMappings: { configurationObject: ConfigurationObject, channelAddress: ChannelAddress }[] = [];
    private subscriptions: Subscription[] = [];

    private installAppCallbacks: (() => Promise<any>)[] = [];

    constructor(private edge: Edge, private config: EdgeConfig, private websocket: Websocket) { }

    /**
     * Adds a configuration object to be configured
     * and determines its configuration state before.
     *
     * @param configurationObject the ConfigurationObject.
     * @param index Index of component to be added.
     */
    public add(configurationObject: ConfigurationObject, index?: number) {
        this.refreshConfigurationState(configurationObject);
        if (index) {
            this.configurationObjects.splice(index, 0, configurationObject);
        } else {
            this.configurationObjects.push(configurationObject);
        }
    }

    public addInstallAppCallback(installAppCallback: () => Promise<any>) {
        this.installAppCallbacks.push(installAppCallback);
    }

    private refreshConfigurationState(configurationObject: ConfigurationObject) {
        if (this.exists(configurationObject.componentId)) {
            configurationObject.configState = ConfigurationState.PreConfigured;
        } else {
            configurationObject.configState = ConfigurationState.Missing;
        }
    }

    private refreshAllConfigurationStates() {
        for (const configurationObject of this.configurationObjects) {
            this.refreshConfigurationState(configurationObject);
        }
    }

    /**
     * Starts the configuration process including all
     * configuration objects which have been added via @method add()
     *
     * @returns a promise of type void
     */
    public start(): Promise<void> {
        return new Promise((resolve, reject) => {
            // first update scheduler to make sure it is created
            this.updateScheduler().then(() => {
                // execute app install callbacks
                const installApp = new Promise<void>((resolve, reject) => {
                    const allPromises: Promise<any>[] = [];
                    this.installAppCallbacks.forEach(callback => {
                        allPromises.push(callback());
                    });
                    Promise.all(allPromises).then(() => resolve())
                        .catch(error => reject(error));
                });

                this.refreshAllConfigurationStates();
                const updateComponents = new Promise((resolve, reject) => {
                    this.clear().then(response => {
                        this.configureNext(0).then(() =>
                            //this.stopFunctionTests(); TODO
                            resolve(response))
                            .catch((reason) => reject(reason));
                    }).catch((reason) => reject(reason));
                });
                Promise.all([installApp, updateComponents])
                    .then(() => resolve())
                    .catch(error => {
                        return reject(error);
                    });
            }).catch(reason => {
                reject(reason);
            });
        });
    }

    /**
     * Returns all configuration objects which have been added to the
     * list of this component configurator instance via @method add()
     * and have the configuration mode 'RemoveAndConfigure'.
     *
     * @returns an array of configuration objects
     */
    public getConfigurationObjectsToBeConfigured(): ConfigurationObject[] {
        const configurationObjectsToBeInstalled: ConfigurationObject[] = [];

        for (const configurationObject of this.configurationObjects) {
            if (configurationObject.mode !== ConfigurationMode.RemoveOnly) {
                configurationObjectsToBeInstalled.push(configurationObject);
            }
        }
        return configurationObjectsToBeInstalled;
    }

    /**
     * Determines, whether all components added via @method add()
     * have the given @param configurationState
     *
     * @returns a boolean representing the result
     */
    public allHaveConfigurationState(configurationState: ConfigurationState): boolean {
        for (const configurationObject of this.configurationObjects) {
            if (configurationObject.configState !== configurationState) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines, whether any component added via @method add()
     * has the given @param configurationState
     *
     * @returns a boolean representing the result
     */
    public anyHasConfigurationState(configurationState: ConfigurationState): boolean {
        for (const configurationObject of this.configurationObjects) {
            if (configurationObject.componentId.startsWith("_")) {
                // ignore core components
                continue;
            }
            if (configurationObject.configState === configurationState) {
                return true;
            }
        }
        return false;
    }

    public allHaveFunctionState(desiredFunctionState: FunctionState): boolean {
        for (const configurationObject of this.configurationObjects) {
            const functionState: FunctionState = configurationObject.functionState;
            if (functionState && functionState !== desiredFunctionState) {
                return false;
            }
        }
        return true;
    }

    /**
     * Helping function to determine whether the component
     * with the given @param componentId exists in the config.
     */
    private exists(componentId: string): boolean {
        return this.config.getComponent(componentId) !== undefined;
    }

    /**
     * Deletes all existing components which
     * have the state 'PreInstalled'.
     */
    private clear(): Promise<void> {
        return new Promise((resolve, reject) => {
            const preConfiguredObjects = this.configurationObjects
                .slice()
                .reverse()
                .filter(config => config.configState === ConfigurationState.PreConfigured);

            if (preConfiguredObjects.length == 0) {
                resolve();
            }

            this.clearComponent(preConfiguredObjects, 0)
                .then(() => resolve())
                .catch(reason => reject(reason));
        });
    }

    /**
     * Delete given pre configured components.
     *
     * @param preConfiguredObjects Components to delete
     * @param index Index of component to be deleted
     * @returns Promise
     */
    private clearComponent(preConfiguredObjects: Array<ConfigurationObject>, index: number): Promise<void> {
        return new Promise((resolve, reject) => {
            const configurationObject = preConfiguredObjects[index];

            let delay = DELAY_CLEAR;
            if (configurationObject.baseMode === BaseMode.AppManager) {
                delay = 0;
            }

            const clearNext = () => {
                configurationObject.configState = ConfigurationState.Missing;

                setTimeout(() => {
                    if (index + 1 < preConfiguredObjects.length) {
                        this.clearComponent(preConfiguredObjects, index + 1).then(() => {
                            resolve();
                        }).catch((reason) => {
                            reject(reason);
                        });
                    } else {
                        resolve();
                    }
                }, delay);
            };

            if (configurationObject.baseMode === BaseMode.AppManager) {
                clearNext();
                return;
            }

            if (configurationObject.mode === ConfigurationMode.UpdateOnly) {
                clearNext();
                return;
            }

            this.edge.deleteComponentConfig(this.websocket, configurationObject.componentId)
                .then(clearNext)
                .catch((reason) => {
                    configurationObject.configState = ConfigurationState.Error;
                    reject(reason);
                });
        });
    }

    /**
     * Helping function to put the properties together
     * as needed.
     *
     * @param   configurationObject
     * @returns the formatted properties
     */
    private generateProperties(configurationObject: ConfigurationObject): { name: string, value: any }[] {

        let properties: { name: string, value: any }[] = [];

        properties.push({ name: "id", value: configurationObject.componentId });
        properties.push({ name: "alias", value: configurationObject.alias });

        if (configurationObject.properties) {
            properties = properties.concat(configurationObject.properties);
        }

        return properties;
    }

    /**
     * Recursive method to configure all components specified in the configuration objects array
     * of this component configurator instance, starting at the given @param index
     *
     * @param   index of the first configuration object to install
     * @returns a promise of type void
     */
    private configureNext(index: number): Promise<void> {
        return new Promise((resolve, reject) => {
            const configurationObject = this.configurationObjects[index];

            configurationObject.configState = ConfigurationState.Configuring;

            this.configure(configurationObject).then(() => {
                configurationObject.configState = ConfigurationState.Configured;

                let timeout = DELAY_CONFIG;

                if (configurationObject.mode !== ConfigurationMode.RemoveAndConfigure) {
                    // Do not wait if in RemoveOnly-Mode
                    timeout = 0;
                }

                // component got already created by app manager
                if (configurationObject.baseMode === BaseMode.AppManager) {
                    timeout = 0;
                }

                // Recursively installs the next elements with the set delay between
                setTimeout(() => {
                    if (index + 1 < this.configurationObjects.length) {
                        this.configureNext(index + 1).then(() => {
                            resolve();
                        }).catch((reason) => {
                            reject(reason);
                        });
                    } else {
                        resolve();
                    }
                }, timeout);

            }).catch((reason) => {
                configurationObject.configState = ConfigurationState.Error;
                reject(reason);
            });
        });
    }

    private configure(configurationObject: ConfigurationObject): Promise<void> {
        return new Promise((resolve, reject) => {
            if (configurationObject.baseMode === BaseMode.AppManager) {
                resolve();
                return;
            }
            const properties: { name: string, value: any }[] = this.generateProperties(configurationObject);

            // When in UpdateOnly-Mode the component gets updated and
            // the Promise resolved. When the configuration fails, the Promise gets rejected.
            if (configurationObject.mode === ConfigurationMode.UpdateOnly) {
                this.edge.updateComponentConfig(this.websocket, configurationObject.componentId, properties).then(() => {
                    resolve();
                }).catch((reason) => {
                    reject(reason);
                });
            } else if (configurationObject.mode === ConfigurationMode.RemoveAndConfigure) {
                // When in RemoveAndConfigure-Mode the component gets configured and
                // the Promise resolved. When the configuration fails, the Promise gets rejected.
                this.edge.createComponentConfig(this.websocket, configurationObject.factoryId, properties).then(() => {
                    resolve();
                }).catch((reason) => {
                    reject(reason);
                });
            } else {
                // When in RemoveOnly-Mode, the component doesen't
                // have to be configured, so this Promise just gets resolved.
                resolve();
            }
        });
    }

    public startFunctionTest(): Promise<void> {

        return new Promise((resolve) => {
            setTimeout(() => {
                for (const configurationObject of this.configurationObjects) {

                    if (configurationObject.mode !== ConfigurationMode.RemoveAndConfigure) {
                        // Only do function test if in RemoveAndConfigure-Mode
                        continue;
                    }

                    configurationObject.functionState = FunctionState.Testing;

                    const channelAddress = new ChannelAddress(configurationObject.componentId, "State");
                    const channelMapping = { configurationObject: configurationObject, channelAddress: channelAddress };

                    // Add the subscription
                    this.channelMappings.push(channelMapping);

                    // Get all channel addresses
                    const channelAddresses: ChannelAddress[] = [];

                    for (const subscription of this.channelMappings) {
                        channelAddresses.push(subscription.channelAddress);
                    }

                    // Do edge subscribe for all channels
                    this.edge.subscribeChannels(this.websocket, "component-configurator", channelAddresses);

                    // Subscribe to the new channel
                    const subscription: Subscription = this.edge.currentData.pipe(
                        filter(currentData => currentData !== null),
                    ).subscribe((currentData) => {
                        const channelAddress: ChannelAddress = channelMapping.channelAddress;
                        const channelValue: number = currentData.channel[channelAddress.componentId + "/" + channelAddress.channelId];

                        let functionState;

                        switch (channelValue) {
                            case 0:
                            case 1:
                                functionState = FunctionState.Ok;
                                break;
                            case 2:
                                functionState = FunctionState.Warning;
                                break;
                            case 3:
                                functionState = FunctionState.Fault;
                                break;
                            default:
                                functionState = undefined;
                                break;
                        }

                        channelMapping.configurationObject.functionState = functionState;
                    });

                    // Add subscription to list
                    this.subscriptions.push(subscription);
                }

                resolve();
            }, DELAY_FUNCTION_TEST);
        });
    }

    private stopFunctionTests() {
        for (const subscription of this.subscriptions) {
            subscription.unsubscribe();
        }
        this.edge.unsubscribeChannels(this.websocket, "component-configurator");
    }

    /**
     * Updates the scheduler.
     *
     * @param config
     */
    private updateScheduler() {
        return new Promise((resolve, reject) => {
            const scheduler: EdgeConfig.Component = this.config.getComponent("scheduler0");
            const ibn = JSON.parse(sessionStorage.ibn);

            const requiredControllerIds: SchedulerId[] = ibn.requiredControllerIds;
            const controllerIds: string[] = [];
            requiredControllerIds.forEach(value => {
                if (AppCenterUtil.isAppManagerAvailable(this.edge) && value.behaviour === SchedulerIdBehaviour.MANAGED_BY_APP_MANAGER) {
                    return;
                }
                controllerIds.push(value.componentId);
            });

            if (!scheduler) {
                // If scheduler doesn't exist, it gets created and configured as required
                this.edge.createComponentConfig(this.websocket, "Scheduler.AllAlphabetically", [
                    { name: "id", value: "scheduler0" },
                    { name: "controllers.ids", value: controllerIds },
                ])
                    .then(value => resolve(value))
                    .catch(error => reject(error));
            } else {
                if (controllerIds.length == 0) {
                    resolve("No Controllers to add.");
                    return;
                }
                // If the scheduler exists, it gets updated
                const existingControllerIds: string[] = scheduler.properties["controllers.ids"];
                let newControllerIds: string[] = [];

                for (const requiredControllerId of controllerIds) {
                    if (!existingControllerIds.find(existingControllerId => requiredControllerId === existingControllerId)) {
                        newControllerIds.push(requiredControllerId);
                    }
                }

                newControllerIds = existingControllerIds.concat(newControllerIds);

                this.edge.updateComponentConfig(this.websocket, "scheduler0", [
                    { name: "controllers.ids", value: newControllerIds },
                ])
                    .then(value => resolve(value))
                    .catch(error => reject(error));
            }
        });
    }
}
