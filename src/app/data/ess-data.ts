interface EssDataArguments {
  soc: number;
  activePower: number;
  reactivePower: number;
}

export class EssData {
  soc: number;
  activePower: number;
  reactivePower: number;

  constructor(args: EssDataArguments) {
    this.soc = args.soc;
    this.activePower = args.activePower;
    this.reactivePower = args.reactivePower;
  }
}