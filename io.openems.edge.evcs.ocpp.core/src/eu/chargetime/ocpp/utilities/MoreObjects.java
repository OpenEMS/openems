package eu.chargetime.ocpp.utilities;

/*
 * Copyright (C) 2014 The Guava Authors
 *
 * Modified by Evgeny Pakhomov <eugene.pakhomov@ubitricity.com>
 *
 * Changes:
 *  * Cut Guava specific annotations
 *  * ToStringHelper renamed to ToStringHelperImpl
 *  * Instead of original ToStringHelper new adapter implementation is used which provides few additional functionalities (output secrets in the masked form, output only size for collections)
 *  * References to Guava versions in methods JavaDoc are cut as it won't be relevant
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * This class consists of {@code static} utility methods for operating on
 * objects. These utilities include {@code null}-safe or {@code null}-tolerant
 * methods for computing the hash code of an object, returning a string for an
 * object, comparing two objects, etc.
 *
 * <p>
 * Cut and modified version from Guava (needed to minimize dependency tree for
 * client).
 *
 * @author <a href=mailto:eugene.pakhomov@ubitricity.com>Eugene Pakhomov</a>
 */
public final class MoreObjects {

	/** Utility classes should have no instances. */
	private MoreObjects() {
		throw new AssertionError("No Objects instances should exist");
	}

	/**
	 * Returns {@code true} if the arguments are equal to each other and
	 * {@code false} otherwise. Consequently, if both arguments are {@code null},
	 * {@code true} is returned and if exactly one argument is {@code null},
	 * {@code false} is returned. Otherwise, equality is determined by using the
	 * {@link Object#equals equals} method of the first argument.
	 *
	 * @param a an object
	 * @param b an object to be compared with {@code a} for equality
	 * @return {@code true} if the arguments are equal to each other and
	 *         {@code false} otherwise
	 * @see Object#equals(Object)
	 */
	public static boolean equals(Object a, Object b) {
		return (a == b) || (a != null && a.equals(b));
	}

	/**
	 * Returns {@code true} if the arguments are deeply equal to each other and
	 * {@code false} otherwise.
	 *
	 * <p>
	 * Two {@code null} values are deeply equal. If both arguments are arrays, the
	 * algorithm in {@link Arrays#deepEquals(Object[], Object[]) Arrays.deepEquals}
	 * is used to determine equality. Otherwise, equality is determined by using the
	 * {@link Object#equals equals} method of the first argument.
	 *
	 * @param a an object
	 * @param b an object to be compared with {@code a} for deep equality
	 * @return {@code true} if the arguments are deeply equal to each other and
	 *         {@code false} otherwise
	 * @see Arrays#deepEquals(Object[], Object[])
	 * @see MoreObjects#equals(Object, Object)
	 */
	public static boolean deepEquals(Object a, Object b) {
		if (a == b) {
			return true;
		} else if (a == null || b == null) {
			return false;
		} else {
			return Arrays.deepEquals(new Object[] { a }, new Object[] { b });
		}
	}

	/**
	 * Returns the hash code of a non-{@code null} argument and 0 for a {@code null}
	 * argument.
	 *
	 * @param o an object
	 * @return the hash code of a non-{@code null} argument and 0 for a {@code null}
	 *         argument
	 * @see Object#hashCode
	 */
	public static int hashCode(Object o) {
		return o != null ? o.hashCode() : 0;
	}

	/**
	 * Generates a hash code for a sequence of input values. The hash code is
	 * generated as if all the input values were placed into an array, and that
	 * array were hashed by calling {@link Arrays#hashCode(Object[])}.
	 *
	 * <p>
	 * This method is useful for implementing {@link Object#hashCode()} on objects
	 * containing multiple fields. For example, if an object that has three fields,
	 * {@code x}, {@code y}, and {@code z}, one could write:
	 *
	 * <blockquote>
	 *
	 * <pre>
	 * &#064;Override
	 * public int hashCode() {
	 * 	return Objects.hash(x, y, z);
	 * }
	 * </pre>
	 *
	 * </blockquote>
	 *
	 * <b>Warning: When a single object reference is supplied, the returned value
	 * does not equal the hash code of that object reference.</b> This value can be
	 * computed by calling {@link #hashCode(Object)}.
	 *
	 * @param values the values to be hashed
	 * @return a hash value of the sequence of input values
	 * @see Arrays#hashCode(Object[])
	 */
	public static int hash(Object... values) {
		return Arrays.hashCode(values);
	}

