/**
 * This package holds helper classes to define a State-Machine inside an OpenEMS
 * Component.
 *
 * <p>
 * Finite State-Machines are a good, reliable and well-tested way of handling
 * the logic inside an OpenEMS Controller or inside OpenEMS Device
 * implementations.
 *
 * <p>
 * This package consists of three classes:
 *
 * <ul>
 * <li>StateMachine manages the different States
 * <li>State is typically implemented by an enum that represents the different
 * States
 * <li>StateHandler holds the actual code of a State
 * </ul>
 */
@org.osgi.annotation.versioning.Version("1.0.0")
@org.osgi.annotation.bundle.Export
package io.openems.edge.common.statemachine;
