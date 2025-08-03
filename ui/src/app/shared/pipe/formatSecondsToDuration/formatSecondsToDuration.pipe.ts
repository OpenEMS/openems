import { DecimalPipe } from "@angular/common";
import { Pipe, PipeTransform, inject } from "@angular/core";

import { Converter } from "../../components/shared/converter";

@Pipe({
    name: "formatSecondsToDuration",
    standalone: false,
})
export class FormatSecondsToDurationPipe implements PipeTransform {
    private decimalPipe = inject(DecimalPipe);

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);


    constructor() { }

    transform(value: number, showMinutes?: boolean): string {

        return Converter.IF_NUMBER(value, (val) => {
            let minutes = val / 60;
            const hours = Math.floor(minutes / 60);
            minutes -= hours * 60;
            if (hours <= 23 || showMinutes) {
                return this.decimalPipe.transform(hours, "1.0-0") + "h" + " " + this.decimalPipe.transform(minutes, "1.0-0") + "m";
            } else {
                return this.decimalPipe.transform(hours, "1.0-0") + "h";
            }
        });
    }

}
