package eu.ardinsys.reflection;

/**
 * Error message templates.
 */
public final class MessageTemplates {
  /**
   * <i>Failed to instantiate type %s (no implementation could be found).</i>
   */
  public static final String ERROR_INSTANTIATE_ABSTRACT_CLASS = "Failed to instantiate type %s (no implementation could be found).";

  /**
   * <i>Failed to instantiate type %s (no subclass compatible with type %s could be found).</i>
   */
  public static final String ERROR_INSTANTIATE_INCOMPATIBLE_CLASSES = "Failed to instantiate type %s (no subclass compatible with type %s could be found).";

  /**
   * <i>Failed to instantiate enum %s (it has no declared enum constants)./<i>
   */
  public static final String ERROR_INSTANTIATE_EMPTY_ENUM = "Failed to instantiate enum %s (it has no declared enum constants).";

  /**
   * <i>Failed to instantiate type %s (it has no default constructor).</i>
   */
  public static final String ERROR_INSTANTIATE_MISSING_DEFAULT_CONSTRUCTOR = "Failed to instantiate type %s (it has no default constructor).";

  /**
   * <i>Failed to instantiate type %s (could not invoke default constructor).</i>
   */
  public static final String ERROR_INSTANTIATE_INVOKE_CONSTRUCTOR = "Failed to instantiate type %s (could not invoke default constructor).";

  /**
   * <i>Failed to invoke method %s of class %s.</i>
   */
  public static final String ERROR_INVOKE_METHOD = "Failed to invoke method %s of class %s.";

  /**
   * <i>Expected an array or a list, received an object of class %s instead.</i>
   */
  public static final String ERROR_TYPE_NOT_SEQUENCE = "Expected an array or a list, received an object of class %s instead.";

  /**
   * <i>Expected a map, received an object of class %s instead.</i>
   */
  public static final String ERROR_TYPE_NOT_MAP = "Expected a map, received an object of class %s instead.";

  /**
   * <i>Invalid path subexpression: %s.</i>
   */
  public static final String ERROR_INVALID_PATH_SUBEXPRESSION = "Invalid path subexpression: %s.";

  /**
   * <i>Class %s has no field with name %s.</i>
   */
  public static final String ERROR_MISSING_PROPERTY = "Class %s has no field with name %s.";

  /**
   * <i>Property %s of class %s does not have a getter method.</i>
   */
  public static final String ERROR_MISSING_PROPERTY_GETTER = "Property %s of class %s does not have a getter method.";

  /**
   * <i>Property %s of class %s does not have a setter method.</i>
   */
  public static final String ERROR_MISSING_PROPERTY_SETTER = "Property %s of class %s does not have a setter method.";

  /**
   * <i>No matching setter in class %s found for getter %s.</i>
   */
  public static final String INFO_NO_MATCHING_SETTER_FOUND = "No matching setter in class %s found for getter %s.";

  /**
   * <i>No properties cloned from class %s to %s.</i>
   */
  public static final String INFO_NO_PROPERTIES_CLONED = "No properties cloned from class %s to %s.";
}
