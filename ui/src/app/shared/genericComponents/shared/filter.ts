import { GridMode } from "../../shared";

export namespace Filter {

  /**
   * Filter passes only if GridMode is OFF_GRID.
   * 
   * @param value the GridMode integer value
   * @returns true if GridMode is OFF_GRID
   */
  export const GRID_MODE_IS_OFF_GRID =
    (value: number): boolean => value === GridMode.OFF_GRID;
}