import { Pipe, PipeTransform } from "@angular/core";

@Pipe({
    name: "sign",
    standalone: false,
})
export class SignPipe implements PipeTransform {
    transform(value: number, args: string[]): number {
        const positive = value * -1;
        return positive;
    }
}
