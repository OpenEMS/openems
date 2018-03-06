import { Component, Input, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs/Subject';

import { Device } from '../../../shared/device/device';
import { Utils } from '../../../shared/service/utils';
import { DefaultTypes } from '../../../shared/service/defaulttypes';
import { CurrentDataAndSummary } from '../../../shared/device/currentdata';

@Component({
  selector: 'state',
  templateUrl: './state.component.html'
})
export class StateComponent {

  @Input()
  public device: Device;

  public _currentData: CurrentDataAndSummary = null;
  private lastThingIds: string[]

  @Input()
  set currentData(currentData: CurrentDataAndSummary) {
    this._currentData = currentData;
    let thingIds = [];
    for (let thingId of Object.keys(currentData.data)) {
      let thing = currentData.data[thingId];
      if (thing['State'] != 0) {
        thingIds.push(thingId);
      }
    }
    console.log(thingIds);
    this.device.warningOrFaultQuery(thingIds).then(warningsOrFaults => {
      console.log(warningsOrFaults);
    });
  }

  @Input()
  public config: DefaultTypes.Config;

  constructor(public utils: Utils) { }
}
