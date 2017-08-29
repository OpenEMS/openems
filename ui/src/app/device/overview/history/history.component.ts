import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import * as moment from 'moment';

import { Device } from '../../../shared/device/device';
import { Dataset, ChannelAddresses } from '../../../shared/shared';

// spinner component
import { SpinnerComponent } from '../../../shared/spinner.component';

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
  public loading: boolean = true;

  private ngUnsubscribe: Subject<void> = new Subject<void>();

  ngOnInit() {
    // TODO
    // if (this.device != null) {
    //   this.loading = true;
    //   this.device.config.takeUntil(this.ngUnsubscribe).subscribe(config => {
    //     this.socChannels = config.getEssSocChannels();
    //     this.loading = false;
    //   });
    // }
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }
}
