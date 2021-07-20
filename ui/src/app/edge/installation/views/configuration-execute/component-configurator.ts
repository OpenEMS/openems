import { Websocket, EdgeConfig, Edge, ChannelAddress } from "src/app/shared/shared";

export enum ConfigurationMode {
    RemoveAndConfigure = "remove-and-configure",    // The component will be removed and then configured as specified
    RemoveOnly = "remove-only",                     // The component will only be removed
}

export enum ConfigurationStatus {
    Missing = "missing",                // Component is not configured
    PreConfigured = "pre-configured",   // Component is already configured

    Configuring = "configuring",        // Component is being configured

    Configured = "configured",          // Configuration of component was successful
    Error = "error",                    // Configuration of component was not successful

    FunctionTestPassed = "function-test-passed",
    FunctionTestFailed = "function-test-failed"
}

export type ConfigurationObject = {
    factoryId: string,
    componentId: string,
    alias: string,
    properties?: { name: string, value: any }[],
    mode: ConfigurationMode,
    status?: ConfigurationStatus
}

const DELAY = 10000;    // Delay between the configuration of every component

export class ComponentConfigurator {

    private configurationObjects: ConfigurationObject[] = [];

    constructor(private edge: Edge, private config: EdgeConfig, private websocket: Websocket) { }

    /**
     * Adds a configuration object to be configured
     * and determines its configuration status before.
     * 
     * @param configurationObject 
     */
    public add(configurationObject: ConfigurationObject) {

        if (this.exists(configurationObject.componentId)) {
            configurationObject.status = ConfigurationStatus.PreConfigured;
        } else {
            configurationObject.status = ConfigurationStatus.Missing;
        }

        this.configurationObjects.push(configurationObject);
    }

    /**
     * Starts the configuration process including all
     * configuration objects which have been added via @method add()
     * 
     * @returns a promise of type void
     */
    public start(): Promise<void> {
        return new Promise((resolve, reject) => {
            this.clear().then(() => {
                this.configureNext(0).then(() => {
                    this.updateScheduler();
                    resolve();
                }).catch((reason) => {
                    reject(reason);
                });
            }).catch((reason) => {
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
        let configurationObjectsToBeInstalled: ConfigurationObject[] = [];

        for (let configurationObject of this.configurationObjects) {
            if (configurationObject.mode === ConfigurationMode.RemoveAndConfigure) {
                configurationObjectsToBeInstalled.push(configurationObject);
            }
        }
        return configurationObjectsToBeInstalled;
    }

    /**
     * Determines, whether all components added via @method add()
     * have the given @param status
     * 
     * @returns a boolean representing the result
     */
    public allHaveStatus(status: ConfigurationStatus): boolean {
        for (let configurationObject of this.configurationObjects) {
            if (configurationObject.status !== status) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines, whether any component added via @method add()
     * has the given @param status
     * 
     * @returns a boolean representing the result
     */
    public anyHasStatus(status: ConfigurationStatus): boolean {
        for (let configurationObject of this.configurationObjects) {
            if (configurationObject.status === status) {
                return true;
            }
        }
        return false;
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
     * have the status 'PreInstalled'.
     */
    private clear(): Promise<void> {
        return new Promise((resolve, reject) => {
            for (let configurationObject of this.configurationObjects) {
                if (configurationObject.status === ConfigurationStatus.PreConfigured) {
                    this.edge.deleteComponentConfig(this.websocket, configurationObject.componentId).then(() => {
                        configurationObject.status = ConfigurationStatus.Missing;
                    }).catch((reason) => {
                        configurationObject.status = ConfigurationStatus.Error;
                        reject(reason);
                    });
                }
            }
            resolve();
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
            let configurationObject = this.configurationObjects[index];
            let properties: { name: string, value: any }[] = this.generateProperties(configurationObject);

            configurationObject.status = ConfigurationStatus.Configuring;

            let status;

            if (configurationObject.mode === ConfigurationMode.RemoveAndConfigure) {
                // When in RemoveAndConfigure-Mode the component gets configured and
                // marked as 'Configured'. When the configuration fails, the corresponding
                // configuration object gets marked with 'Error' and the Promise gets rejected. 
                this.edge.createComponentConfig(this.websocket, configurationObject.factoryId, properties).then(() => {
                    status = ConfigurationStatus.Configured;
                }).catch((reason) => {
                    status = ConfigurationStatus.Error;
                    reject(reason);
                });
            } else {
                // When in RemoveOnly-Mode, the component simply gets marked
                // as 'Configured'.
                status = ConfigurationStatus.Configured;
            }

            setTimeout(() => {
                // Set status of element
                configurationObject.status = status;
                // Recursively installs the next elements
                if (index + 1 < this.configurationObjects.length) {
                    this.configureNext(index + 1).then(() => {
                        resolve();
                    }).catch((reason) => {
                        reject(reason);
                    });
                } else {
                    resolve();
                }
            }, DELAY);
        });
    }

    /**
     * Updates the scheduler.
     * 
     * @param config 
     */
    private updateScheduler() {
        let scheduler: EdgeConfig.Component = this.config.getComponent("scheduler0");
        let requiredControllerIds = ["ctrlEssSurplusFeedToGrid0", "ctrlBalancing0"];

        if (!scheduler) {
            // If scheduler is not existing, it gets configured as required
            this.edge.createComponentConfig(this.websocket, "Scheduler.AllAlphabetically", [
                { name: "id", value: "scheduler0" },
                { name: "controllers.ids", value: requiredControllerIds }
            ]);
        } else {
            // If the scheduler is existing, it gets updated
            let existingControllerIds: string[] = scheduler.properties["controllers.ids"];
            let newControllerIds = [];

            for (let requiredControllerId of requiredControllerIds) {
                if (!existingControllerIds.find(existingControllerId => requiredControllerId === existingControllerId)) {
                    newControllerIds.push(requiredControllerId);
                }
            }

            newControllerIds = existingControllerIds.concat(newControllerIds);

            this.edge.updateComponentConfig(this.websocket, "scheduler0", [
                { name: "controllers.ids", value: newControllerIds }
            ]);
        }
    }

    /**
     * @deprecated in development
     */
    public startFunctionTest() {

        let configurationObjects = this.getConfigurationObjectsToBeConfigured();
        let channelAddresses: ChannelAddress[] = [];
        let subscriptionId = "component-configurator";
        let currentDataSubscription;

        // Subscribe to all required channels
        for (let configurationObject of configurationObjects) {
            channelAddresses.push(new ChannelAddress(configurationObject.componentId, "State"));
        }

        this.edge.subscribeChannels(this.websocket, subscriptionId, channelAddresses);

        // Read all channels and set the state
        currentDataSubscription = this.edge.currentData.subscribe((currentData) => {
            if (!currentData) {
                return;
            }

            for (let configurationObject of configurationObjects) {
                let channelValue = currentData.channel[configurationObject.componentId + "/State"];

                if (channelValue === 0) {
                    configurationObject.status = ConfigurationStatus.FunctionTestPassed;
                } else {
                    configurationObject.status = ConfigurationStatus.FunctionTestFailed;
                }
            }
        });

    }


    /**
     * @deprecated only for development
     */
    public devClear() {
        for (let configurationObject of this.configurationObjects) {
            this.edge.deleteComponentConfig(this.websocket, configurationObject.componentId);
        }
    }

}