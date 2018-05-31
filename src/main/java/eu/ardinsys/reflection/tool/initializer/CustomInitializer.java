package eu.ardinsys.reflection.tool.initializer;

/**
 * Interface for custom type initializer implementations.
 * <p>
 * See {@link #initialize(Object)}.
 *
 * @param <T> The type of the object to clone
 */
public interface CustomInitializer<T> {
  /**
   * Initializes the object.
   *
   * @param object The object to initialize
   */
  void initialize(T object);
}
