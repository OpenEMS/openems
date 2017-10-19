import { Component, Input, OnChanges } from '@angular/core';
import { Subject } from 'rxjs/Subject';

import { ConfigImpl } from '../../../shared/device/config';
import { Device } from '../../../shared/device/device';
import { DefaultTypes } from '../../../shared/service/defaulttypes';

@Component({
  selector: 'history',
  templateUrl: './history.component.html'
})
export class HistoryComponent implements OnChanges {

  @Input() public config: ConfigImpl;

  @Input() public device: Device;

  ngOnChanges() {
    if (this.device != null && this.config != null) {
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
