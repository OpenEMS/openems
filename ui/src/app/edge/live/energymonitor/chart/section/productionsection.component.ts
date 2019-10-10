import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from '../../../../../shared/service/defaulttypes';
import { Service, Utils } from '../../../../../shared/shared';
import { AbstractSection, EnergyFlow, Ratio, SvgEnergyFlow, SvgSquare, SvgSquarePosition } from './abstractsection.component';
import { UnitvaluePipe } from 'src/app/shared/pipe/unitvalue/unitvalue.pipe';
import { trigger, state, style, animate, transition } from '@angular/animations';


@Component({
    selector: '[productionsection]',
    templateUrl: './section.component.html',
    animations: [
        trigger('popOverState', [
            state('show', style({
                opacity: 1,
                transform: 'translateX(0)'
            })),
            state('hide', style({
                opacity: 0,
                transform: 'translateX(-10%)'
            })),
            transition('show => hide', animate('600ms ease-out')),
            transition('hide => show', animate('1600ms ease-in'))
        ])
    ]
})
export class ProductionSectionComponent extends AbstractSection {

    private unitpipe: UnitvaluePipe;
    public show = false;
    private p: any = null;

    constructor(
        translate: TranslateService,
        service: Service,
        unitpipe: UnitvaluePipe,
    ) {
        super('General.Production', "up", "#008DD2", translate, service, "Production");
        this.unitpipe = unitpipe;
    }

    ngOnInit() {
        let timerId = setInterval(() => {
            this.show = !this.show;
        }, 850)
        setTimeout(() => { clearInterval(timerId) }, 10000);
    }

    get stateName() {
        return this.show ? 'show' : 'hide'
    }

    protected getStartAngle(): number {
        return 316;
    }

    protected getEndAngle(): number {
        return 404;
    }

    protected getRatioType(): Ratio {
        return 'Only Positive [0,1]';
    }

    protected _updateCurrentData(sum: DefaultTypes.Summary): void {
        let arrowIndicate: number;
        if (sum.production.activePower > 49) {
            arrowIndicate = Utils.divideSafely(sum.production.activePower, sum.system.totalPower);
        } else {
            arrowIndicate = 0;
        }
        super.updateSectionData(
            sum.production.activePower,
            sum.production.powerRatio,
            arrowIndicate);
    }

    protected getSquarePosition(square: SvgSquare, innerRadius: number): SvgSquarePosition {
        let x = (square.length / 2) * (-1);
        let y = (innerRadius - 5) * (-1);
        return new SvgSquarePosition(x, y);
    }

    protected getImagePath(): string {
        return "production.png";
    }

    protected getValueText(value: number): string {
        if (value == null || Number.isNaN(value)) {
            return "";
        }

        return this.unitpipe.transform(value, 'kW');
    }

    protected initEnergyFlow(radius: number): EnergyFlow {
        return new EnergyFlow(radius, { x1: "50%", y1: "100%", x2: "50%", y2: "0%" });
    }

    protected getSvgEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        let v = Math.abs(ratio);
        if (v < 8 && v != 0) {
            v = 8;
        }
        let r = radius;
        let p = {
            topLeft: { x: v * -1, y: r * -1.2 },
            bottomLeft: { x: v * -1, y: v * -1 },
            topRight: { x: v, y: r * -1.2 },
            bottomRight: { x: v, y: v * -1 },
            middleBottom: { x: 0, y: 0 },
            middleTop: { x: 0, y: r * -1.2 + v }
        }
        if (ratio < 0) {
            // towards top
            p.topLeft.y = p.topLeft.y + v;
            p.middleTop.y = p.middleTop.y - v;
            p.topRight.y = p.topRight.y + v;
        }
        this.p = p;
        return p;
    }

    protected getSvgAnimationEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        let v = Math.abs(ratio);
        if (v < 8 && v != 0) {
            v = 8;
        }
        let r = radius;
        let animationWidth = r * -1.2 + v;
        let p = {
            topLeft: { x: v * -1, y: r * -1.2 },
            bottomLeft: { x: v * -1, y: null },
            topRight: { x: v, y: r * -1.2 },
            bottomRight: { x: v, y: null },
            middleBottom: { x: 0, y: null },
            middleTop: { x: 0, y: r * -1.2 + v }
        }
        p.bottomRight.y = p.bottomLeft.y
        p.bottomLeft.y = p.bottomRight.y
        p.middleBottom.y = p.middleTop.y
        if (ratio < 0) {
            // towards top
            p.topLeft.y = p.topLeft.y + v;
            p.middleTop.y = p.middleTop.y - v;
            p.topRight.y = p.topRight.y + v;
        }
        return p;
    }
}