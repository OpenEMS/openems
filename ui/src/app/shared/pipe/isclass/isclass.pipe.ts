import { Pipe, PipeTransform } from '@angular/core';

/**
 * Checks if an object has a property "class" and this property has the value of the given parameter.
 * Use like: *ngIf="bridge | isclass:'io.openems.impl.protocol.simulator.SimulatorBridge'"
 */
@Pipe({
  name: 'isclass',
})
export class IsclassPipe implements PipeTransform {
  transform(object: any, classname: string): boolean {
    if (object !== null && typeof object === 'object' && object["class"] && object.class == classname) {
      return true;
    } else {
      return false;
    }
  }
}
