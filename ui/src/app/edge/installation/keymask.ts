import { Directive, HostListener } from '@angular/core';

@Directive({
    selector: '[oeKeyMask]',
})
export class KeyMaskDirective {
    @HostListener('input', ['$event'])
    onInput(event: KeyboardEvent) {
        const input = event.target as HTMLInputElement;

        let trimmed = input.value.replace(/\s+/g, '');

        if (trimmed.length > 19) {
            trimmed = trimmed.substr(0, 19);
        }

        let hasDashAsLastChar = trimmed.substr(trimmed.length - 1, 1) == "-";
        trimmed = trimmed.replace(/-/g, '');

        let numbers = [];

        numbers.push(trimmed.substr(0, 4));
        if (trimmed.substr(4, 4) !== '') {
            numbers.push(trimmed.substr(4, 4));
        }
        if (trimmed.substr(8, 4) != '') {
            numbers.push(trimmed.substr(8, 4));
        }
        if (trimmed.substr(12, 4) != '') {
            numbers.push(trimmed.substr(12, 4));
        }

        let result = numbers.join('-').toUpperCase();
        if (hasDashAsLastChar) {
            result += '-';
        }
        input.value = result;
    }
}
