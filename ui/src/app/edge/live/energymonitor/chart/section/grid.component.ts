// @ts-strict-ignore
import { animate, state, style, transition, trigger } from "@angular/animations";
import { Component, OnDestroy, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { UnitvaluePipe } from "src/app/shared/pipe/unitvalue/unitvalue.pipe";
import { DefaultTypes } from "src/app/shared/service/defaulttypes";
import { Icon } from "src/app/shared/type/widget";
import { CurrentData, EdgeConfig, GridMode, Service, Utils } from "../../../../../shared/shared";
import { AbstractSection, EnergyFlow, Ratio, SvgEnergyFlow, SvgSquare, SvgSquarePosition } from "./abstractsection.component";

@Component({
    selector: "[gridsection]",
    templateUrl: "./grid.component.html",
    animations: [
        trigger("GridBuy", [
            state("show", style({
                opacity: 0.4,
                transform: "translateX(0%)",
            })),
            state("hide", style({
                opacity: 0.1,
                transform: "translateX(17%)",
            })),
            transition("show => hide", animate("650ms")),
            transition("hide => show", animate("0ms")),
        ]),
        trigger("GridSell", [
            state("show", style({
                opacity: 0.1,
                transform: "translateX(0%)",
            })),
            state("hide", style({
                opacity: 0.4,
                transform: "translateX(-17%)",
            })),
            transition("show => hide", animate("650ms ease-out")),
            transition("hide => show", animate("0ms ease-in")),
        ]),
    ],
})
export class GridSectionComponent extends AbstractSection implements OnInit, OnDestroy {

    public buyAnimationTrigger: boolean = false;
    public sellAnimationTrigger: boolean = false;

    private unitpipe: UnitvaluePipe;
    // animation variable to stop animation on destroy
    private startAnimation = null;
    private showBuyAnimation = false;
    private showSellAnimation = false;

    constructor(
        translate: TranslateService,
        service: Service,
        unitpipe: UnitvaluePipe,
    ) {
        super("General.grid", "left", "var(--ion-color-dark)", translate, service, "Grid");
        this.unitpipe = unitpipe;
    }

    get stateNameBuy() {
        return this.showBuyAnimation ? "show" : "hide";
    }

    get stateNameSell() {
        return this.showSellAnimation ? "show" : "hide";
    }

    public static getCurrentGridIcon(currentData: CurrentData): Icon {
        const gridMode = currentData.allComponents["_sum/GridMode"];
        const restrictionMode = currentData.allComponents["ctrlEssLimiter14a0/RestrictionMode"];
        if (gridMode === GridMode.OFF_GRID) {
            return {
                color: "dark",
                name: "oe-offgrid",
                size: "",
            };
        }
        if (restrictionMode === 1) {
            return {
                color: "dark",
                name: "oe-grid-restriction",
                size: "",
            };
        }
        return {
            color: "dark",
            name: "oe-grid",
            size: "",
        };
    }

    public static isControllerEnabled(config: EdgeConfig, factoryId: string): boolean {
        return config.getComponentsByFactory(factoryId).filter(component => component.isEnabled).length > 0;
    }

    ngOnInit() {
        this.adjustFillRefbyBrowser();
    }

    ngOnDestroy() {
        clearInterval(this.startAnimation);
    }

    toggleBuyAnimation() {
        this.startAnimation = setInterval(() => {
            this.showBuyAnimation = !this.showBuyAnimation;
        }, this.animationSpeed);
        this.buyAnimationTrigger = true;
        this.sellAnimationTrigger = false;
    }

    toggleSellAnimation() {
        this.startAnimation = setInterval(() => {
            this.showSellAnimation = !this.showSellAnimation;
        }, this.animationSpeed);
        this.buyAnimationTrigger = false;
        this.sellAnimationTrigger = true;
    }

    public _updateCurrentData(sum: DefaultTypes.Summary): void {
        // only reacts to kW values (50 W => 0.1 kW rounded)
        if (sum.grid.buyActivePower && sum.grid.buyActivePower > 49) {
            if (!this.buyAnimationTrigger) {
                this.toggleBuyAnimation();
            }
            let arrowIndicate: number;
            if (sum.grid.buyActivePower > 49) {
                arrowIndicate = Utils.multiplySafely(
                    Utils.divideSafely(sum.grid.buyActivePower, sum.system.totalPower), -1);
            } else {
                arrowIndicate = 0;
            }
            this.name = this.translate.instant("General.gridBuy");
            super.updateSectionData(
                sum.grid.buyActivePower,
                sum.grid.powerRatio,
                arrowIndicate);
            // only reacts to kW values (50 W => 0.1 kW rounded)
        } else if (sum.grid.sellActivePower && sum.grid.sellActivePower > 49) {
            if (!this.sellAnimationTrigger) {
                this.toggleSellAnimation();
            }
            let arrowIndicate: number;
            if (sum.grid.sellActivePower > 49) {
                arrowIndicate = Utils.divideSafely(sum.grid.sellActivePower, sum.system.totalPower);
            } else {
                arrowIndicate = 0;
            }
            this.name = this.translate.instant("General.gridSell");
            super.updateSectionData(
                sum.grid.sellActivePower,
                sum.grid.powerRatio,
                arrowIndicate);
        } else {
            this.name = this.translate.instant("General.grid");
            super.updateSectionData(0, null, null);
        }

        // set grid mode
        this.gridMode = sum.grid.gridMode;
        if (this.square) {
            this.square.image.image = "assets/img/" + this.getImagePath();
        }
    }

    protected getStartAngle(): number {
        return 226;
    }

    protected getEndAngle(): number {
        return 314;
    }

    protected getRatioType(): Ratio {
        return "Negative and Positive [-1,1]";
    }

    protected getSquarePosition(square: SvgSquare, innerRadius: number): SvgSquarePosition {
        const x = (innerRadius - 5) * (-1);
        const y = (square.length / 2) * (-1);
        return new SvgSquarePosition(x, y);
    }

    protected getImagePath(): string {
        if (this.gridMode === GridMode.OFF_GRID) {
            return "icon/offgrid.svg";
        } else if (this.restrictionMode === 1) {
            return "icon/gridRestriction.svg";
        }
        return "icon/grid.svg";
    }

    protected getValueText(value: number): string {
        if (value == null || Number.isNaN(value)) {
            return "";
        }
        return this.unitpipe.transform(value, "kW");
    }

    protected initEnergyFlow(radius: number): EnergyFlow {
        return new EnergyFlow(radius, { x1: "100%", y1: "50%", x2: "0%", y2: "50%" });
    }

    protected setElementHeight() {
        this.square.valueText.y = this.square.valueText.y - (this.square.valueText.y * 0.3);
        this.square.image.y = this.square.image.y - (this.square.image.y * 0.3);
    }

    protected getSvgEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        const v = Math.abs(ratio);
        const r = radius;
        const p = {
            bottomRight: { x: v * -1, y: v },
            bottomLeft: { x: r * -1, y: v },
            topRight: { x: v * -1, y: v * -1 },
            topLeft: { x: r * -1, y: v * -1 },
            middleLeft: { x: r * -1 + v, y: 0 },
            middleRight: { x: 0, y: 0 },
        };
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
        const v = Math.abs(ratio);
        const r = radius;
        const animationWidth = r * -1 + v;
        let p = {
            bottomRight: { x: v * -1, y: v },
            bottomLeft: { x: r * -1, y: v },
            topRight: { x: v * -1, y: v * -1 },
            topLeft: { x: r * -1, y: v * -1 },
            middleLeft: { x: r * -1 + v, y: 0 },
            middleRight: { x: 0, y: 0 },
        };

        if (ratio > 0) {
            // towards left
            p.topLeft.x = p.topRight.x - animationWidth * 0.2;
            p.middleLeft.x = p.middleRight.x - animationWidth * 0.2 - 2 * v;
            p.bottomLeft.x = p.bottomRight.x - animationWidth * 0.2;
            p.middleRight.x = p.middleLeft.x + animationWidth * 0.2;
        } else if (ratio < 0) {
            // towards right
            p.middleRight.x = p.middleLeft.x + animationWidth * 0.2;
            p.topRight.x = p.topLeft.x + animationWidth * 0.2;
            p.bottomRight.x = p.bottomLeft.x + animationWidth * 0.2;
        } else {
            p = null;
        }
        return p;
    }
}
