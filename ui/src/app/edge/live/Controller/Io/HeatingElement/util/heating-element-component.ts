import { TranslateService } from "@ngx-translate/core";
import { validate as isValidVersion } from "compare-versions";
import type { Edge } from "src/app/shared/components/edge/edge";
import { EdgeComponent } from "src/app/shared/components/edge/edge-component";
import type { EdgeConfig } from "src/app/shared/components/edge/edgeconfig";
import { ButtonLabel } from "src/app/shared/components/modal/modal-button/modal-button";
import { Role } from "src/app/shared/type/role";
import { TEnumKeys } from "src/app/shared/type/utility";
import { YAxisType } from "src/app/shared/utils/utils";

export enum Level {
    LEVEL_0 = 0,
    LEVEL_1 = 1,
    LEVEL_2 = 2,
    LEVEL_3 = 3,
    LEVEL_4 = 4,
    LEVEL_5 = 5,
    LEVEL_6 = 6,
}

export enum HeatingElementMode {
    MANUAL_ON = "MANUAL_ON",
    AUTOMATIC = "AUTOMATIC",
    MANUAL_OFF = "MANUAL_OFF",
    FAST_HEAT = "FAST_HEAT",
    /** Legacy received value; it is not offered as a selectable mode. */
    MANUAL = "MANUAL",
}

export type HeatingElementLevelValue = Exclude<TEnumKeys<typeof Level>, "LEVEL_0">;

/** Centralized configuration and firmware-compatibility helper for `Controller.Io.HeatingElement`. */
export class HeatingElementComponent extends EdgeComponent {
    /**
     * Provisional minimum Edge version for extended HeatingElement features (FAST_HEAT and TWO heating elements /
     * Levels 0-6).
     */
    public static readonly MIN_EDGE_VERSION = "2026.11.1";

    private static readonly MODE_UPDATE_NAME = "mode";
    private static readonly DEFAULT_LEVEL_UPDATE_NAME = "defaultLevel";
    private static readonly ONE_ACTIVE_LEVELS: readonly Level[] = [Level.LEVEL_1, Level.LEVEL_2, Level.LEVEL_3];
    private static readonly TWO_ACTIVE_LEVELS: readonly Level[] = [
        Level.LEVEL_1,
        Level.LEVEL_2,
        Level.LEVEL_3,
        Level.LEVEL_4,
        Level.LEVEL_5,
        Level.LEVEL_6,
    ];

    private readonly edge: Edge | null;

    constructor(component: EdgeConfig.Component | Partial<EdgeComponent> | null | undefined, edge?: Edge | null) {
        super(
            component?.id ?? "",
            component?.alias ?? "",
            component?.isEnabled ?? true,
            component?.showProperties ?? true,
            component?.factoryId ?? "",
            component?.properties ?? {},
            component?.channels ?? {},
        );
        this.edge = edge ?? null;
    }

    /** Checks if the given Edge version is valid and meets {@link MIN_EDGE_VERSION}. */
    public static isSupportedEdgeVersion(edge: Edge | null | undefined): boolean {
        if (edge == null || typeof edge.version !== "string" || !isValidVersion(edge.version)) {
            return false;
        }
        return edge.isVersionAtLeast(HeatingElementComponent.MIN_EDGE_VERSION);
    }

    /** Converts a raw `_PropertyMode` channel value to its translated display label. */
    public static createModeLabelConverter(translate: TranslateService): (raw: string | null | undefined) => string {
        return (raw: string | null | undefined): string => {
            // Keep literal keys in translate.instant() so i18n Ally can detect these translations.
            switch (raw) {
                case HeatingElementMode.MANUAL_ON:
                    return translate.instant("GENERAL.ON");
                case HeatingElementMode.FAST_HEAT:
                    return translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.FAST_HEAT");
                case HeatingElementMode.AUTOMATIC:
                    return translate.instant("GENERAL.AUTOMATIC");
                case HeatingElementMode.MANUAL_OFF:
                    return translate.instant("GENERAL.OFF");
                case HeatingElementMode.MANUAL:
                    return translate.instant("GENERAL.MANUALLY");
                default:
                    return "-";
            }
        };
    }

