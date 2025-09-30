import { DecimalPipe } from "@angular/common";
import { Pipe, PipeTransform } from "@angular/core";

import { Converter } from "../../components/shared/converter";

@Pipe({
    name: "formatSecondsToDuration",
    standalone: false,
})
export class FormatSecondsToDurationPipe implements PipeTransform {

    constructor(private decimalPipe: DecimalPipe) { }

    transform(value: number, showMinutes?: boolean): string {

        return Converter.IF_NUMBER(value, (val) => {
            let minutes = val / 60;
            const hours = MATH.FLOOR(minutes / 60);
            minutes -= hours * 60;
            if (hours <= 23 || showMinutes) {
                return THIS.DECIMAL_PIPE.TRANSFORM(hours, "1.0-0") + "h" + " " + THIS.DECIMAL_PIPE.TRANSFORM(minutes, "1.0-0") + "m";
            } else {
                return THIS.DECIMAL_PIPE.TRANSFORM(hours, "1.0-0") + "h";
            }
        });
    }

}
