import { EssData } from './ess-data';

interface CurrentDataArguments {
  ess0: EssData;
  //ess1: EssData;
}

export class CurrentData {
    ess0: EssData;
    ess1: EssData;
    constructor(args: CurrentDataArguments) {
        this.ess0 = args.ess0;
        //this.ess1 = args.ess1;
    }
}
