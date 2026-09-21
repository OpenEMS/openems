import { Pipe, PipeTransform } from "@angular/core";

@Pipe({
    name: "labelToLines",
    standalone: true,
    pure: true,
})
export class LabelToLinesPipe implements PipeTransform {
    transform(
        label: string | string[] | null | undefined,
        onlyFirst: boolean = false
    ): string[] {
        if (!label) {return [];}

        const lines = Array.isArray(label) ? label : [label];

        return onlyFirst ? [lines[0]] : lines;
    }
}
