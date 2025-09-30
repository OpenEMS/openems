// @ts-strict-ignore
import { Component } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { PersistencePriority } from "src/app/shared/components/edge/edgeconfig";

import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { GetChannelsOfComponentRequest } from "src/app/shared/jsonrpc/request/getChannelsOfComponentRequest";
import { SetChannelValueRequest } from "src/app/shared/jsonrpc/request/setChannelValueRequest";
import { Channel, GetChannelsOfComponentResponse } from "src/app/shared/jsonrpc/response/getChannelsOfComponentResponse";
import { environment } from "src/environments";
import { ChannelAddress, Edge, EdgeConfig, EdgePermission, Service, Websocket } from "../../../shared/shared";

@Component({
  selector: CHANNELS_COMPONENT.SELECTOR,
  templateUrl: "./CHANNELS.COMPONENT.HTML",
  standalone: false,
})
export class ChannelsComponent {

  private static readonly SELECTOR = "channels";
  private static readonly URL_PREFIX = "channels";
  public customAlertOptions: any = {
    cssClass: "wide-alert",
  };

  protected isAtLeastOneChannelExistingInEdgeConfig: boolean = false;
  protected readonly spinnerId = CHANNELS_COMPONENT.SELECTOR;
  protected readonly environment = environment;
  protected edge: Edge | null = null;
  protected config: EdgeConfig | null = null;
  protected channelsPerComponent = new Map<string, ComponentChannels>();
  protected selectedComponentChannels = new Map<string, Map<string, { showPersistencePriority: boolean }>>();
  // TODO should be a simple SET but equality checking in SETs is currently not changeable and therefore not very useful for objects
  private subscribedChannels = new Map<string, ChannelAddress>();
  private persistencePriority: string = PersistencePriority.DEFAULT_GLOBAL_PRIORITY;

  constructor(
    private service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute,
    private router: Router,
    protected translate: TranslateService,
  ) { }

  private static readonly ERROR_COMPONENT_COULD_NOT_BE_FOUND = (componentId: string) => `[ComponentId] ${componentId} doesn't exist on this edge`;

