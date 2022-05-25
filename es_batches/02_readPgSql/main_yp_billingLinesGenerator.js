
// luxon for dates manipulation
// https://moment.github.io/luxon/#/ 
const { DateTime } = require("luxon");



const pgLib = require("./pgLib.js");
const inflxLib = require("./influxLib.js");
const ypMeters = require("./ypMeters.js");

const ypPgQueries = require("./ypPgQueries.js");
const ypInflxQueries = require("./ypInflxQueries.js");


const enableOffsetMode = true;

//
// ================================================================================================================================================================
//
//   WARNING: run the following procedure BEFORE running the batch, to provide backup of the startup tables... only during devel (or fixings...)
//
// ================================================================================================================================================================
//
//   CALL "youpower-billingmeters"."EmergencyBackup_Tables_ReadingTasks_And_MetersReadings"();
//
// ================================================================================================================================================================
//

//
//TODO find another spot to shift
// function to calculate kappa in the offset meter
function calculateKappa(mitbill, timeStep, theEdgeForThisMeas){
  const KWHTotals = theEdgeForThisMeas.KWHTotals;

  for(kkk = timeStep; kkk < mitbill.steppi.length; kkk+=timeStep){

    const kappo = {
      "userPow_introTotal": 0,
      "userPow_fromIntro": 0,
      "userPow_fromProd": 0
    };

    //
    // eval intro
    const mainIntro_cons = KWHTotals.introArray[kkk].consumption - KWHTotals.introArray[kkk - timeStep].consumption;
    const mainIntro_prod = KWHTotals.introArray[kkk].production - KWHTotals.introArray[kkk - timeStep].production;
    //
    // eval prod
    const mainProd_prod = KWHTotals.prodArray[kkk].production - KWHTotals.prodArray[kkk - timeStep].production;
    const mainprod_prod4users = mainProd_prod - mainIntro_prod;

    //
    // eval meter under offset ....
    const totalKWHBill_intro = mainIntro_cons + mainprod_prod4users - KWHTotals.offsetMeter.totalBillingOffset_allParts[kkk];
    const totalKWHBill_partFromIntro = mainIntro_cons - KWHTotals.offsetMeter.totalBillingOffset_intro[kkk];
    const totalKWHBill_partFromProd = mainprod_prod4users - KWHTotals.offsetMeter.totalBillingOffset_prod[kkk];

    kappo.userPow_introTotal = mitbill.steppi[kkk].thisStepBill_intro / (totalKWHBill_intro == 0 ? 1 : totalKWHBill_intro); 
    kappo.userPow_fromIntro = mitbill.steppi[kkk].userPow_fromProd / (totalKWHBill_partFromIntro == 0 ? 1 : totalKWHBill_partFromIntro); 
    kappo.userPow_fromProd = mitbill.steppi[kkk].userPow_fromIntro / (totalKWHBill_partFromProd == 0 ? 1 : totalKWHBill_partFromProd);
    
    KWHTotals.billingTotals["meter_" + mitbill.meterid].valuesInOffset.kappa[kkk] = kappo;
  }
}

function getOrSaveMeasClusterOnClusters(measurementClustersReadings,meterContainer){
  measurementClustersReadings["edg_" + meterContainer.idedge] = measurementClustersReadings["edg_" + meterContainer.idedge] ?? 
  {
    idEdge: meterContainer.idedge,
    edgeDesc: meterContainer.edgedesc,
    billingMeters: [],
    testMeters: [],
    introductionMeter: {},
    productionMeter: {},
    // some metas
    // const meterReadMode = theMeter["ReadMode"];
    // const meterOnEdge = theMeter["MeterOnEdge"];
    influxDb: meterContainer.InfluxDb,
    influxQuery: "", // = ypInflxQueries.generateInfluxDbQuery(meterOnEdge, measureDateStart, measureDateEnd);
    influxQueryResult: []


    //
    //  in the next future something will be added
    // 
  };
  return measurementClustersReadings["edg_" + meterContainer.idedge];


}

