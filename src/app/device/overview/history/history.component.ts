import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import * as moment from 'moment';

import { Device, Dataset } from '../../../shared/shared';

@Component({
  selector: 'history',
  templateUrl: './history.component.html'
})
export class HistoryComponent implements OnInit, OnDestroy {

  @Input()
  public device: Device;

  public essDevices: string[] = [];
  public fromDate = moment();
  public toDate = moment();

  private ngUnsubscribe: Subject<void> = new Subject<void>();

  ngOnInit() {
    if (this.device != null) {
      this.device.config.takeUntil(this.ngUnsubscribe).subscribe(config => {
        // get all configured ESS devices
        let essDevices: string[] = [];
        let natures = config._meta.natures;
        for (let nature in natures) {
          if (natures[nature].implements.includes("EssNature")) {
            essDevices.push(nature);
          }
        }
        this.essDevices = essDevices;
      });
    }
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }
}
