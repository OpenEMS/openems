


/*
 *
 *
 *
 

in binary:
every nibble is divided in this way:

b0: function for sys
b1: function for phase F1
b2: function for phase F2
b3: function for phase F3

phases are as follows:

nibble 0:
00 00 00 00 00 00 00 X0 = no signs alterations
00 00 00 00 00 00 00 X1 = change sign to Sys 
00 00 00 00 00 00 00 X2 = change sign to F1  
00 00 00 00 00 00 00 X4 = change sign to F2 
00 00 00 00 00 00 00 X8 = change sign to F3
.....

nibble 1:
00 00 00 00 00 00 00 1X = exchange totals: import with export on Sys field; total is still Sys
00 00 00 00 00 00 00 2X = exchange F1 Import with F1 Export; total is F1 + F2 + F3
00 00 00 00 00 00 00 4X = exchange F2 Import with F2 Export; total is F1 + F2 + F3 
00 00 00 00 00 00 00 8X = exchange F3 Import with F3 Export; total is F1 + F2 + F3
....




 *
 *
 *
 * 
 */

//
//
// sum proper energy term according to the given meter read mode
//
// previously called this way
//
// var totalImportOffset = sumPartsOrWorkOnTotals(sumModeFlag,signF1, signF2, signF3, signFSys, colConsForF1, colConsForF2, colConsForF3, colConsForFSys, timeSeries[0]);
// var totalExportOffset = sumPartsOrWorkOnTotals(sumModeFlag,signF1, signF2, signF3, signFSys, colProdForF1, colProdForF2, colProdForF3, colProdForFSys, timeSeries[0]);
//
// function sumPartsOrWorkOnTotals(sumModeFlag, signF1, signF2, signF3, signFSys, colF1, colF2, colF3, colFSys, theRow) {
//
//
//

function sumPartsOrWorkOnTotals(billMeterOnEdgeDescriptor, theRow) {
  const consumption = (billMeterOnEdgeDescriptor.sumModeFlag ? 
    billMeterOnEdgeDescriptor.signF1 * theRow[billMeterOnEdgeDescriptor.colConsForF1] 
    + billMeterOnEdgeDescriptor.signF2 * theRow[billMeterOnEdgeDescriptor.colConsForF2] 
    + billMeterOnEdgeDescriptor.signF3 * theRow[billMeterOnEdgeDescriptor.colConsForF3] 
    : 
    billMeterOnEdgeDescriptor.signFSys * theRow[billMeterOnEdgeDescriptor.colConsForFSys]);  
  const production = (billMeterOnEdgeDescriptor.sumModeFlag ? 
    billMeterOnEdgeDescriptor.signF1 * theRow[billMeterOnEdgeDescriptor.colProdForF1] 
    + billMeterOnEdgeDescriptor.signF2 * theRow[billMeterOnEdgeDescriptor.colProdForF2] 
    + billMeterOnEdgeDescriptor.signF3 * theRow[billMeterOnEdgeDescriptor.colProdForF3] 
    : 
    billMeterOnEdgeDescriptor.signFSys * theRow[billMeterOnEdgeDescriptor.colProdForFSys]);  
  
  return {
    consumption,
    production
  };
}


  /*
  // assign fieds for import and export and sign term
  //
  // F1 
  const invertConsAndProdFlagF1 = (meterReadMode & 0x20);
  const colProdForF1 = invertConsAndProdFlagF1 ? "ConsumptionF1" : "ProductionF1";
  const colConsForF1 = invertConsAndProdFlagF1 ? "ProductionF1" : "ConsumptionF1";
  const signF1 = (meterReadMode & 0x02) ? -1 : 1;
  // F2 
  const invertConsAndProdFlagF2 = (meterReadMode & 0x40);
  const colProdForF2 = invertConsAndProdFlagF2 ? "ConsumptionF2" : "ProductionF2";
  const colConsForF2 = invertConsAndProdFlagF2 ? "ProductionF2" : "ConsumptionF2";
  const signF2 = meterReadMode & 0x04 ? -1 : 1;
  // F3 
  const invertConsAndProdFlagF3 = (meterReadMode & 0x80);
  const colProdForF3 = invertConsAndProdFlagF3 ? "ConsumptionF3" : "ProductionF3";
  const colConsForF3 = invertConsAndProdFlagF3 ? "ProductionF3" : "ConsumptionF3";
  const signF3 = meterReadMode & 0x08 ? -1 : 1;
  // FSys 
  const invertConsAndProdFlagFSys = (meterReadMode & 0x10);
  const colProdForFSys =  invertConsAndProdFlagFSys ? "ConsumptionSys" : "ProductionSys";
  const colConsForFSys =  invertConsAndProdFlagFSys ? "ProductionSys" : "ConsumptionSys";
  const signFSys = meterReadMode & 0x01 === 0x01 ? -1 : 1;
  // check if measure comes from components or from Sys/total
  const sumModeFlag = (meterReadMode & 0xE0);
  */
