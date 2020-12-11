import { DecimalPipe } from '@angular/common';
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'sectohour'
})
export class SecToHourMinPipe implements PipeTransform {

    constructor(private decimalPipe: DecimalPipe) { }

    transform(value: number): string {
        let minutes = value / 60;
        let hours = value / 60 / 60;
        if (minutes < 60) {
            return this.decimalPipe.transform(minutes, '1.0-0') + '\u00A0' + 'm';
        } else {
            return this.decimalPipe.transform(hours, '1.0-0') + '\u00A0' + 'h';
        }
    }
}