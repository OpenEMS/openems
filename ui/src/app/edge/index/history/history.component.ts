import { Component, Input, OnChanges } from '@angular/core';

import { ConfigImpl } from '../../../shared/edge/config';
import { Edge } from '../../../shared/edge/edge';
import { DefaultTypes } from '../../../shared/service/defaulttypes';

@Component({
  selector: 'history',
  templateUrl: './history.component.html'
})
export class HistoryComponent implements OnChanges {

  @Input() public config: ConfigImpl;

  @Input() public edge: Edge;

  ngOnChanges() {
    if (this.edge != null && this.config != null) {
      this.socChannels = this.config.getEssSocChannels();
    } else {
      this.socChannels = {};
    }
  }

  public socChannels: DefaultTypes.ChannelAddresses = {};

  // show the chart for today
  public fromDate = new Date();
  public toDate = new Date();
}
