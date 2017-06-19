import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { FormControl, FormGroup, FormArray, AbstractControl, FormBuilder } from '@angular/forms';

import { WebsocketService, Device, Log } from '../../../shared/shared';
import { AbstractConfig, ConfigureRequest } from '../abstractconfig';
import 'rxjs/add/operator/retryWhen';
import 'rxjs/add/operator/delay';

import * as moment from 'moment';

interface SimulatorForm {
  id: string,
  class: string
  // gridMeter: FormGroup,
  // productionMeter: FormGroup
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
        device.subscribeCurrentData({
          meter0: [
            "ActivePower", "minActivePower"
          ],
          meter1: [
            "ActivePower"
          ],
          cluster0: [
            "ActivePower", "Soc"
          ],
          output0: [
            "DO1", "DO2", "DO3", "DO4", "DO5", "DO6", "DO7", "DO8"
          ],
          sps0: [
            "PivotOn", "Borehole1On", "Borehole2On", "Borehole3On", "Clima1On", "Clima2On", "OfficeOn", "TraineeCenterOn", "SignalBus1On", "SignalBus2On", "SignalOnGrid", "SignalWatchdog",
            "WaterLevelBorehole1On", "WaterLevelBorehole1Off", "WaterLevelBorehole2On", "WaterLevelBorehole2Off", "WaterLevelBorehole3On", "WaterLevelBorehole3Off"
          ],
          ess0: [
            "Soc", "SystemState"
          ],
          ess1: [
            "Soc", "SystemState"
          ],
          ess2: [
            "Soc", "SystemState"
          ],
          ess3: [
            "Soc", "SystemState"
          ]
        }).takeUntil(this.ngUnsubscribe).subscribe(data => {
          // subscribed to data
          if (data != null) {
            for (let thing in data) {
              if (!this.data[thing]) {
                this.data[thing] = [];
              }
              for (let channel in data[thing]) {
                let newData = { name: moment(), value: <number>data[thing][channel] };
                // if (!this.data[thing][channel]) {
                //   // create new array
                //   this.data[thing][channel] = [];
                // }
                // if (this.data[thing][channel].length > 9) {
                //   // max 10 entries
                //   this.data[thing][channel].shift();
                // }
                this.data[thing][channel] = newData;
              }
            }
          }
        }, error => {
          console.error("error", error);
        }, () => {
          console.error("complete");
        });
      }
    });
  }

  ngOnDestroy() {
    let device = this.device.getValue();
    if (device != null) {
      device.unsubscribeCurrentData();
    }
    super.ngOnDestroy();
  }

  initForm(config) {
    // finds all Simulator device configurations and adds them to "forms"
    this.forms = [];
    if (config["things"]) {
      for (let bridge of config.things) {
        if (bridge["class"] && bridge.class == 'io.openems.impl.protocol.simulator.SimulatorBridge' && bridge["devices"]) {
          for (let simulator of bridge.devices) {
            if (simulator["class"] && simulator.class == 'io.openems.impl.device.simulator.Simulator') {
              let form = {
                id: simulator.id,
                class: simulator.class,
              }
              for (let key of Object.keys(simulator)) {
                if (key == 'id' || key == 'class') {
                  continue;
                }
                form[key] = <FormGroup>this.buildForm(simulator[key]);
              }
              this.forms.push(form);
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

