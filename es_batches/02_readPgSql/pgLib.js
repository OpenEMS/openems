//
// ==================================================================
//

// postgres
const { Pool, Client } = require("pg");


//
// postgres client libs
//


// postgres
const pgCredentials = {
  user: "youpower",
  host: "10.192.1.26",
  database: "youpower-v0",
  password: "youpower2022",
  port: 5432,
};


// Connect with a client.
async function client(theQuery, params) {
  const client = new Client(pgCredentials);
  await client.connect();
  const rowsData = await (params ? client.query(theQuery, params) : client.query(theQuery));
  await client.end();

  return rowsData;
}
// Connect with a connection pool.
async function pool(theQuery) {
  const pool = new Pool(pgCredentials);
  const rowsData = await pool.query(theQuery);
  await pool.end();

  return rowsData;
}

// pool and client tests...
async function poolDemo() {
  return pool("SELECT NOW()");
}
async function clientDemo() {
  return client("SELECT NOW()");
}


module.exports = {
  client,
  pool,
  poolDemo,
  clientDemo,
  pgCredentials
}