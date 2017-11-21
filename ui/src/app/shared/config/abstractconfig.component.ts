import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { Subject } from 'rxjs/Subject';

import { Utils } from '../service/utils';
import { Device } from '../device/device';
import { Websocket } from '../shared';
import { DefaultTypes } from '../service/defaulttypes';
import { ConfigImpl } from '../device/config';

@Component({
  selector: 'abstractconfig',
  templateUrl: 'abstractconfig.component.html'
})
export class AbstractConfigComponent implements OnInit {

  public showSubThings: boolean = false;
  public device: Device = null;
  public config: ConfigImpl = null;
  public things: string[] = [];
  private stopOnDestroy: Subject<void> = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private websocket: Websocket,
    public utils: Utils
  ) { }

  ngOnInit() {
    this.websocket.setCurrentDevice(this.route)
      .takeUntil(this.stopOnDestroy)
      .filter(device => device != null)
      .subscribe(device => {
        this.device = device;
        device.config
          .filter(device => device != null)
          .takeUntil(this.stopOnDestroy).subscribe(config => {
            this.config = config;
            this.things = this.filterThings(config);
          });
      });
  }

  ngOnDestroy() {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }

  protected filterThings(config: ConfigImpl): string[] {
    return this.utils.keys(this.config.things);
  }
}