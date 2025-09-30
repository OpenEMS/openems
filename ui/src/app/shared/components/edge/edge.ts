// @ts-strict-ignore
import { TranslateService } from "@ngx-translate/core";
import { compareVersions } from "compare-versions";
import { BehaviorSubject, Subject } from "rxjs";
import { filter, first } from "rxjs/operators";
import { hasUpdateAppVersion } from "src/app/edge/settings/app/permissions";
import { SumState } from "src/app/index/shared/sumState";
import { JsonrpcRequest, JsonrpcResponseSuccess } from "../../jsonrpc/base";
import { CurrentDataNotification } from "../../jsonrpc/notification/currentDataNotification";
import { EdgeConfigNotification } from "../../jsonrpc/notification/edgeConfigNotification";
import { SystemLogNotification } from "../../jsonrpc/notification/systemLogNotification";
import { ComponentJsonApiRequest } from "../../jsonrpc/request/componentJsonApiRequest";
import { CreateComponentConfigRequest } from "../../jsonrpc/request/createComponentConfigRequest";
import { DeleteComponentConfigRequest } from "../../jsonrpc/request/deleteComponentConfigRequest";
import { EdgeRpcRequest } from "../../jsonrpc/request/edgeRpcRequest";
import { GetChannelRequest } from "../../jsonrpc/request/getChannelRequest";
import { GetChannelsOfComponentRequest } from "../../jsonrpc/request/getChannelsOfComponentRequest";
import { GetEdgeConfigRequest } from "../../jsonrpc/request/getEdgeConfigRequest";
import { GetPropertiesOfFactoryRequest } from "../../jsonrpc/request/getPropertiesOfFactoryRequest";
import { SubscribeChannelsRequest } from "../../jsonrpc/request/subscribeChannelsRequest";
import { SubscribeSystemLogRequest } from "../../jsonrpc/request/subscribeSystemLogRequest";
import { UpdateAppConfigRequest } from "../../jsonrpc/request/updateAppConfigRequest";
import { UpdateComponentConfigRequest } from "../../jsonrpc/request/updateComponentConfigRequest";
import { GetChannelResponse } from "../../jsonrpc/response/getChannelResponse";
import { Channel, GetChannelsOfComponentResponse } from "../../jsonrpc/response/getChannelsOfComponentResponse";
import { GetEdgeConfigResponse } from "../../jsonrpc/response/getEdgeConfigResponse";
import { GetPropertiesOfFactoryResponse } from "../../jsonrpc/response/getPropertiesOfFactoryResponse";
import { ChannelAddress, EdgePermission, SystemLog, Websocket } from "../../shared";
import { Role } from "../../type/role";
import { ArrayUtils } from "../../utils/array/ARRAY.UTILS";
import { NavigationId, NavigationTree } from "../navigation/shared";
import { Name } from "../shared/name";
import { CurrentData } from "./currentdata";
import { EdgeConfig } from "./edgeconfig";

export class Edge {

  // holds current data
  public currentData: BehaviorSubject<CurrentData> = new BehaviorSubject<CurrentData>(new CurrentData({}));

  // holds system log
  public systemLog: Subject<SystemLog> = new Subject<SystemLog>();

  // determine if subscribe on channels was successful
  // used in live component to hide elements while no channel data available
  public subscribeChannelsSuccessful: boolean = false;
  public isSubscribed: boolean = false;

  // holds config
  private config: BehaviorSubject<EdgeConfig> = new BehaviorSubject<EdgeConfig>(null);

  // holds currently subscribed channels, identified by source id
  private subscribedChannels: { [sourceId: string]: ChannelAddress[] } = {};
  private isRefreshConfigBlocked: boolean = false;
  private subscribeChannelsTimeout: any = null;


  constructor(
    public readonly id: string,
    public readonly comment: string,
    public readonly producttype: string,
    public readonly version: string,
    public readonly role: Role,
    public isOnline: boolean,
    public readonly lastmessage: Date,
    public readonly sumState: SumState,
    public readonly firstSetupProtocol: Date,
  ) { }

  setIsSubscribed(isSubscribed: boolean) {
    THIS.IS_SUBSCRIBED = isSubscribed;
  }
  /**
   * Gets the Config. If not available yet, it requests it via Websocket.
   *
   * @param websocket the Websocket connection
   */
  public getConfig(websocket: Websocket): BehaviorSubject<EdgeConfig> {
    if (THIS.CONFIG.VALUE == null || !THIS.CONFIG.VALUE.IS_VALID()) {
      THIS.REFRESH_CONFIG(websocket);
    }
    return THIS.CONFIG;
  }

