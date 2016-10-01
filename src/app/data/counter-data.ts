interface CounterDataArguments {
  activePower: number;
  reactivePower: number;
  apparentPower: number;
}

export class CounterData {
  activePower: number;
  reactivePower: number;
  apparentPower: number;

  constructor(args: CounterDataArguments) {
    this.activePower = args.activePower;
    this.reactivePower = args.reactivePower;
    this.apparentPower = args.apparentPower;
  }
}