package eu.ardinsys.reflection;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Bean property descriptor.
 */
public class PropertyDescriptor {
  /**
   * The name of the property.
   */
  private final String name;

  /**
   * The declared type of the property.
   */
  private final Type type;

  /**
   * The getter of the property.
   */
  private final Method getter;

  /**
   * The list of potential setters of the property.
   */
  private final List<Method> setters;

  public PropertyDescriptor(Method getter, List<Method> setters) {
    this.name = Utils.propertyNameOf(getter);
    this.type = getter.getGenericReturnType();
    this.getter = getter;
    this.setters = setters;
  }

  /**
   * @return The current value of {@link #name}
   */
  public String getName() {
    return name;
  }

  /**
   * @return The current value of {@link #type}
   */
  public Type getType() {
    return type;
  }

  /**
   * @return The current value of {@link #getter}
   */
  public Method getGetter() {
    return getter;
  }

  /**
   * @return The current value of {@link #setters}
   */
  public List<Method> getSetters() {
    return setters;
  }
}