  /**
   * Gets the current config. If not available null.
   */
  public getCurrentConfig(): EdgeConfig | null {
    return THIS.CONFIG.VALUE;
  }

  /**
   * Gets the first valid Config. If not available yet, it requests it via Websocket.
   *
   * @param websocket the Websocket connection
   */
  public getFirstValidConfig(websocket: Websocket): Promise<EdgeConfig> {
    return THIS.GET_CONFIG(websocket)
      .pipe(filter(config => config != null && CONFIG.IS_VALID()),
        first())
      .toPromise();
  }

  /**
   * Gets a channel either from {@link EdgeConfig edgeconfig} or requests it from the edge.
   *
   * @param websocket the websocket to send a request if the
   *                  channel is not included in the edgeconfig
   * @param channel   the address of the channel to get
   * @returns a promise of the found channel
   */
  public async getChannel(websocket: Websocket, channel: ChannelAddress): Promise<Channel> {
    if (EDGE_PERMISSION.HAS_CHANNELS_IN_EDGE_CONFIG(this)) {
      const config = await THIS.GET_FIRST_VALID_CONFIG(websocket);
      const foundChannel = CONFIG.GET_CHANNEL(channel);
      if (!foundChannel) {
        throw new Error("Channel not found: " + channel);
      }
      return { id: CHANNEL.CHANNEL_ID, ...foundChannel };
    }

    const response = await THIS.SEND_REQUEST<GetChannelResponse>(websocket, new ComponentJsonApiRequest({
      componentId: "_componentManager",
      payload: new GetChannelRequest({
        componentId: CHANNEL.COMPONENT_ID,
        channelId: CHANNEL.CHANNEL_ID,
      }),
    }));

    return RESPONSE.RESULT.CHANNEL;
  }

  /**
   * Gets all channels of the component with the provided component id.
   *
   * @param websocket   the websocket to send a request if the
   *                    channels are not included in the edgeconfig
   * @param componentId the id of the component
   * @returns a promise with the reuslt channels
   */
  public async getChannels(websocket: Websocket, componentId: string): Promise<Channel[]> {
    if (EDGE_PERMISSION.HAS_CHANNELS_IN_EDGE_CONFIG(this)) {
      const config = await THIS.GET_FIRST_VALID_CONFIG(websocket);
      const component = CONFIG.COMPONENTS[componentId];
      if (!component) {
        throw new Error("Component not found");
      }
      return OBJECT.ENTRIES(COMPONENT.CHANNELS).reduce((p, c) => {
        return [...p, { id: c[0], ...c[1] }];
      }, []);
    }

    const response = await THIS.SEND_REQUEST<GetChannelsOfComponentResponse>(websocket, new ComponentJsonApiRequest({
      componentId: "_componentManager",
      payload: new GetChannelsOfComponentRequest({ componentId: componentId }),
    }));

    return RESPONSE.RESULT.CHANNELS;
  }

  public async getFactoryProperties(websocket: Websocket, factoryId: string): Promise<[EDGE_CONFIG.FACTORY, EDGE_CONFIG.FACTORY_PROPERTY[]]> {
    if (EDGE_PERMISSION.HAS_REDUCED_FACTORIES(this)) {
      const response = await THIS.SEND_REQUEST<GetPropertiesOfFactoryResponse>(websocket, new ComponentJsonApiRequest({
        componentId: "_componentManager",
        payload: new GetPropertiesOfFactoryRequest({ factoryId }),
      }));
      return [RESPONSE.RESULT.FACTORY, RESPONSE.RESULT.PROPERTIES];
    }

    const factory = (await THIS.GET_FIRST_VALID_CONFIG(websocket)).factories[factoryId];
    return [factory, FACTORY.PROPERTIES];
  }

  /**
   * Called by Service, when this Edge is set as currentEdge.
   */
  public markAsCurrentEdge(websocket: Websocket): void {
    THIS.GET_CONFIG(websocket);
  }

  /**
   * Add Channels to subscription
   *
   * @param websocket the Websocket
   * @param id        a unique ID for this subscription (E.G. the component selector)
   * @param channels  the subscribed Channel-Addresses
   */
  public subscribeChannels(websocket: Websocket, id: string, channels: ChannelAddress[]): void {
    THIS.SUBSCRIBED_CHANNELS[id] = channels;
    THIS.SEND_SUBSCRIBE_CHANNELS(websocket);
  }

