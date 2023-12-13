export namespace ColorUtils {

  /**
   * Converts a rgb-string into a rgba-string
   * 
   * @param color the color
   * @param opacity the opacity
   * @returns a string in rgba format
   */
  export function rgbStringToRGBA(color: string, opacity: number): string {

    if (!color) {
      return null;
    }

    return 'rgba(' + color.split('(').pop().split(')')[0] + ',' + (opacity ?? 0) + ')';
  }

  /**
   * Changes opacity of a passed rgba string
   * 
   * @param color the color
   * @param opacity the opacity
   * @returns a string in rgba format
   */
  export function changeOpacityFromRGBA(color: string, opacity: number): string {

    if (!color) {
      return null;
    }

    var rgba = color.split('(').pop().split(')')[0];
    var rgb = rgba.split(',').slice(0, -1).join(',');

    return 'rgba(' + rgb + ',' + (opacity ?? 0) + ')';
  }
}
