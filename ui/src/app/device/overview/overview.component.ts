import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { Subject } from 'rxjs/Subject';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';

import { Device } from '../../shared/device/device';
import { DefaultTypes } from '../../shared/service/defaulttypes';
import { Utils, Websocket } from '../../shared/shared';
import { ConfigImpl } from '../../shared/device/config';
import { CurrentDataAndSummary } from '../../shared/device/currentdata';
import { Widget } from '../../shared/type/widget';
import { CustomFieldDefinition } from '../../shared/type/customfielddefinition';
import { environment } from '../../../environments';

@Component({
  selector: 'overview',
  templateUrl: './overview.component.html'
})
export class OverviewComponent implements OnInit, OnDestroy {

  public device: Device = null
  public config: ConfigImpl = null;
  public currentData: CurrentDataAndSummary = null;
  public widgets: Widget[] = [];
  //public customFields: CustomFieldDefinition = {};

  private stopOnDestroy: Subject<void> = new Subject<void>();
  private currentDataTimeout: number;

  constructor(
    public websocket: Websocket,
    private route: ActivatedRoute,
    public utils: Utils
  ) { }

  ngOnInit() {
    this.websocket.setCurrentDevice(this.route)
      .takeUntil(this.stopOnDestroy)
      .subscribe(device => {
        this.device = device;
        if (device == null) {
          this.config = null;
        } else {

          device.config
            .takeUntil(this.stopOnDestroy)
            .subscribe(config => {
              this.config = config;
              if (config != null) {
                // get widgets
                this.widgets = config.getWidgets();

                // subscribe channels
                let channels = config.getImportantChannels();
                device.subscribeCurrentData(channels)
                  .takeUntil(this.stopOnDestroy)
                  .subscribe(currentData => {
                    this.currentData = currentData;

                    // resubscribe on timeout
                    clearInterval(this.currentDataTimeout);
                    this.currentDataTimeout = window.setInterval(() => {
                      this.currentData = null;
                      if (this.websocket.status == 'online') {
                        device.subscribeCurrentData(channels);
                      }
                    }, Websocket.TIMEOUT);
                  });
              }
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
            });
        }
      });
  }

  ngOnDestroy() {
    clearInterval(this.currentDataTimeout);
    if (this.device) {
      this.device.unsubscribeCurrentData();
    }
    this.device = null;
    this.config = null;
    this.currentData = null;
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }
}