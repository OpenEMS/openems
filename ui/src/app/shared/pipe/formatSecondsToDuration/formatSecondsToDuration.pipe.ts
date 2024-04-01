import { DecimalPipe } from '@angular/common';
import { Pipe, PipeTransform } from '@angular/core';

import { Converter } from '../../genericComponents/shared/converter';

@Pipe({
    name: 'formatSecondsToDuration',
})
export class FormatSecondsToDurationPipe implements PipeTransform {

    constructor(private decimalPipe: DecimalPipe) { }

    transform(value: number): string {

        return Converter.IF_NUMBER(value, (val) => {
            let minutes = val / 60;
            const hours = Math.floor(minutes / 60);
            minutes -= hours * 60;
            if (hours <= 23) {
                return this.decimalPipe.transform(hours, '1.0-0') + 'h' + " " + this.decimalPipe.transform(minutes, '1.0-0') + 'm';
            } else {
                return this.decimalPipe.transform(hours, '1.0-0') + 'h';
            }
        });
    }

}
