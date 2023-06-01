import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import { ChannelAddress } from 'src/app/shared/shared';

@Component({
  selector: 'fixDigitalOutputWidget',
  templateUrl: './flat.html'
})
export class FlatComponent extends AbstractFlatWidget {

  protected outputChannel: string | null = null;

  protected override getChannelAddresses(): ChannelAddress[] {

    this.outputChannel = this.component.properties['outputChannelAddress'];
    return [];
  }
}
