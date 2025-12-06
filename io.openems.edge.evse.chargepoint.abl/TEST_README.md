# ABL EVSE Testing Infrastructure

Comprehensive testing framework for the ABL EVCC2/3 charging station component.

## Overview

This testing infrastructure provides **two complementary testing paths**:

1. **Unit Testing** - Fast, in-memory tests using dummy component
2. **Integration Testing** - Full Modbus TCP simulation for realistic testing

Both paths support:
- ✅ **Automated testing** via JUnit test suites
- ✅ **Manual testing** via REST API + HTML UI
- ✅ **Error injection** for robustness testing
- ✅ **State machine validation**
- ✅ **Current control verification**

---

## Testing Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Testing Infrastructure                    │
├──────────────────────────────┬──────────────────────────────┤
│      Unit Testing            │    Integration Testing        │
├──────────────────────────────┼──────────────────────────────┤
│  DummyAblChargePoint         │  AblModbusSimulator          │
│  (In-memory simulation)       │  (Full Modbus TCP server)    │
│                              │                              │
│  ├─ State machine            │  ├─ ModbusTcpServer          │
│  ├─ Channel simulation       │  ├─ RegisterMap              │
│  └─ Direct control           │  ├─ AblStateMachine          │
│                              │  └─ Real Modbus protocol     │
├──────────────────────────────┼──────────────────────────────┤
│  AblTestRestController       │  AblSimulatorRestApi         │
│  (REST API for dummy)        │  (REST API for simulator)    │
├──────────────────────────────┴──────────────────────────────┤
│            Unified HTML Test UI (abl-test.html)             │
│        (Single interface for both testing modes)            │
└─────────────────────────────────────────────────────────────┘
```

---

## Quick Start

### 1. Unit Testing (Fastest)

#### Automated Tests
```bash
# Run JUnit tests
./gradlew test --tests DummyAblChargePointTest
./gradlew test --tests ChargingStateTest
```

#### Manual Testing
```java
// Create dummy component
DummyAblChargePoint chargePoint = new DummyAblChargePoint("test0");

// Simulate EV connection
chargePoint.connectEv();

// Apply charging current
ChargePointAbilities abilities = chargePoint.getChargePointAbilities();
ChargePointActions actions = ChargePointActions.create(abilities)
    .setApplySetPoint(new ApplySetPoint.Action.MilliAmpere(16000))
    .build();
chargePoint.apply(actions);

// Verify state
assert chargePoint.getCurrentState() == ChargingState.C2;
```

---

### 2. Integration Testing (Most Realistic)

#### Start Modbus Simulator
```bash
# Standalone mode
java -cp ... io.openems.edge.evse.chargepoint.abl.simulator.AblModbusSimulator \
    127.0.0.1 502 1

# Or use REST API to start programmatically
AblSimulatorRestApi api = new AblSimulatorRestApi("127.0.0.1", 502, 1);
api.start();
```

#### Run Integration Tests
```bash
./gradlew test --tests AblModbusIntegrationTest
```

---

### 3. Manual Testing with Web UI

1. **Start OpenEMS Edge** (with REST controller activated)
2. **Open browser**: `http://localhost:8080/test-ui/abl-test.html`
3. **Choose testing mode**:
   - **Unit Test** tab: Control dummy component
   - **Integration Test** tab: Control Modbus simulator

---

## Directory Structure

```
io.openems.edge.evse.chargepoint.abl/
├── src/                          # Main component source
│   └── io/openems/edge/evse/chargepoint/abl/
│       ├── Config.java
│       ├── EvseChargePointAbl.java
│       ├── EvseChargePointAblImpl.java
│       └── enums/
│           ├── ChargingState.java
│           └── Status.java
│
├── test/                         # Unit tests and test infrastructure
│   └── io/openems/edge/evse/chargepoint/abl/
│       ├── ChargingStateTest.java              # Enum tests
│       ├── DummyAblChargePoint.java            # Dummy component
│       ├── DummyAblChargePointTest.java        # Dummy tests
│       ├── AblModbusIntegrationTest.java       # Integration tests
│       │
│       ├── rest/
│       │   └── AblTestRestController.java      # REST API for dummy
│       │
│       └── simulator/                           # Modbus simulator
│           ├── AblModbusSimulator.java         # Main simulator
│           ├── AblStateMachine.java            # State logic
│           ├── RegisterMap.java                # Virtual registers
│           ├── ModbusTcpServer.java            # Modbus TCP server
│           └── AblSimulatorRestApi.java        # REST API for simulator
│
├── test-ui/                      # Manual testing UI
│   └── abl-test.html            # Web interface
│
├── readme.md                     # Component documentation
└── TEST_README.md               # This file
```

