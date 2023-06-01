import { Pipe, PipeTransform } from '@angular/core';

/**
 * Runs a deep search in the given object for a property 'class' with the given parameter. Similar to "isclass"-pipe, but with deep search.
 * Use like: *ngIf="bridge | hasclass:'io.openems.impl.protocol.simulator.SimulatorBridge'"
 */
@Pipe({
  name: 'hasclass'
})
export class HasclassPipe implements PipeTransform {
  transform(param: any, classname: string): boolean {
    if (param == null) {
      return false;
    }
    if (Array.isArray(param)) {
      let found: boolean = false;
      for (let element of param) {
        found = this.transform(element, classname);
        if (found) {
          break;
        };
      }
      return found;

    } else if (typeof param === 'object') {
      if (param["class"] && param.class == classname) {
        return true;
      }

      let found: boolean = false;
      for (let property in param) {
        found = this.transform(param[property], classname);
        if (found) {
          break;
        }
      }
      return found;

    } else {
      return false;
    }
  }
}