import { Component, Input } from '@angular/core';

import { Utils } from '../../../../shared/service/utils';
import { DefaultTypes } from '../../../../shared/service/defaulttypes';
import { CurrentDataAndSummary_2018_7 } from '../../../../shared/edge/currentdata.2018.7';
import { Edge } from '../../../../shared/edge/edge';

@Component({
  selector: 'channelthreshold-2018-7',
  templateUrl: './channelthreshold.component.html'
})
export class ChannelthresholdComponent_2018_7 {

  @Input()
  public currentData: CurrentDataAndSummary_2018_7;

  @Input()
  public config: DefaultTypes.Config_2018_7;

  @Input()
  public edge: Edge;

  constructor(public utils: Utils) { }

  /*use an object inside an *ngFor loop*/

  private keys(object: {}) {
    return Object.keys(object);
  }

  /**
   * Receive messages from Channel
   * 
   * @param message 
   * @param channelId 
   */
  private onChannelChange(message) {
    if (message != null) {
      this.edge.send(message);
    }
  }
}