---

## Testing Paths Explained

### Path 1: Unit Testing (Dummy Component)

**Purpose**: Fast iteration, logic verification, CI/CD
**Speed**: ⚡⚡⚡ Very fast (milliseconds)
**Realism**: ⭐⭐ Medium (no Modbus communication)

**What it tests:**
- ✅ State machine logic
- ✅ Channel mappings
- ✅ Power calculations
- ✅ Ready-for-charging evaluation
- ✅ Error state handling

**What it doesn't test:**
- ❌ Modbus communication
- ❌ Register encoding/decoding
- ❌ Network issues
- ❌ Timing/concurrency

**Use when:**
- Developing new features
- Quick regression testing
- CI/CD pipeline
- TDD (Test-Driven Development)

---

### Path 2: Integration Testing (Modbus Simulator)

**Purpose**: End-to-end verification, realistic behavior
**Speed**: ⚡⚡ Fast (sub-second)
**Realism**: ⭐⭐⭐ High (full Modbus stack)

**What it tests:**
- ✅ Everything from unit testing, PLUS:
- ✅ Modbus TCP communication
- ✅ Register read/write operations
- ✅ Data encoding/decoding
- ✅ Communication timeouts
- ✅ Real component integration

**Use when:**
- Final verification before deployment
- Testing Modbus protocol correctness
- Debugging communication issues
- Acceptance testing

---

## Test Scenarios

### Scenario 1: Normal Charging Cycle

```java
@Test
public void testNormalChargingCycle() {
    // 1. Initial state
    assertEquals(ChargingState.A1, chargePoint.getCurrentState());

    // 2. EV connects
    chargePoint.connectEv();
    assertEquals(ChargingState.B1, chargePoint.getCurrentState());

    // 3. Start charging (16A)
    ChargePointActions actions = ChargePointActions.create(abilities)
        .setApplySetPoint(new ApplySetPoint.Action.MilliAmpere(16000))
        .build();
    chargePoint.apply(actions);
    assertEquals(ChargingState.C2, chargePoint.getCurrentState());

    // 4. Stop charging
    actions = ChargePointActions.create(abilities)
        .setApplySetPoint(new ApplySetPoint.Action.MilliAmpere(0))
        .build();
    chargePoint.apply(actions);
    assertEquals(ChargingState.B2, chargePoint.getCurrentState());

    // 5. EV disconnects
    chargePoint.disconnectEv();
    assertEquals(ChargingState.A1, chargePoint.getCurrentState());
}
```

### Scenario 2: Overcurrent Error

```java
@Test
public void testOvercurrentError() {
    chargePoint.connectEv();
    chargePoint.apply(chargingActions);

    // Inject overcurrent error
    chargePoint.injectError(ChargingState.F9);
    assertEquals(ChargingState.F9, chargePoint.getCurrentState());

    // Verify charging stopped
    assertEquals(0, chargePoint.getPhaseCurrentL1());
}
```

### Scenario 3: Communication Timeout (Simulator only)

```java
@Test
public void testCommunicationTimeout() throws InterruptedException {
    simulator.connectEv();
    simulator.getRegisterMap().setIcmaxSetpoint(0x00A6);
    simulator.getStateMachine().onCurrentSetpointChanged(0x00A6);

    // Wait for timeout (15 seconds)
    Thread.sleep(16000);

    // Verify F4 state
    assertEquals(ChargingState.F4, simulator.getCurrentState());

    // Send command to recover
    simulator.getStateMachine().onCurrentSetpointChanged(0x00A6);

    // Verify recovery
    assertEquals(ChargingState.E2, simulator.getCurrentState());
}
```