  ionViewWillEnter() {
    THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
      THIS.EDGE = edge;
    });
    THIS.SERVICE.GET_CONFIG().then(config => {
      THIS.CONFIG = config;
      THIS.PERSISTENCE_PRIORITY = THIS.CONFIG.GET_COMPONENTS_BY_FACTORY("CONTROLLER.API.BACKEND")?.[0]?.properties["persistencePriority"] ?? PersistencePriority.DEFAULT_GLOBAL_PRIORITY;
      THIS.SERVICE.START_SPINNER(THIS.SPINNER_ID);
      THIS.LOAD_SAVED_CHANNELS().then(message => {
        if (message) {
          THIS.SERVICE.TOAST(message, "success");
        }
      }).catch(reason => {
        THIS.SERVICE.TOAST(reason, "danger");
        THIS.SELECTED_COMPONENT_CHANNELS = new Map();
        THIS.IS_AT_LEAST_ONE_CHANNEL_EXISTING_IN_EDGE_CONFIG = true;
      }).finally(() => {
        THIS.SERVICE.STOP_SPINNER(THIS.SPINNER_ID);
      });
    });
  }

  ionViewDidLeave() {
    THIS.SELECTED_COMPONENT_CHANNELS = new Map();
    THIS.EDGE?.unsubscribeChannels(THIS.WEBSOCKET, CHANNELS_COMPONENT.SELECTOR);
  }

  /**
   * Subscribes a channel.
   *
   * @param componentId the componentId
   * @param channelId the channelId
   */
  protected async subscribeChannel(componentId: string, channelId: string): Promise<void> {
    const channelEntry = {
      showPersistencePriority: true,
    };
    let selectedChannels = THIS.SELECTED_COMPONENT_CHANNELS.GET(componentId);
    if (!selectedChannels) {
      selectedChannels = new Map();
      THIS.SELECTED_COMPONENT_CHANNELS.SET(componentId, selectedChannels);
    }
    SELECTED_CHANNELS.SET(channelId, channelEntry);

    const channelData = await THIS.GET_CHANNEL(componentId, channelId);
    CHANNEL_ENTRY.SHOW_PERSISTENCE_PRIORITY = PERSISTENCE_PRIORITY.IS_LESS_THAN(CHANNEL_DATA.PERSISTENCE_PRIORITY, THIS.PERSISTENCE_PRIORITY);

    if (CHANNEL_DATA.ACCESS_MODE != "WO") {
      const channelAddress = new ChannelAddress(componentId, channelId);
      THIS.SUBSCRIBED_CHANNELS.SET(CHANNEL_ADDRESS.TO_STRING(), channelAddress);
      if (THIS.EDGE) {
        THIS.EDGE.SUBSCRIBE_CHANNELS(THIS.WEBSOCKET, CHANNELS_COMPONENT.SELECTOR, ARRAY.FROM(THIS.SUBSCRIBED_CHANNELS.VALUES()));
      }
    }
    THIS.SAVE_CHANNELS_IN_URL();
  }

  /**
   * Unsubscribes a channel
   *
   * @param channelAddress the channelAddress to be unsubscribed
   */
  protected unsubscribeChannel(componentId: string, channelId: string): void {
    const channels = THIS.SELECTED_COMPONENT_CHANNELS.GET(componentId);
    if (channels) {
      CHANNELS.DELETE(channelId);

      if (CHANNELS.SIZE === 0) {
        THIS.SELECTED_COMPONENT_CHANNELS.DELETE(componentId);
      }
    }

    const channelAddress = new ChannelAddress(componentId, channelId);
    if (THIS.SUBSCRIBED_CHANNELS.DELETE(CHANNEL_ADDRESS.TO_STRING())) {
      THIS.EDGE.SUBSCRIBE_CHANNELS(THIS.WEBSOCKET, CHANNELS_COMPONENT.SELECTOR, ARRAY.FROM(THIS.SUBSCRIBED_CHANNELS.VALUES()));
    }

    THIS.SAVE_CHANNELS_IN_URL();
  }

  protected setChannelValue(componentId: string, channelId: string, channelValue: any) {
    if (THIS.EDGE) {
      THIS.EDGE.SEND_REQUEST(
        THIS.SERVICE.WEBSOCKET,
        new SetChannelValueRequest({
          componentId: componentId,
          channelId: channelId,
          value: channelValue,
        }),
      ).then(() => {
        THIS.SERVICE.TOAST("Successfully set " + componentId + "/" + channelId + " to [" + channelValue + "]", "success");
      }).catch(() => {
        THIS.SERVICE.TOAST("Error setting " + componentId + "/" + channelId + " to [" + channelValue + "]", "danger");
      });
    }
  }

  protected saveChannelsInLocalStorage() {
    const selectedChannels = THIS.GET_SELECTED_CHANNELS();
    if (selectedChannels && SELECTED_CHANNELS.LENGTH > 0) {
      LOCAL_STORAGE.SET_ITEM(ChannelsComponent.URL_PREFIX + "-" + THIS.EDGE.ID, JSON.STRINGIFY(selectedChannels));
    } else {
      LOCAL_STORAGE.REMOVE_ITEM(ChannelsComponent.URL_PREFIX + "-" + THIS.EDGE.ID);
    }
    THIS.SERVICE.TOAST("Successfully saved subscribed channels", "success");
  }

  protected onSelectedComponentChanged(event) {
    const componentId: string = EVENT.DETAIL.VALUE;

    if (!componentId || THIS.CHANNELS_PER_COMPONENT.HAS(componentId)) {
      return;
    }

    THIS.LOAD_CHANNELS_AND_STORE(componentId).then(() => {
      // ignore
    }).catch(reason => {
      THIS.SERVICE.TOAST("Unable to load channels for " + componentId + ": " + reason, "danger");
    });
  }

  private saveChannelsInUrl(): void {
    const selectedChannels = THIS.GET_SELECTED_CHANNEL_STRINGS();
    if (selectedChannels && SELECTED_CHANNELS.LENGTH > 0) {
      THIS.ROUTER.NAVIGATE(["device/" + (THIS.EDGE.ID) + "/settings/channels/"], { queryParams: { save: SELECTED_CHANNELS.TO_STRING() } });
      THIS.IS_AT_LEAST_ONE_CHANNEL_EXISTING_IN_EDGE_CONFIG = false;
    } else {
      THIS.ROUTER.NAVIGATE(["device/" + (THIS.EDGE.ID) + "/settings/channels/"]);
    }
  }

  private getSelectedChannelStrings(): string[] {
    return THIS.GET_SELECTED_CHANNELS().map(e => E.TO_STRING());
  }

  private getSelectedChannels(): ChannelAddress[] {
    const channels: ChannelAddress[] = [];
    for (const [componentId, value] of THIS.SELECTED_COMPONENT_CHANNELS.ENTRIES()) {
      for (const [channelId] of VALUE.ENTRIES()) {
        CHANNELS.PUSH(new ChannelAddress(componentId, channelId));
      }
    }
    return channels;
  }

  private async loadSavedChannels(): Promise<string> {
    const address = THIS.ROUTE.SNAPSHOT.QUERY_PARAM_MAP.GET("save");
    if (address) {
      const channels = ADDRESS.SPLIT(",")?.map(element => CHANNEL_ADDRESS.FROM_STRING(element));
      try {
        const existingComponents = CHANNELS.FILTER(el => EL.COMPONENT_ID in THIS.CONFIG.COMPONENTS);

        if (EXISTING_COMPONENTS.LENGTH > 1) {
          THIS.IS_AT_LEAST_ONE_CHANNEL_EXISTING_IN_EDGE_CONFIG = true;
          return "No component matches this edges components";
        }

        await PROMISE.ALL(CHANNELS.MAP(el => THIS.SUBSCRIBE_CHANNEL(EL.COMPONENT_ID, EL.CHANNEL_ID)));
        return "Successfully loaded saved channels from url";
      } catch (reason) {
        throw "Some channels may not have been loaded from url: " + reason;
      }
    }

    const storedValue = LOCAL_STORAGE.GET_ITEM(ChannelsComponent.URL_PREFIX + "-" + THIS.EDGE.ID);
    if (storedValue) {
      const savedData: ChannelAddress[] = JSON.PARSE(storedValue);
      try {
        await PROMISE.ALL(SAVED_DATA.MAP(el => THIS.SUBSCRIBE_CHANNEL(EL.COMPONENT_ID, EL.CHANNEL_ID)));
        return "Successfully loaded saved channels from session";
      } catch (reason) {
        throw "Some channels may not have been loaded from session: " + reason;
      }
    }
  }

  private getChannel(componentId: string, channelId: string): Promise<Channel> {
    return new Promise((resolve, reject) => {
      // check if channels of component are already loaded
      const componentEntry = THIS.CHANNELS_PER_COMPONENT.GET(componentId);
      if (componentEntry && !COMPONENT_ENTRY.ACTIVE_REQUEST) {
        const channel = COMPONENT_ENTRY.CHANNELS[channelId];
        if (channel) {
          resolve(channel);
        } else {
          reject(channelId + " is not defined by component " + componentId);
        }
        return;
      }
      // get channels from edge and store
      THIS.LOAD_CHANNELS_AND_STORE(componentId).then(channels => {
        const channel = CHANNELS.CHANNELS[channelId];
        if (channel) {
          resolve(channel);
        } else {
          reject(channelId + " is not defined by component " + componentId);
        }
      }).catch(reject);
    });
  }

  private loadChannels(componentId: string): Promise<Channel[]> {
    return new Promise((resolve, reject) => {
      if (EDGE_PERMISSION.HAS_CHANNELS_IN_EDGE_CONFIG(THIS.EDGE)) {
        const component = THIS.CONFIG.COMPONENTS[componentId];
        if (!component) {
          reject();
          return;
        }
        const channels: Channel[] = [];
        for (const [key, value] of OBJECT.ENTRIES(COMPONENT.CHANNELS)) {
          CHANNELS.PUSH({
            id: key,
            ...value,
          });
        }
        resolve(channels);
        return;
      }

      if (!(componentId in THIS.CONFIG.COMPONENTS)) {
        CONSOLE.WARN(ChannelsComponent.ERROR_COMPONENT_COULD_NOT_BE_FOUND(componentId));
        THIS.IS_AT_LEAST_ONE_CHANNEL_EXISTING_IN_EDGE_CONFIG = true;
        reject();
        return;
      }

      THIS.EDGE.SEND_REQUEST(THIS.WEBSOCKET, new ComponentJsonApiRequest({
        componentId: "_componentManager",
        payload: new GetChannelsOfComponentRequest({ componentId: componentId }),
      })).then((response: GetChannelsOfComponentResponse) => {
        resolve(RESPONSE.RESULT.CHANNELS);
      }).catch(reject);
    });
  }

  private loadChannelsAndStore(componentId: string): Promise<ComponentChannels> {
    return new Promise((resolve, reject) => {
      if (!componentId) {
        reject();
        return;
      }
      let componentEntry: ComponentChannels;
      if (!THIS.CHANNELS_PER_COMPONENT.HAS(componentId)) {
        componentEntry = { channels: {} };
        THIS.CHANNELS_PER_COMPONENT.SET(componentId, componentEntry);
      } else {
        componentEntry = THIS.CHANNELS_PER_COMPONENT.GET(componentId);
      }

      THIS.SERVICE.START_SPINNER_TRANSPARENT_BACKGROUND(componentId);
      const request = COMPONENT_ENTRY.ACTIVE_REQUEST ?? (COMPONENT_ENTRY.ACTIVE_REQUEST = THIS.LOAD_CHANNELS(componentId));
      REQUEST.THEN(channels => {
        CHANNELS.FOR_EACH(channel => {
          COMPONENT_ENTRY.CHANNELS[CHANNEL.ID] = channel;
        });
        resolve(componentEntry);
      }).catch(reject)
        .finally(() => {
          COMPONENT_ENTRY.ACTIVE_REQUEST = undefined;
          THIS.SERVICE.STOP_SPINNER(componentId);
        });
    });
  }

}

type ComponentChannels = {
  activeRequest?: Promise<Channel[]>,
  channels: { [channelId: string]: Channel },
};
