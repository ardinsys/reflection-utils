package eu.ardinsys.reflection;

import eu.ardinsys.reflection.tool.cloner.ReflectionCloner;
import eu.ardinsys.reflection.tool.dumper.ReflectionDumper;
import eu.ardinsys.reflection.tool.initializer.ReflectionInitializer;

import java.lang.reflect.Method;

/**
 * An interface to determine which properties to exclude (from whatever operation). By default, a property is included
 * if any of the following conditions are fulfilled:
 * <ul>
 * <li>it has a getter and setter (with default signatures)</li>
 * <li>it was registered via {@link Utils#registerProperty(String, Method, Method)}</li>
 * </ul>
 * <p>
 * See {@link #excludeProperty(String)}.
 * <p>
 * You can register a property filter for the various tools via:
 * <ul>
 * <li>{@link ReflectionInitializer#addPropertyFilter(Class, PropertyFilter)}</li>
 * <li>{@link ReflectionCloner#addPropertyFilter(Class, PropertyFilter)}</li>
 * <li>{@link ReflectionDumper#addPropertyFilter(Class, PropertyFilter)}</li>
 * </ul>
 * where the first argument is a class whose properties will be subject to filtering.<br>
 * <p>
 * A property of class <b>A</b> is excluded if any of the <i>relevant</i> property filters exclude it (via
 * {@link #excludeProperty(String)}). A property filter registered for class <b>B</b> is <i>relevant</i> for class
 * <b>A</b> if <b>A</b> &lt;= <b>B</b> and no property filter registered for a class <b>C</b> exists such that <b>A</b> &lt;=
 * <b>C</b> &lt; <b>B</b>, where <b>X</b> &lt;(=) <b>Y</b> means "<b>X</b> is a subclass of <b>Y</b> (or <b>X</b> is
 * <b>Y</b>)".<br>
 * In short, this means that a property filter may "override" the decision of another if it was registered to a more
 * direct class.
 * <p>
 * Note that you may only have a single property filter registered for a given class (registering another will override
 * the previous).
 */
public interface PropertyFilter {
  /**
   * @param propertyName The name of the property
   * @return True if the property named <b>propertyName</b> should be excluded
   */
  boolean excludeProperty(String propertyName);
}