---

## Manual Testing Guide

### Using the HTML UI

1. **Open UI**: Navigate to `http://localhost:8080/test-ui/abl-test.html`

2. **Select Test Mode**:
   - Click "Unit Test (Dummy)" for fast in-memory testing
   - Click "Integration Test (Modbus Simulator)" for realistic testing

3. **Monitor Status**: The UI auto-refreshes every 2 seconds showing:
   - Current state
   - EV connection status
   - Charging setpoint
   - Active power

4. **Control EV**:
   - Click "Connect EV" to simulate plug-in
   - Click "Disconnect EV" to simulate unplug

5. **Control Charging**:
   - Enter desired current in mA (e.g., 16000)
   - Click "Set Current"
   - Watch state transition to C2 (charging)

6. **Test Error Handling**:
   - Select error type from dropdown
   - Click "Inject Error"
   - Watch state change to F-state
   - Click "Clear Error" (simulator only) to recover

---

## REST API Reference

### Dummy Component API

Base URL: `/rest/abl/test`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/status` | GET | Get current status |
| `/ev/connect` | POST | Simulate EV connection |
| `/ev/disconnect` | POST | Simulate EV disconnection |
| `/current/{mA}` | POST | Set charging current |
| `/state/{hex}` | POST | Force state change |
| `/error/{hex}` | POST | Inject error state |
| `/currents?l1={A}&l2={A}&l3={A}` | POST | Set phase currents |
| `/reset` | POST | Reset to initial state |

**Example**:
```bash
# Get status
curl http://localhost:8080/rest/abl/test/status

# Connect EV
curl -X POST http://localhost:8080/rest/abl/test/ev/connect

# Set 16A charging
curl -X POST http://localhost:8080/rest/abl/test/current/16000

# Inject overcurrent error
curl -X POST http://localhost:8080/rest/abl/test/error/F9
```

---

### Modbus Simulator API

Base URL: `/rest/abl/simulator`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/status` | GET | Get simulator status |
| `/start` | POST | Start simulator |
| `/stop` | POST | Stop simulator |
| `/ev/connect` | POST | Simulate EV connection |
| `/ev/disconnect` | POST | Simulate EV disconnection |
| `/state/{hex}` | POST | Force state change |
| `/error/inject?code={hex}&recovery={sec}` | POST | Inject error with auto-recovery |
| `/error/clear` | POST | Clear error |
| `/currents?l1={A}&l2={A}&l3={A}` | POST | Set phase currents |

**Example**:
```bash
# Start simulator
curl -X POST http://localhost:8080/rest/abl/simulator/start

# Get status
curl http://localhost:8080/rest/abl/simulator/status

# Connect EV
curl -X POST http://localhost:8080/rest/abl/simulator/ev/connect

# Inject overcurrent with 10s auto-recovery
curl -X POST "http://localhost:8080/rest/abl/simulator/error/inject?code=F9&recovery=10"
```

---

## Running Simulator Standalone

For testing without OpenEMS:

```bash
# Compile
javac -cp <classpath> io/openems/edge/evse/chargepoint/abl/simulator/*.java

# Run
java -cp <classpath> io.openems.edge.evse.chargepoint.abl.simulator.AblModbusSimulator \
    127.0.0.1 502 1

# Output:
[ABL Modbus Simulator] Starting server on 127.0.0.1:502 (Unit ID: 1)
==============================================
ABL EVCC2/3 Modbus Simulator
==============================================
Listening on: 127.0.0.1:502
Device ID: 1
Firmware: 1.2

Press Ctrl+C to stop...
```

Then connect with Modbus client:
```bash
# Read device info (registers 0x0001-0x0002)
mbpoll -a 1 -r 1 -c 2 -t 3 127.0.0.1

# Read state and currents (registers 0x0033-0x0035)
mbpoll -a 1 -r 51 -c 3 -t 3 127.0.0.1

# Write current setpoint (register 0x0014, 16A ≈ 166)
mbpoll -a 1 -r 20 -t 3 127.0.0.1 166
```

---

## Error Injection Guide

### Available Error States

