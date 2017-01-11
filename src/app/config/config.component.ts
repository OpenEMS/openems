import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormGroup, FormBuilder } from '@angular/forms';

import { WebsocketService } from '../service/websocket.service';
import { Device } from '../service/device';

@Component({
  selector: 'app-config',
  templateUrl: './config.component.html'
})
export class ConfigComponent implements OnInit {

  private device: Device;
  private mode: "" | "manualPQ" | "bridge" | "controller" | "scheduler";

  private manualPQForm: FormGroup;

  constructor(
    private route: ActivatedRoute,
    private websocketService: WebsocketService,
    private formBuilder: FormBuilder
  ) { }

  ngOnInit() {
    this.device = this.websocketService.setCurrentDevice(this.route.snapshot.params);
    this.mode = "";
    this.manualPQForm = this.formBuilder.group({
      "p": this.formBuilder.control(''),
      "q": this.formBuilder.control('')
    });
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
}