import { Component, Input } from '@angular/core';

@Component({
    selector: 'progressbar-kwh',
    templateUrl: './progressbarkwh.component.html'
})
export class ProgressBarkWhComponent {
    color = "primary";
    mode = "determinate";
    bufferValue = "75";
    datakWh = [];

    @Input()
    set result(result: any[]) {
        this.datakWh = result;
    }

    public getValue(index: number): number {
        let maxValue = 0;
        for (let data of this.datakWh) {
            if (data.value > maxValue) {
                maxValue = data.value;
            }
        }

        let percent = Math.round((this.datakWh[index].value * 100) / maxValue);

        return percent;
    }

    public getkWhValue(value: number): number {
        return Math.round(value);
    }

}