	/**
	 * Creates shallow copy of input array. Returns null in case when null is passed
	 * as argument.
	 *
	 * @param array the array to be copied
	 * @param <T>   type of passed array elements
	 * @return copy of input array or null in case when null is passed as argument
	 */
	public static <T> T[] clone(T[] array) {
		return array == null ? null : Arrays.copyOf(array, array.length);
	}

	/**
	 * Creates shallow copy of input array. Returns null in case when null is passed
	 * as argument.
	 *
	 * @param array the array to be copied
	 * @return copy of input array or null in case when null is passed as argument
	 */
	public static byte[] clone(byte[] array) {
		return array == null ? null : Arrays.copyOf(array, array.length);
	}

	/**
	 * Returns helper to generate string representation of given input object.
	 *
	 * @param self the object to be represented as string
	 * @return helper to generate string representation of given input object
	 */
	public static ToStringHelper toStringHelper(Object self) {
		return new ToStringHelper(self);
	}

	/**
	 * Returns helper to generate string representation of given input object.
	 *
	 * @param self              the object to be represented as string
	 * @param outputFullDetails the flag to be set to output all elements of
	 *                          container (list, set, queue, map) or array of
	 *                          objects
	 * @return helper to generate string representation of given input object
	 */
	public static ToStringHelper toStringHelper(Object self, boolean outputFullDetails) {
		return new ToStringHelper(self, outputFullDetails);
	}

	/**
	 * Returns helper to generate string representation of given input class.
	 *
	 * @param clazz the class to be represented as string
	 * @return helper to generate string representation of given input class
	 */
	public static ToStringHelper toStringHelper(Class<?> clazz) {
		return new ToStringHelper(clazz);
	}

	/**
	 * Returns helper to generate string representation of given input class.
	 *
	 * @param clazz             the class to be represented as string
	 * @param outputFullDetails the flag to be set to output all elements of
	 *                          container (list, set, queue, map) or array of
	 *                          objects
	 * @return helper to generate string representation of given input class
	 */
	public static ToStringHelper toStringHelper(Class<?> clazz, boolean outputFullDetails) {
		return new ToStringHelper(clazz, outputFullDetails);
	}

	/**
	 * Returns helper to generate string representation of class with given
	 * className.
	 *
	 * @param className the name of class to be represented as string
	 * @return helper to generate string representation of class with given
	 *         className
	 */
	public static ToStringHelper toStringHelper(String className) {
		return new ToStringHelper(className);
	}

	/**
	 * Returns helper to generate string representation of class with given
	 * className.
	 *
	 * @param className         the name of class to be represented as string
	 * @param outputFullDetails the flag to be set to output all elements of
	 *                          container (list, set, queue, map) or array of
	 *                          objects
	 * @return helper to generate string representation of class with given
	 *         className
	 */
	public static ToStringHelper toStringHelper(String className, boolean outputFullDetails) {
		return new ToStringHelper(className, outputFullDetails);
	}

	/**
	 * Simple decorator to encapsulate actual toString helper implementation. If
	 * array of primitives passed as input parameter to {@link ToStringHelper#add}
	 * function when if array length more than
	 * {@link ToStringHelper#MAXIMUM_ARRAY_SIZE_TO_OUTPUT_DETAILS} then only length
	 * of that array will be written in output. If any container (list, set, queue,
	 * map) or array of objects passed as input parameter to
	 * {@link ToStringHelper#add} function then only size of that container (array
	 * of objects) will be written in output (this behaviour might be changed with
	 * {@link #outputFullDetails} constructor argument).
	 */
	public static final class ToStringHelper {

		public static final int MAXIMUM_ARRAY_SIZE_TO_OUTPUT_DETAILS = 32;
		public static final String FIELD_NAME_LENGTH_POSTFIX = ".length";
		public static final String FIELD_NAME_SIZE_POSTFIX = ".size";
		public static final String SECURE_FIELD_VALUE_REPLACEMENT = "********";

		private final boolean outputFullDetails;
		private final ToStringHelperImpl helperImplementation;

