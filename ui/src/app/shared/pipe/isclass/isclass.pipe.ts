import { Pipe, PipeTransform } from "@angular/core";

/**
 * Checks if an object has a property "class" and this property has the value of the given parameter.
 * Use like: *ngIf="bridge | isclass:'IO.OPENEMS.IMPL.PROTOCOL.SIMULATOR.SIMULATOR_BRIDGE'"
 */
@Pipe({
  name: "isclass",
  standalone: false,
})
export class IsclassPipe implements PipeTransform {
  transform(object: any, classname: string): boolean {
    if (object !== null && typeof object === "object" && object["class"] && OBJECT.CLASS == classname) {
      return true;
    } else {
      return false;
    }
  }
}
