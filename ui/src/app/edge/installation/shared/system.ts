import { TranslateService } from "@ngx-translate/core";
import { AbstractIbn } from "../installation-systems/abstract-ibn";
import { Commercial30AnschlussIbn } from "../installation-systems/commercial/commercial-30/commercial30-anschluss";
import { Commercial30NetztrennIbn } from "../installation-systems/commercial/commercial-30/commercial30-netztrenn";
import { Commercial50EigenverbrauchsOptimierung } from "../installation-systems/commercial/commercial-50/commercial50-eigenverbrauchsoptimierung";
import { Commercial50Lastspitzenkappung } from "../installation-systems/commercial/commercial-50/commercial50-lastspitzenkappung";
import { GeneralIbn } from "../installation-systems/general-ibn";
import { Home10FeneconIbn } from "../installation-systems/home/home-10";
import { Home10HeckertIbn } from "../installation-systems/home/home-10-heckert";
import { Home20FeneconIbn } from "../installation-systems/home/home-20";
import { Home30FeneconIbn } from "../installation-systems/home/home-30";

export enum SystemType {
    FENECON_HOME_10,
    FENECON_HOME_20,
    FENECON_HOME_30,
    HECKERT_HOME_10,
    COMMERCIAL_30,
    COMMERCIAL_50,
}

export enum SystemId {
    GENERAL,
    FENECON_HOME_10,
    FENECON_HOME_20,
    FENECON_HOME_30,
    HECKERT_HOME_10,
    COMMERCIAL_30_ANSCHLUSS,
    COMMERCIAL_30_NETZTRENNSTELLE,
    COMMERCIAL_50_BALANCING,
    COMMERCIAL_50_PEAK_SHAVING,
}

export namespace System {
    /**
     * Returns the Label to be displayed for the System type.
     * 
     * Typically used while displaying labels for selection.
     * 
     * @param systemType Type of the System.
     * @returns Label for the System type.
     */
    export function getSystemTypeLabel(systemType: SystemType): string {
        switch (systemType) {
            case SystemType.FENECON_HOME_10:
                return 'FENECON Home 10';
            case SystemType.HECKERT_HOME_10:
                return 'heckert-home';
            case SystemType.COMMERCIAL_30:
                return 'FENECON Commercial 30';
            case SystemType.COMMERCIAL_50:
                return 'FENECON Commercial 50';
            case SystemType.FENECON_HOME_20:
                return 'FENECON Home 20';
            case SystemType.FENECON_HOME_30:
                return 'FENECON Home 30';
        }
    }

    /**
     * Returns the IBN Object from the System type.
     * 
     * @param systemType Type of the System.
     * @param translate the translate service
     * @returns the System IBN Object.
     */
    export function getSystemObjectFromSystemType(systemType: SystemType, translate: TranslateService): AbstractIbn {

        switch (systemType) {
            case SystemType.FENECON_HOME_10:
                return new Home10FeneconIbn(translate);
            case SystemType.HECKERT_HOME_10:
                return new Home10HeckertIbn(translate);
            case SystemType.COMMERCIAL_30:
                return new Commercial30AnschlussIbn(translate);
            case SystemType.COMMERCIAL_50:
                return new Commercial50EigenverbrauchsOptimierung(translate);
            case SystemType.FENECON_HOME_20:
                return new Home20FeneconIbn(translate);
            case SystemType.FENECON_HOME_30:
                return new Home30FeneconIbn(translate);
        }
    }

    /**
     * Returns the String to show for the System selected.
     * 
     * @param systemType Type of the System.
     * @returns the string 
     */
    export function getSystemTypeStringFromSystemType(systemType: SystemType) {
        switch (systemType) {
            case SystemType.FENECON_HOME_10:
                return 'Fenecon-Home-10';
            case SystemType.HECKERT_HOME_10:
                return 'Symphon-E';
            case SystemType.COMMERCIAL_30:
                return 'Fenecon-Commercial-30';
            case SystemType.COMMERCIAL_50:
                return 'Fenecon-Commercial-50';
            case SystemType.FENECON_HOME_20:
                return 'Fenecon-Home-20';
            case SystemType.FENECON_HOME_30:
                return 'Fenecon-Home-30';
        }
    }

    /**
     * Returns the IBN Object based on the System id. 
     * 
     * @param systemId Id of the System.
     * @param translate the translate service.
     * @returns the IBN object.
     */
    export function getSystemObjectFromSystemId(systemId: SystemId, translate: TranslateService): AbstractIbn {

        switch (systemId) {
            case SystemId.GENERAL:
                return new GeneralIbn(translate);
            case SystemId.FENECON_HOME_10:
                return new Home10FeneconIbn(translate);
            case SystemId.HECKERT_HOME_10:
                return new Home10HeckertIbn(translate);
            case SystemId.COMMERCIAL_30_ANSCHLUSS:
                return new Commercial30AnschlussIbn(translate);
            case SystemId.COMMERCIAL_30_NETZTRENNSTELLE:
                return new Commercial30NetztrennIbn(translate);
            case SystemId.COMMERCIAL_50_BALANCING:
                return new Commercial50EigenverbrauchsOptimierung(translate);
            case SystemId.COMMERCIAL_50_PEAK_SHAVING:
                return new Commercial50Lastspitzenkappung(translate);
            case SystemId.FENECON_HOME_20:
                return new Home20FeneconIbn(translate);
            case SystemId.FENECON_HOME_30:
                return new Home30FeneconIbn(translate);
        }
    }

    /**
     * Returns the URL for the System type.
     * 
     * @param systemType Type of the System.
     * @returns the url.
     */
    export function getSystemTypeLink(systemType: SystemType): string {
        switch (systemType) {
            case SystemType.FENECON_HOME_10:
                return 'https://fenecon.de/download/montage-und-serviceanleitung-feneconhome/?wpdmdl=17765&refresh=62a048d9acf401654671577';
            case SystemType.HECKERT_HOME_10:
                return 'https://www.heckertsolar.com/wp-content/uploads/2022/06/Montage_und-Serviceanleitung-Symphon-E-1.pdf';
            case SystemType.COMMERCIAL_30:
                return 'https://fenecon.de/downloadcenter-commercial-30/';
            case SystemType.COMMERCIAL_50:
                return 'https://fenecon.de/downloadcenter-commercial-50/';
            case SystemType.FENECON_HOME_20:
                return '#';
            case SystemType.FENECON_HOME_30:
                return '#';
        }
    }
}