		private ToStringHelper(ToStringHelperImpl helperImplementation, boolean outputFullDetails) {
			this.helperImplementation = helperImplementation;
			this.outputFullDetails = outputFullDetails;
		}

		private ToStringHelper(Object self) {
			this(toStringHelper(self), false);
		}

		private ToStringHelper(Class<?> clazz) {
			this(toStringHelper(clazz), false);
		}

		private ToStringHelper(String className) {
			this(toStringHelper(className), false);
		}

		private ToStringHelper(Object self, boolean outputFullDetails) {
			this(toStringHelper(self), outputFullDetails);
		}

		private ToStringHelper(Class<?> clazz, boolean outputFullDetails) {
			this(toStringHelper(clazz), outputFullDetails);
		}

		private ToStringHelper(String className, boolean outputFullDetails) {
			this(toStringHelper(className), outputFullDetails);
		}

		/**
		 * Exclude from output fields with null value.
		 *
		 * @return ToStringHelper instance
		 */
		public ToStringHelper omitNullValues() {
			helperImplementation.omitNullValues();
			return this;
		}

		/**
		 * Add field name and value to output. It's safe to pass null as value.
		 *
		 * @param name  field name
		 * @param value field value
		 * @return ToStringHelper instance
		 */
		public ToStringHelper add(String name, Object value) {
			helperImplementation.add(name, value);
			return this;
		}

		/**
		 * Add field name and value to output. It's safe to pass null as value.
		 *
		 * @param name  field name
		 * @param value field value
		 * @return ToStringHelper instance
		 */
		public ToStringHelper add(String name, Calendar value) {
			helperImplementation.add(name, value);
			return this;
		}

		/**
		 * Add field name and value to output. It's safe to pass null as value.
		 *
		 * @param name  field name
		 * @param value field value
		 * @return ToStringHelper instance
		 */
		public ToStringHelper add(String name, boolean value) {
			helperImplementation.add(name, String.valueOf(value));
			return this;
		}

		/**
		 * Add field name and value to output. It's safe to pass null as value.
		 *
		 * @param name  field name
		 * @param value field value
		 * @return ToStringHelper instance
		 */
		public ToStringHelper add(String name, char value) {
			helperImplementation.add(name, String.valueOf(value));
			return this;
		}

		/**
		 * Add field name and value to output. It's safe to pass null as value.
		 *
		 * @param name  field name
		 * @param value field value
		 * @return ToStringHelper instance
		 */
		public ToStringHelper add(String name, double value) {
			helperImplementation.add(name, String.valueOf(value));
			return this;
		}

		/**
		 * Add field name and value to output. It's safe to pass null as value.
		 *
		 * @param name  field name
		 * @param value field value
		 * @return ToStringHelper instance
		 */
		public ToStringHelper add(String name, float value) {
			helperImplementation.add(name, String.valueOf(value));
			return this;
		}

		/**
		 * Add field name and value to output. It's safe to pass null as value.
		 *
		 * @param name  field name
		 * @param value field value
		 * @return ToStringHelper instance
		 */
		public ToStringHelper add(String name, int value) {
			helperImplementation.add(name, String.valueOf(value));
			return this;
		}

		/**
		 * Add field name and value to output. It's safe to pass null as value.
		 *
		 * @param name  field name
		 * @param value field value
		 * @return ToStringHelper instance
		 */
		public ToStringHelper add(String name, long value) {
			helperImplementation.add(name, String.valueOf(value));
			return this;
		}

		/**
		 * Add field name and value to output. It's safe to pass null as value.
		 *
		 * @param name  field name
		 * @param value field value
		 * @return ToStringHelper instance
		 */
		public ToStringHelper add(String name, List<?> value) {
			return addCollection(name, value);
		}

		/**
		 * Add field name and value to output. It's safe to pass null as value.
		 *
		 * @param name  field name
		 * @param value field value
		 * @return ToStringHelper instance
		 */
		public ToStringHelper add(String name, Set<?> value) {
			return addCollection(name, value);
		}

		/**
		 * Add field name and value to output. It's safe to pass null as value.
		 *
		 * @param name  field name
		 * @param value field value
		 * @return ToStringHelper instance
		 */
		public ToStringHelper add(String name, Map<?, ?> value) {
			return addMap(name, value);
		}

