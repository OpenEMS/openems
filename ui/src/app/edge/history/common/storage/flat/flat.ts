import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';

import { Utils } from '../../../../../shared/shared';

@Component({
    selector: 'storageWidget',
    templateUrl: './flat.html'
})
export class FlatComponent extends AbstractFlatWidget {

    public readonly CONVERT_TO_KILO_WATTHOURS = Utils.CONVERT_TO_KILO_WATTHOURS;

}
