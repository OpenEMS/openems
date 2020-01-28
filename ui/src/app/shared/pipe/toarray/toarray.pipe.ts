import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'toarray'
})
export class ToArray implements PipeTransform {
    transform(value, args: string[]): any {
        let string = '';
        let array = [];
        try {
            string = value.replace('[', '');
            string = string.replace(']', '');
            array = string.split(',');
        } catch (error) {

        }


        return array;
    }
} 