    private static getLevelDefinitions(translate: TranslateService) {
        return [
            { level: Level.LEVEL_1, label: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.LEVEL_ONE") },
            { level: Level.LEVEL_2, label: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.LEVEL_TWO") },
            { level: Level.LEVEL_3, label: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.LEVEL_THREE") },
            { level: Level.LEVEL_4, label: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.LEVEL_FOUR") },
            { level: Level.LEVEL_5, label: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.LEVEL_FIVE") },
            { level: Level.LEVEL_6, label: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.LEVEL_SIX") },
        ];
    }

    /** Returns true when the user is Admin and the connected Edge firmware supports the new HeatingElement features. */
    public canUseTwoHeatingElementsAndFastHeat(): boolean {
        return (
            HeatingElementComponent.isSupportedEdgeVersion(this.edge) && this.edge?.roleIsAtLeast(Role.ADMIN) === true
        );
    }

    /**
     * Returns true when the component is configured with two heating elements (`numberOfHeatingElements === "TWO"`).
     * Missing or unrecognized property values default to ONE (false).
     */
    public isTwoHeatingElements(): boolean {
        return this.hasPropertyValue("numberOfHeatingElements", "TWO");
    }

    /**
     * Returns true when the component is configured with two heating elements (`TWO`) AND the connected Edge version
     * supports the extended 6-level functionality.
     */
    public supportsTwoHeatingElements(): boolean {
        return this.canUseTwoHeatingElementsAndFastHeat() && this.isTwoHeatingElements();
    }

    /**
     * Returns the active non-zero heating levels (`[1, 2, 3]` for ONE or unsupported Edge, `[1, 2, 3, 4, 5, 6]` for TWO
     * on a supported Edge).
     */
    public getActiveLevels(): number[] {
        return [
            ...(this.supportsTwoHeatingElements()
                ? HeatingElementComponent.TWO_ACTIVE_LEVELS
                : HeatingElementComponent.ONE_ACTIVE_LEVELS),
        ];
    }

    /** Returns the supported selectable level enum strings (`LEVEL_1`..`LEVEL_3` or `LEVEL_1`..`LEVEL_6`). */
    public getSupportedLevelValues(): HeatingElementLevelValue[] {
        return this.getActiveLevels().map((level) => `LEVEL_${level}` as HeatingElementLevelValue);
    }

    /** Returns whether the given `defaultLevel` value is supported for submission. */
    public isSupportedLevelValue(levelValue: unknown): levelValue is HeatingElementLevelValue {
        return typeof levelValue === "string" && (this.getSupportedLevelValues() as string[]).includes(levelValue);
    }

    /** Returns the translated level select options for the supported levels. */
    public getLevelOptions(translate: TranslateService): { name: string; value: HeatingElementLevelValue }[] {
        const levels = HeatingElementComponent.getLevelDefinitions(translate);
        const activeLevels = this.getActiveLevels();
        return levels
            .filter(({ level }) => activeLevels.includes(level))
            .map(({ level, label }) => ({
                value: `LEVEL_${level}` as HeatingElementLevelValue,
                name: label,
            }));
    }

    /** Returns the history duration channel ID for a given level (e.g. `Level1CumulatedTime`). */
    public getLevelDurationChannelId(level: number): string {
        return `Level${level}CumulatedTime`;
    }

    /** Returns the Y-axis type for the history chart based on chart type (`line` vs `bar`) and heating element count. */
    public getChartYAxisType(chartType: "line" | "bar"): YAxisType {
        if (chartType !== "line") {
            return YAxisType.TIME;
        }
        return this.supportsTwoHeatingElements() ? YAxisType.HEATING_ELEMENT_TWO : YAxisType.HEATING_ELEMENT;
    }

    /** Returns the available operating modes for this Edge version. */
    public getAvailableModes(): HeatingElementMode[] {
        if (this.canUseTwoHeatingElementsAndFastHeat()) {
            return [
                HeatingElementMode.MANUAL_ON,
                HeatingElementMode.FAST_HEAT,
                HeatingElementMode.AUTOMATIC,
                HeatingElementMode.MANUAL_OFF,
            ];
        }
        return [HeatingElementMode.MANUAL_ON, HeatingElementMode.AUTOMATIC, HeatingElementMode.MANUAL_OFF];
    }

    /** Returns whether the given mode value is supported for submission on this Edge version. */
    public isSupportedMode(mode: unknown): mode is HeatingElementMode {
        return typeof mode === "string" && (this.getAvailableModes() as string[]).includes(mode);
    }

    /** Returns the translated mode selection buttons for modal and formly views. */
    public getModeButtons(translate: TranslateService): ButtonLabel[] {
        const size = "medium" as const;
        const buttons: ButtonLabel[] = [
            {
                name: translate.instant("GENERAL.ON"),
                value: HeatingElementMode.MANUAL_ON,
                icon: { color: "success", name: "play-outline", size },
            },
        ];

        if (this.canUseTwoHeatingElementsAndFastHeat()) {
            buttons.push({
                name: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.FAST_HEAT"),
                value: HeatingElementMode.FAST_HEAT,
                icon: { color: "warning", name: "flame-outline", size },
            });
        }

        buttons.push(
            {
                name: translate.instant("GENERAL.AUTOMATIC"),
                value: HeatingElementMode.AUTOMATIC,
                icon: { color: "primary", name: "sunny", size },
            },
            {
                name: translate.instant("GENERAL.OFF"),
                value: HeatingElementMode.MANUAL_OFF,
                icon: { color: "danger", name: "power-outline", size },
            },
        );

        return buttons;
    }

    /** Filters out unsupported `mode` or `defaultLevel` entries from an update payload array. */
    public filterSupportedUpdates(updates: { name: string; value: unknown }[]): { name: string; value: unknown }[] {
        return updates.filter((entry) => {
            if (entry.name === HeatingElementComponent.MODE_UPDATE_NAME) {
                return this.isSupportedMode(entry.value);
            }
            if (entry.name === HeatingElementComponent.DEFAULT_LEVEL_UPDATE_NAME) {
                return this.isSupportedLevelValue(entry.value);
            }
            return true;
        });
    }
}