//
// create measurement cluster container if missing
function createOrUpdateEdgeContainer(meterContainer, measurementClustersReadings, yyy) {
  // console.log(`meter ${yyy} : `, meterContainer);
  const theMeasClust = getOrSaveMeasClusterOnClusters(measurementClustersReadings, meterContainer);
  
  //
  // add to billing set if billing
  switch (meterContainer.metertype) {
    case "Billing":
      // console.log(meterContainer);
      theMeasClust.billingMeters.push(meterContainer);
    break;
    case "Introduction":
      theMeasClust.introductionMeter = meterContainer;
      break;
    case "Production":
      theMeasClust.productionMeter = meterContainer;
      break;
    case "test":
      theMeasClust.testMeters.push(meterContainer);
      break;

  }
  return theMeasClust;

}
function appendInfluxMeterQueryFields(theEdgeForThisMeas, theMeter){
  theEdgeForThisMeas.fullInfluxQueryFields += (theEdgeForThisMeas.fullInfluxQueryFields.length > 0 ? ",\n": "");
  theEdgeForThisMeas.fullInfluxQueryFields += theMeter.influxDataSetFieldsNames;
  inflxLib.generateInfluxFieldsDescriptors(theMeter.MeterOnEdge, theEdgeForThisMeas.fullInfluxFieldsMetadata);


}
//
// build hyperInfluxQuery !!!
function hyperInfluxQuery(theEdgeForThisMeas) {
  theEdgeForThisMeas.fullInfluxQueryFields = "";
  theEdgeForThisMeas.fullInfluxFieldsMetadata = {};
  if(!!theEdgeForThisMeas.introductionMeter) {
    theEdgeForThisMeas.introductionMeter.influxDataSetFieldsNames = ypInflxQueries.generateInfluxDbQueryMeterSection(theEdgeForThisMeas.introductionMeter.MeterOnEdge);
    appendInfluxMeterQueryFields(theEdgeForThisMeas, theEdgeForThisMeas.introductionMeter);
  }
  if(!!theEdgeForThisMeas.productionMeter) {
    theEdgeForThisMeas.productionMeter.influxDataSetFieldsNames = ypInflxQueries.generateInfluxDbQueryMeterSection(theEdgeForThisMeas.productionMeter.MeterOnEdge);
    appendInfluxMeterQueryFields(theEdgeForThisMeas, theEdgeForThisMeas.productionMeter);
  } 
  if(!!theEdgeForThisMeas.billingMeters) {
    for (var kkk = 0; kkk < theEdgeForThisMeas.billingMeters.length; kkk++){
      let theMeter = theEdgeForThisMeas.billingMeters[kkk];
      theMeter.influxDataSetFieldsNames = ypInflxQueries.generateInfluxDbQueryMeterSection(theMeter.MeterOnEdge);
      appendInfluxMeterQueryFields(theEdgeForThisMeas, theMeter);
    }
  } 
  if(!!theEdgeForThisMeas.testMeters) {
    for (var kkk = 0; kkk < theEdgeForThisMeas.testMeters.length; kkk++){
      let theMeter = theEdgeForThisMeas.testMeters[kkk];
      theMeter.influxDataSetFieldsNames = ypInflxQueries.generateInfluxDbQueryMeterSection(theMeter.MeterOnEdge);
      appendInfluxMeterQueryFields(theEdgeForThisMeas, theMeter);
    }
  } 
}
 


//
// =================================================================================================================================================================================
//
// here is the main... but ....
//
// Use a self-calling function so we can use async / await.

