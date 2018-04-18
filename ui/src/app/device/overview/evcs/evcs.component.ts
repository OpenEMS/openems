import { Component, Input, OnDestroy, ViewChildren, QueryList } from '@angular/core';
import { Subject } from 'rxjs/Subject';

import { Utils } from '../../../shared/service/utils';
import { DefaultTypes } from '../../../shared/service/defaulttypes';
import { CurrentDataAndSummary } from '../../../shared/device/currentdata';
import { Device } from '../../../shared/device/device';
import { ChannelComponent } from '../../../shared/config/channel.component';

@Component({
  selector: 'evcs',
  templateUrl: './evcs.component.html'
})
export class EvcsComponent {

  @Input()
  public currentData: CurrentDataAndSummary;

  @Input()
  public config: DefaultTypes.Config;

  @Input()
  public device: Device;

  constructor(public utils: Utils) { }

  /**
   * Receive messages from Channel
   * 
   * @param message 
   * @param channelId 
   */
  private onChannelChange(message) {
    if (message != null) {
      this.device.send(message);
    }
  }
}