		/**
		 * Add field name and value to output. It's safe to pass null as value.
		 *
		 * @param name  field name
		 * @param value field value
		 * @return ToStringHelper instance
		 */
		public ToStringHelper add(String name, Queue<?> value) {
			return addCollection(name, value);
		}

		/**
		 * Add field name and value to output. It's safe to pass null as value.
		 *
		 * @param name  field name
		 * @param value field value
		 * @param <T>   type of passed array elements
		 * @return ToStringHelper instance
		 */
		public <T> ToStringHelper add(String name, T[] value) {
			if (value != null && !outputFullDetails) {
				helperImplementation.add(name + FIELD_NAME_LENGTH_POSTFIX, value.length);
			} else {
				helperImplementation.add(name, Arrays.toString(value));
			}
			return this;
		}

		/**
		 * Add field name and value to output. It's safe to pass null as value.
		 *
		 * @param name  field name
		 * @param value field value
		 * @return ToStringHelper instance
		 */
		public ToStringHelper add(String name, byte[] value) {
			if (value != null && value.length > MAXIMUM_ARRAY_SIZE_TO_OUTPUT_DETAILS && !outputFullDetails) {
				helperImplementation.add(name + FIELD_NAME_LENGTH_POSTFIX, value.length);
			} else {
				helperImplementation.add(name, Arrays.toString(value));
			}
			return this;
		}

		/**
		 * Add field name and value to output. It's safe to pass null as value.
		 *
		 * @param name  field name
		 * @param value field value
		 * @return ToStringHelper instance
		 */
		public ToStringHelper add(String name, boolean[] value) {
			if (value != null && value.length > MAXIMUM_ARRAY_SIZE_TO_OUTPUT_DETAILS && !outputFullDetails) {
				helperImplementation.add(name + FIELD_NAME_LENGTH_POSTFIX, value.length);
			} else {
				helperImplementation.add(name, Arrays.toString(value));
			}
			return this;
		}

		/**
		 * Add field name and value to output. It's safe to pass null as value.
		 *
		 * @param name  field name
		 * @param value field value
		 * @return ToStringHelper instance
		 */
		public ToStringHelper add(String name, char[] value) {
			if (value != null && value.length > MAXIMUM_ARRAY_SIZE_TO_OUTPUT_DETAILS && !outputFullDetails) {
				helperImplementation.add(name + FIELD_NAME_LENGTH_POSTFIX, value.length);
			} else {
				helperImplementation.add(name, Arrays.toString(value));
			}
			return this;
		}

		/**
		 * Add field name and value to output. It's safe to pass null as value.
		 *
		 * @param name  field name
		 * @param value field value
		 * @return ToStringHelper instance
		 */
		public ToStringHelper add(String name, double[] value) {
			if (value != null && value.length > MAXIMUM_ARRAY_SIZE_TO_OUTPUT_DETAILS && !outputFullDetails) {
				helperImplementation.add(name + FIELD_NAME_LENGTH_POSTFIX, value.length);
			} else {
				helperImplementation.add(name, Arrays.toString(value));
			}
			return this;
		}

		/**
		 * Add field name and value to output. It's safe to pass null as value.
		 *
		 * @param name  field name
		 * @param value field value
		 * @return ToStringHelper instance
		 */
		public ToStringHelper add(String name, float[] value) {
			if (value != null && value.length > MAXIMUM_ARRAY_SIZE_TO_OUTPUT_DETAILS && !outputFullDetails) {
				helperImplementation.add(name + FIELD_NAME_LENGTH_POSTFIX, value.length);
			} else {
				helperImplementation.add(name, Arrays.toString(value));
			}
			return this;
		}

		/**
		 * Add field name and value to output. It's safe to pass null as value.
		 *
		 * @param name  field name
		 * @param value field value
		 * @return ToStringHelper instance
		 */
		public ToStringHelper add(String name, int[] value) {
			if (value != null && value.length > MAXIMUM_ARRAY_SIZE_TO_OUTPUT_DETAILS && !outputFullDetails) {
				helperImplementation.add(name + FIELD_NAME_LENGTH_POSTFIX, value.length);
			} else {
				helperImplementation.add(name, Arrays.toString(value));
			}
			return this;
		}

