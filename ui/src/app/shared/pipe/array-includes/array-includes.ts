import { Pipe, PipeTransform } from "@angular/core";

@Pipe({ name: "arrIncludes", standalone: true })
export class ArrayIncludes implements PipeTransform {
    transform(arr: string[], value: string): boolean {
        return arr?.includes(value);
    }
}
