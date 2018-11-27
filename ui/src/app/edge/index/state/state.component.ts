import { Component, Input, OnDestroy, EventEmitter, Output } from '@angular/core';

import { Utils } from '../../../shared/service/utils';
import { DefaultTypes } from '../../../shared/service/defaulttypes';
import { CurrentDataAndSummary } from '../../../shared/edge/currentdata';
import { THING_STATES } from './thingstates';
import { ConfigImpl_2018_7 } from '../../../shared/edge/config.2018.7';

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
  public config: ConfigImpl_2018_7;

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
        if (thing['State'] != null && thing['State'] != 0) {
          // Thing has a warning or fault
          if (thingId in this.lastRequiredSubscribes) {
            // was like this before -> copy subscribes from last time
            newRequiredSubscribes[thingId] = this.lastRequiredSubscribes[thingId];
          } else {
            // this is new -> generate required subscribes
            newRequiredSubscribes[thingId] = this.getStateChannelAddresses(thingId);
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
            if (thing[channelId] != null && thing[channelId] != 0) {
              if (this.ignoreWarningOrFault(thingId, channelId)) {
                continue;
              }
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
            if (this.config.esss.includes(thingId)) {
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

  private getStateChannelAddresses(thingId: string): string[] {
    let result = [];
    let clazz = this.config.things[thingId].class;
    if (clazz instanceof Array) {
      clazz = clazz[0];
    }
    if (clazz in THING_STATES) {
      let meta = THING_STATES[clazz];
      for (let id in meta.faults) {
        result.push("Fault/" + id);
      }
      for (let id in meta.warnings) {
        result.push("Warning/" + id);
      }
    }
    return result;
  }

  /*
   * List of Warnings or Faults that should not be displayed
   */
  private ignoreWarningOrFault(thingId: string, channelId: string): boolean {
    let clazz = this.config.things[thingId].class;
    if (clazz instanceof Array) {
      clazz = clazz[0];
    }
    switch (clazz) {
      case 'io.openems.impl.device.minireadonly.FeneconMiniEss':
        if (['Warning/92'].includes(channelId)) {
          return true;
        }
        break;
    }
    return false;
  }

  constructor(public utils: Utils) { }
}
