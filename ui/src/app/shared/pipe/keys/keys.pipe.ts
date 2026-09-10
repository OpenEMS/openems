import { Pipe, PipeTransform } from "@angular/core";

@Pipe({
    name: "keys",
    standalone: false,
})
export class KeysPipe implements PipeTransform {
    transform<T>(value: Record<string, T> | null | undefined, args: string[]): { key: string; value: T }[] | null | undefined {
        if (!value) {
            return value as null | undefined;
        }

        const keys = [];
        for (const key in value) {
            keys.push({ key: key, value: value[key] });
        }
        return keys;
    }
}