function  buildMeterReadModes(theMeter){
  //
  // basic data for all the following
  const meterNameOnEdge = theMeter.MeterOnEdge;
  const metForFld = `${meterNameOnEdge}_`;
  const meterReadMode = theMeter.ReadMode;
  // check if measure comes from components or from Sys/total
  const sumModeFlag = (meterReadMode & 0xE0);

  // flags invert Introductions with productions
  const invertConsAndProdFlagF1 = (meterReadMode & 0x20);
  const invertConsAndProdFlagF2 = (meterReadMode & 0x40);
  const invertConsAndProdFlagF3 = (meterReadMode & 0x80);
  const invertConsAndProdFlagFSys = (meterReadMode & 0x10);


  //
  //
  // assign fieds for:
  //  - import
  //  - export
  //  - sign term
  //
  const meterDescriptor = {
    // 
    theMeter: theMeter,
    meterNameOnEdge: meterNameOnEdge,
    meterReadMode: meterReadMode,
    sumModeFlag: sumModeFlag,
    // F1
    invertConsAndProdFlagF1: invertConsAndProdFlagF1,
    colProdForF1: invertConsAndProdFlagF1 ? `${metForFld}ConsumptionF1` : `${metForFld}ProductionF1`,
    colConsForF1: invertConsAndProdFlagF1 ? `${metForFld}ProductionF1` : `${metForFld}ConsumptionF1`,
    signF1: (meterReadMode & 0x02) ? -1 : 1,
    // F2 
    invertConsAndProdFlagF2: (meterReadMode & 0x40),
    colProdForF2: invertConsAndProdFlagF2 ? `${metForFld}ConsumptionF2` : `${metForFld}ProductionF2`,
    colConsForF2: invertConsAndProdFlagF2 ? `${metForFld}ProductionF2` : `${metForFld}ConsumptionF2`,
    signF2: meterReadMode & 0x04 ? -1 : 1,
    // F3 
    invertConsAndProdFlagF3: (meterReadMode & 0x80),
    colProdForF3: invertConsAndProdFlagF3 ? `${metForFld}ConsumptionF3` : `${metForFld}ProductionF3`,
    colConsForF3: invertConsAndProdFlagF3 ? `${metForFld}ProductionF3` : `${metForFld}ConsumptionF3`,
    signF3: meterReadMode & 0x08 ? -1 : 1,
    // FSys 
    invertConsAndProdFlagFSys: (meterReadMode & 0x10),
    colProdForFSys:  invertConsAndProdFlagFSys ? `${metForFld}ConsumptionSys` : `${metForFld}ProductionSys`,
    colConsForFSys:  invertConsAndProdFlagFSys ? `${metForFld}ProductionSys` : `${metForFld}ConsumptionSys`,
    signFSys: meterReadMode & 0x01 === 0x01 ? -1 : 1

  };
 
  return meterDescriptor;
}


