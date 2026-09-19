import { RGBColor } from "../../type/defaulttypes";

export namespace ColorUtils {

    /**
   * Converts a rgb-string into a rgba-string
   *
   * @param color the color
   * @param opacity the opacity
   * @returns a string in rgba format
   */
    export function rgbStringToRgba(color: string | null, opacity: number | null): string {
        return RGBColor.fromString(color).toRgba(opacity);
    }

    /**
   * Changes opacity of a passed rgba string
   *
   * @param color the color
   * @param opacity the opacity
   * @returns a string in rgba format
   */
    export function changeOpacityFromRGBA(color: string | null, opacity: number | null): string | null {
        return RGBColor.fromString(color).toRgba(opacity);
    }
}
