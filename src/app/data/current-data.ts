import { EssData } from './ess-data';
import { SlData } from './sl-data';
import { CounterData } from './counter-data';
import { IoData } from './io-data';

interface CurrentDataArguments {
  ess0: EssData;
  ess1: EssData;
  sl0: SlData;
  counter0: CounterData;
  io0: IoData;
}

export class CurrentData {
    ess0: EssData;
    ess1: EssData;
    sl0: SlData;
    counter0: CounterData;
    io0: IoData;
    constructor(args: CurrentDataArguments) {
        this.ess0 = args.ess0;
        this.ess1 = args.ess1;
        this.sl0 = args.sl0;
        this.counter0 = args.counter0;
        this.io0 = args.io0;
    }
}
