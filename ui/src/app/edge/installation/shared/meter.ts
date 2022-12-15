export enum Meter {
    SOCOMEC,
    KDK
}

export namespace Meter {

    /**
     * Returns the factory id of the meter type selected.
     * 
     * @param meter The Meter.
     * @returns the factory id of the meter.
     */
    export function toFactoryId(meter: Meter): string {
        switch (meter) {
            case Meter.SOCOMEC:
                return 'Meter.Socomec.Threephase';
            case Meter.KDK:
                return 'Meter.KDK.2PUCT';
        }
    }

    /**
     * Returns the label of the meter selected as string.
     * 
     * @param meter The meter type.
     * @returns the meter label as string.
     */
    export function toLabelString(meter: Meter): string {
        switch (meter) {
            case Meter.SOCOMEC:
                return 'Socomec';
            case Meter.KDK:
                return 'KDK';
        }
    }
}