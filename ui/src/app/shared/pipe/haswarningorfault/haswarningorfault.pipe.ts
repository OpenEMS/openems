import { Pipe, PipeTransform } from '@angular/core';

import { DefaultTypes } from '../../service/defaulttypes';

/**
 * Runs a deep search in the given currentData object for a Channel 'State' which is non-equal to zero.
 * Use like: *ngFor="let thingId in currentData | haswarningorfault"
 */
@Pipe({
  name: 'haswarningorfault'
})
export class HasWarningOrFaultPipe implements PipeTransform {
  transform(currentData: DefaultTypes.Data): DefaultTypes.Data {
    if (currentData == null) {
      return {}
    }
    let result = {}
    for (let thingId of Object.keys(currentData.data)) {
      let thing = currentData.data[thingId]
      if (thing['State'] != 0) {
        result[thingId] = thing
      }
    }
    console.log(result)
    return result
  }
}