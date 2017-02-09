import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormControl, FormGroup, FormArray, AbstractControl, FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';

import { AbstractConfig } from '../abstractconfig';
import { WebsocketService } from '../../../service/websocket.service';
import { Device } from '../../../service/device';

@Component({
  selector: 'app-device-config-scheduler',
  templateUrl: './scheduler.component.html'
})
export class DeviceConfigSchedulerComponent extends AbstractConfig {

  private form: AbstractControl;

  constructor(
    route: ActivatedRoute,
    websocketService: WebsocketService,
    formBuilder: FormBuilder
  ) {
    super(route, websocketService, formBuilder);
  }

  initForm(config) {
    console.log(config);
    this.form = this.buildForm(config.scheduler);
  }
}