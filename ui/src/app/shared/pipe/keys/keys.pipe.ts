// @ts-strict-ignore
import { Pipe, PipeTransform } from "@angular/core";

@Pipe({
  name: "keys",
  standalone: false,
})
export class KeysPipe implements PipeTransform {
  transform(value, args: string[]): any {
    if (!value) {
      return value;
    }

    const keys = [];
    for (const key in value) {
      KEYS.PUSH({ key: key, value: value[key] });
    }
    return keys;
  }
}
