import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { PersistencePriority } from 'src/app/shared/edge/edgeconfig';
import { SetChannelValueRequest } from 'src/app/shared/jsonrpc/request/setChannelValueRequest';
import { environment } from 'src/environments';

import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';

export type ComponentChannels = {
  [componentId: string]: ChannelAddress[];
}

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
  protected channelsToBeSubscribed: ChannelAddress[] = [];
  private channels: ChannelAddress[] = [];
  protected componentChannels: ComponentChannels[] = [];
  protected componentChannelConfig: Map<string, EdgeConfig.ComponentChannel & { showPersistencePriority: boolean }> = new Map();

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
      this.service.startSpinner(this.spinnerId);
      this.loadSavedChannels();
    });
  }

  /**
   * Subscribes a channel
   *
   * @param componentId the componentId
   * @param channelId the channelId
   */
  protected subscribeChannel(componentId: string, channelId: string): void {
    const address = new ChannelAddress(componentId, channelId);
    if (this.componentChannels[componentId]?.filter(element => element.channelId == address.channelId)?.length === 0) {
      this.componentChannels[componentId].push(address);
    } else {
      this.componentChannels[componentId] = [address];
    }
    this.channelsToBeSubscribed.push(address);
    this.componentChannelConfig.set(address.toString(), { ...this.config.getChannel(address), ...{ showPersistencePriority: false } });

    if (this.config) {
      const globalPersistencePriority = this.config.getComponentsByFactory("Controller.Api.Backend")?.[0]?.properties['persistencePriority'] ?? PersistencePriority.DEFAULT_GLOBAL_PRIORITY;

      const channelConfig = this.config.getChannel(address);
      if (channelConfig) {
        if (channelConfig.accessMode == "WO") {
          // do not subscribe Write-Only Channels
          return;
        }

        if (PersistencePriority.isAtLeast(channelConfig.persistencePriority, globalPersistencePriority)) {
          this.componentChannelConfig.set(address.toString(), { ...this.config.getChannel(address), ...{ showPersistencePriority: true } });
        }
      }
    }

    if (this.edge) {
      this.edge.subscribeChannels(this.websocket, ChannelsComponent.SELECTOR, this.channelsToBeSubscribed);
    }
    this.saveChannels();
  }
  /**
   * Unsubscribes a channel
   *
   * @param channelAddress the channelAddress to be unsubscribed
   */
  protected unsubscribeChannel(channelAddress: ChannelAddress): void {
    this.componentChannels[channelAddress.componentId] = this.componentChannels[channelAddress.componentId]?.
      filter(element => element.channelId !== channelAddress.channelId);

    if (this.componentChannels[channelAddress.componentId]?.length === 0) {
      delete this.componentChannels[channelAddress.componentId];
    }
    this.channelsToBeSubscribed.forEach((item, index) => {
      if (item.componentId === channelAddress.componentId && item.channelId === channelAddress.channelId) {
        this.channelsToBeSubscribed.splice(index, 1);
      }
    });
    this.saveChannels();
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

  /**
   * Saves Channels as queryParams in route
   *  and navigates to the new route
   */
  private saveChannels(): void {
    const data = Object.entries(this.channelsToBeSubscribed).map(([componentId, channels]) => {
      return channels.toString();
    }).toString();
    this.router.navigate(['device/' + (this.edge.id) + '/settings/channels/'], { queryParams: { save: data } });
  }

  /**
   * Saves channels for the current edge in localstorage
   */
  protected localSave() {
    const dataStr = JSON.stringify(this.channelsToBeSubscribed);
    localStorage.setItem(ChannelsComponent.URL_PREFIX + "-" + this.edge.id, dataStr);
    this.service.toast("Successfully saved subscribed channels", "success");
  }

  protected loadSavedChannels() {
    this.service.startSpinner(ChannelsComponent.SELECTOR);
    const address = this.route.snapshot.queryParamMap.get('save');
    const storedValue = localStorage.getItem(ChannelsComponent.URL_PREFIX + "-" + this.edge.id);
    if (address) {
      this.channels = address.split(',')?.map(element => ChannelAddress.fromString(element));
      this.channels.map(el => this.subscribeChannel(el.componentId, el.channelId));
    } else if (storedValue) {
      const savedData = JSON.parse(storedValue);
      savedData.map(el => this.subscribeChannel(el.componentId, el.channelId));
      this.service.toast("Successfully loaded saved channels", "success");
    }
    this.service.stopSpinner(this.spinnerId);
  }

  ionViewDidLeave() {
    this.componentChannels = [];
    this.channelsToBeSubscribed = [];
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, ChannelsComponent.SELECTOR);
    }
  }
}
