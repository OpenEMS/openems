import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import * as moment from 'moment';

import { Device, Dataset, ChannelAddresses } from '../../../shared/shared';

@Component({
  selector: 'history',
  templateUrl: './history.component.html'
})
export class HistoryComponent implements OnInit, OnDestroy {

  @Input()
  public device: Device;

  public socChannels: ChannelAddresses = {};
  public fromDate = moment();
  public toDate = moment();

  private ngUnsubscribe: Subject<void> = new Subject<void>();

  ngOnInit() {
    if (this.device != null) {
      this.device.config.takeUntil(this.ngUnsubscribe).subscribe(config => {
        this.socChannels = config.getEssSocChannels();
      });
    }
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }
}
