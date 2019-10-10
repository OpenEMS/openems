import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from '../../../../../shared/service/defaulttypes';
import { Service, Utils } from '../../../../../shared/shared';
import { AbstractSection, EnergyFlow, Ratio, SvgEnergyFlow, SvgSquare, SvgSquarePosition } from './abstractsection.component';
import { UnitvaluePipe } from 'src/app/shared/pipe/unitvalue/unitvalue.pipe';
import { trigger, state, style, animate, transition } from '@angular/animations';


@Component({
    selector: '[consumptionsection]',
    templateUrl: './section.component.html',
    animations: [
        trigger('popOverState', [
            state('show', style({
                opacity: 0.5,
                transform: 'translateX(0%)',
            })),
            state('hide', style({
                opacity: 0,
                transform: 'translateX(10%)'
            })),
            transition('show => hide', animate('300ms')),
            transition('hide => show', animate('0ms'))
        ])
    ]
})
export class ConsumptionSectionComponent extends AbstractSection {

    private unitpipe: UnitvaluePipe;
    public show = false;

    constructor(
        unitpipe: UnitvaluePipe,
        translate: TranslateService,
        service: Service,
    ) {
        super('General.Consumption', "right", "#FDC507", translate, service, "Consumption");
        this.unitpipe = unitpipe;
    }

    ngOnInit() {
        // let timerId = setInterval(() => {
        //     this.show = !this.show;
        // }, 850)
        // setTimeout(() => { clearInterval(timerId) }, 10000);
        this.toggleAnimation();
    }

    toggleAnimation() {
        setInterval(() => {
            this.show = !this.show;
        }, 850);
    }

    get stateName() {
        return this.show ? 'show' : 'hide'
    }

    protected getStartAngle(): number {
        return 46;
    }

    protected getEndAngle(): number {
        return 134;
    }

    protected getRatioType(): Ratio {
        return 'Only Positive [0,1]';
    }

    protected _updateCurrentData(sum: DefaultTypes.Summary): void {
        let arrowIndicate: number;
        if (sum.consumption.activePower > 49) {
            arrowIndicate = Utils.divideSafely(sum.consumption.activePower, sum.system.totalPower);
        } else {
            arrowIndicate = 0;
        }
        super.updateSectionData(
            sum.consumption.activePower,
            sum.consumption.powerRatio,
            arrowIndicate);
    }

    protected getSquarePosition(square: SvgSquare, innerRadius: number): SvgSquarePosition {
        let x = innerRadius - 5 - square.length;
        let y = (square.length / 2) * (-1);
        return new SvgSquarePosition(x, y);
    }

    protected getImagePath(): string {
        return "consumption.png";
    }

    protected getValueText(value: number): string {
        if (value == null || Number.isNaN(value)) {
            return "";
        }
        return this.unitpipe.transform(value, 'kW')
    }

    protected initEnergyFlow(radius: number): EnergyFlow {
        return new EnergyFlow(radius, { x1: "0%", y1: "50%", x2: "100%", y2: "50%" });
    }

    protected getSvgEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        let v = Math.abs(ratio);
        if (v < 8 && v != 0) {
            v = 8;
        }
        let r = radius;
        let p = {
            topLeft: { x: v, y: v * -1 },
            middleLeft: { x: 0, y: 0 },
            bottomLeft: { x: v, y: v },
            topRight: { x: r * 1.2, y: v * -1 },
            bottomRight: { x: r * 1.2, y: v },
            middleRight: { x: (r * 1.2) - v, y: 0 }
        }
        if (ratio > 0) {
            // towards right
            p.topRight.x = p.topRight.x - v;
            p.middleRight.x = p.middleRight.x + v;
            p.bottomRight.x = p.bottomRight.x - v;
        }
        return p;
    }

    protected getSvgAnimationEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        let v = Math.abs(ratio);
        // if (v < 8 && v != 0) {
        v = 8;
        // }
        let r = radius;
        let animationWidth = 0.1 * ((r * - 1.2) - v);
        let p = {
            topLeft: { x: v, y: v * -1 },
            middleLeft: { x: 0, y: 0 },
            bottomLeft: { x: v, y: v },
            topRight: { x: r * 1.2, y: v * -1 },
            bottomRight: { x: r * 1.2, y: v },
            middleRight: { x: (r * 1.2) - v, y: 0 }
        }

        if (ratio < 0) {
            // towards left
        } else if (ratio > 0) {
            // towards right
            p.middleRight.x = p.middleLeft.x + animationWidth * 0.1;
            p.topRight.x = p.topLeft.x + animationWidth * 0.1;
            p.bottomRight.x = p.bottomLeft.x + animationWidth * 0.1;
        }
        console.log("TOPLEFT,CONSUMPTION", p.topLeft.x)
        console.log("TOPRIGHT,CONSUMPTION", p.topRight.x)
        return p;
    }
}

// if (ratio < 0) {
//     // towards top
//     p.middleTop.y = p.middleBottom.y + animationWidth * 0.1;
//     p.topLeft.y = p.bottomLeft.y + animationWidth * 0.1;
//     p.topRight.y = p.bottomRight.y + animationWidth * 0.1;
// } else if (ratio > 0) {
//     // towards bottom
//     p.middleTop.y = p.middleBottom.y + animationWidth * 0.75 + 2 * v;
//     p.topRight.y = p.bottomRight.y + animationWidth * 0.75;
//     p.topLeft.y = p.bottomLeft.y + animationWidth * 0.75;

//     p.middleBottom.y = p.middleTop.y + animationWidth * 0.1;
//     p.bottomRight.y = p.topRight.y + animationWidth * 0.1;
//     p.bottomLeft.y = p.topLeft.y + animationWidth * 0.1;
// }