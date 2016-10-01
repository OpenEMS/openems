interface IoDataArguments {
  digitalOutput_1_1: boolean;
  digitalOutput_1_2: boolean;
  digitalOutput_2_1: boolean;
  digitalOutput_2_2: boolean;
}

export class IoData {
  digitalOutput_1_1: boolean;
  digitalOutput_1_2: boolean;
  digitalOutput_2_1: boolean;
  digitalOutput_2_2: boolean;

  constructor(args: IoDataArguments) {
    this.digitalOutput_1_1 = args.digitalOutput_1_1;
    this.digitalOutput_1_2 = args.digitalOutput_1_2;
    this.digitalOutput_2_1 = args.digitalOutput_2_1;
    this.digitalOutput_2_2 = args.digitalOutput_2_2;
  }
}