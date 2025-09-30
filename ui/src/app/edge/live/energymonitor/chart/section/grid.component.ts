// @ts-strict-ignore
import { animate, state, style, transition, trigger } from "@angular/animations";
import { Component, OnDestroy, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { UnitvaluePipe } from "src/app/shared/pipe/unitvalue/UNITVALUE.PIPE";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";
import { Icon } from "src/app/shared/type/widget";
import { CurrentData, EdgeConfig, GridMode, Service, Utils } from "../../../../../shared/shared";
import { AbstractSection, EnergyFlow, Ratio, SvgEnergyFlow, SvgSquare, SvgSquarePosition } from "./ABSTRACTSECTION.COMPONENT";

@Component({
    selector: "[gridsection]",
    templateUrl: "./GRID.COMPONENT.HTML",
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
    standalone: false,
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
        super("GENERAL.GRID", "left", "var(--ion-color-dark)", translate, service, "Grid");
        THIS.UNITPIPE = unitpipe;
    }

    get stateNameBuy() {
        return THIS.SHOW_BUY_ANIMATION ? "show" : "hide";
    }

    get stateNameSell() {
        return THIS.SHOW_SELL_ANIMATION ? "show" : "hide";
    }

    public static getCurrentGridIcon(currentData: CurrentData): Icon {
        const gridMode = CURRENT_DATA.ALL_COMPONENTS["_sum/GridMode"];
        const restrictionMode = CURRENT_DATA.ALL_COMPONENTS["ctrlEssLimiter14a0/RestrictionMode"];
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
        return CONFIG.GET_COMPONENTS_BY_FACTORY(factoryId).filter(component => COMPONENT.IS_ENABLED).length > 0;
    }

    ngOnInit() {
        THIS.ADJUST_FILL_REFBY_BROWSER();
    }

    ngOnDestroy() {
        clearInterval(THIS.START_ANIMATION);
    }

    toggleBuyAnimation() {
        THIS.START_ANIMATION = setInterval(() => {
            THIS.SHOW_BUY_ANIMATION = !THIS.SHOW_BUY_ANIMATION;
        }, THIS.ANIMATION_SPEED);
        THIS.BUY_ANIMATION_TRIGGER = true;
        THIS.SELL_ANIMATION_TRIGGER = false;
    }

    toggleSellAnimation() {
        THIS.START_ANIMATION = setInterval(() => {
            THIS.SHOW_SELL_ANIMATION = !THIS.SHOW_SELL_ANIMATION;
        }, THIS.ANIMATION_SPEED);
        THIS.BUY_ANIMATION_TRIGGER = false;
        THIS.SELL_ANIMATION_TRIGGER = true;
    }

    public _updateCurrentData(sum: DEFAULT_TYPES.SUMMARY): void {
        // only reacts to kW values (50 W => 0.1 kW rounded)
        if (SUM.GRID.BUY_ACTIVE_POWER && SUM.GRID.BUY_ACTIVE_POWER > 49) {
            if (!THIS.BUY_ANIMATION_TRIGGER) {
                THIS.TOGGLE_BUY_ANIMATION();
            }
            let arrowIndicate: number;
            if (SUM.GRID.BUY_ACTIVE_POWER > 49) {
                arrowIndicate = UTILS.MULTIPLY_SAFELY(
                    UTILS.DIVIDE_SAFELY(SUM.GRID.BUY_ACTIVE_POWER, SUM.SYSTEM.TOTAL_POWER), -1);
            } else {
                arrowIndicate = 0;
            }
            THIS.NAME = THIS.TRANSLATE.INSTANT("GENERAL.GRID_BUY");
            SUPER.UPDATE_SECTION_DATA(
                SUM.GRID.BUY_ACTIVE_POWER,
                SUM.GRID.POWER_RATIO,
                arrowIndicate);
            // only reacts to kW values (50 W => 0.1 kW rounded)
        } else if (SUM.GRID.SELL_ACTIVE_POWER && SUM.GRID.SELL_ACTIVE_POWER > 49) {
            if (!THIS.SELL_ANIMATION_TRIGGER) {
                THIS.TOGGLE_SELL_ANIMATION();
            }
            let arrowIndicate: number;
            if (SUM.GRID.SELL_ACTIVE_POWER > 49) {
                arrowIndicate = UTILS.DIVIDE_SAFELY(SUM.GRID.SELL_ACTIVE_POWER, SUM.SYSTEM.TOTAL_POWER);
            } else {
                arrowIndicate = 0;
            }
            THIS.NAME = THIS.TRANSLATE.INSTANT("GENERAL.GRID_SELL");
            SUPER.UPDATE_SECTION_DATA(
                SUM.GRID.SELL_ACTIVE_POWER,
                SUM.GRID.POWER_RATIO,
                arrowIndicate);
        } else {
            THIS.NAME = THIS.TRANSLATE.INSTANT("GENERAL.GRID");
            SUPER.UPDATE_SECTION_DATA(0, null, null);
        }

        // set grid mode
        THIS.GRID_MODE = SUM.GRID.GRID_MODE;
        if (THIS.SQUARE) {
            THIS.SQUARE.IMAGE.IMAGE = "assets/img/" + THIS.GET_IMAGE_PATH();
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
        const y = (SQUARE.LENGTH / 2) * (-1);
        return new SvgSquarePosition(x, y);
    }

    protected getImagePath(): string {
        if (THIS.GRID_MODE === GridMode.OFF_GRID) {
            return "icon/OFFGRID.SVG";
        } else if (THIS.RESTRICTION_MODE === 1) {
            return "icon/GRID_RESTRICTION.SVG";
        }
        return "icon/GRID.SVG";
    }

    protected getValueText(value: number): string {
        if (value == null || NUMBER.IS_NA_N(value)) {
            return "";
        }
        return THIS.UNITPIPE.TRANSFORM(value, "kW");
    }

    protected initEnergyFlow(radius: number): EnergyFlow {
        return new EnergyFlow(radius, { x1: "100%", y1: "50%", x2: "0%", y2: "50%" });
    }

    protected setElementHeight() {
        THIS.SQUARE.VALUE_TEXT.Y = THIS.SQUARE.VALUE_TEXT.Y - (THIS.SQUARE.VALUE_TEXT.Y * 0.3);
        THIS.SQUARE.IMAGE.Y = THIS.SQUARE.IMAGE.Y - (THIS.SQUARE.IMAGE.Y * 0.3);
    }

    protected getSvgEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        const v = MATH.ABS(ratio);
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
            P.TOP_LEFT.X = P.TOP_LEFT.X + v;
            P.MIDDLE_LEFT.X = P.MIDDLE_LEFT.X - v;
            P.BOTTOM_LEFT.X = P.BOTTOM_LEFT.X + v;
            P.MIDDLE_RIGHT.X = P.TOP_RIGHT.X - v;
        }
        return p;
    }

    protected getSvgAnimationEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        const v = MATH.ABS(ratio);
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
            P.TOP_LEFT.X = P.TOP_RIGHT.X - animationWidth * 0.2;
            P.MIDDLE_LEFT.X = P.MIDDLE_RIGHT.X - animationWidth * 0.2 - 2 * v;
            P.BOTTOM_LEFT.X = P.BOTTOM_RIGHT.X - animationWidth * 0.2;
            P.MIDDLE_RIGHT.X = P.MIDDLE_LEFT.X + animationWidth * 0.2;
        } else if (ratio < 0) {
            // towards right
            P.MIDDLE_RIGHT.X = P.MIDDLE_LEFT.X + animationWidth * 0.2;
            P.TOP_RIGHT.X = P.TOP_LEFT.X + animationWidth * 0.2;
            P.BOTTOM_RIGHT.X = P.BOTTOM_LEFT.X + animationWidth * 0.2;
        } else {
            p = null;
        }
        return p;
    }
}
