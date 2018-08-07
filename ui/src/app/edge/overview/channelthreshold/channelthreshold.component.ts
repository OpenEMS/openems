import { Component, Input } from '@angular/core';

import { Utils } from '../../../shared/service/utils';
import { DefaultTypes } from '../../../shared/service/defaulttypes';
import { CurrentDataAndSummary } from '../../../shared/edge/currentdata';
import { Edge } from '../../../shared/edge/edge';

@Component({
  selector: 'channelthreshold',
  templateUrl: './channelthreshold.component.html'
})
export class ChannelthresholdComponent {

  @Input()
  public currentData: CurrentDataAndSummary;

  @Input()
  public config: DefaultTypes.Config;

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