  /**
   * Refreshes Channels subscriptions on websocket reconnect.
   *
   * @param websocket the Websocket
   */
  public subscribeChannelsOnReconnect(websocket: Websocket): void {
    if (OBJECT.KEYS(THIS.SUBSCRIBED_CHANNELS).length > 0) {
      THIS.SEND_SUBSCRIBE_CHANNELS(websocket);
    }
  }

  /**
   * Removes Channels from subscription
   *
   * @param websocket the Websocket
   * @param id        the unique ID for this subscription
   * @deprecated Use `unsubscribeFromChannels` instead.
   *
   * @todo should be removed
   */
  public unsubscribeChannels(websocket: Websocket, id: string): void {
    delete THIS.SUBSCRIBED_CHANNELS[id];
    THIS.SEND_SUBSCRIBE_CHANNELS(websocket);
  }

  /**
   * Removes all Channels from subscription
   *
   * @param websocket the Websocket
   */
  public unsubscribeAllChannels(websocket: Websocket) {
    THIS.SUBSCRIBED_CHANNELS = {};
    THIS.SEND_SUBSCRIBE_CHANNELS(websocket);
  }

  /**
   * Unsubscribes from passed channels
   *
   * @param websocket the Websocket
   * @param channels the channels
   *
   * @todo should be renamed to `unsubscribeChannels` after unsubscribeChannels is removed
   */
  public unsubscribeFromChannels(websocket: Websocket, channels: ChannelAddress[]) {
    THIS.SUBSCRIBED_CHANNELS = OBJECT.ENTRIES(THIS.SUBSCRIBED_CHANNELS).reduce((arr, [id, subscribedChannels]) => {

      if (ARRAY_UTILS.EQUALS_CHECK(CHANNELS.MAP(channel => CHANNEL.TO_STRING()), SUBSCRIBED_CHANNELS.MAP(channel => CHANNEL.TO_STRING()))) {
        return arr;
      }

      arr[id] = subscribedChannels;

      return arr;
    }, {});

    THIS.SEND_SUBSCRIBE_CHANNELS(websocket);
  }

  /**
   * Subscribe to System-Log
   *
   * @param websocket the Websocket
   */
  public subscribeSystemLog(websocket: Websocket): Promise<JsonrpcResponseSuccess> {
    return THIS.SEND_REQUEST(websocket, new SubscribeSystemLogRequest({ subscribe: true }));
  }

  /**
   * Unsubscribe from System-Log
   *
   * @param websocket the Websocket
   */
  public unsubscribeSystemLog(websocket: Websocket): Promise<JsonrpcResponseSuccess> {
    return THIS.SEND_REQUEST(websocket, new SubscribeSystemLogRequest({ subscribe: false }));
  }

  /**
   * Handles a EdgeConfigNotification
   */
  public handleEdgeConfigNotification(message: EdgeConfigNotification): void {
    THIS.CONFIG.NEXT(new EdgeConfig(this, MESSAGE.PARAMS));
  }

  /**
   * Handles a CurrentDataNotification
   */
  public handleCurrentDataNotification(message: CurrentDataNotification): void {
    THIS.CURRENT_DATA.NEXT(new CurrentData(MESSAGE.PARAMS));
  }

  /**
   * Handles a SystemLogNotification
   */
  public handleSystemLogNotification(message: SystemLogNotification): void {
    THIS.SYSTEM_LOG.NEXT(MESSAGE.PARAMS.LINE);
  }

  /**
   * Creates a configuration for a OpenEMS Edge Component.
   *
   * @param ws          the Websocket
   * @param factoryPId  the OpenEMS Edge Factory-PID
   * @param properties  the properties to be updated.
   */
  public createComponentConfig(ws: Websocket, factoryPid: string, properties: { name: string, value: any }[]): Promise<JsonrpcResponseSuccess> {
    const request = new CreateComponentConfigRequest({ factoryPid: factoryPid, properties: properties });
    return THIS.SEND_REQUEST(ws, request);
  }

  /**
   * Updates the configuration of a OpenEMS Edge Component.
   *
   * @param ws          the Websocket
   * @param componentId the OpenEMS Edge Component-ID
   * @param properties  the properties to be updated.
   */
  public updateComponentConfig(ws: Websocket, componentId: string, properties: { name: string, value: any }[]): Promise<JsonrpcResponseSuccess> {
    const request = new UpdateComponentConfigRequest({ componentId: componentId, properties: properties });
    return THIS.SEND_REQUEST(ws, request);
  }