//
// having an Influxdb time series, calculate the total energy used 
//
function buildTotalsForMeter(totalsContainer, theMeterWeAreBilling, timeSeries){
  /*
  // assign fieds for import and export and sign term
  //
  // F1 
  const invertConsAndProdFlagF1 = (meterReadMode & 0x20);
  const colProdForF1 = invertConsAndProdFlagF1 ? "ConsumptionF1" : "ProductionF1";
  const colConsForF1 = invertConsAndProdFlagF1 ? "ProductionF1" : "ConsumptionF1";
  const signF1 = (meterReadMode & 0x02) ? -1 : 1;
  // F2 
  const invertConsAndProdFlagF2 = (meterReadMode & 0x40);
  const colProdForF2 = invertConsAndProdFlagF2 ? "ConsumptionF2" : "ProductionF2";
  const colConsForF2 = invertConsAndProdFlagF2 ? "ProductionF2" : "ConsumptionF2";
  const signF2 = meterReadMode & 0x04 ? -1 : 1;
  // F3 
  const invertConsAndProdFlagF3 = (meterReadMode & 0x80);
  const colProdForF3 = invertConsAndProdFlagF3 ? "ConsumptionF3" : "ProductionF3";
  const colConsForF3 = invertConsAndProdFlagF3 ? "ProductionF3" : "ConsumptionF3";
  const signF3 = meterReadMode & 0x08 ? -1 : 1;
  // FSys 
  const invertConsAndProdFlagFSys = (meterReadMode & 0x10);
  const colProdForFSys =  invertConsAndProdFlagFSys ? "ConsumptionSys" : "ProductionSys";
  const colConsForFSys =  invertConsAndProdFlagFSys ? "ProductionSys" : "ConsumptionSys";
  const signFSys = meterReadMode & 0x01 === 0x01 ? -1 : 1;
  // check if measure comes from components or from Sys/total
  const sumModeFlag = (meterReadMode & 0xE0);
  */


  const keyForThisMeterResult = "meter_" + theMeterWeAreBilling.meterid;
  totalsContainer.KWHTotals.billingTotals[keyForThisMeterResult] = {
    billAtStart: 0,
    billAtEnd: 0,
    totalKWHBill_intro: 0,
    totalKWHBill_partFromProd: 0,
    totalKWHBill_partFromIntro: 0,
    totalKWHBill_prod: 0,
    meter: theMeterWeAreBilling

  };


  const billMeterOnEdgeDescriptor = buildMeterReadModes(theMeterWeAreBilling);
  const introMeterOnEdgeDescriptor = buildMeterReadModes(totalsContainer.introductionMeter);
  const prodMeterOnEdgeDescriptor = buildMeterReadModes(totalsContainer.productionMeter);

  //
  // loop through the results
  //
  //   former impl:
  // 
  // var totalImportOffset = sumPartsOrWorkOnTotals(sumModeFlag,signF1, signF2, signF3, signFSys, colConsForF1, colConsForF2, colConsForF3, colConsForFSys, timeSeries[0]);
  // var totalExportOffset = sumPartsOrWorkOnTotals(sumModeFlag,signF1, signF2, signF3, signFSys, colProdForF1, colProdForF2, colProdForF3, colProdForFSys, timeSeries[0]);
  //
  // new buffers startup
  var totalBillPreviousStep = sumPartsOrWorkOnTotals(billMeterOnEdgeDescriptor, timeSeries[0]);
  var totalIntroPreviousStep = sumPartsOrWorkOnTotals(introMeterOnEdgeDescriptor, timeSeries[0]);
  var totalProdPreviousStep = sumPartsOrWorkOnTotals(prodMeterOnEdgeDescriptor, timeSeries[0]);
  totalsContainer.KWHTotals.billingTotals[keyForThisMeterResult].billAtStart = totalBillPreviousStep;
  totalsContainer.KWHTotals.introAtStart = totalIntroPreviousStep;
  totalsContainer.KWHTotals.prodAtStart = totalProdPreviousStep;

  totalsContainer.KWHTotals.totalKWHBill_intro = 0;
  totalsContainer.KWHTotals.totalKWHBill_prod = 0;

  let lastRow = timeSeries[0];
  for(var yyy = 1; yyy < timeSeries.length; yyy++) {
    const theRow = timeSeries[yyy];
    // billing meter readings
    var totalBillAtThisStep = sumPartsOrWorkOnTotals(billMeterOnEdgeDescriptor, theRow);
    var totalIntroAtThisStep = sumPartsOrWorkOnTotals(introMeterOnEdgeDescriptor, theRow);
    var totalProdAtThisStep = sumPartsOrWorkOnTotals(prodMeterOnEdgeDescriptor, theRow);
    //
    // eval intro
    totalsContainer.KWHTotals.totalKWHImport_intro = totalIntroAtThisStep.consumption - totalIntroPreviousStep.consumption;
    totalsContainer.KWHTotals.totalKWHImport_prod = totalIntroAtThisStep.production - totalIntroPreviousStep.production;
    //
    totalsContainer.KWHTotals.totalKWHExport_intro = totalProdAtThisStep.consumption - totalProdPreviousStep.consumption;
    totalsContainer.KWHTotals.totalKWHExport_prod = totalProdAtThisStep.production - totalProdPreviousStep.production;
    //
    // some overall evals in this step between total imported and exported energy
    //
    totalsContainer.KWHTotals.percentProdOverIntro_straight = totalsContainer.KWHTotals.totalKWHImport_intro != 0 
                                                            ? totalsContainer.KWHTotals.totalKWHExport_prod / totalsContainer.KWHTotals.totalKWHImport_intro : 0;
    totalsContainer.KWHTotals.percentProdOverIntro_reverse = totalsContainer.KWHTotals.totalKWHImport_prod != 0 
                                                            ? totalsContainer.KWHTotals.totalKWHExport_intro / totalsContainer.KWHTotals.totalKWHImport_prod : 0;



    // this billing meter levels...
    const thisStepBill_intro = totalBillAtThisStep.consumption - totalBillPreviousStep.consumption;
    const thisStepBill_prod = totalBillAtThisStep.production - totalBillPreviousStep.production;;


    // the result
    totalsContainer.KWHTotals.billingTotals[keyForThisMeterResult].totalKWHBill_intro += thisStepBill_intro;
    totalsContainer.KWHTotals.billingTotals[keyForThisMeterResult].totalKWHBill_partFromProd += thisStepBill_intro * totalsContainer.KWHTotals.percentProdOverIntro_straight;
    totalsContainer.KWHTotals.billingTotals[keyForThisMeterResult].totalKWHBill_partFromIntro += thisStepBill_intro * (1 - totalsContainer.KWHTotals.percentProdOverIntro_straight);
    // should be always zero or near to zero
    totalsContainer.KWHTotals.billingTotals[keyForThisMeterResult].totalKWHBill_prod += thisStepBill_prod



    //
    // update offsets
    totalBillPreviousStep = sumPartsOrWorkOnTotals(billMeterOnEdgeDescriptor, theRow);
    totalIntroPreviousStep = sumPartsOrWorkOnTotals(introMeterOnEdgeDescriptor, theRow);
    totalProdPreviousStep = sumPartsOrWorkOnTotals(prodMeterOnEdgeDescriptor, theRow);
    lastRow = theRow;
  };
  // set foinal values as red from db at the end...
  totalsContainer.KWHTotals.billingTotals[keyForThisMeterResult].billAtEnd = totalBillPreviousStep;
  totalsContainer.KWHTotals.introAtEnd = totalIntroPreviousStep;
  totalsContainer.KWHTotals.prodAtEnd = totalProdPreviousStep;

  return 0;
}



