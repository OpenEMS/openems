import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormBuilder } from '@angular/forms';
import { Subject } from 'rxjs/Subject';

import { TemplateHelper } from './../../../../shared/service/templatehelper';
import { WebsocketService, Device } from '../../../../shared/shared';
import { Controller } from '../controller';

@Component({
  selector: 'overview',
  templateUrl: './overview.component.html'
})
export class OverviewComponent implements OnInit, OnDestroy {

  public controllers: Controller[] = [];
  public currentControllerIndex: number = -1;
  public device: Device;

  private ngUnsubscribe: Subject<void> = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private websocketService: WebsocketService,
    private formBuilder: FormBuilder,
    private tmpl: TemplateHelper
  ) { }

  ngOnInit() {
    this.websocketService.setCurrentDevice(this.route.snapshot.params).takeUntil(this.ngUnsubscribe).subscribe(device => {
      this.device = device;
      if (device != null) {
        device.config.takeUntil(this.ngUnsubscribe).subscribe(config => {
          if (config != null) {
            this.controllers = Controller.getControllers(config, this.formBuilder);
          }
        });
      }
    });
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }
}