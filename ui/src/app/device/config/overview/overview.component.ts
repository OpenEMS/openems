import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormGroup, FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';

import { Device } from '../../../shared/device/device';
import { Websocket } from '../../../shared/shared';

@Component({
  selector: 'overview',
  templateUrl: './overview.component.html'
})
export class OverviewComponent implements OnInit {

  public device: Device;

  private deviceSubscription: Subscription;

  constructor(
    private route: ActivatedRoute,
    public websocket: Websocket,
    private formBuilder: FormBuilder
  ) { }

  ngOnInit() {
    this.websocket.setCurrentDevice(this.route);
  }
}