//
//
// read influx query results and evaluate total consumptions
// on every billed edge meter
//
//
function buildTotals(theEdgeForThisMeas, result) {
/**
 * 
 *     
    idEdge: meterContainer.idedge,
    edgeDesc: meterContainer.edgedesc,
    billingMeters: [],
    testMeters: [],
    introduction: {},
    production: {},
    // some metas
    // const meterReadMode = theMeter["ReadMode"];
    // const meterOnEdge = theMeter["MeterOnEdge"];
    influxDb: meterContainer.InfluxDb,
    influxQuery: "", // = ypInflxQueries.generateInfluxDbQuery(meterOnEdge, measureDateStart, measureDateEnd);
    influxQueryResult: [],
    KWHTotals: {
              totalKWHImport : 0,
              totalKWHExport : 0
            }


    //
    // in the next future something will be added
    //

 * 
 * 
 */
  const billingMeters = theEdgeForThisMeas.billingMeters;
  theIntroMeter = theEdgeForThisMeas.introductionMeter;
  theProdsMeter = theEdgeForThisMeas.productionMeter;
  for (xxx = 0; xxx < billingMeters.length; xxx++) {
    theMeterWeAreBilling = billingMeters[xxx];
    buildTotalsForMeter(theEdgeForThisMeas, theMeterWeAreBilling, result);

  }

}



module.exports = {
  sumPartsOrWorkOnTotals,
  buildTotals
}