  /**
   * Deletes the configuration of a OpenEMS Edge Component.
   *
   * @param ws          the Websocket
   * @param componentId the OpenEMS Edge Component-ID
   */
  public deleteComponentConfig(ws: Websocket, componentId: string): Promise<JsonrpcResponseSuccess> {
    const request = new DeleteComponentConfigRequest({ componentId: componentId });
    return THIS.SEND_REQUEST(ws, request);
  }

  /**
   * Sends a JSON-RPC Request. The Request is wrapped in a EdgeRpcRequest.
   *
   * @param ws               the Websocket
   * @param request          the JSON-RPC Request
   * @param responseCallback the JSON-RPC Response callback
   */
  public sendRequest<T = JsonrpcResponseSuccess>(ws: Websocket, request: JsonrpcRequest): Promise<T> {
    const wrap = new EdgeRpcRequest({ edgeId: THIS.ID, payload: request });
    return new Promise((resolve, reject) => {
      WS.SEND_REQUEST(wrap).then(response => {
        resolve(response["result"]["payload"]);
      }).catch(reason => {
        reject(reason);
      });
    });
  }

  /**
   * Updates the configuration of a OpenEMS Edge App.
   *
   * @param ws          the Websocket
   * @param componentId the OpenEMS Edge Component-ID that the app is searched by
   * @param properties  the properties to be updated.
   */
  public updateAppConfig(ws: Websocket, componentId: string, properties: { name: string, value: string | number | boolean }[]): Promise<JsonrpcResponseSuccess> {
    let request;
    if (!hasUpdateAppVersion(this)) {
      request = new UpdateComponentConfigRequest({ componentId: componentId, properties: properties });
    } else {
      const jsonObject = PROPERTIES.REDUCE((acc, current) => {
        acc[CURRENT.NAME] = CURRENT.VALUE;
        return acc;
      }, {});
      const payload = new UpdateAppConfigRequest({ componentId: componentId, properties: jsonObject });
      request = new ComponentJsonApiRequest({
        componentId: "_appManager",
        payload: payload,
      });
    }
    return THIS.SEND_REQUEST(ws, request);
  }

  /**
   * Mark this edge as online or offline
   *
   * @param isOnline
   */
  public setOnline(isOnline: boolean): void {
    THIS.IS_ONLINE = isOnline;
  }

  /**
   * Returns whether the given version is higher than the Edge' version
   *
   * Example: {{ EDGE.IS_VERSION_AT_LEAST('2018.9') }}
   *
   * @param version
   */
  public isVersionAtLeast(version: string): boolean {
    return compareVersions(THIS.VERSION, version) >= 0;
  }

  /**
   * Determines if the version of the edge is a SNAPSHOT.
   *
   * <p>
   * Version strings are built like `MAJOR.MINOR.PATCH-BRANCH.DATE.HASH`. So any version string that contains a hyphen is a SNAPSHOT.
   *
   * @returns true if the version of the edge is a SNAPSHOT
   */
  public isSnapshot(): boolean {
    return THIS.VERSION.INCLUDES("-");
  }

  /**
   * Evaluates whether the current Role is equal or more privileged than the
   * given Role.
   *
   * @param role     the compared Role
   * @return true if the current Role is equal or more privileged than the given Role
   */
  public roleIsAtLeast(role: Role | string): boolean {
    return ROLE.IS_AT_LEAST(THIS.ROLE, role);
  }

  /**
   * Gets the Role of the Edge as a human-readable string.
  *
  * @returns the name of the role
  */
  public getRoleString(): string {
    return Role[THIS.ROLE].toLowerCase();
  }

