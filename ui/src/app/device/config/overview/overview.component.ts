import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormGroup, FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';

import { Websocket, Device } from '../../../shared/shared';

@Component({
  selector: 'overview',
  templateUrl: './overview.component.html'
})
export class OverviewComponent implements OnInit, OnDestroy {

  public device: Device;

  private deviceSubscription: Subscription;

  constructor(
    private route: ActivatedRoute,
    private websocket: Websocket,
    private formBuilder: FormBuilder
  ) { }

  ngOnInit() {
    // TODO
    // this.deviceSubscription = this.websocket.setCurrentDevice(this.route.snapshot.params).subscribe(device => {
    //   this.device = device;
    // })
  }

  ngOnDestroy() {
    this.deviceSubscription.unsubscribe();
  }
}