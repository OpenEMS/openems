import { GridMode } from "../../shared";

export type Filter = (value: number | string | null) => boolean;

export namespace Filter {

  /**
   * Dummy/default filter. Always returns true.
   *
   * @param value the value
   * @returns always true
   */
  export const NO_FILTER: Filter = (value): boolean => { return true; };

  /**
   * Filter passes only if GridMode is OFF_GRID.
   *
   * @param value the GridMode integer value
   * @returns true if GridMode is OFF_GRID
   */
  export const GRID_MODE_IS_OFF_GRID: Filter = (value): boolean => value === GridMode.OFF_GRID;

  /**
   * Filter to check if the value is not null or undefined.
   *
   * @param value the value to check
   * @returns true if the value is neither null nor undefined
   */
  export const NOT_NULL_OR_UNDEFINED: Filter = (value): boolean => {
    return value !== null && value !== undefined;
  };

  export const HIDE_NEGATIVE_VALUES: Filter = (value: number): boolean => value < 0;
}
