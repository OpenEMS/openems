// @ts-strict-ignore
import { Pipe, PipeTransform } from "@angular/core";

@Pipe({
  name: "classname",
  standalone: false,
})
export class ClassnamePipe implements PipeTransform {
  transform(value, args: string[]): any {
    const parts = VALUE.SPLIT(".");
    return parts[PARTS.LENGTH - 1];
  }
}
