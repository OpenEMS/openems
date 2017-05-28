import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { FormControl, FormGroup, FormArray, AbstractControl, FormBuilder } from '@angular/forms';

import { WebsocketService, Device, Log } from '../../../shared/shared';
import { AbstractConfig, ConfigureRequest } from '../abstractconfig';

import * as moment from 'moment';

interface SimulatorForm {
  id: string,
  class: string,
  gridMeter: FormGroup,
  productionMeter: FormGroup
}

class DataIndex {
  [thing: string]: {
    [channel: string]: number
  }
}

@Component({
  selector: 'simulator',
  templateUrl: './simulator.component.html'
})
export class SimulatorComponent extends AbstractConfig implements OnInit, OnDestroy {

  private forms: SimulatorForm[] = [];
  public data = {};

  constructor(
    route: ActivatedRoute,
    websocketService: WebsocketService,
    formBuilder: FormBuilder,
  ) {
    super(route, websocketService, formBuilder);
  }

  keys(object: {}) {
    return Object.keys(object);
  }

  ngOnInit() {
    super.ngOnInit();

    this.device.takeUntil(this.ngUnsubscribe).subscribe(device => {
      // subscribed to device
      if (device != null) {
        device.subscribeChannels({
          meter0: [
            "ActivePower", "minActivePower"
          ],
          meter1: [
            "ActivePower"
          ]
        });
        device.data.takeUntil(this.ngUnsubscribe).subscribe(data => {
          // subscribed to data
          if (data != null) {
            for (let thing in data) {
              if (!this.data[thing]) {
                this.data[thing] = {};
              }
              for (let channel in data[thing]) {
                let newData = { name: moment(), value: <number>data[thing][channel] };
                if (!this.data[thing][channel]) {
                  // create new array
                  this.data[thing][channel] = [];
                }
                if (this.data[thing][channel].length > 9) {
                  // max 10 entries
                  this.data[thing][channel].shift();
                }
                this.data[thing][channel] = [...this.data[thing][channel], newData];
              }
            }
          }
        });
      }
    });
  }

  ngOnDestroy() {
    let device = this.device.getValue();
    if (device != null) {
      device.unsubscribeChannels();
    }
    super.ngOnDestroy();
  }

  initForm(config) {
    // finds all Simulator device configurations and adds them to "forms"
    if (config["things"]) {
      for (let bridge of config.things) {
        if (bridge["class"] && bridge.class == 'io.openems.impl.protocol.simulator.SimulatorBridge' && bridge["devices"]) {
          for (let simulator of bridge.devices) {
            if (simulator["class"] && simulator.class == 'io.openems.impl.device.simulator.Simulator' && simulator["gridMeter"] && simulator["productionMeter"]) {
              this.forms.push({
                id: simulator.id,
                class: simulator.class,
                gridMeter: <FormGroup>this.buildForm(simulator.gridMeter),
                productionMeter: <FormGroup>this.buildForm(simulator.productionMeter)
              });
            }
          }
        }
      }
    }
  }

  protected getConfigureCreateRequests(form: FormGroup): ConfigureRequest[] {
    return;
  }
}

