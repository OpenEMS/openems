import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { Subject } from 'rxjs/Subject';

import { Device } from '../../shared/device/device';
import { DefaultTypes } from '../../shared/service/defaulttypes';
import { Utils } from '../../shared/shared';
import { ConfigImpl } from '../../shared/device/config';
import { CurrentDataAndSummary } from '../../shared/device/currentdata';
import { Websocket, Data } from '../../shared/shared';
import { CustomFieldDefinition } from '../../shared/type/customfielddefinition';
import { environment } from '../../../environments';

@Component({
  selector: 'overview',
  templateUrl: './overview.component.html'
})
export class OverviewComponent implements OnInit, OnDestroy {

  public device: Device
  public config: ConfigImpl = null;
  public currentData: CurrentDataAndSummary = null;
  //public customFields: CustomFieldDefinition = {};

  private stopCurrentData: Subject<void> = new Subject<void>();
  private currentDataTimeout: number;

  constructor(
    public websocket: Websocket,
    private route: ActivatedRoute,
    public utils: Utils
  ) { }

  ngOnInit() {
    this.websocket.setCurrentDevice(this.route)
      .filter(device => device != null)
      .first()
      .subscribe(device => {
        this.device = device;
        device.config.first().subscribe(config => {
          this.config = config;
          let channels = config.getImportantChannels();
          // TODO fieldstatus
          // /*
          //  * Add custom fields for fieldstatus component
          //  */
          // for (let thing in this.customFields) {
          //   let thingChannels = []
          //   for (let channel in this.customFields[thing]) {
          //     thingChannels.push(channel);
          //   }
          //   channels[thing] = thingChannels;
          // }
          device.subscribeCurrentData(channels).takeUntil(this.stopCurrentData).subscribe(currentData => {
            this.currentData = currentData;
            clearInterval(this.currentDataTimeout);
            this.currentDataTimeout = window.setInterval(() => {
              this.currentData = null;
              if (this.websocket.status == 'online') {
                device.subscribeCurrentData(channels);
              }
            }, Websocket.TIMEOUT);
          });
        })
      })
  }

  ngOnDestroy() {
    if (this.device) {
      this.device.unsubscribeCurrentData();
    }
    this.device = null;
    this.config = null;
    this.currentData = null;
    this.stopCurrentData.next();
    this.stopCurrentData.complete();
  }
}