import { Component, Input, OnChanges } from '@angular/core';

import { ConfigImpl } from '../../../shared/edge/config';
import { Edge } from '../../../shared/edge/edge';
import { ChannelAddress } from '../../../shared/type/channeladdress';

@Component({
  selector: 'history',
  templateUrl: './history.component.html'
})
export class HistoryComponent {

  @Input() protected config: ConfigImpl;
  @Input() protected edge: Edge;

  // show the chart for today
  protected fromDate = new Date();
  protected toDate = new Date();

}
