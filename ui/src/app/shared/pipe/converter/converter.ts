import { Pipe, PipeTransform } from "@angular/core";

import { Converter } from "../../components/shared/converter";

@Pipe({
    name: "converter",
    standalone: false,
})
export class ConverterPipe implements PipeTransform {

    constructor() { }

    /**
     * Transforms the value with a given converter
     *
     * @param value the passed value
     * @param converter the passed converter
     * @returns the result of the converter as a string
     */
    transform(value: number, converter: Converter): string {

        // Converter not provided, returns prev value
        if (!converter) {
            return value.toString();
        }
        return converter(value);
    }
}