(async () => {
  //
  // hello world....
  const poolResult = await pgLib.poolDemo();
  console.log("PG Time with pool: " + poolResult.rows[0]["now"]);

  const clientResult = await pgLib.clientDemo();
  console.log("PG Time with client: " + clientResult.rows[0]["now"]);


  //
  //this constant is number of mesurement intervals. 
  // 2022.05.25 - bonde : this number times 5s - 
  const timeStep = 180;
  //3
  //
  // first of all, load read tasks list
  const readTasks = await pgLib.client(ypPgQueries.generateReadTasksQuery());

  if (readTasks.rows.length === 0){
    console.log("no read tasks");
    return 0;
  } else {
    console.log(readTasks);
  }
  // then for each read task, get meters list
  for (var xxx=0; xxx < readTasks.rows.length; xxx++) {
    const readTask = readTasks.rows[xxx];
    const readTaskId = readTask["Id"];
    const measureDateStart = DateTime.fromJSDate(readTask["MeasureDateStart"]).toFormat('yyyy-MM-dd HH:mm:ss');
    const measureDateEnd = DateTime.fromJSDate(readTask["MeasureDateEnd"]).toFormat('yyyy-MM-dd HH:mm:ss');
    const idBillingLevel = readTask["MeterBillingLevelId"];
    const idBillingLevelsSet = readTask["MeterBillingLevelSetId"];
    const billLevelForeignUid = readTask["BillLevelForeignUID"];
    const billLevelsSetForeignUid = readTask["BillLevelsSetForeignUID"];
    // get meters list
    const theMeters = await pgLib.client(ypPgQueries.generateMetersReadingsFromReadTasksQuery(idBillingLevelsSet, idBillingLevel));
    // console.log(`Read Task ${xxx} :`, readTask);
    console.log(`\n\n**** **** **** \n\n${xxx} - ReadTask Id: ${readTask['Id']} - MeterBillingLevelId ${idBillingLevel}; - dateStart ${measureDateStart}; - dateEnd ${measureDateEnd};`);
    //
    // if we have data, we have to scan the meters list
    // in order to build the measuments Clusters
    // a measurement cluster is an edge, which will contains:
    //
    // - an introduction meter (if not present, meters reading levels will default to 0)
    // - a production meter (if not present, meters reading levels will default to 0)
    // - one or more billing meters
    //
    // each mesurement set will concurr in defining the billing measure for a given billing meter
    if (theMeters.rows.length > 0) {
      console.log(`there are ${theMeters.rows.length} meters to read for this read task...`);
      const measurementClusters = {};

      measurementClusters.measureDateStart = measureDateStart;
      measurementClusters.measureDateEnd = measureDateEnd;
      measurementClusters.idBillingLevel = idBillingLevel;
      measurementClusters.idBillingLevelsSet = idBillingLevelsSet;
      measurementClusters.billLevelForeignUid = billLevelForeignUid;
      measurementClusters.billLevelsSetForeignUid = billLevelsSetForeignUid;
      measurementClusters.readings = {};
      //
      //
      // build up all the measurements containers
      for (var yyy=0; yyy < theMeters.rows.length; yyy++) {
        const theMeter = theMeters.rows[yyy];
        createOrUpdateEdgeContainer(theMeter, measurementClusters.readings, yyy);
      }

      // console.log(measurementClusters);

      // prepare all influx queries,
      // ad run them one by one...
      var zzz = 0;
      for (var key in  measurementClusters.readings) {
        zzz++;
        theEdgeForThisMeas =  measurementClusters.readings[key];
        console.log(`building data queries for edge ${theEdgeForThisMeas.idEdge}`)
        hyperInfluxQuery(theEdgeForThisMeas);
        // console.log(theEdge);
        // console.log(theEdge.fullInfluxQueryFields);
        theEdgeForThisMeas.fullInfluxQuery = ypInflxQueries.generateGivenMetersInfluxDbQuery(theEdgeForThisMeas.fullInfluxQueryFields,measureDateStart,measureDateEnd);
        console.log("Query will be:");
        console.log(theEdgeForThisMeas.fullInfluxQuery);
        const influxConn = inflxLib.startInfluxConnection(theEdgeForThisMeas.influxDb,theEdgeForThisMeas.fullInfluxFieldsMetadata);
        try {
          theEdgeForThisMeas.influxDataPromiseResult = await influxConn.query(theEdgeForThisMeas.fullInfluxQuery);
          const result = theEdgeForThisMeas.influxDataPromiseResult;
          if (!result || (result.length < 1)) {
              console.log(`no data on DB ${theEdgeForThisMeas.influxDb} --- Edge ${theEdgeForThisMeas.idEdge}-${theEdgeForThisMeas.edgeDesc} in dates interval ${measureDateStart} to ${measureDateEnd}`);
          } else {
            console.log(`decoding from DB ${theEdgeForThisMeas.influxDb} --- Edge ${theEdgeForThisMeas.idEdge}-${theEdgeForThisMeas.edgeDesc} a time series of ${result.length} in dates interval ${measureDateStart} to ${measureDateEnd}`);
            theEdgeForThisMeas.KWHTotals = {
              totalKWHImport : 0,
              totalKWHExport : 0,
              offsetMeter: {
                totalBillingOffset_allParts: [],
                totalBillingOffset_intro: [],
                totalBillingOffset_prod: []
              },
              billingTotals: {}
            };
            ypMeters.buildTotals(theEdgeForThisMeas, result, timeStep);
            console.log(`\n\n\n ${theEdgeForThisMeas.influxDb} totals: `, theEdgeForThisMeas.KWHTotals);
            console.log("Going to insert data in the DB");
            let ciccabc = await pgLib.client(
              'CALL "youpower-billingmeters"."helloWorld"($1)',
              ['chicco']
            );
            for (var keyyy in theEdgeForThisMeas.KWHTotals.billingTotals){
              const billedMeterReadData = theEdgeForThisMeas.KWHTotals.billingTotals[keyyy];
              const mitbill = billedMeterReadData.meter;
              //TODO find another spot for this function
              calculateKappa(mitbill, timeStep, theEdgeForThisMeas);
              const valuesSource = 
                  billedMeterReadData.meter.inOffsetFlag
                     && 
                     enableOffsetMode
                      ?
                        billedMeterReadData.valuesInOffset
                            :
                        billedMeterReadData
              ;
              const parametersQuery = [
                
                  measureDateStart, 
                  measureDateEnd, 
                  readTaskId,
                  true, 
  
                  mitbill.meterid,
                  mitbill.MeterUid,
                  mitbill.userid,
                  mitbill.useruid,
  
                  measurementClusters.idBillingLevel,
                  measurementClusters.idBillingLevelsSet,
                  measurementClusters.billLevelForeignUid,
                  measurementClusters.billLevelsSetForeignUid,    
                  // billLevelForeignUid,
                  // billLevelsSetForeignUid,
            
                  valuesSource.totalKWHBill_intro, 0, 0, 0,
  
                  valuesSource.totalKWHBill_prod, 0, 0, 0,
  
                  valuesSource.totalKWHBill_intro,
                  valuesSource.totalKWHBill_partFromProd, 
                  valuesSource.totalKWHBill_partFromIntro
                ];
              
              console.log(parametersQuery);
              ciccabc = await pgLib.client(
                'CALL "youpower-billingmeters"."saveLectureFromMeter"($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19, $20, $21, $22, $23)',
                parametersQuery
              );

            }
            
            ciccabc = await pgLib.client(
              'CALL "youpower-billingmeters"."SetReadingTaskToCompleted"($1)',
              [readTaskId]
            );
        

          }
        } catch(error) {
          console.log(error);
        }
      }
    } else {
      console.log("no meters !!!");
    }
  }

})();

