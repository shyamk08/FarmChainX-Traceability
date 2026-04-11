import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'find',
    standalone: true
})
export class FindPipe implements PipeTransform {
    transform(array: any[], id: number): any {
        if (!array || id === null || id === undefined) {
            return null;
        }
        return array.find(item => item.id === id);
    }
}
