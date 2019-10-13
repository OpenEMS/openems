import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from '../../../../../shared/service/defaulttypes';
import { Service, Utils } from '../../../../../shared/shared';
import { AbstractSection, EnergyFlow, Ratio, SvgEnergyFlow, SvgSquare, SvgSquarePosition } from './abstractsection.component';
import { UnitvaluePipe } from 'src/app/shared/pipe/unitvalue/unitvalue.pipe';
import { trigger, state, style, animate, transition } from '@angular/animations';
import { interval } from 'rxjs';

@Component({
    selector: '[gridsection]',
    templateUrl: './grid.component.html',
    animations: [
        trigger('GridBuy', [
            state('show', style({
                opacity: 0.3,
                transform: 'translateX(0%)',
            })),
            state('hide', style({
                opacity: 0,
                transform: 'translateX(10%)'
            })),
            transition('show => hide', animate('300ms')),
            transition('hide => show', animate('0ms'))
        ]),
        trigger('GridSell', [
            state('show', style({
                opacity: 0.3,
                transform: 'translateX(0%)',
            })),
            state('hide', style({
                opacity: 0,
                transform: 'translateX(-10%)'
            })),
            transition('show => hide', animate('300ms')),
            transition('hide => show', animate('0ms'))
        ])
    ]
})
export class GridSectionComponent extends AbstractSection implements OnInit {

    private unitpipe: UnitvaluePipe;
    public showBuy = false;
    public showSell = false;
    public buy: boolean = false;
    public sell: boolean = false;

    constructor(
        translate: TranslateService,
        service: Service,
        unitpipe: UnitvaluePipe,
    ) {
        super('General.Grid', "left", "#1d1d1d", translate, service, "Grid");
        this.unitpipe = unitpipe;
    }

    ngOnInit() {
    }

    toggleBuy() {
        setInterval(() => {
            this.showBuy = !this.showBuy;
        }, this.animationSpeed);
        this.buy = true;
        this.sell = false;
    }

    toggleSell() {
        setInterval(() => {
            this.showSell = !this.showSell;
        }, this.animationSpeed);
        this.buy = false;
        this.sell = true;
    }

    get stateNameBuy() {
        return this.showBuy ? 'show' : 'hide'
    }

    get stateNameSell() {
        return this.showSell ? 'show' : 'hide'
    }

    protected getStartAngle(): number {
        return 226;
    }

    protected getEndAngle(): number {
        return 314;
    }

    protected getRatioType(): Ratio {
        return 'Negative and Positive [-1,1]';
    }

    public _updateCurrentData(sum: DefaultTypes.Summary): void {
        if (sum.grid.buyActivePower && sum.grid.buyActivePower > 0) {
            if (!this.buy) {
                this.toggleBuy();
            }
            // this.show = !this.show;
            let arrowIndicate: number;
            if (sum.grid.buyActivePower > 49) {
                arrowIndicate = Utils.multiplySafely(
                    Utils.divideSafely(sum.grid.buyActivePower, sum.system.totalPower), -1)
            } else {
                arrowIndicate = 0;
            }
            this.name = this.translate.instant('General.GridBuy');
            super.updateSectionData(
                sum.grid.buyActivePower,
                sum.grid.powerRatio,
                arrowIndicate);
        } else if (sum.grid.sellActivePower && sum.grid.sellActivePower > 49) {
            if (!this.sell) {
                this.toggleSell();
            }
            let arrowIndicate: number;
            if (sum.grid.sellActivePower > 49) {
                arrowIndicate = Utils.divideSafely(sum.grid.sellActivePower, sum.system.totalPower)
            } else {
                arrowIndicate = 0;
            }
            this.name = this.translate.instant('General.GridSell');
            super.updateSectionData(
                sum.grid.sellActivePower,
                sum.grid.powerRatio,
                arrowIndicate);
        } else {
            this.name = this.translate.instant('General.Grid')
            super.updateSectionData(null, null, null);
        }

        // set grid mode
        this.gridMode = sum.grid.gridMode;
        if (this.square) {
            this.square.image.image = "assets/img/" + this.getImagePath()
        }
    }

    protected getSquarePosition(square: SvgSquare, innerRadius: number): SvgSquarePosition {
        let x = (innerRadius - 5) * (-1);
        let y = (square.length / 2) * (-1);
        return new SvgSquarePosition(x, y);
    }

    protected getImagePath(): string {
        if (this.gridMode == 0 || this.gridMode == 2) {
            return "offgrid.png"
        } else {
            return "grid.png"
        }
    }

    protected getValueText(value: number): string {
        if (value == null || Number.isNaN(value)) {
            return "";
        }

        return this.unitpipe.transform(value, 'kW');
    }

    protected initEnergyFlow(radius: number, animationSpeed: number): EnergyFlow {
        this.animationSpeed = animationSpeed;
        return new EnergyFlow(radius, { x1: "100%", y1: "50%", x2: "0%", y2: "50%" });
    }

    protected getSvgEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        let v = Math.abs(ratio);
        let r = radius;
        let p = {
            bottomRight: { x: v * -1, y: v },
            bottomLeft: { x: r * -1.2, y: v },
            topRight: { x: v * -1, y: v * -1 },
            topLeft: { x: r * -1.2, y: v * -1 },
            middleLeft: { x: r * -1.2 + v, y: 0 },
            middleRight: { x: 0, y: 0 }
        }
        if (ratio > 0) {
            // towards left
            p.topLeft.x = p.topLeft.x + v;
            p.middleLeft.x = p.middleLeft.x - v;
            p.bottomLeft.x = p.bottomLeft.x + v;
            p.middleRight.x = p.topRight.x - v;
        }
        return p;
    }

    protected getSvgAnimationEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        let v = Math.abs(ratio);
        let r = radius;
        let animationWidth = r * 1.2 + v;
        let p = {
            bottomRight: { x: v * -1, y: v },
            bottomLeft: { x: r * -1.2, y: v },
            topRight: { x: v * -1, y: v * -1 },
            topLeft: { x: r * -1.2, y: v * -1 },
            middleLeft: { x: r * -1.2 + v, y: 0 },
            middleRight: { x: 0, y: 0 }
        }

        if (ratio > 0) {
            // towards left
            p.topLeft.x = p.topRight.x - animationWidth * 0.1;
            p.middleLeft.x = p.middleRight.x - animationWidth * 0.1 - 2 * v;
            p.bottomLeft.x = p.bottomRight.x - animationWidth * 0.1;
            p.middleRight.x = p.middleLeft.x + animationWidth * 0.1;
        } else if (ratio < 0) {
            // towards right
            p.middleRight.x = p.middleLeft.x + animationWidth * 0.1;
            p.topRight.x = p.topLeft.x + animationWidth * 0.1;
            p.bottomRight.x = p.bottomLeft.x + animationWidth * 0.1;
        } else {
            p = null;
        }
        return p;
    }
}