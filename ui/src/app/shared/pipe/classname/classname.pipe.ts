// @ts-strict-ignore
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'classname',
})
export class ClassnamePipe implements PipeTransform {
  transform(value, args: string[]): any {
    const parts = value.split(".");
    return parts[parts.length - 1];
  }
}
