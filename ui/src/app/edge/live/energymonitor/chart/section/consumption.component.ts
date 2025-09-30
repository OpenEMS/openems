// @ts-strict-ignore
import { animate, state, style, transition, trigger } from "@angular/animations";
import { Component, OnDestroy, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { UnitvaluePipe } from "src/app/shared/pipe/unitvalue/UNITVALUE.PIPE";
import { Service, Utils } from "../../../../../shared/shared";
import { DefaultTypes } from "../../../../../shared/type/defaulttypes";
import { AbstractSection, EnergyFlow, Ratio, SvgEnergyFlow, SvgSquare, SvgSquarePosition } from "./ABSTRACTSECTION.COMPONENT";

@Component({
    selector: "[consumptionsection]",
    templateUrl: "./CONSUMPTION.COMPONENT.HTML",
    animations: [
        trigger("Consumption", [
            state("show", style({
                opacity: 0.1,
                transform: "translateX(0%)",
            })),
            state("hide", style({
                opacity: 0.6,
                transform: "translateX(17%)",
            })),
            transition("show => hide", animate("650ms ease-out")),
            transition("hide => show", animate("0ms ease-in")),
        ]),
    ],
    standalone: false,
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
        super("GENERAL.CONSUMPTION", "right", "#FDC507", translate, service, "Consumption");
        THIS.UNITPIPE = unitpipe;
    }

    get stateName() {
        return THIS.SHOW_ANIMATION ? "show" : "hide";
    }

    ngOnInit() {
        THIS.ADJUST_FILL_REFBY_BROWSER();
    }

    toggleAnimation() {
        THIS.START_ANIMATION = setInterval(() => {
            THIS.SHOW_ANIMATION = !THIS.SHOW_ANIMATION;
        }, THIS.ANIMATION_SPEED);
        THIS.ANIMATION_TRIGGER = true;
    }

    ngOnDestroy() {
        clearInterval(THIS.START_ANIMATION);
    }

    protected getStartAngle(): number {
        return 46;
    }

    protected getEndAngle(): number {
        return 134;
    }

    protected getRatioType(): Ratio {
        return "Only Positive [0,1]";
    }

    protected _updateCurrentData(sum: DEFAULT_TYPES.SUMMARY): void {
        let arrowIndicate: number;
        // only reacts to kW values (50 W => 0.1 kW rounded)
        if (SUM.CONSUMPTION.ACTIVE_POWER > 49) {
            if (!THIS.ANIMATION_TRIGGER) {
                THIS.TOGGLE_ANIMATION();
            }
            arrowIndicate = UTILS.DIVIDE_SAFELY(SUM.CONSUMPTION.ACTIVE_POWER, SUM.SYSTEM.TOTAL_POWER);
        } else {
            arrowIndicate = 0;
        }
        SUPER.UPDATE_SECTION_DATA(
            SUM.CONSUMPTION.ACTIVE_POWER,
            SUM.CONSUMPTION.POWER_RATIO,
            arrowIndicate);
    }

    protected getSquarePosition(square: SvgSquare, innerRadius: number): SvgSquarePosition {
        const x = innerRadius - 5 - SQUARE.LENGTH;
        const y = (SQUARE.LENGTH / 2) * (-1);
        return new SvgSquarePosition(x, y);
    }
    protected getImagePath(): string {
        return "icon/CONSUMPTION.SVG";
    }

    protected getValueText(value: number): string {
        if (value == null || NUMBER.IS_NA_N(value)) {
            return "";
        }
        return THIS.UNITPIPE.TRANSFORM(value, "kW");
    }

    protected initEnergyFlow(radius: number): EnergyFlow {
        return new EnergyFlow(radius, { x1: "0%", y1: "50%", x2: "100%", y2: "50%" });
    }

    protected setElementHeight() {
        THIS.SQUARE.VALUE_TEXT.Y = THIS.SQUARE.VALUE_TEXT.Y - (THIS.SQUARE.VALUE_TEXT.Y * 0.3);
        THIS.SQUARE.IMAGE.Y = THIS.SQUARE.IMAGE.Y - (THIS.SQUARE.IMAGE.Y * 0.3);
    }

    protected getSvgEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        const v = MATH.ABS(ratio);
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
            P.TOP_RIGHT.X = P.TOP_RIGHT.X - v;
            P.MIDDLE_RIGHT.X = P.MIDDLE_RIGHT.X + v;
            P.BOTTOM_RIGHT.X = P.BOTTOM_RIGHT.X - v;
            P.MIDDLE_LEFT.X = P.TOP_LEFT.X + v;
        }
        return p;
    }

    protected getSvgAnimationEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        const v = MATH.ABS(ratio);
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
            P.TOP_RIGHT.X = P.TOP_LEFT.X + animationWidth * 0.2;
            P.MIDDLE_RIGHT.X = P.MIDDLE_LEFT.X + animationWidth * 0.2 + 2 * v;
            P.BOTTOM_RIGHT.X = P.BOTTOM_LEFT.X + animationWidth * 0.2;
            P.MIDDLE_LEFT.X = P.MIDDLE_RIGHT.X - animationWidth * 0.2;
        } else {
            p = null;
        }
        return p;
    }

}
