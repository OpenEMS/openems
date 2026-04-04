package io.openems.common.referencetarget;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.osgi.service.component.annotations.ComponentPropertyType;

/**
 * Annotation to mark which reference targets should be generated.
 *
 * <p>
 * Example usage:
 *
 * <pre>
 *  // Configuration Class
 * {@literal @}ObjectClassDefinition(...)
 * {@literal @}interface Config {
 *
 *    {@literal @}AttributeDefinition(name = "Component-ID")
 *     String component_id(); // (1)
 *
 *  }
 *  
 *  // Component Class
 * {@literal @}Designate(ocd = Config.class (2), factory = true)
 * {@literal @}Component(...)
 * {@literal @}GenerateTargetsFromReferences("component") // (3)
 *  public class MyOpenemsComponent extends AbstractOpenemsComponent implements OpenemsComponent {
 * 
 * {@literal @}Reference(target = "(id=${config.component_id})") // (4)
 *  private OpenemsComponent component; // (5)
 *
 *  ...
 *
 *  }
 * </pre>
 * <ol>
 * <li>Simple Configuration Property to configure a component id for a
 * Component.</li>
 * <li>Use correct Configuration class in annotation</li>
 * <li>Define for which attributes the target filter should be generated (in
 * this example the name of the variable at point 5)</li>
 * <li>Define the target filter with your variable</li>
 * <li>Your variable of the injected component</li>
 * </ol>
 * </p>
 *
 */
@ComponentPropertyType
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface GenerateTargetsFromReferences {

	/**
	 * The names of the references to generate targets from.
	 * 
	 * 
	 * @return an array of the reference names to generate targets from.
	 */
	String[] value() default {};

}
