import { TranslateService } from "@ngx-translate/core";
import { ChartDataset } from "CHART.JS";
import { Converter } from "src/app/shared/components/shared/converter";
import { TimeOfUseTariffUtils } from "src/app/shared/utils/utils";
import { environment } from "src/environments";

export namespace ControllerEvseSingleShared {

    /**
     * Converts a string mode to a presentable label
     *
     * @param raw the raw value
     * @returns the value for chosen mode
     */
    export const CONVERT_TO_MODE_LABEL = (translate: TranslateService) => {
        return (raw: string | null): string => {
            return Converter.IF_STRING(raw, value => {
                switch (value) {
                    case MODE.ZERO:
                        return TRANSLATE.INSTANT("EVSE_SINGLE.HOME.MODE.ZERO");
                    case MODE.MINIMUM:
                        return TRANSLATE.INSTANT("EVSE_SINGLE.HOME.MODE.MINIMUM");
                    case MODE.SURPLUS:
                        return TRANSLATE.INSTANT("EVSE_SINGLE.HOME.MODE.SURPLUS");
                    case MODE.FORCE:
                        return TRANSLATE.INSTANT("EVSE_SINGLE.HOME.MODE.FORCE");
                    default:
                        return Converter.HIDE_VALUE(value);
                }
            });
        };
    };

    export function getImgUrlByFactoryId(factoryId: string): string | null {
        switch (factoryId) {
            case "EVSE.CHARGE_POINT.KEBA.UDP":
                return ENVIRONMENT.IMAGES.EVSE.KEBA_P30;
            case "EVSE.CHARGE_POINT.KEBA.MODBUS":
                return ENVIRONMENT.IMAGES.EVSE.KEBA_P40;
            case "EVSE.CHARGE_POINT.HARDY_BARTH":
                return ENVIRONMENT.IMAGES.EVSE.HARDY_BARTH;
            default:
                return null;
        }
    }

    export type ScheduleChartData = {
        datasets: ChartDataset[],
        colors: any[],
        labels: Date[]
    };

    export enum Mode {
        ZERO = "ZERO",
        MINIMUM = "MINIMUM",
        SURPLUS = "SURPLUS",
        FORCE = "FORCE",
    }

    /**
     * Gets the schedule chart data containing datasets, colors and labels.
     *
     * @param size The length of the dataset
     * @param prices The quarterly price array
     * @param modes The modes array
     * @param timestamps The timestamps array
     * @param translate The Translate service
     * @returns The ScheduleChartData.
     */
    export function getScheduleChartData(size: number, prices: number[], modes: number[], timestamps: string[],
        translate: TranslateService): CONTROLLER_EVSE_SINGLE_SHARED.SCHEDULE_CHART_DATA {

        const datasets: ChartDataset[] = [];
        const colors: any[] = [];
        const labels: Date[] = [];

        // Initializing States.
        const barZero = Array(size).fill(null);
        const barMinimum = Array(size).fill(null);
        const barSurplus = Array(size).fill(null);
        const barForce = Array(size).fill(null);

        for (let index = 0; index < size; index++) {
            const quarterlyPrice = TIME_OF_USE_TARIFF_UTILS.FORMAT_PRICE(prices[index]);
            const mode = modes[index];
            LABELS.PUSH(new Date(timestamps[index]));

            const modeStates = OBJECT.KEYS(CONTROLLER_EVSE_SINGLE_SHARED.MODE);

            if (mode !== null) {
                switch (mode) {
                    case MODE_STATES.INDEX_OF(CONTROLLER_EVSE_SINGLE_SHARED.MODE.ZERO):
                        barZero[index] = quarterlyPrice;
                        break;
                    case MODE_STATES.INDEX_OF(CONTROLLER_EVSE_SINGLE_SHARED.MODE.MINIMUM):
                        barMinimum[index] = quarterlyPrice;
                        break;
                    case MODE_STATES.INDEX_OF(CONTROLLER_EVSE_SINGLE_SHARED.MODE.SURPLUS):
                        barSurplus[index] = quarterlyPrice;
                        break;
                    case MODE_STATES.INDEX_OF(CONTROLLER_EVSE_SINGLE_SHARED.MODE.FORCE):
                        barForce[index] = quarterlyPrice;
                        break;
                }
            }
        }

        // Set datasets
        DATASETS.PUSH({
            type: "bar",
            label: "No Charge",
            data: barZero,
            order: 1,
        });
        COLORS.PUSH({
            backgroundColor: "rgba(0,0,0,0.8)",
            borderColor: "rgba(0,0,0,0.9)",
        });

        DATASETS.PUSH({
            type: "bar",
            label: "Minimum",
            data: barMinimum,
            order: 1,
        });
        COLORS.PUSH({
            backgroundColor: "rgba(25, 19, 82, 0.8)",
            borderColor: "rgba(51,102,0,1)",
        });

        DATASETS.PUSH({
            type: "bar",
            label: "Surplus PV",
            data: barSurplus,
            order: 1,
        });
        COLORS.PUSH({
            backgroundColor: "rgba(51,102,0,0.8)",
            borderColor: "rgba(51,102,0,1)",
        });

        DATASETS.PUSH({
            type: "bar",
            label: "Force Charge",
            data: barForce,
            order: 1,
        });
        COLORS.PUSH({
            backgroundColor: "rgba(0, 204, 204,0.5)",
            borderColor: "rgba(0, 204, 204,0.7)",
        });

        const scheduleChartData: CONTROLLER_EVSE_SINGLE_SHARED.SCHEDULE_CHART_DATA = {
            colors: colors,
            datasets: datasets,
            labels: labels,
        };

        return scheduleChartData;
    }
}
