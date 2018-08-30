import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { takeUntil } from 'rxjs/operators';
import { FormGroup, FormBuilder } from '@angular/forms';

import { Websocket } from '../../../../shared/shared';
import { AbstractConfig, ConfigureRequest } from '../abstractconfig';


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
    websocket: Websocket,
    formBuilder: FormBuilder,
  ) {
    super(route, websocket, formBuilder);
  }

  keys(object: {}) {
    let keys = Object.keys(object);
    keys.sort();
    return keys;
  }

  ngOnInit() {
    super.ngOnInit();

    this.edge.pipe(takeUntil(this.ngUnsubscribe)).subscribe(edge => {
      // subscribed to edge
      if (edge != null) {
        edge.subscribeCurrentData({
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
            "SetPivotOn", "SetBorehole1On", "SetBorehole2On", "SetBorehole3On", "SetClima1On", "SetClima2On", "SetOfficeOn", "SetTraineeCenterOn", "SignalBus1On", "SignalBus2On", "SignalOnGrid", "SignalWatchdog",
            "WaterLevelBorehole1On", "WaterLevelBorehole1Off", "WaterLevelBorehole2On", "WaterLevelBorehole2Off", "WaterLevelBorehole3On", "WaterLevelBorehole3Off"
          ],
          ess0: [
            "Soc", "SystemState", "ActivePower"
          ],
          ess1: [
            "Soc", "SystemState", "ActivePower"
          ],
          ess2: [
            "Soc", "SystemState", "ActivePower"
          ],
          ess3: [
            "Soc", "SystemState", "ActivePower"
          ]
        }).pipe(takeUntil(this.ngUnsubscribe)).subscribe(data => {
          let tmpData = {};
          // subscribed to data
          // TODO
          // if (data.data != null) {
          //   for (let thing in data.data) {
          //     if (!tmpData[thing]) {
          //       tmpData[thing] = {};
          //     }
          //     for (let channel in data.data[thing]) {
          //       let newData = { name: moment(), value: <number>data.data[thing][channel] };
          //       // if (!this.data[thing][channel]) {
          //       //   // create new array
          //       //   this.data[thing][channel] = [];
          //       // }
          //       // if (this.data[thing][channel].length > 9) {
          //       //   // max 10 entries
          //       //   this.data[thing][channel].shift();
          //       // }
          //       tmpData[thing][channel] = newData;
          //     }
          //   }
          // }
          this.data = tmpData;
        }, error => {
          console.error("error", error);
        });
      }
    });
  }

  ngOnDestroy() {
    let edge = this.edge.getValue();
    if (edge != null) {
      edge.unsubscribeCurrentData();
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

  public setJsonObjectToString(form: FormGroup): String {
    let value: String = JSON.stringify(form.value);

    return value;
  }

}

