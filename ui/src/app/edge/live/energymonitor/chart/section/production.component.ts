import { animate, state, style, transition, trigger } from '@angular/animations';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { UnitvaluePipe } from 'src/app/shared/pipe/unitvalue/unitvalue.pipe';
import { DefaultTypes } from '../../../../../shared/service/defaulttypes';
import { Service, Utils } from '../../../../../shared/shared';
import { AbstractSection, EnergyFlow, Ratio, SvgEnergyFlow, SvgSquare, SvgSquarePosition } from './abstractsection.component';

@Component({
    selector: '[productionsection]',
    templateUrl: './production.component.html',
    animations: [
        trigger('Production', [
            state('show', style({
                opacity: 0.4,
                transform: 'translateY(0)'
            })),
            state('hide', style({
                opacity: 0.1,
                transform: 'translateY(17%)'
            })),
            transition('show => hide', animate('650ms ease-out')),
            transition('hide => show', animate('0ms ease-in'))
        ])
    ]
})
export class ProductionSectionComponent extends AbstractSection implements OnInit, OnDestroy {

    private unitpipe: UnitvaluePipe;
    // animation variable to stop animation on destroy
    private startAnimation = null;
    private showAnimation: boolean = false;
    private animationTrigger: boolean = false;

    constructor(
        translate: TranslateService,
        service: Service,
        unitpipe: UnitvaluePipe,
    ) {
        super('General.production', "up", "#36aed1", translate, service, "Common_Production");
        this.unitpipe = unitpipe;
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

    get stateName() {
        return this.showAnimation ? 'show' : 'hide';
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
        // only reacts to kW values (50 W => 0.1 kW rounded)
        if (sum.production.activePower > 49) {
            if (!this.animationTrigger) {
                this.toggleAnimation();
            }
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
        let y = (innerRadius - 10) * (-1);
        return new SvgSquarePosition(x, y);
    }

    protected getImagePath(): string {
        return "icon/production.svg";
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

    protected setElementHeight() {
        this.square.valueText.y = this.square.valueText.y - (this.square.valueText.y * 0.4);
        this.square.image.y = this.square.image.y - (this.square.image.y * 0.45);
    }

    protected getSvgEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        let v = Math.abs(ratio);
        let r = radius;
        let p = {
            topLeft: { x: v * -1, y: r * -1 },
            bottomLeft: { x: v * -1, y: v * -1 },
            topRight: { x: v, y: r * -1 },
            bottomRight: { x: v, y: v * -1 },
            middleBottom: { x: 0, y: 0 },
            middleTop: { x: 0, y: r * -1 + v }
        };
        if (ratio < 0) {
            // towards top
            p.topLeft.y = p.topLeft.y + v;
            p.middleTop.y = p.middleTop.y - v;
            p.topRight.y = p.topRight.y + v;
        }
        return p;
    }

    protected getSvgAnimationEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        let v = Math.abs(ratio);
        let r = radius;
        let animationWidth = r * -1 + v;
        let p = {
            topLeft: { x: v * -1, y: r * -1 },
            bottomLeft: { x: v * -1, y: v * -1 },
            topRight: { x: v, y: r * -1 },
            bottomRight: { x: v, y: v * -1 },
            middleBottom: { x: 0, y: 0 },
            middleTop: { x: 0, y: r * -1 + v }
        };
        if (ratio > 0) {
            // towards bottom
            p.bottomRight.y = p.topRight.y + animationWidth * 0.2;
            p.bottomLeft.y = p.topLeft.y + animationWidth * 0.2;
            p.middleBottom.y = p.middleTop.y + animationWidth * 0.2;
        } else {
            p = null;
        }
        return p;
    }

    ngOnDestroy() {
        clearInterval(this.startAnimation);
    }
}