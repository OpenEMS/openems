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
import { Widgets } from "../../type/widgets";
import { ArrayUtils } from "../../utils/array/array.utils";
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
    this.isSubscribed = isSubscribed;
  }
  /**
   * Gets the Config. If not available yet, it requests it via Websocket.
   *
   * @param websocket the Websocket connection
   */
  public getConfig(websocket: Websocket): BehaviorSubject<EdgeConfig> {
    if (this.config.value == null || !this.config.value.isValid()) {
      this.refreshConfig(websocket);
    }
    return this.config;
  }

  /**
   * Gets the current config. If not available null.
   */
  public getCurrentConfig(): EdgeConfig | null {
    return this.config.value;
  }

  /**
   * Gets the first valid Config. If not available yet, it requests it via Websocket.
   *
   * @param websocket the Websocket connection
   */
  public getFirstValidConfig(websocket: Websocket): Promise<EdgeConfig> {
    return this.getConfig(websocket)
      .pipe(filter(config => config != null && config.isValid()),
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
    if (EdgePermission.hasChannelsInEdgeConfig(this)) {
      const config = await this.getFirstValidConfig(websocket);
      const foundChannel = config.getChannel(channel);
      if (!foundChannel) {
        throw new Error("Channel not found: " + channel);
      }
      return { id: channel.channelId, ...foundChannel };
    }

    const response = await this.sendRequest<GetChannelResponse>(websocket, new ComponentJsonApiRequest({
      componentId: "_componentManager",
      payload: new GetChannelRequest({
        componentId: channel.componentId,
        channelId: channel.channelId,
      }),
    }));

    return response.result.channel;
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
    if (EdgePermission.hasChannelsInEdgeConfig(this)) {
      const config = await this.getFirstValidConfig(websocket);
      const component = config.components[componentId];
      if (!component) {
        throw new Error("Component not found");
      }
      return Object.entries(component.channels).reduce((p, c) => {
        return [...p, { id: c[0], ...c[1] }];
      }, []);
    }

    const response = await this.sendRequest<GetChannelsOfComponentResponse>(websocket, new ComponentJsonApiRequest({
      componentId: "_componentManager",
      payload: new GetChannelsOfComponentRequest({ componentId: componentId }),
    }));

    return response.result.channels;
  }

  public async getFactoryProperties(websocket: Websocket, factoryId: string): Promise<[EdgeConfig.Factory, EdgeConfig.FactoryProperty[]]> {
    if (EdgePermission.hasReducedFactories(this)) {
      const response = await this.sendRequest<GetPropertiesOfFactoryResponse>(websocket, new ComponentJsonApiRequest({
        componentId: "_componentManager",
        payload: new GetPropertiesOfFactoryRequest({ factoryId }),
      }));
      return [response.result.factory, response.result.properties];
    }

    const factory = (await this.getFirstValidConfig(websocket)).factories[factoryId];
    return [factory, factory.properties];
  }

  /**
   * Called by Service, when this Edge is set as currentEdge.
   */
  public markAsCurrentEdge(websocket: Websocket): void {
    this.getConfig(websocket);
  }

  /**
   * Add Channels to subscription
   *
   * @param websocket the Websocket
   * @param id        a unique ID for this subscription (e.g. the component selector)
   * @param channels  the subscribed Channel-Addresses
   */
  public subscribeChannels(websocket: Websocket, id: string, channels: ChannelAddress[]): void {
    const previousChannels = Object.values(this.subscribedChannels).flat().map(channel => channel.toString());
    this.subscribedChannels[id] = channels;

    const channelsToSubscribe = channels.map(channel => channel.toString());

    if (ArrayUtils.containsAll({ strings: channelsToSubscribe, arr: previousChannels })) {
      return;
    }

    this.sendSubscribeChannels(websocket);
  }

  /**
   * Refreshes Channels subscriptions on websocket reconnect.
   *
   * @param websocket the Websocket
   */
  public subscribeChannelsOnReconnect(websocket: Websocket): void {
    if (Object.keys(this.subscribedChannels).length > 0) {
      this.sendSubscribeChannels(websocket);
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
    const subscribedChannelsById = this.subscribedChannels[id];
    delete this.subscribedChannels[id];

    const previousChannels = Object.values(this.subscribedChannels).flat().map(channel => channel.toString());
    const unsubscribeChannels = Object.values(subscribedChannelsById ?? {}).flat().map(channel => channel.toString());

    if (ArrayUtils.containsAll({ arr: previousChannels, strings: unsubscribeChannels })) {
      return;
    }

    this.sendSubscribeChannels(websocket);
  }

  /**
   * Removes all Channels from subscription
   *
   * @param websocket the Websocket
   */
  public unsubscribeAllChannels(websocket: Websocket) {
    this.subscribedChannels = {};
    this.sendSubscribeChannels(websocket);
  }

  /**
   * Unsubscribes from passed channels
   *
   * @param websocket the Websocket
   * @param channels the channels
   *
   * @todo should be renamed to `unsubscribeChannels` after unsubscribeChannels is removed
   */
  public unsubscribeFromChannels(subscribeId: string, websocket: Websocket, channels: ChannelAddress[]) {
    const subscribedChannels = Object.entries(this.subscribedChannels)
      .reduce((arr, [id, subscribedChannels]) => {

        const areChannelsEqual = ArrayUtils.equalsCheck(channels.map(channel => channel.toString()), subscribedChannels.map(channel => channel.toString()));
        const channelsUsedByOtherSubscriptions = Object.entries(this.subscribedChannels)
          .filter(([otherId, _]) => otherId !== id)
          .some(([_, otherSubscribedChannels]) => {
            return channels.some(channel => otherSubscribedChannels.some(osc => osc.toString() === channel.toString()));
          });

        if (areChannelsEqual && channelsUsedByOtherSubscriptions == false) {
          // removes matching channels from subscribedChannels
          return arr;
        }

        arr[id] = subscribedChannels;
        return arr;
      }, {});

    const previousChannels = Object.values(this.subscribedChannels)
      .map((channel) => channel.toString());
    const newChannels = Object.entries(subscribedChannels)
      .filter(([otherId, _]) => otherId !== subscribeId).map(([_, channel]) => channel.toString());

    if (ArrayUtils.containsAll({ strings: newChannels, arr: previousChannels }) && previousChannels.length === newChannels.length) {
      // no change in channels, do not send subscribe request
      return;
    }

    this.subscribedChannels = subscribedChannels;
    this.sendSubscribeChannels(websocket);
  }

  /**
   * Subscribe to System-Log
   *
   * @param websocket the Websocket
   */
  public subscribeSystemLog(websocket: Websocket): Promise<JsonrpcResponseSuccess> {
    return this.sendRequest(websocket, new SubscribeSystemLogRequest({ subscribe: true }));
  }

  /**
   * Unsubscribe from System-Log
   *
   * @param websocket the Websocket
   */
  public unsubscribeSystemLog(websocket: Websocket): Promise<JsonrpcResponseSuccess> {
    return this.sendRequest(websocket, new SubscribeSystemLogRequest({ subscribe: false }));
  }

  /**
   * Handles a EdgeConfigNotification
   */
  public handleEdgeConfigNotification(message: EdgeConfigNotification): void {
    this.config.next(new EdgeConfig(this, message.params));
  }

  /**
   * Handles a CurrentDataNotification
   */
  public handleCurrentDataNotification(message: CurrentDataNotification): void {
    this.currentData.next(new CurrentData(message.params));
  }

  /**
   * Handles a SystemLogNotification
   */
  public handleSystemLogNotification(message: SystemLogNotification): void {
    this.systemLog.next(message.params.line);
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
    return this.sendRequest(ws, request);
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
    return this.sendRequest(ws, request);
  }

  /**
   * Deletes the configuration of a OpenEMS Edge Component.
   *
   * @param ws          the Websocket
   * @param componentId the OpenEMS Edge Component-ID
   */
  public deleteComponentConfig(ws: Websocket, componentId: string): Promise<JsonrpcResponseSuccess> {
    const request = new DeleteComponentConfigRequest({ componentId: componentId });
    return this.sendRequest(ws, request);
  }

  /**
   * Sends a JSON-RPC Request. The Request is wrapped in a EdgeRpcRequest.
   *
   * @param ws               the Websocket
   * @param request          the JSON-RPC Request
   * @param responseCallback the JSON-RPC Response callback
   */
  public sendRequest<T = JsonrpcResponseSuccess>(ws: Websocket, request: JsonrpcRequest): Promise<T> {
    const wrap = new EdgeRpcRequest({ edgeId: this.id, payload: request });
    return new Promise((resolve, reject) => {
      ws.sendRequest(wrap).then(response => {
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
      const jsonObject = properties.reduce((acc, current) => {
        acc[current.name] = current.value;
        return acc;
      }, {});
      const payload = new UpdateAppConfigRequest({ componentId: componentId, properties: jsonObject });
      request = new ComponentJsonApiRequest({
        componentId: "_appManager",
        payload: payload,
      });
    }
    return this.sendRequest(ws, request);
  }

  /**
   * Mark this edge as online or offline
   *
   * @param isOnline
   */
  public setOnline(isOnline: boolean): void {
    this.isOnline = isOnline;
  }

  /**
   * Returns whether the given version is higher than the Edge' version
   *
   * Example: {{ edge.isVersionAtLeast('2018.9') }}
   *
   * @param version
   */
  public isVersionAtLeast(version: string): boolean {
    return compareVersions(this.version, version) >= 0;
  }

  /**
   * Determines if the version of the edge is a SNAPSHOT.
   *
   * <p>
   * Version strings are built like `major.minor.patch-branch.date.hash`. So any version string that contains a hyphen is a SNAPSHOT.
   *
   * @returns true if the version of the edge is a SNAPSHOT
   */
  public isSnapshot(): boolean {
    return this.version.includes("-");
  }

  /**
   * Evaluates whether the current Role is equal or more privileged than the
   * given Role.
   *
   * @param role     the compared Role
   * @return true if the current Role is equal or more privileged than the given Role
   */
  public roleIsAtLeast(role: Role | string): boolean {
    return Role.isAtLeast(this.role, role);
  }

  /**
   * Gets the Role of the Edge as a human-readable string.
  *
  * @returns the name of the role
  */
  public getRoleString(): string {
    return Role[this.role].toLowerCase();
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
      NavigationId.LIVE, { baseString: "live" }, { name: "home-outline" }, "live", "icon", [],
      null,
    ];

    const _baseNavigationTree: ConstructorParameters<typeof NavigationTree> = baseNavigationTree(translate).slice() as ConstructorParameters<typeof NavigationTree>;
    const navigationTree = new NavigationTree(..._baseNavigationTree);

    // TODO find automated way to create reference for parents
    navigationTree.setChild(NavigationId.LIVE, new NavigationTree(NavigationId.HISTORY, { baseString: "history" }, { name: "stats-chart-outline" }, translate.instant("GENERAL.HISTORY"), "label", [], null));

    if (edge.isOnline === false) {
      return navigationTree;
    }

    const conf = await this.config.getValue();
    this.addCommonWidgetNavigation(edge, conf, navigationTree, translate);
    const baseMode: NavigationTree["mode"] = "label";
    for (const [componentId, component] of Object.entries(conf.components)) {
      if (component.isEnabled == false) {
        continue;
      }

      switch (component.factoryId) {
        case "Evse.Controller.Single":
          navigationTree.setChild(NavigationId.LIVE,
            new NavigationTree(
              componentId, { baseString: "evse/" + componentId }, { name: "oe-evcs", color: "success" }, Name.METER_ALIAS_OR_ID(component), baseMode, [
              ...(this.roleIsAtLeast(Role.ADMIN)
                ? [new NavigationTree("forecast", { baseString: "forecast" }, { name: "stats-chart-outline", color: "success" }, translate.instant("INSTALLATION.CONFIGURATION_EXECUTE.PROGNOSIS"), baseMode, [], null)]
                : []),

              new NavigationTree("history", { baseString: "history" }, { name: "stats-chart-outline", color: "warning" }, translate.instant("GENERAL.HISTORY"), baseMode, [], null),
              new NavigationTree("settings", { baseString: "settings" }, { name: "settings-outline", color: "medium" }, translate.instant("MENU.SETTINGS"), baseMode, [], null),

              ...(this.roleIsAtLeast(Role.OWNER)
                ? [new NavigationTree("car", { baseString: "car/update/App.Evse.ElectricVehicle.Generic" }, { name: "car-sport-outline", color: "success" }, translate.instant("EVSE_SINGLE.HOME.VEHICLES"), baseMode, [], null)]
                : []),
            ], navigationTree));
          break;
        case "Controller.IO.Heating.Room":
          navigationTree.setChild(NavigationId.LIVE,
            new NavigationTree(
              componentId, { baseString: "io-heating-room/" + componentId }, { name: "flame", color: "danger" }, Name.METER_ALIAS_OR_ID(component), baseMode, [],
              navigationTree,));
          break;
      }
    }

    return navigationTree;
  }

  private addCommonWidgetNavigation(edge: Edge, conf: EdgeConfig, currentNavigationTree: NavigationTree, translate: TranslateService): void {
    const classes = Widgets.parseWidgets(edge, conf).classes;

    for (const clazz of classes) {
      const navigationTree: ConstructorParameters<typeof NavigationTree> | null = Widgets.getCommonNavigationTree(edge, clazz, translate, conf);

      if (navigationTree == null) {
        continue;
      }

      currentNavigationTree.setChild(NavigationId.LIVE, new NavigationTree(...navigationTree));
    }
  }

  /**
 * Refresh the config.
 */
  private refreshConfig(websocket: Websocket): void {
    // make sure to send not faster than every 1000 ms
    if (this.isRefreshConfigBlocked) {
      return;
    }
    // block refreshConfig()
    this.isRefreshConfigBlocked = true;
    setTimeout(() => {
      // unblock refreshConfig()
      this.isRefreshConfigBlocked = false;
    }, 1000);

    const request = new GetEdgeConfigRequest();
    this.sendRequest(websocket, request).then(response => {
      const edgeConfigResponse = response as GetEdgeConfigResponse;
      this.config.next(new EdgeConfig(this, edgeConfigResponse.result));
    }).catch(reason => {
      console.warn("Unable to refresh config", reason);
      this.config.next(new EdgeConfig(this));
    });
  }

  /**
 * Sends a SubscribeChannelsRequest for all Channels in 'this.subscribedChannels'
 *
 * @param websocket the Websocket
 */
  private sendSubscribeChannels(websocket: Websocket): void {
    // make sure to send not faster than every 100 ms
    if (this.subscribeChannelsTimeout == null) {
      this.subscribeChannelsTimeout = setTimeout(() => {
        // reset subscribeChannelsTimeout
        this.subscribeChannelsTimeout = null;

        // merge channels from currentDataSubscribes
        const channels: ChannelAddress[] = [];
        for (const componentId in this.subscribedChannels) {
          channels.push(...this.subscribedChannels[componentId]);
        }
        const request = new SubscribeChannelsRequest(channels);
        this.sendRequest(websocket, request).then(() => {
          this.subscribeChannelsSuccessful = true;
        }).catch(reason => {
          this.subscribeChannelsSuccessful = false;
          console.warn(reason);
        });
      }, 100);
    }
  }

}
