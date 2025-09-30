// @ts-strict-ignore
import { animate, state, style, transition, trigger } from "@angular/animations";
import { Component, OnDestroy, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { CurrentData } from "src/app/shared/components/edge/currentdata";
import { UnitvaluePipe } from "src/app/shared/pipe/unitvalue/UNITVALUE.PIPE";
import { Service, Utils } from "../../../../../shared/shared";
import { DefaultTypes } from "../../../../../shared/type/defaulttypes";
import { AbstractSection, EnergyFlow, Ratio, SvgEnergyFlow, SvgSquare, SvgSquarePosition } from "./ABSTRACTSECTION.COMPONENT";

@Component({
    selector: "[storagesection]",
    templateUrl: "./STORAGE.COMPONENT.HTML",
    animations: [
        trigger("Discharge", [
            state("show", style({
                opacity: 0.4,
                transform: "translateY(0)",
            })),
            state("hide", style({
                opacity: 0.1,
                transform: "translateY(-17%)",
            })),
            transition("show => hide", animate("650ms ease-out")),
            transition("hide => show", animate("0ms ease-in")),
        ]),
        trigger("Charge", [
            state("show", style({
                opacity: 0.1,
                transform: "translateY(0)",
            })),
            state("hide", style({
                opacity: 0.4,
                transform: "translateY(17%)",
            })),
            transition("show => hide", animate("650ms ease-out")),
            transition("hide => show", animate("0ms ease-out")),
        ]),
    ],
    standalone: false,
})
export class StorageSectionComponent extends AbstractSection implements OnInit, OnDestroy {

    public chargeAnimationTrigger: boolean = false;
    public dischargeAnimationTrigger: boolean = false;
    public svgStyle: string;
    private socValue: number;
    private unitpipe: UnitvaluePipe;
    // animation variable to stop animation on destroy
    private startAnimation = null;
    private showChargeAnimation: boolean = false;
    private showDischargeAnimation: boolean = false;

    constructor(
        translate: TranslateService,
        protected override service: Service,
        unitpipe: UnitvaluePipe,
    ) {
        super("EDGE.INDEX.ENERGYMONITOR.STORAGE", "down", "#009846", translate, service, "Storage");
        THIS.UNITPIPE = unitpipe;
    }

    get stateNameCharge() {
        return THIS.SHOW_CHARGE_ANIMATION ? "show" : "hide";
    }

    get stateNameDischarge() {
        return THIS.SHOW_DISCHARGE_ANIMATION ? "show" : "hide";
    }

    ngOnInit() {
        THIS.ADJUST_FILL_REFBY_BROWSER();
    }

    ngOnDestroy() {
        clearInterval(THIS.START_ANIMATION);
    }

    toggleCharge() {
        THIS.START_ANIMATION = setInterval(() => {
            THIS.SHOW_CHARGE_ANIMATION = !THIS.SHOW_CHARGE_ANIMATION;
        }, THIS.ANIMATION_SPEED);
        THIS.CHARGE_ANIMATION_TRIGGER = true;
        THIS.DISCHARGE_ANIMATION_TRIGGER = false;
    }

    toggleDischarge() {
        setInterval(() => {
            THIS.SHOW_DISCHARGE_ANIMATION = !THIS.SHOW_DISCHARGE_ANIMATION;
        }, THIS.ANIMATION_SPEED);
        THIS.CHARGE_ANIMATION_TRIGGER = false;
        THIS.DISCHARGE_ANIMATION_TRIGGER = true;
    }

    public _updateCurrentData(sum: DEFAULT_TYPES.SUMMARY): void {

        THIS.SERVICE.GET_CURRENT_EDGE()
            .then(async edge => {
                EDGE.CURRENT_DATA.SUBSCRIBE(curr => {
                    const maxApparentPower = EDGE.IS_VERSION_AT_LEAST("2024.2.2")
                        ? CURR.CHANNEL["_sum/EssMaxDischargePower"]
                        : CURR.CHANNEL["_sum/EssMaxApparentPower"];
                    const minDischargePower = EDGE.IS_VERSION_AT_LEAST("2024.2.2")
                        ? CURR.CHANNEL["_sum/EssMinDischargePower"]
                        : CURR.CHANNEL["_sum/EssMaxApparentPower"];

                    SUM.STORAGE.POWER_RATIO = CURRENT_DATA.GET_ESS_POWER_RATIO(maxApparentPower, minDischargePower, SUM.STORAGE.EFFECTIVE_POWER);

                    if (SUM.STORAGE.EFFECTIVE_CHARGE_POWER != null) {
                        let arrowIndicate: number;
                        // only reacts to kW values (50 W => 0.1 kW rounded)
                        if (SUM.STORAGE.EFFECTIVE_CHARGE_POWER > 49) {
                            if (!THIS.CHARGE_ANIMATION_TRIGGER) {
                                THIS.TOGGLE_CHARGE();
                            }
                            arrowIndicate = UTILS.DIVIDE_SAFELY(SUM.STORAGE.EFFECTIVE_CHARGE_POWER, SUM.SYSTEM.TOTAL_POWER);
                        } else {
                            arrowIndicate = 0;
                        }

                        THIS.NAME = THIS.TRANSLATE.INSTANT("EDGE.INDEX.ENERGYMONITOR.STORAGE_CHARGE");
                        SUPER.UPDATE_SECTION_DATA(
                            SUM.STORAGE.EFFECTIVE_CHARGE_POWER,
                            SUM.STORAGE.POWER_RATIO,
                            arrowIndicate);
                    } else if (SUM.STORAGE.EFFECTIVE_DISCHARGE_POWER != null) {
                        let arrowIndicate: number;
                        if (SUM.STORAGE.EFFECTIVE_DISCHARGE_POWER > 49) {
                            if (!THIS.DISCHARGE_ANIMATION_TRIGGER) {
                                THIS.TOGGLE_DISCHARGE();
                            }
                            arrowIndicate = UTILS.MULTIPLY_SAFELY(
                                UTILS.DIVIDE_SAFELY(SUM.STORAGE.EFFECTIVE_DISCHARGE_POWER, SUM.SYSTEM.TOTAL_POWER), -1);
                        } else {
                            arrowIndicate = 0;
                        }
                        THIS.NAME = THIS.TRANSLATE.INSTANT("EDGE.INDEX.ENERGYMONITOR.STORAGE_DISCHARGE");
                        SUPER.UPDATE_SECTION_DATA(
                            SUM.STORAGE.EFFECTIVE_DISCHARGE_POWER,
                            SUM.STORAGE.POWER_RATIO,
                            arrowIndicate);
                    } else {
                        THIS.NAME = THIS.TRANSLATE.INSTANT("EDGE.INDEX.ENERGYMONITOR.STORAGE");
                        SUPER.UPDATE_SECTION_DATA(null, null, null);
                    }

                    THIS.SOC_VALUE = SUM.STORAGE.SOC;
                    if (THIS.SQUARE) {
                        THIS.SQUARE.IMAGE.IMAGE = "assets/img/" + THIS.GET_IMAGE_PATH();
                        THIS.SVG_STYLE = "storage-" + UTILS.GET_STORAGE_SOC_SEGMENT(THIS.SOC_VALUE);
                    }
                });
            });
    }

    protected getStartAngle(): number {
        return 136;
    }

    protected getEndAngle(): number {
        return 224;
    }

    protected getRatioType(): Ratio {
        return "Negative and Positive [-1,1]";
    }

    protected getSquarePosition(square: SvgSquare, innerRadius: number): SvgSquarePosition {
        const x = (SQUARE.LENGTH / 2) * (-1);
        const y = innerRadius - 5 - SQUARE.LENGTH;
        return new SvgSquarePosition(x, y);
    }

    protected getImagePath(): string {
        return "icon/STORAGE.SVG";
    }

    protected getValueText(value: number): string {
        if (value == null || NUMBER.IS_NA_N(value)) {
            return "";
        }
        return THIS.UNITPIPE.TRANSFORM(value, "kW");
    }

    protected initEnergyFlow(radius: number): EnergyFlow {
        return new EnergyFlow(radius, { x1: "50%", y1: "0%", x2: "50%", y2: "100%" });
    }

    // no adjustments needed
    protected setElementHeight() { }

    protected getSvgEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        const v = MATH.ABS(ratio);
        const r = radius;
        const p = {
            topLeft: { x: v * -1, y: v },
            bottomLeft: { x: v * -1, y: r },
            topRight: { x: v, y: v },
            bottomRight: { x: v, y: r },
            middleBottom: { x: 0, y: r - v },
            middleTop: { x: 0, y: 0 },
        };
        if (ratio > 0) {
            // towards bottom
            P.BOTTOM_LEFT.Y = P.BOTTOM_LEFT.Y - v;
            P.MIDDLE_BOTTOM.Y = P.MIDDLE_BOTTOM.Y + v;
            P.BOTTOM_RIGHT.Y = P.BOTTOM_RIGHT.Y - v;
            P.MIDDLE_TOP.Y = P.TOP_LEFT.Y + v;
        }
        return p;
    }

    protected getSvgAnimationEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        const v = MATH.ABS(ratio);
        const r = radius;
        const animationWidth = r - v;
        let p = {
            topLeft: { x: v * -1, y: v },
            bottomLeft: { x: v * -1, y: r },
            topRight: { x: v, y: v },
            bottomRight: { x: v, y: r },
            middleBottom: { x: 0, y: r - v },
            middleTop: { x: 0, y: 0 },
        };
        if (ratio < 0) {
            // towards top
            P.MIDDLE_TOP.Y = P.MIDDLE_BOTTOM.Y + animationWidth * 0.2;
            P.TOP_RIGHT.Y = P.BOTTOM_RIGHT.Y + animationWidth * 0.2;
            P.TOP_LEFT.Y = P.BOTTOM_LEFT.Y + animationWidth * 0.2;
        } else if (ratio > 0) {
            // towards bottom
            P.BOTTOM_LEFT.Y = P.TOP_LEFT.Y - animationWidth * 0.2;
            P.MIDDLE_BOTTOM.Y = P.MIDDLE_TOP.Y - animationWidth * 0.2 + 2 * v;
            P.BOTTOM_RIGHT.Y = P.TOP_RIGHT.Y - animationWidth * 0.2;
            P.MIDDLE_TOP.Y = P.MIDDLE_BOTTOM.Y + animationWidth * 0.2;
        } else {
            p = null;
        }
        return p;
    }

}
