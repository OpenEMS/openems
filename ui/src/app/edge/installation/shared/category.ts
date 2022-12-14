import { TranslateService } from "@ngx-translate/core";
import { environment } from "src/environments";

export enum Category {
    LINE_SIDE_METER_FUSE_COMMERCIAL,
    LINE_SIDE_METER_FUSE_HOME,
    FEED_IN_MANAGEMENT,
    ADDITIONAL_AC_PRODUCERS,
    EMS_DETAILS,
    EMERGENCY_RESERVE,
    DC_PV_INSTALLATION,
    GENERAL,
    INSTALLER,
    CUSTOMER,
    BATTERY_LOCATION,
    BATTERY,
    INVERTER,
    PRODUCER,
    EMS_BOX_LABEL_HOME,
    EMS_BOX_LABEL_HECKERT,
    PEAK_SHAVING,
    APPS,
    BALANCING,
    PEAK_SHAVING_SYMMETRIC,
    PEAK_SHAVING_ASYMMETRIC,
    PEAK_SHAVING_SYMMETRIC_HEADER,
    PEAK_SHAVING_ASYMMETRIC_HEADER,
}

export namespace Category {
    export function toTranslatedString(category: Category, translate: TranslateService): string {
        switch (category) {
            case Category.LINE_SIDE_METER_FUSE_COMMERCIAL:
                return translate.instant('INSTALLATION.CONFIGURATION_LINE_SIDE_METER_FUSE.TITLE_COMMERCIAL');
            case Category.LINE_SIDE_METER_FUSE_HOME:
                return translate.instant('INSTALLATION.CONFIGURATION_LINE_SIDE_METER_FUSE.TITLE_HOME');
            case Category.FEED_IN_MANAGEMENT:
                return translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.TITLE');
            case Category.ADDITIONAL_AC_PRODUCERS:
                return translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.ADDITIONAL_GENERATOR');
            case Category.EMS_DETAILS:
                return translate.instant('Edge.title', { edgeShortName: environment.edgeShortName });
            case Category.EMERGENCY_RESERVE:
                return translate.instant('INSTALLATION.CONFIGURATION_EMERGENCY_RESERVE.TITLE');
            case Category.DC_PV_INSTALLATION:
                return translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.TITLE_DC');
            case Category.GENERAL:
                return translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.GENERAL');
            case Category.INSTALLER:
                return translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.INSTALLER');
            case Category.CUSTOMER:
                return translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.CUSTOMER');
            case Category.BATTERY_LOCATION:
                return translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.BATTERY_LOCATION');
            case Category.BATTERY:
                return translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.BATTERY');
            case Category.INVERTER:
                return translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.INVERTER');
            case Category.PRODUCER:
                return translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.PRODUCER');
            case Category.EMS_BOX_LABEL_HOME:
                return translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.EMS_BOX_HOME', { edgeShortName: environment.edgeShortName });
            case Category.EMS_BOX_LABEL_HECKERT:
                return translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.EMS_BOX_HECKERT');
            case Category.PEAK_SHAVING:
                return translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.PEAK_SHAVING');
            case Category.APPS:
                return translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.APPS');
            case Category.BALANCING:
                return translate.instant('INSTALLATION.CONFIGURATION_FEATURES_STORAGE_SYSTEM.BALANCING');
            case Category.PEAK_SHAVING_SYMMETRIC:
                return translate.instant('INSTALLATION.CONFIGURATION_FEATURES_STORAGE_SYSTEM.PEAK_SHAVING_SYMMETRIC');
            case Category.PEAK_SHAVING_ASYMMETRIC:
                return translate.instant('INSTALLATION.CONFIGURATION_FEATURES_STORAGE_SYSTEM.PEAK_SHAVING_ASYMMETRIC');
            case Category.PEAK_SHAVING_SYMMETRIC_HEADER:
                return translate.instant('INSTALLATION.CONFIGURATION_FEATURES_STORAGE_SYSTEM.PEAK_SHAVING_SYMMETRIC_HEADER');
            case Category.PEAK_SHAVING_ASYMMETRIC_HEADER:
                return translate.instant('INSTALLATION.CONFIGURATION_FEATURES_STORAGE_SYSTEM.PEAK_SHAVING_ASYMMETRIC_HEADER');
        }
    }
}