import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { Edge, Websocket } from "src/app/shared/shared";
import { AddAppInstance } from "../../settings/app/jsonrpc/addAppInstance";
import { DeleteAppInstance } from "../../settings/app/jsonrpc/deleteAppInstance";
import { GetAppInstances } from "../../settings/app/jsonrpc/getAppInstances";
import { UpdateAppInstance } from "../../settings/app/jsonrpc/updateAppInstance";


export class AppCenterUtil {

    /**
     * Creates an instance with the given properties 
     * or updates the first found instance of the given appId.
     * 
     * @param edge       the edge
     * @param websocket  the websocket connection
     * @param appId      the Id of the app that should get created or updated
     * @param alias      the alias of the new instance
     * @param properties the properties of the new instance
     * @returns a Promise of the new instance
     */
    public static createOrUpdateApp(edge: Edge, websocket: Websocket, appId: string, alias: string, properties: {}, key: string)
        : Promise<GetAppInstances.AppInstance> {
        return new Promise<GetAppInstances.AppInstance>((resolve, reject) => {
            AppCenterUtil.getAppInstances(edge, websocket, appId)
                .then(result => {
                    // app already installed => update
                    if (result.result.instances.length > 0) {
                        // take first found instance e. g. a Home can only be instantiated 
                        // once so there should only be one instance available
                        const alreadyExistingInstance = result.result.instances[0];
                        AppCenterUtil.updateApp(edge, websocket, alreadyExistingInstance.instanceId, alias, properties)
                            .then(response => resolve(response.result.instance))
                            .catch(error => reject(error));
                    } else {
                        AppCenterUtil.createAppInstance(edge, websocket, appId, alias, properties, key)
                            .then(response => {
                                let result = response as AddAppInstance.Response;
                                resolve(result.result.instance);
                            }).catch(error => reject(error));
                    }
                }).catch(error => reject(error));
        });
    }

    /**
     * Gets the Instance of the matching the given appId and instanceId.
     * 
     * @param edge       the edge
     * @param websocket  the websocket connection
     * @param appId      the AppId of the instance
     * @param instanceId the instanceId
     * @returns a Promise of the instance
     */
    public static getAppInstance(edge: Edge, websocket: Websocket, appId: string, instanceId: string): Promise<GetAppInstances.AppInstance> {
        return new Promise<GetAppInstances.AppInstance>((resolve, reject) => {
            AppCenterUtil.getAppInstances(edge, websocket, appId)
                .then(response => {
                    let matchingIds = response.result.instances.filter(instance => {
                        return instance.instanceId == instanceId;
                    });
                    if (matchingIds.length == 0) {
                        reject("Instance not found!");
                        return;
                    }
                    resolve(matchingIds[0]);
                })
                .catch(error => {
                    reject(error);
                });
        });
    }

    /**
     * Gets all Instances of the given AppId.
     * 
     * @param edge       the edge
     * @param websocket  the websocket connection
     * @param appId      the AppId of the instances
     * @returns a Promise of GetAppInstances.Response
     */
    public static getAppInstances(edge: Edge, websocket: Websocket, appId: string): Promise<GetAppInstances.Response> {
        return new Promise<GetAppInstances.Response>((resolve, reject) => {
            edge.sendRequest(websocket,
                new ComponentJsonApiRequest({
                    componentId: "_appManager",
                    payload: new GetAppInstances.Request({
                        appId: appId
                    })
                }))
                .then(response => resolve(response as GetAppInstances.Response))
                .catch(error => reject(error));
        });
    }

    /**
     * Deinstalls all instances of the given AppId.
     * 
     * @param edge       the edge
     * @param websocket  the websocket connection
     * @param appId      the AppId of the instances
     * @returns a Promise
     */
    public static removeInstancesOfApp(edge: Edge, websocket: Websocket, appId: string) {
        return new Promise((resolve, reject) => {
            AppCenterUtil.getAppInstances(edge, websocket, appId)
                .then(response => {
                    if (!response.result.instances || response.result.instances.length == 0) {
                        resolve(response);
                        return;
                    }
                    let instanceIds: string[] = [];
                    response.result.instances.forEach(instance => { instanceIds.push(instance.instanceId); });
                    this.removeInstances(edge, websocket, instanceIds)
                        .then(response => resolve(response))
                        .catch(error => reject(error));
                })
                .catch(error => reject(error));
        });
    }

    /**
     * Deinstalls all instances that are given.
     * 
     * @param edge       the edge
     * @param websocket  the websocket connection
     * @param instanceIds the instanceIds that should get deinstalled
     * @returns a Promise that resolves after every instance got deinstalled
     */
    public static removeInstances(edge: Edge, websocket: Websocket, instanceIds: string[]): Promise<any[]> {
        let promises: Promise<any>[] = [];
        instanceIds.forEach(instanceId => {
            promises.push(edge.sendRequest(websocket,
                new ComponentJsonApiRequest({
                    componentId: "_appManager",
                    payload: new DeleteAppInstance.Request({
                        instanceId: instanceId
                    })
                })
            ));
        });
        return Promise.all(promises);
    }

    /**
     * Updates an App Instance.
     * 
     * @param edge       the edge
     * @param websocket  the websocket connection
     * @param instanceId the instance that should get updated
     * @param alias      the alias of the updated instance
     * @param properties the properties of the updated instance
     * @returns a Promise of UpdateAppInstance.Response
     */
    public static updateApp(edge: Edge, websocket: Websocket, instanceId: string, alias: string, properties: {})
        : Promise<UpdateAppInstance.Response> {
        return new Promise<UpdateAppInstance.Response>((resolve, reject) => {
            edge.sendRequest(websocket,
                new ComponentJsonApiRequest({
                    componentId: "_appManager",
                    payload: new UpdateAppInstance.Request({
                        instanceId: instanceId,
                        alias: alias,
                        properties: properties
                    })
                }))
                .then(response => resolve(response as UpdateAppInstance.Response))
                .catch(error => reject(error));
        });
    }

    /**
     * Creates an App Instance.
     * 
     * @param edge       the edge
     * @param websocket  the websocket connection
     * @param appId      the AppId of the App that should get created
     * @param alias      the alias of the new instance
     * @param properties the propertes of the new instance
     * @returns a Promise of AddAppInstance.Response
     */
    public static createAppInstance(edge: Edge, websocket: Websocket, appId: string, alias: string, properties: {}, key: string)
        : Promise<AddAppInstance.Response> {
        return new Promise<AddAppInstance.Response>((resolve, reject) => {
            edge.sendRequest(websocket,
                new ComponentJsonApiRequest({
                    componentId: "_appManager",
                    payload: new AddAppInstance.Request({
                        key: key,
                        appId: appId,
                        alias: alias,
                        properties: properties
                    })
                }))
                .then(response => resolve(response as AddAppInstance.Response))
                .catch(error => reject(error));
        });
    }

    /**
     * Determines if the appManager can be used to install a system.
     * 
     * @param edge to check the version of
     * @returns true if the appManager can be used else false
     */
    public static isAppManagerAvailable(edge: Edge): boolean {
        return edge.isVersionAtLeast('2021.19.1') && !edge.isSnapshot();
    }

    // TODO this key will probably removed with a separate request for installing integrated systems
    public static keyForIntegratedSystems(): string {
        return '0000-0000-0000-0001';
    }

}