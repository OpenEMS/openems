// @ts-strict-ignore
import { Component, OnDestroy, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { Subscription } from "rxjs";
import { UnitvaluePipe } from "src/app/shared/pipe/unitvalue/unitvalue.pipe";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";
import { Icon } from "src/app/shared/type/widget";
import { CurrentData, EdgeConfig, GridMode, Service, Utils } from "../../../../../shared/shared";
import { AbstractSection, EnergyFlow, Ratio, SvgEnergyFlow, SvgSquare, SvgSquarePosition } from "./abstractsection.component";
import { AnimationService } from "./animation.service";

@Component({
    selector: "[gridsection]",
    templateUrl: "./grid.component.html",
    styleUrls: ["../animation.scss"],
    standalone: false,
})
export class GridSectionComponent extends AbstractSection implements OnInit, OnDestroy {

    public buyAnimationTrigger: boolean = false;
    public sellAnimationTrigger: boolean = false;

    private unitpipe: UnitvaluePipe;
    private subShow?: Subscription;
    private sellAnimationClass: string = "grid-sell-hide";
    private buyAnimationClass: string = "grid-buy-hide"

    constructor(
        translate: TranslateService,
        service: Service,
        unitpipe: UnitvaluePipe,
        private animationService: AnimationService,
    ) {
        super("GENERAL.GRID", "left", "var(--ion-color-dark)", translate, service, "Grid");
        this.unitpipe = unitpipe;
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
        this.subShow = this.animationService.toggleAnimation$.subscribe((show) => {
            this.buyAnimationClass = show ? "grid-buy-show" : "grid-buy-hide";
            this.sellAnimationClass = show ? "grid-sell-hide" : "grid-sell-show";
        });
    }

    ngOnDestroy() {
        this.subShow?.unsubscribe();
    }

    toggleBuyAnimation() {
        this.buyAnimationTrigger = true;
        this.sellAnimationTrigger = false;
    }

    toggleSellAnimation() {
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
            this.name = this.translate.instant("GENERAL.GRID_BUY");
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
            this.name = this.translate.instant("GENERAL.GRID_SELL");
            super.updateSectionData(
                sum.grid.sellActivePower,
                sum.grid.powerRatio,
                arrowIndicate);
        } else {
            this.name = this.translate.instant("GENERAL.GRID");
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
