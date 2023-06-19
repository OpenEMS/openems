import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { SetChannelValueRequest } from 'src/app/shared/jsonrpc/request/setChannelValueRequest';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';


@Component({
  selector: ChannelsComponent.SELECTOR,
  templateUrl: './channels.component.html'
})
export class ChannelsComponent implements OnInit, OnDestroy {

  private static readonly SELECTOR = "channels";

  public edge: Edge = null;
  public config: EdgeConfig = null;
  public subscribedChannels: ChannelAddress[] = [];

  constructor(
    private service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute
  ) { }

  public customAlertOptions: any = {
    cssClass: 'wide-alert'
  };

  ngOnInit() {
    this.service.setCurrentComponent("Channels" /* TODO translate */, this.route).then(edge => {
      this.edge = edge;
    });
    this.service.getConfig().then(config => {
      this.config = config;
    });
    setTimeout(_ => this.loadSavedChannels(), 2000);
  }

  subscribeChannel(componentId: string, channelId: string) {
    this.subscribedChannels.forEach((item, index) => {
      if (item.componentId === componentId && item.channelId === channelId) {
        // had already been in the list
        return;
      }
    });

    let address = new ChannelAddress(componentId, channelId);
    this.subscribedChannels.push(address);

    if (this.config) {
      let channelConfig = this.config.getChannel(address);
      if (channelConfig) {
        if (channelConfig.accessMode == "WO") {
          // do not subscribe Write-Only Channels
          return;
        }
      }
    }

    if (this.edge) {
      this.edge.subscribeChannels(this.websocket, ChannelsComponent.SELECTOR, this.subscribedChannels);
    }
  }

  unsubscribeChannel(address: ChannelAddress) {
    this.subscribedChannels.forEach((item, index) => {
      if (item.componentId === address.componentId && item.channelId === address.channelId) {
        this.subscribedChannels.splice(index, 1);
      }
    });
  }

  setChannelValue(address: ChannelAddress, value: any) {
    if (this.edge) {
      this.edge.sendRequest(
        this.service.websocket,
        new SetChannelValueRequest({
          componentId: address.componentId,
          channelId: address.channelId,
          value: value
        })
      ).then(response => {
        this.service.toast("Successfully set " + address.toString() + " to [" + value + "]", "success");
      }).catch(reason => {
        this.service.toast("Error setting " + address.toString() + " to [" + value + "]", 'danger');
      });
    }
  }

  saveChannels() {
    let dataStr = JSON.stringify(this.subscribedChannels);
    localStorage.setItem("openems-ui-channels", dataStr);
    localStorage.setItem("openems-ui-channels-date", new Date().toUTCString());
    this.service.toast("Successfully saved subscribed channels", "success");
  }

  loadSavedChannels() {
    let storedValue = localStorage.getItem("openems-ui-channels");
    let date = localStorage.getItem("openems-ui-channels-date");
    if (storedValue) {
      let channels: ChannelAddress[] = JSON.parse(storedValue);
      let that = this;
      channels.map(el => that.subscribeChannel(el.componentId, el.channelId));
      this.service.toast(`Successfully loaded save from ${date}`, "success");
    }
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, ChannelsComponent.SELECTOR);
    }
  }

}