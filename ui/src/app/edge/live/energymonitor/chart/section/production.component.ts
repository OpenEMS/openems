// @ts-strict-ignore
import { animate, state, style, transition, trigger } from "@angular/animations";
import { Component, OnDestroy, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { UnitvaluePipe } from "src/app/shared/pipe/unitvalue/UNITVALUE.PIPE";
import { Service, Utils } from "../../../../../shared/shared";
import { DefaultTypes } from "../../../../../shared/type/defaulttypes";
import { AbstractSection, EnergyFlow, Ratio, SvgEnergyFlow, SvgSquare, SvgSquarePosition } from "./ABSTRACTSECTION.COMPONENT";

@Component({
    selector: "[productionsection]",
    templateUrl: "./PRODUCTION.COMPONENT.HTML",
    animations: [
        trigger("Production", [
            state("show", style({
                opacity: 0.4,
                transform: "translateY(0)",
            })),
            state("hide", style({
                opacity: 0.1,
                transform: "translateY(17%)",
            })),
            transition("show => hide", animate("650ms ease-out")),
            transition("hide => show", animate("0ms ease-in")),
        ]),
    ],
    standalone: false,
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
        super("GENERAL.PRODUCTION", "up", "var(--ion-color-primary)", translate, service, "Common_Production");
        THIS.UNITPIPE = unitpipe;
    }

    get stateName() {
        return THIS.SHOW_ANIMATION ? "show" : "hide";
    }

    ngOnInit() {
        THIS.ADJUST_FILL_REFBY_BROWSER();
    }

    ngOnDestroy() {
        clearInterval(THIS.START_ANIMATION);
    }

    toggleAnimation() {
        THIS.START_ANIMATION = setInterval(() => {
            THIS.SHOW_ANIMATION = !THIS.SHOW_ANIMATION;
        }, THIS.ANIMATION_SPEED);
        THIS.ANIMATION_TRIGGER = true;
    }

    protected getStartAngle(): number {
        return 316;
    }

    protected getEndAngle(): number {
        return 404;
    }

    protected getRatioType(): Ratio {
        return "Only Positive [0,1]";
    }

    protected _updateCurrentData(sum: DEFAULT_TYPES.SUMMARY): void {
        let arrowIndicate: number;
        // only reacts to kW values (50 W => 0.1 kW rounded)
        if (SUM.PRODUCTION.ACTIVE_POWER > 49) {
            if (!THIS.ANIMATION_TRIGGER) {
                THIS.TOGGLE_ANIMATION();
            }
            arrowIndicate = UTILS.DIVIDE_SAFELY(SUM.PRODUCTION.ACTIVE_POWER, SUM.SYSTEM.TOTAL_POWER);
        } else {
            arrowIndicate = 0;
        }
        SUPER.UPDATE_SECTION_DATA(
            SUM.PRODUCTION.ACTIVE_POWER,
            SUM.PRODUCTION.POWER_RATIO,
            arrowIndicate);
    }

    protected getSquarePosition(square: SvgSquare, innerRadius: number): SvgSquarePosition {
        const x = (SQUARE.LENGTH / 2) * (-1);
        const y = (innerRadius - 10) * (-1);
        return new SvgSquarePosition(x, y);
    }

    protected getImagePath(): string {
        return "icon/PRODUCTION.SVG";
    }

    protected getValueText(value: number): string {
        if (value == null || NUMBER.IS_NA_N(value)) {
            return "";
        }

        return THIS.UNITPIPE.TRANSFORM(value, "kW");
    }

    protected initEnergyFlow(radius: number): EnergyFlow {
        return new EnergyFlow(radius, { x1: "50%", y1: "100%", x2: "50%", y2: "0%" });
    }

    protected setElementHeight() {
        THIS.SQUARE.VALUE_TEXT.Y = THIS.SQUARE.VALUE_TEXT.Y - (THIS.SQUARE.VALUE_TEXT.Y * 0.4);
        THIS.SQUARE.IMAGE.Y = THIS.SQUARE.IMAGE.Y - (THIS.SQUARE.IMAGE.Y * 0.45);
    }

    protected getSvgEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        const v = MATH.ABS(ratio);
        const r = radius;
        const p = {
            topLeft: { x: v * -1, y: r * -1 },
            bottomLeft: { x: v * -1, y: v * -1 },
            topRight: { x: v, y: r * -1 },
            bottomRight: { x: v, y: v * -1 },
            middleBottom: { x: 0, y: 0 },
            middleTop: { x: 0, y: r * -1 + v },
        };
        if (ratio < 0) {
            // towards top
            P.TOP_LEFT.Y = P.TOP_LEFT.Y + v;
            P.MIDDLE_TOP.Y = P.MIDDLE_TOP.Y - v;
            P.TOP_RIGHT.Y = P.TOP_RIGHT.Y + v;
        }
        return p;
    }

    protected getSvgAnimationEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        const v = MATH.ABS(ratio);
        const r = radius;
        const animationWidth = r * -1 + v;
        let p = {
            topLeft: { x: v * -1, y: r * -1 },
            bottomLeft: { x: v * -1, y: v * -1 },
            topRight: { x: v, y: r * -1 },
            bottomRight: { x: v, y: v * -1 },
            middleBottom: { x: 0, y: 0 },
            middleTop: { x: 0, y: r * -1 + v },
        };
        if (ratio > 0) {
            // towards bottom
            P.BOTTOM_RIGHT.Y = P.TOP_RIGHT.Y + animationWidth * 0.2;
            P.BOTTOM_LEFT.Y = P.TOP_LEFT.Y + animationWidth * 0.2;
            P.MIDDLE_BOTTOM.Y = P.MIDDLE_TOP.Y + animationWidth * 0.2;
        } else {
            p = null;
        }
        return p;
    }

}
