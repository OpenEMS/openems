import { Component, Input, OnDestroy, EventEmitter, Output } from '@angular/core';
import { Subject } from 'rxjs/Subject';

import { Device } from '../../../shared/device/device';
import { Utils } from '../../../shared/service/utils';
import { DefaultTypes } from '../../../shared/service/defaulttypes';
import { CurrentDataAndSummary } from '../../../shared/device/currentdata';
import { THING_STATES } from './thingstates';
import { ConfigImpl } from '../../../shared/device/config';

interface WarningOrFault {
  channelId: string,
  name: string
}

interface WarningsAndFaults {
  thing: {
    id: string,
    name?: string
  },
  warnings: WarningOrFault[],
  faults: WarningOrFault[]
}

@Component({
  selector: 'state',
  templateUrl: './state.component.html'
})
export class StateComponent {

  @Input()
  public config: ConfigImpl;

  @Input()
  set currentData(currentData: CurrentDataAndSummary) {
    this.generateRequiredSubscribes(currentData);
    this.fillWarningsAndFaultsList(currentData);
  }

  /**
   * Generates the requiredSubscribes.
   * 
   * If a Things has 'State' different to 0, it has a warning or fault. In that case define the needed subscribe 
   * channels (like 'ess0/Fault/0') and emit the 'requiredSubscribes' event.
   * 
   * @param currentData 
   */
  private generateRequiredSubscribes(currentData: CurrentDataAndSummary) {
    let subscribesChanged: boolean = false;
    let newRequiredSubscribes: DefaultTypes.ChannelAddresses = {};
    if (currentData == null && this.lastRequiredSubscribes != {}) {
      subscribesChanged = true;
    } else {
      for (let thingId of Object.keys(currentData.data)) {
        let thing = currentData.data[thingId];
        if (thing['State'] != 0) {
          // Thing has a warning or fault
          if (thingId in this.lastRequiredSubscribes) {
            // was like this before -> copy subscribes from last time
            newRequiredSubscribes[thingId] = this.lastRequiredSubscribes[thingId];
          } else {
            // this is new -> generate required subscribes
            // TODO
            newRequiredSubscribes[thingId] = ["Fault/0", "Fault/1", "Warning/0"];
            subscribesChanged = true;
          }
        } else {
          // Thing has no warning or fault
          if (thingId in this.lastRequiredSubscribes) {
            // it had an error before -> do not add to required subscribes
            subscribesChanged = true;
          }
        }
      }
    }
    if (subscribesChanged) {
      this.requiredSubscribes.emit(newRequiredSubscribes);
      this.lastRequiredSubscribes = newRequiredSubscribes;
    }
  }
  private lastRequiredSubscribes: DefaultTypes.ChannelAddresses = {};
  @Output()
  public requiredSubscribes = new EventEmitter<DefaultTypes.ChannelAddresses>();

  /**
   * Generates the list of warnings and faults that is shown in the widget
   * 
   * @param currentData 
   */
  private fillWarningsAndFaultsList(currentData: CurrentDataAndSummary) {
    let warningsAndFaultss: WarningsAndFaults[] = [];
    if (currentData != null) {
      for (let thingId in currentData.data) {
        let thing = currentData.data[thingId];
        if ('State' in thing && thing['State'] != 0) {
          let warnings: WarningOrFault[] = [];
          let faults: WarningOrFault[] = [];
          for (let channelId of Object.keys(thing)) {
            if (thing[channelId] != 0) {
              if (channelId.startsWith('Fault/')) {
                faults.push(this.getWarningOrFault(thingId, channelId, 'fault'));
              } else if (channelId.startsWith('Warning/')) {
                warnings.push(this.getWarningOrFault(thingId, channelId, 'warning'));
              }
            }
          }
          if (faults.length > 0 || warnings.length > 0) {
            // get Thing name
            let name = null;
            if (this.config.storageThings.includes(thingId)) {
              name = "Speichersystem"
            }
            warningsAndFaultss.push({
              thing: {
                id: thingId,
                name: name
              },
              warnings: warnings,
              faults: faults
            });
          }
        }
      }
    }
    this.warningsAndFaultss = warningsAndFaultss;
  };

  public warningsAndFaultss: WarningsAndFaults[] = [];

  private getWarningOrFault(thingId: string, channelId: string, type: 'fault' | 'warning'): WarningOrFault {
    let id = channelId.substr(type.length + 1);
    let clazz = this.config.things[thingId].class;
    if (clazz instanceof Array) {
      clazz = clazz[0];
    }
    let name = "Undefined " + type + " (" + channelId + ")";
    if (clazz in THING_STATES) {
      let meta = THING_STATES[clazz];
      if (meta[type + 's'][id]) {
        name = meta[type + 's'][id];
      }
    }
    return {
      channelId: channelId,
      name: name
    };
  }

  constructor(public utils: Utils) { }
}
