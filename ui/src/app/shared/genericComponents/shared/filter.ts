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

  export const HIDE_NEGATIVE_VALUES: Filter = (value: number): boolean => value < 0
}