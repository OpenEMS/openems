import { TranslateService } from "@ngx-translate/core";

export namespace Meter {

    export enum GridMeter {
        SOCOMEC,
        KDK
    }

    export enum GridMeterCategory {
        SMART_METER = 'SMART_METER',
        COMMERCIAL_METER = 'COMMERCIAL_METER',
    }

    /**
     * Returns the factory id of the meter type selected.
     *
     * @param meter The Meter.
     * @returns the factory id of the meter.
     */
    export function toFactoryId(meter: GridMeter): string {
        switch (meter) {
            case GridMeter.SOCOMEC:
                return 'Meter.Socomec.Threephase';
            case GridMeter.KDK:
                return 'Meter.KDK.2PUCT';
        }
    }

    /**
     * Returns the label of the meter selected as string.
     *
     * @param meterCategory The {@link GridMeterCategory}.
     * @returns the meter label as string.
     */
    export function toGridMeterCategoryLabelString(meterCategory: GridMeterCategory, translate: TranslateService): string {
        switch (meterCategory) {
            case GridMeterCategory.SMART_METER:
                return translate.instant('INSTALLATION.CONFIGURATION_ENERGY_FLOW_METER.METER.WITH_CT');
            case GridMeterCategory.COMMERCIAL_METER:
                return translate.instant('INSTALLATION.CONFIGURATION_ENERGY_FLOW_METER.METER.WITHOUT_CT');
        }
    }
}
