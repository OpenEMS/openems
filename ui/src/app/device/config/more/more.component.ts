import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormGroup, FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';
import 'rxjs/add/operator/filter';
import 'rxjs/add/operator/first';

import { Websocket, Service } from '../../../shared/shared';
import { Device } from '../../../shared/device/device';

@Component({
  selector: 'more',
  templateUrl: './more.component.html'
})
export class MoreComponent implements OnInit {

  public device: Device;
  public manualMessageForm: FormGroup;

  constructor(
    private route: ActivatedRoute,
    private websocket: Websocket,
    private service: Service,
    private formBuilder: FormBuilder
  ) { }

  ngOnInit() {
    this.websocket.setCurrentDevice(this.route)
      .filter(device => device != null)
      .first()
      .subscribe(device => {
        this.device = device;
      });
    this.manualMessageForm = this.formBuilder.group({
      "message": this.formBuilder.control('')
    });
  }
}