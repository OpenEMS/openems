export interface Dataset {
  label: string;
  data: number[];
  hidden: boolean;
}

export const EMPTY_DATASET = [{
  label: "",
  data: [],
  hidden: false
}];