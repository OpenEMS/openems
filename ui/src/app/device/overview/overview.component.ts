import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { Subject } from 'rxjs/Subject';

import { Websocket, Notification, Device, Data, Config } from '../../shared/shared';
import { CustomFieldDefinition } from '../../shared/type/customfielddefinition';
import { environment } from '../../../environments';

@Component({
  selector: 'overview',
  templateUrl: './overview.component.html'
})
export class OverviewComponent implements OnInit, OnDestroy {

  public currentData: Data;
  public config: Config;
  public customFields: CustomFieldDefinition = {};

  private ngUnsubscribe: Subject<void> = new Subject<void>();

  constructor(
    public websocket: Websocket,
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    let device = this.websocket.getCurrentDeviceFromRoute(this.route);
    device.getConfig().then(config => {
      console.log(config);
    }).catch(reason => {
      console.error(reason);
    })

    // TODO
    // this.websocket.setCurrentDevice(this.route.snapshot.params).takeUntil(this.ngUnsubscribe).subscribe(device => {
    //   this.device = device;
    //   if (device != null) {
    //     this.device.config.takeUntil(this.ngUnsubscribe).subscribe(config => {
    //       this.config = config;
    //       this.customFields = environment.getCustomFields(config);
    //       let channels = config.getImportantChannels();
    //       /*
    //        * Add custom fields for fieldstatus component
    //        */
    //       for (let thing in this.customFields) {
    //         let thingChannels = []
    //         for (let channel in this.customFields[thing]) {
    //           thingChannels.push(channel);
    //         }
    //         channels[thing] = thingChannels;
    //       }

    //       device.subscribeCurrentData(channels).takeUntil(this.ngUnsubscribe).subscribe(currentData => {
    //         this.currentData = currentData;
    //       });
    //     })
    //   }
    // })
  }

  ngOnDestroy() {
    // if (this.device) {
    //   this.device.unsubscribeCurrentData();
    // }
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }
}