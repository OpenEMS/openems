import { Pipe, PipeTransform } from "@angular/core";

@Pipe({
    name: "timedisplay",
    standalone: false,
})
export class TimedisplayPipe implements PipeTransform {
    /**
     * Formats a time string for display (HH:mm).
     *
     * This pipe handles ISO 8601 strings (extracts HH:mm) and plain HH:mm strings.
     * Defaults to "00:00" if the input is null, undefined, or an empty string.
     *
     * @param value The time string (E.G., "2025-07-14T14:30:00.000Z" or "09:15").
     * @returns The formatted time string in "HH:mm" format, or "00:00".
     */
    transform(value: string | null | undefined): string {
        if (value?.includes("T")) {
            return VALUE.SUBSTRING(11, 16);
        }
        return value || "00:00";
    }
}
