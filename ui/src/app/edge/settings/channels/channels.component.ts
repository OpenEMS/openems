// @ts-strict-ignore
import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { PersistencePriority } from 'src/app/shared/edge/edgeconfig';
import { SetChannelValueRequest } from 'src/app/shared/jsonrpc/request/setChannelValueRequest';
import { environment } from 'src/environments';

import { ChannelAddress, Edge, EdgeConfig, EdgePermission, Service, Websocket } from '../../../shared/shared';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { Channel, GetChannelsOfComponentResponse } from 'src/app/shared/jsonrpc/response/getChannelsOfComponentResponse';
import { GetChannelsOfComponentRequest } from 'src/app/shared/jsonrpc/request/getChannelsOfComponentRequest';

@Component({
  selector: ChannelsComponent.SELECTOR,
  templateUrl: './channels.component.html',
})
export class ChannelsComponent {

  private static readonly SELECTOR = "channels";
  private static readonly URL_PREFIX = "channels";
  protected readonly spinnerId = ChannelsComponent.SELECTOR;
  protected readonly environment = environment;
  protected edge: Edge = null;
  protected config: EdgeConfig = null;
  private persistencePriority: string = PersistencePriority.DEFAULT_GLOBAL_PRIORITY;
  protected channelsPerComponent = new Map<string, ComponentChannels>();

  protected selectedComponentChannels = new Map<string, Map<string, { showPersistencePriority: boolean }>>();
  // TODO should be a simple SET but equality checking in SETs is currently not changeable and therefore not very useful for objects
  private subscribedChannels = new Map<string, ChannelAddress>();

  constructor(
    private service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute,
    private router: Router,
    protected translate: TranslateService,
  ) { }

  public customAlertOptions: any = {
    cssClass: 'wide-alert',
  };

  ionViewWillEnter() {
    this.service.setCurrentComponent("Channels", this.route).then(edge => {
      this.edge = edge;
    });
    this.service.getConfig().then(config => {
      this.config = config;
      this.persistencePriority = this.config.getComponentsByFactory("Controller.Api.Backend")?.[0]?.properties['persistencePriority'] ?? PersistencePriority.DEFAULT_GLOBAL_PRIORITY;
      this.service.startSpinner(this.spinnerId);
      this.loadSavedChannels().then(message => {
        if (message) {
          this.service.toast(message, 'success');
        }
      }).catch(reason => {
        this.service.toast(reason, 'danger');
      }).finally(() => {
        this.service.stopSpinner(this.spinnerId);
      });
    });
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
    let selectedChannels = this.selectedComponentChannels.get(componentId);
    if (!selectedChannels) {
      selectedChannels = new Map();
      this.selectedComponentChannels.set(componentId, selectedChannels);
    }
    selectedChannels.set(channelId, channelEntry);

    const channelData = await this.getChannel(componentId, channelId);
    channelEntry.showPersistencePriority = PersistencePriority.isLessThan(channelData.persistencePriority, this.persistencePriority);

    if (channelData.accessMode != 'WO') {
      const channelAddress = new ChannelAddress(componentId, channelId);
      this.subscribedChannels.set(channelAddress.toString(), channelAddress);
      if (this.edge) {
        this.edge.subscribeChannels(this.websocket, ChannelsComponent.SELECTOR, Array.from(this.subscribedChannels.values()));
      }
    }
    this.saveChannelsInUrl();
  }

  /**
   * Unsubscribes a channel
   *
   * @param channelAddress the channelAddress to be unsubscribed
   */
  protected unsubscribeChannel(componentId: string, channelId: string): void {
    const channels = this.selectedComponentChannels.get(componentId);
    if (channels) {
      channels.delete(channelId);

      if (channels.size === 0) {
        this.selectedComponentChannels.delete(componentId);
      }
    }

    const channelAddress = new ChannelAddress(componentId, channelId);
    if (this.subscribedChannels.delete(channelAddress.toString())) {
      this.edge.subscribeChannels(this.websocket, ChannelsComponent.SELECTOR, Array.from(this.subscribedChannels.values()));
    }

    this.saveChannelsInUrl();
  }

  protected setChannelValue(address: ChannelAddress, channelValue: any) {
    if (this.edge) {
      this.edge.sendRequest(
        this.service.websocket,
        new SetChannelValueRequest({
          componentId: address.componentId,
          channelId: address.channelId,
          value: channelValue,
        }),
      ).then(() => {
        this.service.toast("Successfully set " + address.toString() + " to [" + channelValue + "]", "success");
      }).catch(() => {
        this.service.toast("Error setting " + address.toString() + " to [" + channelValue + "]", 'danger');
      });
    }
  }

  private saveChannelsInUrl(): void {
    const selectedChannels = this.getSelectedChannelStrings();
    if (selectedChannels && selectedChannels.length > 0) {
      this.router.navigate(['device/' + (this.edge.id) + '/settings/channels/'], { queryParams: { save: selectedChannels.toString() } });
    } else {
      this.router.navigate(['device/' + (this.edge.id) + '/settings/channels/']);
    }
  }

