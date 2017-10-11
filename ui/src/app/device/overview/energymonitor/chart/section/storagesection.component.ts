import { Component, OnInit, trigger, state, style, transition, animate } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { AbstractSection, SvgSquarePosition, SvgSquare, CircleDirection, Circle } from './abstractsection.component';
import { Observable } from "rxjs/Rx";


let PULSE = 1000;

@Component({
    selector: '[storagesection]',
    templateUrl: './section.component.html',
    animations: [
        trigger('circle', [
            state('one', style({
                r: 7,
                fill: 'none',
                stroke: 'white'
            })),
            state('two', style({
                r: 7,
                fill: 'none',
                stroke: '#009846'
            })),
            state('three', style({
                r: 7,
                fill: 'none',
                stroke: 'none'
            })),
            transition('one => two', animate(PULSE + 'ms')),
            transition('two => one', animate(PULSE + 'ms'))
        ])
    ]
})
export class StorageSectionComponent extends AbstractSection implements OnInit {

    private state: "charging" | "discharging" | "standby" = "standby";

    constructor(translate: TranslateService) {
        super('Device.Overview.Energymonitor.Storage', 136, 224, "#009846", translate);
    }

    ngOnInit() {
        Observable.interval(this.pulsetime)
            .subscribe(x => {
                if (this.state == "standby") {
                    for (let i = 0; i < this.circles.length; i++) {
                        this.circles[i].hide();
                    }
                } else if (this.state == "charging") {
                    for (let i = 0; i < this.circles.length; i++) {
                        setTimeout(() => {
                            this.circles[i].switchState();
                        }, this.pulsetime / 4 * i);
                    }
                } else if (this.state == "discharging") {
                    for (let i = 0; i < this.circles.length; i++) {
                        setTimeout(() => {
                            this.circles[this.circles.length - i - 1].switchState();
                        }, this.pulsetime / 4 * i);
                    }
                }
            })
    }

    public updateStorageValue(chargeAbsolute: number, dischargeAbsolute: number, percentage: number) {
        if (chargeAbsolute != null && chargeAbsolute > 0) {
            this.name = "Speicher-Beladung" //TODO translate
            super.updateValue(chargeAbsolute, percentage);
            this.state = "charging";
        } else {
            this.name = "Speicher-Entladung" //TODO translate
            super.updateValue(dischargeAbsolute, percentage);
            if (dischargeAbsolute > 0) {
                this.state = "discharging";
            } else {
                this.state = "standby";
            }
        }
        if (percentage != null) {
            this.valueText2 = percentage + " %";
        } else {
            this.valueText2 = "";
        }
    }

    protected getCircleDirection(): CircleDirection {
        return new CircleDirection("down");
    }

    protected getSquarePosition(square: SvgSquare, innerRadius: number): SvgSquarePosition {
        let x = (square.length / 2) * (-1);
        let y = innerRadius - 5 - square.length;
        return new SvgSquarePosition(x, y);
    }

    protected getImagePath(): string {
        return "storage.png";
    }

    protected getValueText(value: number): string {
        if (value == null || Number.isNaN(value)) {
            return "";
        }

        return this.lastValue.absolute + " W";
    }
}