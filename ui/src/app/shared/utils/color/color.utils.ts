import { RGBColor } from "../../type/defaulttypes";

export namespace ColorUtils {
    /**
     * Converts a rgb-string into a rgba-string
     *
     * @param color The color
     * @param opacity The opacity
     * @returns A string in rgba format
     */
    export function rgbStringToRgba(color: string, opacity: number): string {
        return RGBColor.fromString(color).toRgba(opacity);
    }

    /**
     * Changes opacity of a passed rgba string
     *
     * @param color The color
     * @param opacity The opacity
     * @returns A string in rgba format
     */
    export function changeOpacityFromRGBA(color: string, opacity: number): string {
        return RGBColor.fromString(color).toRgba(opacity);
    }
}
