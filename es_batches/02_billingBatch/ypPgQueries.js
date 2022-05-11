



//
// ==================================================================
//
// postgres queries
//
// extract the read tasks list which still need to be performed
function generateReadTasksQuery() {

  const queryReadTasks = `
	SELECT 
		* 
	FROM 
		"youpower-billingmeters"."MeterReadingTasksList" "MRTL"
	WHERE
		"MRTL"."Completed"=false
	ORDER BY
		"MRTL"."Id"
	`;
  return queryReadTasks;
}

//
//
//
// list meters to be read in a given billing levels set
function generateMetersReadingsFromReadTasksQuery(billingLevelsSetId) {

  const queryReadTasks = `
	SELECT 
		* 
	FROM 
		"youpower-meters"."MetersListForReading" "MLFR"
	WHERE
		"MLFR"."idbillinglevelsset"=${billingLevelsSetId}
	ORDER BY
		"MLFR"."meterid"
	`;
  return queryReadTasks;
}






module.exports = {
  generateReadTasksQuery,
  generateMetersReadingsFromReadTasksQuery
}