| Code | State | Description | Auto-Recovery |
|------|-------|-------------|---------------|
| F1 | Welding | Unintended closed contact | Manual |
| F2 | Internal | Internal error | After 5s |
| F3 | DC Current | DC residual current | Manual |
| F4 | Timeout | Communication timeout | On next write |
| F5 | Lock Failed | Socket lock failure | Retry |
| F9 | Overcurrent | Current > threshold | After 10s |
| F10 | Temperature | Temperature outside limits | When cool |

### Injection Examples

```java
// Dummy component - no auto-recovery
chargePoint.injectError(ChargingState.F9);

// Simulator - with 10-second auto-recovery
simulator.injectError(ChargingState.F9, Duration.ofSeconds(10));

// Simulator - no auto-recovery
simulator.injectError(ChargingState.F1, null);

// Manual recovery
simulator.clearError();
```

---

## Best Practices

### For Development

1. **Start with unit tests** - Fast iteration
2. **Use dummy component** for TDD
3. **Add integration tests** for critical paths
4. **Use HTML UI** for visual debugging

### For CI/CD

1. **Run unit tests** on every commit
2. **Run integration tests** before merge
3. **Use headless mode** (no UI)
4. **Collect test coverage**

### For Debugging

1. **Enable debug mode** in Config
2. **Use HTML UI** for real-time monitoring
3. **Check Modbus traffic** with Wireshark
4. **Use simulator logs** for state transitions

---

## Troubleshooting

### Dummy Component Issues

**Problem**: State doesn't change
**Solution**: Check if read-only mode is enabled

**Problem**: Currents not set
**Solution**: Call `setPhaseCurrents()` manually or use `apply()` with setpoint > 0

### Simulator Issues

**Problem**: Simulator won't start
**Solution**: Check if port 502 is already in use, try different port

**Problem**: No Modbus response
**Solution**: Verify firewall settings, check IP address binding

**Problem**: State transitions don't work
**Solution**: Check logs for state machine output, verify setpoint value

### REST API Issues

**Problem**: 404 Not Found
**Solution**: Verify REST controller is activated in OSGi

**Problem**: CORS errors in browser
**Solution**: Configure CORS headers in OpenEMS REST layer

---

## Advanced Topics

### Custom Test Scenarios

Create reusable scenarios:

```java
public class TestScenarios {
    public static void fastCharging(DummyAblChargePoint cp) {
        cp.connectEv();
        cp.apply(actions(32000)); // 32A
    }

    public static void loadImbalance(AblModbusSimulator sim) {
        sim.connectEv();
        sim.setPhaseCurrents(25, 10, 8); // Unbalanced
        sim.forceState(ChargingState.C4);
    }
}
```

### Performance Testing

```java
@Test
public void testHighFrequencyUpdates() {
    for (int i = 0; i < 1000; i++) {
        chargePoint.apply(actions(6000 + i * 26)); // 6-32A sweep
        assertEquals(ChargingState.C2, chargePoint.getCurrentState());
    }
}
```

### Concurrent Testing

```java
@Test
public void testConcurrentAccess() throws InterruptedException {
    ExecutorService executor = Executors.newFixedThreadPool(10);
    for (int i = 0; i < 100; i++) {
        executor.submit(() -> {
            chargePoint.apply(actions(16000));
            assert chargePoint.getCurrentState() != ChargingState.UNDEFINED;
        });
    }
    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);
}
```

---

## Summary

### When to Use What

| Scenario | Use Dummy | Use Simulator |
|----------|-----------|---------------|
| Quick logic test | ✅ | ❌ |
| Modbus protocol test | ❌ | ✅ |
| CI/CD pipeline | ✅ | ✅ |
| Manual debugging | ✅ | ✅ |
| Integration with OpenEMS | ❌ | ✅ |
| Before hardware deployment | ❌ | ✅ |

### Test Coverage Goals

- ✅ All state transitions
- ✅ All error states
- ✅ Current range (6-32A)
- ✅ Phase current calculations
- ✅ Power calculations
- ✅ Ready-for-charging logic
- ✅ Modbus register mappings
- ✅ Communication timeout handling

---

**Happy Testing!** 🎉

For questions or issues, check the main [readme.md](readme.md) or open an issue.
