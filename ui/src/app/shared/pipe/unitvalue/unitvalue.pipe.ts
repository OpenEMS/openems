import { Pipe, PipeTransform } from '@angular/core';
import { DecimalPipe } from '@angular/common';

@Pipe({
    name: 'unitvalue'
})
export class UnitvaluePipe implements PipeTransform {

    constructor(private decimalPipe: DecimalPipe) { }

    transform(value: any, unit: string): any {
        if (value == null) {
            return '-' + '\u00A0';
        } else {
            if (unit == 'kWh' || unit == 'kW') {
                return this.decimalPipe.transform(value / 1000, '1.0-1') + '\u00A0' + unit;
            } else {
                return this.decimalPipe.transform(value, '1.0-0') + '\u00A0' + unit;
            }
        }
    }
}