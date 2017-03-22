import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormGroup, FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';

import { WebsocketService, WebappService, Device } from '../../../shared/shared';

@Component({
  selector: 'more',
  templateUrl: './more.component.html'
})
export class DeviceConfigMoreComponent implements OnInit {

  private device: Device;
  private deviceSubscription: Subscription;

  private manualPQForm: FormGroup;
  private manualMessageForm: FormGroup;

  constructor(
    private route: ActivatedRoute,
    private websocketService: WebsocketService,
    private webappService: WebappService,
    private formBuilder: FormBuilder
  ) { }

  ngOnInit() {
    this.deviceSubscription = this.websocketService.setCurrentDevice(this.route.snapshot.params).subscribe(device => {
      this.device = device;
    })
    this.manualPQForm = this.formBuilder.group({
      "p": this.formBuilder.control(''),
      "q": this.formBuilder.control('')
    });
    this.manualMessageForm = this.formBuilder.group({
      "message": this.formBuilder.control('')
    });
  }

  ngOnDestroy() {
    this.deviceSubscription.unsubscribe();
  }

  private applyManualPQ(form: FormGroup) {
    this.device.send({ manualPQ: form["value"] });
  }

  private removeManualPQ() {
    this.device.send({ manualPQ: {} });
  }

  private setInverterState(state: boolean) {
    this.device.send({
      channel: {
        thing: "RefuWorkState0",
        channel: "start",
        value: state
      }
    });
  }

  private sendManualMessage(form: FormGroup) {
    try {
      let obj = JSON.parse(form["value"]["message"]);
      this.device.send(obj);
    } catch (e) {
      this.webappService.notify({
        type: "error",
        message: (<Error>e).message
      });
    }
  }
}