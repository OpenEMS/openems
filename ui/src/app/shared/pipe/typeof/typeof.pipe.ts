import { Pipe, PipeTransform } from "@angular/core";

@Pipe({
  name: "typeof",
  standalone: false,
})
export class TypeofPipe implements PipeTransform {

  transform(value: any): string {
    return typeof value;
  }
}
