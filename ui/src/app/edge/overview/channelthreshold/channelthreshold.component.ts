import { Component, Input, OnDestroy, ViewChildren, QueryList } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { Utils } from '../../../shared/service/utils';
import { DefaultTypes } from '../../../shared/service/defaulttypes';
import { CurrentDataAndSummary } from '../../../shared/edge/currentdata';
import { Edge } from '../../../shared/edge/edge';
import { ChannelComponent } from '../../../shared/config/channel.component';

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