		/**
		 * Add field name and value to output. It's safe to pass null as value.
		 *
		 * @param name  field name
		 * @param value field value
		 * @return ToStringHelper instance
		 */
		public ToStringHelper add(String name, long[] value) {
			if (value != null && value.length > MAXIMUM_ARRAY_SIZE_TO_OUTPUT_DETAILS && !outputFullDetails) {
				helperImplementation.add(name + FIELD_NAME_LENGTH_POSTFIX, value.length);
			} else {
				helperImplementation.add(name, Arrays.toString(value));
			}
			return this;
		}

		private ToStringHelper addCollection(String name, Collection<?> value) {
			if (value != null && !outputFullDetails) {
				helperImplementation.add(name + FIELD_NAME_SIZE_POSTFIX, value.size());
			} else {
				helperImplementation.add(name, value);
			}
			return this;
		}

		private ToStringHelper addMap(String name, Map<?, ?> value) {
			if (value != null && !outputFullDetails) {
				helperImplementation.add(name + FIELD_NAME_SIZE_POSTFIX, value.size());
			} else {
				helperImplementation.add(name, value);
			}
			return this;
		}

		/**
		 * Add field name and mask instead of real value to output. It's safe to pass
		 * null as value.
		 *
		 * @param name  field name
		 * @param value field value
		 * @return ToStringHelper instance
		 */
		public ToStringHelper addSecure(String name, String value) {
			value = SECURE_FIELD_VALUE_REPLACEMENT;
			helperImplementation.add(name, value);
			return this;
		}

		/**
		 * Add value to output.
		 *
		 * @param value to add to output
		 * @return ToStringHelper instance
		 */
		public ToStringHelper addValue(Object value) {
			helperImplementation.addValue(value);
			return this;
		}

		/**
		 * Add value to output.
		 *
		 * @param value to add to output
		 * @return ToStringHelper instance
		 */
		public ToStringHelper addValue(boolean value) {
			helperImplementation.addValue(String.valueOf(value));
			return this;
		}

		/**
		 * Add value to output.
		 *
		 * @param value to add to output
		 * @return ToStringHelper instance
		 */
		public ToStringHelper addValue(char value) {
			helperImplementation.addValue(String.valueOf(value));
			return this;
		}

		/**
		 * Add value to output.
		 *
		 * @param value to add to output
		 * @return ToStringHelper instance
		 */
		public ToStringHelper addValue(double value) {
			helperImplementation.addValue(String.valueOf(value));
			return this;
		}

		/**
		 * Add value to output.
		 *
		 * @param value to add to output
		 * @return ToStringHelper instance
		 */
		public ToStringHelper addValue(float value) {
			helperImplementation.addValue(String.valueOf(value));
			return this;
		}

		/**
		 * Add value to output.
		 *
		 * @param value to add to output
		 * @return ToStringHelper instance
		 */
		public ToStringHelper addValue(int value) {
			helperImplementation.addValue(String.valueOf(value));
			return this;
		}

		/**
		 * Add value to output.
		 *
		 * @param value to add to output
		 * @return ToStringHelper instance
		 */
		public ToStringHelper addValue(long value) {
			helperImplementation.addValue(String.valueOf(value));
			return this;
		}

		/**
		 * Returns resulting output string.
		 *
		 * @return resulting output string
		 */
		@Override
		public String toString() {
			return helperImplementation.toString();
		}

		/**
		 * Creates an instance of {@link ToStringHelperImpl}.
		 *
		 * @param self the object to generate the string for (typically {@code this}),
		 *             used only for its class name.
		 * @return ToStringHelperImpl
		 */
		static ToStringHelperImpl toStringHelper(Object self) {
			return new ToStringHelperImpl(self.getClass().getSimpleName());
		}

		/**
		 * Creates an instance of {@link ToStringHelperImpl} in the same manner as
		 * {@link #toStringHelper(Object)}, but using the simple name of {@code clazz}
		 * instead of using an instance's {@link Object#getClass()}.
		 *
		 * @param clazz the {@link Class} of the instance
		 * @return ToStringHelperImpl
		 */
		static ToStringHelperImpl toStringHelper(Class<?> clazz) {
			return new ToStringHelperImpl(clazz.getSimpleName());
		}