  protected saveChannelsInLocalStorage() {
    const selectedChannels = this.getSelectedChannels();
    if (selectedChannels && selectedChannels.length > 0) {
      localStorage.setItem(ChannelsComponent.URL_PREFIX + "-" + this.edge.id, JSON.stringify(selectedChannels));
    } else {
      localStorage.removeItem(ChannelsComponent.URL_PREFIX + "-" + this.edge.id);
    }
    this.service.toast("Successfully saved subscribed channels", "success");
  }

  private getSelectedChannelStrings(): string[] {
    return this.getSelectedChannels().map(e => e.toString());
  }

  private getSelectedChannels(): ChannelAddress[] {
    const channels: ChannelAddress[] = [];
    for (const [componentId, value] of this.selectedComponentChannels.entries()) {
      for (const [channelId] of value.entries()) {
        channels.push(new ChannelAddress(componentId, channelId));
      }
    }
    return channels;
  }

  private async loadSavedChannels(): Promise<string> {
    const address = this.route.snapshot.queryParamMap.get('save');
    if (address) {
      const channels = address.split(',')?.map(element => ChannelAddress.fromString(element));
      try {
        await Promise.all(channels.map(el => this.subscribeChannel(el.componentId, el.channelId)));
        return 'Successfully loaded saved channels from url';
      } catch (reason) {
        throw 'Some channels may not have been loaded from url: ' + reason;
      }
    }

    const storedValue = localStorage.getItem(ChannelsComponent.URL_PREFIX + "-" + this.edge.id);
    if (storedValue) {
      const savedData: ChannelAddress[] = JSON.parse(storedValue);
      try {
        await Promise.all(savedData.map(el => this.subscribeChannel(el.componentId, el.channelId)));
        return 'Successfully loaded saved channels from session';
      } catch (reason) {
        throw 'Some channels may not have been loaded from session: ' + reason;
      }
    }
  }

  protected onSelectedComponentChanged(event) {
    const componentId: string = event.detail.value;

    if (!componentId || this.channelsPerComponent.has(componentId)) {
      return;
    }

    this.loadChannelsAndStore(componentId).then(() => {
      // ignore
    }).catch(reason => {
      this.service.toast('Unable to load channels for ' + componentId + ': ' + reason, 'danger');
    });
  }

  private getChannel(componentId: string, channelId: string): Promise<Channel> {
    return new Promise((resolve, reject) => {
      // check if channels of component are already loaded
      const componentEntry = this.channelsPerComponent.get(componentId);
      if (componentEntry && !componentEntry.activeRequest) {
        const channel = componentEntry.channels[channelId];
        if (channel) {
          resolve(channel);
        } else {
          reject(channelId + ' is not defined by component ' + componentId);
        }
        return;
      }
      // get channels from edge and store
      this.loadChannelsAndStore(componentId).then(channels => {
        const channel = channels.channels[channelId];
        if (channel) {
          resolve(channel);
        } else {
          reject(channelId + ' is not defined by component ' + componentId);
        }
      }).catch(reject);
    });
  }

  private loadChannels(componentId: string): Promise<Channel[]> {
    return new Promise((resolve, reject) => {
      if (EdgePermission.hasChannelsInEdgeConfig(this.edge)) {
        const component = this.config.components[componentId];
        if (!component) {
          reject();
          return;
        }
        const channels: Channel[] = [];
        for (const [key, value] of Object.entries(component.channels)) {
          channels.push({
            id: key,
            ...value,
          });
        }
        resolve(channels);
        return;
      }

      this.edge.sendRequest(this.websocket, new ComponentJsonApiRequest({
        componentId: '_componentManager',
        payload: new GetChannelsOfComponentRequest({ componentId: componentId }),
      })).then((response: GetChannelsOfComponentResponse) => {
        resolve(response.result.channels);
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
      if (!this.channelsPerComponent.has(componentId)) {
        componentEntry = { channels: {} };
        this.channelsPerComponent.set(componentId, componentEntry);
      } else {
        componentEntry = this.channelsPerComponent.get(componentId);
      }

      this.service.startSpinnerTransparentBackground(componentId);
      const request = componentEntry.activeRequest ?? (componentEntry.activeRequest = this.loadChannels(componentId));
      request.then(channels => {
        channels.forEach(channel => {
          componentEntry.channels[channel.id] = channel;
        });
        resolve(componentEntry);
      }).catch(reject)
        .finally(() => {
          componentEntry.activeRequest = undefined;
          this.service.stopSpinner(componentId);
        });
    });
  }

  ionViewDidLeave() {
    this.selectedComponentChannels = new Map();
    this.edge?.unsubscribeChannels(this.websocket, ChannelsComponent.SELECTOR);
  }
}

type ComponentChannels = {
  activeRequest?: Promise<Channel[]>,
  channels: { [channelId: string]: Channel },
};
