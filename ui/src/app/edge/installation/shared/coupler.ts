import { TranslateService } from "@ngx-translate/core";

export enum Coupler {
    WEIDMUELLER,
    WAGO
}

export namespace Coupler {
    /**
     * Returns the Factory Id of the coupler.
     * 
     * @param coupler the coupler selected.
     * @returns the Factory Id of the corresponding Coupler.
     */
    export function toFactoryId(coupler: Coupler): string {
        switch (coupler) {
            case Coupler.WEIDMUELLER:
                return 'IO.Weidmueller.UR20';
            case Coupler.WAGO:
                return 'IO.WAGO';
        }
    }

    /**
     * returns the URL of the image of the Coupler.
     * 
     * @param coupler the coupler selected.
     * @returns the URL.
     */
    export function toImageUrl(coupler: Coupler): string {
        switch (coupler) {
            case Coupler.WEIDMUELLER:
                return 'assets/img/Netztrennstelle_Weidmüller.PNG';
            case Coupler.WAGO:
                return 'assets/img/Netztrennstelle_WAGO.PNG';
        }
    }

    /**
     * Returns the Label of the corresponding Coupler.
     * 
     * @param coupler the coupler selected.
     * @param translate the Translate Service
     * @returns the Label.
     */
    export function toLabelString(coupler: Coupler, translate: TranslateService): string {
        switch (coupler) {
            case Coupler.WEIDMUELLER:
                return translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.LABEL.WEIDMUELLER_BRIDGE');
            case Coupler.WAGO:
                return translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.LABEL.WAGO_BRIDGE');
        }
    }

    /**
     * Returns the default alias of the coupler.
     * 
     * @param coupler the coupler selected.
     * @param translate the Translate Service
     * @returns the Alias.
     */
    export function toAliasString(coupler: Coupler, translate: TranslateService): string {
        switch (coupler) {
            case Coupler.WEIDMUELLER:
                return translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.WEIDMUELLER_BRIDGE');
            case Coupler.WAGO:
                return translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.WAGO_BRIDGE');
        }
    }
}