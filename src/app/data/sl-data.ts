interface SlDataArguments {
  pac: number;
  limit: number;
}

export class SlData {
  pac: number;
  limit: number;

  constructor(args: SlDataArguments) {
    this.pac = args.pac;
    this.limit = args.limit;
  }
}