		/**
		 * Creates an instance of {@link ToStringHelperImpl} in the same manner as
		 * {@link #toStringHelper(Object)}, but using {@code className} instead of using
		 * an instance's {@link Object#getClass()}.
		 *
		 * @param className the name of the instance type
		 * @return ToStringHelperImpl
		 */
		public static ToStringHelperImpl toStringHelper(String className) {
			return new ToStringHelperImpl(className);
		}
	}

	/**
	 * Support class for {@link MoreObjects#toStringHelper}.
	 *
	 * @author Jason Lee
	 */
	public static final class ToStringHelperImpl {
		private final String className;
		private final ValueHolder holderHead = new ValueHolder();
		private ValueHolder holderTail = holderHead;
		private boolean omitNullValues = false;

		private ToStringHelperImpl(String className) {
			this.className = className;
		}

		/**
		 * Configures the {@link ToStringHelperImpl} so {@link #toString()} will ignore
		 * properties with null value. The order of calling this method, relative to the
		 * {@code add()}/{@code
		 * addValue()} methods, is not significant.
		 * 
		 * @return ToStringHelperImpl
		 */
		public ToStringHelperImpl omitNullValues() {
			omitNullValues = true;
			return this;
		}

		/**
		 * Adds a name/value pair to the formatted output in {@code name=value} format.
		 * If {@code value} is {@code null}, the string {@code "null"} is used, unless
		 * {@link #omitNullValues()} is called, in which case this name/value pair will
		 * not be added.
		 * 
		 * @param name  name.
		 * @param value value.
		 * @return ToStringHelperImpl
		 */
		public ToStringHelperImpl add(String name, Object value) {
			return addHolder(name, value);
		}

		/**
		 * Adds a name/value pair to the formatted output in {@code name=value} format.
		 * 
		 * @param name  name.
		 * @param value value.
		 * @return ToStringHelperImpl
		 */
		public ToStringHelperImpl add(String name, Calendar value) {
			return addHolder(name, value);
		}

		/**
		 * Adds a name/value pair to the formatted output in {@code name=value} format.
		 * 
		 * @param name  name.
		 * @param value value.
		 * @return ToStringHelperImpl
		 */
		public ToStringHelperImpl add(String name, boolean value) {
			return addHolder(name, String.valueOf(value));
		}

		/**
		 * Adds a name/value pair to the formatted output in {@code name=value} format.
		 * 
		 * @param name  name.
		 * @param value value.
		 * @return ToStringHelperImpl
		 */
		public ToStringHelperImpl add(String name, char value) {
			return addHolder(name, String.valueOf(value));
		}

		/**
		 * Adds a name/value pair to the formatted output in {@code name=value} format.
		 * 
		 * @param name  name.
		 * @param value value.
		 * @return ToStringHelperImpl
		 */
		public ToStringHelperImpl add(String name, double value) {
			return addHolder(name, String.valueOf(value));
		}

		/**
		 * Adds a name/value pair to the formatted output in {@code name=value} format.
		 * 
		 * @param name  name.
		 * @param value value.
		 * @return ToStringHelperImpl
		 */
		public ToStringHelperImpl add(String name, float value) {
			return addHolder(name, String.valueOf(value));
		}

		/**
		 * Adds a name/value pair to the formatted output in {@code name=value} format.
		 * 
		 * @param name  name.
		 * @param value value.
		 * @return ToStringHelperImpl
		 */
		public ToStringHelperImpl add(String name, int value) {
			return addHolder(name, String.valueOf(value));
		}

		/**
		 * Adds a name/value pair to the formatted output in {@code name=value} format.
		 * 
		 * @param name  name.
		 * @param value value.
		 * @return ToStringHelperImpl
		 */
		public ToStringHelperImpl add(String name, long value) {
			return addHolder(name, String.valueOf(value));
		}

		/**
		 * Adds an unnamed value to the formatted output.
		 *
		 * <p>
		 * It is strongly encouraged to use {@link #add(String, Object)} instead and
		 * give value a readable name.
		 * 
		 * @param value value.
		 * @return ToStringHelperImpl
		 */
		public ToStringHelperImpl addValue(Object value) {
			return addHolder(value);
		}

		/**
		 * Adds an unnamed value to the formatted output.
		 *
		 * <p>
		 * It is strongly encouraged to use {@link #add(String, boolean)} instead and
		 * give value a readable name.
		 * 
		 * @param value value.
		 * @return ToStringHelperImpl
		 */
		public ToStringHelperImpl addValue(boolean value) {
			return addHolder(String.valueOf(value));
		}

