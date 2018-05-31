package eu.ardinsys.reflection.tool;

import eu.ardinsys.reflection.ClassHashMap;
import eu.ardinsys.reflection.MessageTemplates;
import eu.ardinsys.reflection.PropertyFilter;
import eu.ardinsys.reflection.Utils;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Common base for all reflection related utils. Public API:
 * <ul>
 * <li>{@link #getImplementationProvider()}</li>
 * <li>{@link #setImplementationProvider(ImplementationProvider)}</li>
 * <li>{@link #getPropertyFilter(Class)}</li>
 * <li>{@link #addPropertyFilter(Class, PropertyFilter)}</li>
 * <li>{@link #removePropetyFilter(Class)}</li>
 * <li>{@link #registerImmutable(Class)}</li>
 * <li>{@link #unregisterImmutable(Class)}</li>
 * <li>{@link #isRegisteredImmutable(Class)}</li>
 * <li>{@link #setLogger(Logger)}</li>
 * <li>{@link #getLogger()}</li>
 * <li>{@link #setThrowOnError(boolean)}</li>
 * <li>{@link #getThrowOnError()}</li>
 * </ul>
 */
public abstract class ReflectionBase {
  /**
   * The property filters.
   */
  protected final ClassHashMap<PropertyFilter> propertyFilters = new ClassHashMap<PropertyFilter>();
  /**
   * The user registered immutable classes.
   */
  protected final Set<Class<?>> registeredImmutableClasses = new HashSet<Class<?>>();
  /**
   * The instance provider.<br>
   * Default value: an instance of {@link BasicInstanceProvider}.
   */
  protected InstanceProvider instanceProvider = new BasicInstanceProvider();
  /**
   * The implementation provider.<br>
   * Default value: an instance of {@link BasicImplementationProvider}.
   */
  protected ImplementationProvider implementationProvider = new BasicImplementationProvider();
  /**
   * The logger to report infos and error messages.<br>
   * Defaults value: the global java logger (<code>Logger.getLogger(Logger.GLOBAL_LOGGER_NAME)</code>).
   */
  protected Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * If true, the process will throw an exception on error.<br>
   * Default value: <code>false</code>.
   */
  protected boolean throwOnError = false;

  /**
   * @return The current value of {@link #instanceProvider}
   */
  public InstanceProvider getInstanceProvider() {
    return instanceProvider;
  }

  /**
   * @param instanceProvider The new value of {@link #instanceProvider}
   */
  public void setInstanceProvider(InstanceProvider instanceProvider) {
    this.instanceProvider = instanceProvider;
  }

  /**
   * @return The current value of {@link #implementationProvider}
   */
  public final ImplementationProvider getImplementationProvider() {
    return implementationProvider;
  }

  /**
   * @param provider The new value of {@link #implementationProvider}
   */
  public final void setImplementationProvider(ImplementationProvider provider) {
    this.implementationProvider = provider;
  }

  /**
   * Returns the property filter registered for the given class.
   *
   * @param c The class
   * @return The property filter
   */
  public final PropertyFilter getPropertyFilter(Class<?> c) {
    return propertyFilters.get(c);
  }

  /**
   * Registers a property filter for the given class.
   *
   * @param c              The class
   * @param propertyFilter The property filter
   */
  public final void addPropertyFilter(Class<?> c, PropertyFilter propertyFilter) {
    propertyFilters.put(c, propertyFilter);
  }

  /**
   * Unregisters a property filter registered to the given class.
   *
   * @param c The class
   */
  public final void removePropetyFilter(Class<?> c) {
    propertyFilters.remove(c);
  }

  /**
   * @param c The class
   * @return True if <b>c</b> is registered as immutable
   */
  public final boolean isRegisteredImmutable(Class<?> c) {
    return registeredImmutableClasses.contains(c);
  }

  /**
   * Registers the class as immutable
   *
   * @param c The class
   */
  public final void registerImmutable(Class<?> c) {
    registeredImmutableClasses.add(c);
  }

  /**
   * Unregisters the class previously registered as immutable.
   *
   * @param c The class
   */
  public final void unregisterImmutable(Class<?> c) {
    registeredImmutableClasses.remove(c);
  }

  /**
   * @return The current value of {@link #logger}
   */
  public final Logger getLogger() {
    return logger;
  }

  /**
   * @param logger The new value of {@link #logger}
   */
  public final void setLogger(Logger logger) {
    this.logger = logger;
  }

  /**
   * @return The current value of {@link #throwOnError}
   */
  public final boolean getThrowOnError() {
    return throwOnError;
  }

  /**
   * @param throwOnError The new value of {@link #throwOnError}
   */
  public final void setThrowOnError(boolean throwOnError) {
    this.throwOnError = throwOnError;
  }

  // ///////////////////////////////////////////////////////////
  //
  // Delegated operations
  //
  // ///////////////////////////////////////////////////////////

  /**
   * @return The size.
   * @see InstanceProvider#getCompositeSize()
   */
  public final int getCompositeSize() {
    return instanceProvider.getCompositeSize();
  }

  /**
   * @param compositeSize The size.
   * @see InstanceProvider#setCompositeSize(int)
   */
  public final void setCompositeSize(int compositeSize) {
    instanceProvider.setCompositeSize(compositeSize);
  }

  // ///////////////////////////////////////////////////////////
  //
  // Common operations
  //
  // ///////////////////////////////////////////////////////////

  private String getToolName() {
    return getClass().getSimpleName();
  }

  private Class<?> getInstantiableClass(Class<?> rawClass) {
    if (Utils.isConcrete(rawClass)) {
      return rawClass;
    }

    for (Class<?> implementation : implementationProvider.provideImplementations(rawClass)) {
      if (Utils.isConcrete(implementation)) {
        return implementation;
      }
    }

    throw new RuntimeException(
        String.format(MessageTemplates.ERROR_INSTANTIATE_ABSTRACT_CLASS, rawClass.getName()));
  }

  private <T> T instantiateClass(Class<T> instantiableClass) {
    return Utils.cast(instantiableClass, instanceProvider.provideInstance(instantiableClass));
  }

  private <T> T instantiateRawClass(Class<T> rawClass) {
    return Utils.cast(rawClass, instantiateClass(getInstantiableClass(rawClass)));
  }

  protected final Object instantiateType(Type type) {
    return instantiateRawClass(Utils.getRawClass(type));
  }

  protected final <T> T instantiateForAbstraction(Class<T> abstraction, Class<?> sourceClass,
                                                  Type targetType) {
    Class<?> targetRawClass = Utils.getRawClass(targetType);

    if (abstraction.isAssignableFrom(targetRawClass)) {
      return targetRawClass.isAssignableFrom(sourceClass)
          ? Utils.cast(abstraction, instantiateRawClass(sourceClass))
          : Utils.cast(abstraction, instantiateRawClass(targetRawClass));
    }

    if (targetRawClass.isAssignableFrom(abstraction)) {
      return Utils.cast(abstraction, instantiateRawClass(sourceClass));
    }

    throw new RuntimeException(
        String.format(MessageTemplates.ERROR_INSTANTIATE_INCOMPATIBLE_CLASSES,
            Utils.formatTypeName(targetType), sourceClass.getName()));
  }

  protected final void log(Level level, String message) {
    logger.log(level, String.format("%s: %s", getToolName(), message));
  }

  protected final void log(Level level, String message, Exception e) {
    logger.log(level, String.format("%s: %s", getToolName(), message), e);
  }

  protected boolean isImmutable(Class<?> c) {
    return Utils.isImmutable(c) || registeredImmutableClasses.contains(c);
  }
}
