import { Flag } from "./flag";
import { FlagType } from "./flagType";

export namespace Flags {

    export const SHOW_AFTER_KEY_REDEEM: FlagType<Flag> = { name: 'showAfterKeyRedeem' };

    /**
     * Gets a flag by its type from an array of flags.
     *
     * @param flags the flags to search for the specific type
     * @param type the FlagType of the flag
     * @returns the flag or undefined if not found
     *
     * @see Flag
     */
    export function getByType<Type extends Flag>(flags: Flag[], type: FlagType<Type>): Type | undefined {
        if (!flags) {
            return undefined;
        }

        return flags.find(f => f.name === type.name) as Type;
    }

}
