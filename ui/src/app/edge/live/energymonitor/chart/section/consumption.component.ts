// @ts-strict-ignore
import { animate, state, style, transition, trigger } from '@angular/animations';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { UnitvaluePipe } from 'src/app/shared/pipe/unitvalue/unitvalue.pipe';
import { DefaultTypes } from '../../../../../shared/service/defaulttypes';
import { Service, Utils } from '../../../../../shared/shared';
import { AbstractSection, EnergyFlow, Ratio, SvgEnergyFlow, SvgSquare, SvgSquarePosition } from './abstractsection.component';

@Component({
    selector: '[consumptionsection]',
    templateUrl: './consumption.component.html',
    animations: [
        trigger('Consumption', [
            state('show', style({
                opacity: 0.1,
                transform: 'translateX(0%)',
            })),
            state('hide', style({
                opacity: 0.6,
                transform: 'translateX(17%)',
            })),
            transition('show => hide', animate('650ms ease-out')),
            transition('hide => show', animate('0ms ease-in')),
        ]),
    ],
})
export class ConsumptionSectionComponent extends AbstractSection implements OnInit, OnDestroy {

    private unitpipe: UnitvaluePipe;
    private showAnimation: boolean = false;
    private animationTrigger: boolean = false;
    // animation variable to stop animation on destroy
    private startAnimation = null;

    constructor(
        unitpipe: UnitvaluePipe,
        translate: TranslateService,
        service: Service,
    ) {
        super('General.consumption', "right", "#FDC507", translate, service, "Consumption");
        this.unitpipe = unitpipe;
    }

    get stateName() {
        return this.showAnimation ? 'show' : 'hide';
    }

    ngOnInit() {
        this.adjustFillRefbyBrowser();
    }

    toggleAnimation() {
        this.startAnimation = setInterval(() => {
            this.showAnimation = !this.showAnimation;
        }, this.animationSpeed);
        this.animationTrigger = true;
    }

    ngOnDestroy() {
        clearInterval(this.startAnimation);
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
        // only reacts to kW values (50 W => 0.1 kW rounded)
        if (sum.consumption.activePower > 49) {
            if (!this.animationTrigger) {
                this.toggleAnimation();
            }
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
        const x = innerRadius - 5 - square.length;
        const y = (square.length / 2) * (-1);
        return new SvgSquarePosition(x, y);
    }
    protected getImagePath(): string {
        return "icon/consumption.svg";
    }

    protected getValueText(value: number): string {
        if (value == null || Number.isNaN(value)) {
            return "";
        }
        return this.unitpipe.transform(value, 'kW');
    }

    protected initEnergyFlow(radius: number): EnergyFlow {
        return new EnergyFlow(radius, { x1: "0%", y1: "50%", x2: "100%", y2: "50%" });
    }

    protected setElementHeight() {
        this.square.valueText.y = this.square.valueText.y - (this.square.valueText.y * 0.3);
        this.square.image.y = this.square.image.y - (this.square.image.y * 0.3);
    }

    protected getSvgEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        const v = Math.abs(ratio);
        const r = radius;
        const p = {
            topLeft: { x: v, y: v * -1 },
            middleLeft: { x: 0, y: 0 },
            bottomLeft: { x: v, y: v },
            topRight: { x: r, y: v * -1 },
            bottomRight: { x: r, y: v },
            middleRight: { x: r - v, y: 0 },
        };
        if (ratio > 0) {
            // towards right
            p.topRight.x = p.topRight.x - v;
            p.middleRight.x = p.middleRight.x + v;
            p.bottomRight.x = p.bottomRight.x - v;
            p.middleLeft.x = p.topLeft.x + v;
        }
        return p;
    }

    protected getSvgAnimationEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        const v = Math.abs(ratio);
        const r = radius;
        const animationWidth = (r * -1) - v;
        let p = {
            topLeft: { x: v, y: v * -1 },
            middleLeft: { x: 0, y: 0 },
            bottomLeft: { x: v, y: v },
            topRight: { x: r, y: v * -1 },
            bottomRight: { x: r, y: v },
            middleRight: { x: r - v, y: 0 },
        };
        if (ratio > 0) {
            // towards right
            p.topRight.x = p.topLeft.x + animationWidth * 0.2;
            p.middleRight.x = p.middleLeft.x + animationWidth * 0.2 + 2 * v;
            p.bottomRight.x = p.bottomLeft.x + animationWidth * 0.2;
            p.middleLeft.x = p.middleRight.x - animationWidth * 0.2;
        } else {
            p = null;
        }
        return p;
    }

}