		/**
		 * Adds an unnamed value to the formatted output.
		 *
		 * <p>
		 * It is strongly encouraged to use {@link #add(String, char)} instead and give
		 * value a readable name.
		 * 
		 * @param value value.
		 * @return ToStringHelperImpl
		 */
		public ToStringHelperImpl addValue(char value) {
			return addHolder(String.valueOf(value));
		}

		/**
		 * Adds an unnamed value to the formatted output.
		 *
		 * <p>
		 * It is strongly encouraged to use {@link #add(String, double)} instead and
		 * give value a readable name.
		 * 
		 * @param value value.
		 * @return ToStringHelperImpl
		 */
		public ToStringHelperImpl addValue(double value) {
			return addHolder(String.valueOf(value));
		}

		/**
		 * Adds an unnamed value to the formatted output.
		 *
		 * <p>
		 * It is strongly encouraged to use {@link #add(String, float)} instead and give
		 * value a readable name.
		 * 
		 * @param value value.
		 * @return ToStringHelperImpl
		 */
		public ToStringHelperImpl addValue(float value) {
			return addHolder(String.valueOf(value));
		}

		/**
		 * Adds an unnamed value to the formatted output.
		 *
		 * <p>
		 * It is strongly encouraged to use {@link #add(String, int)} instead and give
		 * value a readable name.
		 * 
		 * @param value value.
		 * @return ToStringHelperImpl
		 */
		public ToStringHelperImpl addValue(int value) {
			return addHolder(String.valueOf(value));
		}

		/**
		 * Adds an unnamed value to the formatted output.
		 *
		 * <p>
		 * It is strongly encouraged to use {@link #add(String, long)} instead and give
		 * value a readable name.
		 * 
		 * @param value value.
		 * @return ToStringHelperImpl
		 */
		public ToStringHelperImpl addValue(long value) {
			return addHolder(String.valueOf(value));
		}

		/**
		 * Returns a string in the format specified by
		 * {@link MoreObjects#toStringHelper(Object)}.
		 *
		 * <p>
		 * After calling this method, you can keep adding more properties to later call
		 * toString() again and get a more complete representation of the same object;
		 * but properties cannot be removed, so this only allows limited reuse of the
		 * helper instance. The helper allows duplication of properties (multiple
		 * name/value pairs with the same name can be added).
		 */
		@Override
		public String toString() {
			// create a copy to keep it consistent in case value changes
			boolean omitNullValuesSnapshot = omitNullValues;
			String nextSeparator = "";
			StringBuilder builder = new StringBuilder(32).append(className).append('{');
			for (ValueHolder valueHolder = holderHead.next; valueHolder != null; valueHolder = valueHolder.next) {
				Object value = valueHolder.value;
				if (!omitNullValuesSnapshot || value != null) {
					builder.append(nextSeparator);
					nextSeparator = ", ";

					if (valueHolder.name != null) {
						builder.append(valueHolder.name).append('=');
					}
					if (value != null && value.getClass().isArray()) {
						Object[] objectArray = { value };
						String arrayString = Arrays.deepToString(objectArray);
						builder.append(arrayString, 1, arrayString.length() - 1);
					} else {
						builder.append(value);
					}
				}
			}
			return builder.append('}').toString();
		}

		private ValueHolder addHolder() {
			ValueHolder valueHolder = new ValueHolder();
			holderTail = holderTail.next = valueHolder;
			return valueHolder;
		}

		private ToStringHelperImpl addHolder(Object value) {
			ValueHolder valueHolder = addHolder();
			valueHolder.value = value;
			return this;
		}

		private ToStringHelperImpl addHolder(String name, Object value) {
			ValueHolder valueHolder = addHolder();
			valueHolder.value = value;
			valueHolder.name = name;
			return this;
		}

		private ToStringHelperImpl addHolder(String name, Calendar value) {
			ValueHolder valueHolder = addHolder();
			valueHolder.value = "\"" + SugarUtil.calendarToString(value) + "\"";
			valueHolder.name = name;
			return this;
		}

		private static final class ValueHolder {
			String name;
			Object value;
			ValueHolder next;
		}
	}
}
