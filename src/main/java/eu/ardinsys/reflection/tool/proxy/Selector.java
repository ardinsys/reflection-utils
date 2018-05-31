package eu.ardinsys.reflection.tool.proxy;

/**
 * Path selector. Can mean:
 * <ul>
 * <li><b>property access</b> with getters and setters<br>
 * Examples: <code>.foo, .bar</code><br>
 * </li>
 * <li><b>map indexing</b> with string keys<br>
 * Examples: <code>[foo], [bar]</code></li>
 * <li><b>array or list indexing</b> with numbers or special symbols<br>
 * Examples: <code>[10], [+], [-]</code><br>
 * <code>+</code> is interpreted as <i>1 + index of the last value</i><br>
 * <code>-</code> is interpreted as <i>index of the last value</i><br>
 * When using {@link ReflectionProxy#get(String)}, these both return the last value.<br>
 * When using {@link ReflectionProxy#set(String, Object)}, <code>+</code> can be used to append a new value.</li>
 * </ul>
 */
public class Selector {
  /**
   * The type of the selector.
   */
  private final SelectorType type;

  /**
   * The value of the selector.
   */
  private final String value;

  public Selector(String value, SelectorType type) {
    this.value = value;
    this.type = type;
  }

  /**
   * @return The current value of {@link #type}
   */
  public SelectorType getType() {
    return type;
  }

  /**
   * @return The current value of {@link #value}
   */
  public String getValue() {
    return value;
  }

  /**
   * @return The numeric value of {@link #value}
   * @throws NumberFormatException If the value isn't numeric
   */
  public int getNumericValue() throws NumberFormatException {
    return Integer.parseInt(value);
  }

  @Override
  public String toString() {
    return String.format("[%s %s]", type, value);
  }

  /**
   * The type of the selector.
   */
  public enum SelectorType {
    PROPERTY, INDEX, KEY, LAST_INDEX, APPEND_INDEX
  }
}
