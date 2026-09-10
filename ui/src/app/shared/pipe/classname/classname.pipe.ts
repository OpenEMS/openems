import { Pipe, PipeTransform } from "@angular/core";

@Pipe({
    name: "classname",
    standalone: false,
})
export class ClassnamePipe implements PipeTransform {
    transform(value: string, args: string[]): string {
        const parts = value.split(".");
        return parts[parts.length - 1];
    }
}
