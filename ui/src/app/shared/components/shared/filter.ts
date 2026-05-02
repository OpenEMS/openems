import { Edge, GridMode } from "../../shared";
import { Role } from "../../type/role";

export type Filter = (value: number | string | null) => boolean;

export namespace Filter {

    /**
     * Dummy/default filter. Always returns true.
     *
     * @param value the value
     * @returns always true
     */
    export const NO_FILTER: Filter = (value): boolean => true;

    /**
     * Filter passes only if GridMode is OFF_GRID.
     *
     * @param value the GridMode integer value
     * @returns true if GridMode is OFF_GRID
     */
    export const GRID_MODE_IS_OFF_GRID: Filter = (value): boolean => value === GridMode.OFF_GRID;

    /**
     * Filter passes only if GridMode is OFF_GRID or GENERATOR.
    *
    * @param value the GridMode integer value
    * @returns true if GridMode is OFF_GRID or GENERATOR
    */
    export const GRID_MODE_IS_OFF_GRID_OR_GENERATOR: Filter = (value): boolean => value === GridMode.OFF_GRID || value === GridMode.GENERATOR;

    /**
     * Filter to check if the value is not null or undefined.
     *
     * @param value the value to check
     * @returns true if the value is neither null nor undefined
     */
    export const NOT_NULL_OR_UNDEFINED: Filter = (value): boolean => {
        return value !== null && value !== undefined;
    };

    export const HIDE_NEGATIVE_VALUES: Filter = (value): boolean => {
        if (typeof value !== "number") {
            return true;
        }
        return value < 0;
    };

    /**
     * Filter passes only if user is atlest role admin
     *
     * @param value the DelayChargeState integer value
     * @returns true if user is atleast admin
     */
    export const IS_AT_LEAST_ROLE = (role: Role, edge: Edge): Filter => {
        return (): boolean => edge.roleIsAtLeast(role);
    };
}