  /**
   * Gets the navigation tree
   *
   * @param navigationTree current navigation tree
   * @param translate the translate
   * @returns the new navigation tree
   */
  public async createNavigationTree(translate: TranslateService, edge: Edge): Promise<NavigationTree> {
    const baseNavigationTree: (translate: TranslateService) => ConstructorParameters<typeof NavigationTree> = (translate) => [
      NAVIGATION_ID.LIVE, "live", { name: "home-outline" }, "live", "icon", [],
      null,
    ];

    const _baseNavigationTree: ConstructorParameters<typeof NavigationTree> = baseNavigationTree(translate).slice() as ConstructorParameters<typeof NavigationTree>;
    const navigationTree = new NavigationTree(..._baseNavigationTree);

    // TODO find automated way to create reference for parents
    NAVIGATION_TREE.SET_CHILD(NAVIGATION_ID.LIVE, new NavigationTree(NAVIGATION_ID.HISTORY, "history", { name: "stats-chart-outline" }, TRANSLATE.INSTANT("GENERAL.HISTORY"), "label", [], null));

    if (EDGE.IS_ONLINE === false) {
      return navigationTree;
    }

    const conf = await THIS.CONFIG.GET_VALUE();
    const baseMode: NavigationTree["mode"] = "label";
    for (const [componentId, component] of OBJECT.ENTRIES(CONF.COMPONENTS)) {
      switch (COMPONENT.FACTORY_ID) {
        case "EVSE.CONTROLLER.SINGLE":
          NAVIGATION_TREE.SET_CHILD(NAVIGATION_ID.LIVE,
            new NavigationTree(
              componentId, "evse/" + componentId, { name: "oe-evcs", color: "success" }, Name.METER_ALIAS_OR_ID(component), baseMode, [
              ...(THIS.ROLE_IS_AT_LEAST(ROLE.ADMIN)
                ? [new NavigationTree("forecast", "forecast", { name: "stats-chart-outline", color: "success" }, TRANSLATE.INSTANT("INSTALLATION.CONFIGURATION_EXECUTE.PROGNOSIS"), baseMode, [], null)]
                : []),

              new NavigationTree("history", "history", { name: "stats-chart-outline", color: "warning" }, TRANSLATE.INSTANT("GENERAL.HISTORY"), baseMode, [], null),
              new NavigationTree("settings", "settings", { name: "settings-outline", color: "medium" }, TRANSLATE.INSTANT("MENU.SETTINGS"), baseMode, [], null),
            ], navigationTree));
          break;
        case "CONTROLLER.IO.HEATING.ROOM":
          NAVIGATION_TREE.SET_CHILD(NAVIGATION_ID.LIVE,
            new NavigationTree(
              componentId, "io-heating-room/" + componentId, { name: "flame", color: "danger" }, Name.METER_ALIAS_OR_ID(component), baseMode, [],
              navigationTree,));
          break;
      }
    }

    return navigationTree;
  }

  /**
 * Refresh the config.
 */
  private refreshConfig(websocket: Websocket): void {
    // make sure to send not faster than every 1000 ms
    if (THIS.IS_REFRESH_CONFIG_BLOCKED) {
      return;
    }
    // block refreshConfig()
    THIS.IS_REFRESH_CONFIG_BLOCKED = true;
    setTimeout(() => {
      // unblock refreshConfig()
      THIS.IS_REFRESH_CONFIG_BLOCKED = false;
    }, 1000);

    const request = new GetEdgeConfigRequest();
    THIS.SEND_REQUEST(websocket, request).then(response => {
      const edgeConfigResponse = response as GetEdgeConfigResponse;
      THIS.CONFIG.NEXT(new EdgeConfig(this, EDGE_CONFIG_RESPONSE.RESULT));
    }).catch(reason => {
      CONSOLE.WARN("Unable to refresh config", reason);
      THIS.CONFIG.NEXT(new EdgeConfig(this));
    });
  }

  /**
 * Sends a SubscribeChannelsRequest for all Channels in 'THIS.SUBSCRIBED_CHANNELS'
 *
 * @param websocket the Websocket
 */
  private sendSubscribeChannels(websocket: Websocket): void {
    // make sure to send not faster than every 100 ms
    if (THIS.SUBSCRIBE_CHANNELS_TIMEOUT == null) {
      THIS.SUBSCRIBE_CHANNELS_TIMEOUT = setTimeout(() => {
        // reset subscribeChannelsTimeout
        THIS.SUBSCRIBE_CHANNELS_TIMEOUT = null;

        // merge channels from currentDataSubscribes
        const channels: ChannelAddress[] = [];
        for (const componentId in THIS.SUBSCRIBED_CHANNELS) {
          CHANNELS.PUSH(...THIS.SUBSCRIBED_CHANNELS[componentId]);
        }
        const request = new SubscribeChannelsRequest(channels);
        THIS.SEND_REQUEST(websocket, request).then(() => {
          THIS.SUBSCRIBE_CHANNELS_SUCCESSFUL = true;
        }).catch(reason => {
          THIS.SUBSCRIBE_CHANNELS_SUCCESSFUL = false;
          CONSOLE.WARN(reason);
        });
      }, 100);
    }
  }

}
