package eu.ardinsys.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Public utility methods.
 */
public final class Utils {
  private static final Pattern PATTERN_GETTER = Pattern.compile("(get|is)([a-zA-Z]\\w*)");
  private static final Pattern PATTERN_SETTER = Pattern.compile("(set)([a-zA-Z]\\w*)");
  private static final Map<Method, String> REGISTERED_GETTERS = new HashMap<Method, String>();
  private static final Map<Method, String> REGISTERED_SETTERS = new HashMap<Method, String>();

  /**
   * A set of classes representing boxed classes of primitive types.
   */
  private static final Set<Class<?>> BOXED_CLASSES = new HashSet<Class<?>>();

  static {
    BOXED_CLASSES.addAll(Arrays.asList(
        Byte.class,
        Short.class,
        Integer.class,
        Long.class,
        Float.class,
        Double.class,
        Character.class,
        Boolean.class));
  }

  /**
   * A set of classes representing immutable types.
   */
  private static final Set<Class<?>> IMMUTABLE_CLASSES = new HashSet<Class<?>>();

  static {
    IMMUTABLE_CLASSES.addAll(BOXED_CLASSES);
    IMMUTABLE_CLASSES.addAll(Arrays.asList(
        String.class,
        BigDecimal.class,
        BigInteger.class,
        Date.class,
        UUID.class,
        Timestamp.class,
        XMLGregorianCalendar.class));
  }

  // Caches for memoized methods
  private static final Map<Class<?>, Map<String, PropertyDescriptor>> CACHE_DESCRIPTION = new HashMap<Class<?>, Map<String, PropertyDescriptor>>();
  private static final Map<TypeDistanceCacheKey, Integer> CACHE_TYPE_DISTANCE = new HashMap<TypeDistanceCacheKey, Integer>();

  private Utils() {
    // private since we never actually instantiate this class (static members only)
  }

  // ///////////////////////////////////////////////////////////
  //
  // Misc. utils
  //
  // ///////////////////////////////////////////////////////////

  /**
   * Calculates the Levenshtein distance between two character sequences.<br>
   * See <a href= "https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java"> Wikipedia:
   * Levenshtein distance </a>
   *
   * @param from The first character sequence
   * @param to   The second character sequence
   * @return The Levenshtein-distance between two character sequences
   */
  public static int levenshteinDistance(CharSequence from, CharSequence to) {
    int len0 = from.length() + 1;
    int len1 = to.length() + 1;
    int[] cost = new int[len0];
    int[] newCost = new int[len0];

    for (int i = 0; i < len0; i++) {
      cost[i] = i;
    }

    for (int j = 1; j < len1; j++) {
      newCost[0] = j;

      for (int i = 1; i < len0; i++) {
        int match = (from.charAt(i - 1) == to.charAt(j - 1)) ? 0 : 1;
        int costReplace = cost[i - 1] + match;
        int costInsert = cost[i] + 1;
        int costDelete = newCost[i - 1] + 1;
        newCost[i] = Math.min(Math.min(costInsert, costDelete), costReplace);
      }

      int[] swap = cost;
      cost = newCost;
      newCost = swap;
    }

    return cost[len0 - 1];
  }

  /**
   * Uncapitalizes the first letter of a string. If the string is empty, returns an empty string.
   *
   * @param s The string
   * @return The modified string
   */
  public static String uncapitalizeFirstLetter(String s) {
    return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
  }

  /**
   * Capitalizes the first letter of a string. If the string is empty, returns an empty string.
   *
   * @param s The string
   * @return The modified string
   */
  public static String capitalizeFirstLetter(String s) {
    return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  /**
   * Calculates the size of the intersection of the given sets.
   *
   * @param setA The first set
   * @param setB The second set
   * @return The size of the intersection
   */
  public static int intersectionSize(Set<?> setA, Set<?> setB) {
    int size = 0;
    for (Object item : setA) {
      if (setB.contains(item)) {
        size++;
      }
    }

    return size;
  }

  // ///////////////////////////////////////////////////////////
  //
  // Method utils
  //
  // ///////////////////////////////////////////////////////////

  /**
   * Returns true if the given method is a getter method (is non-static, non-abstract, has a return type other than
   * <code>void</code>, takes no parameters, and has a name beginning with "get" or "is").
   *
   * @param method The method
   * @return True if <b>method</b> is a getter method
   */
  public static boolean isGetter(Method method) {
    return REGISTERED_GETTERS.containsKey(method)
        || (!Modifier.isStatic(method.getModifiers())
        && !Modifier.isAbstract(method.getModifiers())
        && PATTERN_GETTER.matcher(method.getName()).matches()
        && method.getReturnType() != void.class
        && method.getParameterTypes().length == 0);
  }

  /**
   * Returns true if the given method is an array getter method (is a getter method(see {@link #isGetter(Method)}), and
   * the return type is array.
   *
   * @param method The method
   * @return True if <b>method</b> is an array getter method
   */
  public static boolean isArrayGetter(Method method) {
    return isGetter(method) && isArray(method.getReturnType());
  }

  /**
   * Returns true if the given method is a collection getter method (is a getter method(see {@link #isGetter(Method)}),
   * and the return type is a subclass of {@link Collection}).
   *
   * @param method The method
   * @return True if <b>method</b> is a collection getter method
   */
  public static boolean isCollectionGetter(Method method) {
    return isGetter(method) && isCollection(method.getReturnType());
  }

  /**
   * Returns true if the given method is a map getter method (is a getter method(see {@link #isGetter(Method)}), and the
   * return type is a subclass of {@link Map}).
   *
   * @param method The method
   * @return True if <b>method</b> is a map getter method
   */
  public static boolean isMapGetter(Method method) {
    return isGetter(method) && isMap(method.getReturnType());
  }

  /**
   * Returns true if the method is a setter method (is non-static, non-abstract, has a return type of <code>void</code>,
   * takes exactly one parameter, and has a name beginning with "set").
   *
   * @param method The method
   * @return True if <b>method</b> is a setter method
   */
  public static boolean isSetter(Method method) {
    return REGISTERED_SETTERS.containsKey(method)
        || (!Modifier.isStatic(method.getModifiers())
        && !Modifier.isAbstract(method.getModifiers())
        && PATTERN_SETTER.matcher(method.getName()).matches()
        && method.getReturnType() == void.class
        && method.getParameterTypes().length == 1);
  }

  /**
   * Returns the matching property name for the given method.
   * <p>
   * If the method was registered as a getter or a setter via {@link #registerProperty(String, Method, Method)}, returns
   * the registered property name.<br>
   * If the name of the method resembles that of a getter or a setter, returns the extracted property name. <br>
   * Otherwise, returns null.
   *
   * @param method The method name
   * @return The property name or <code>null</code>
   */
  public static String propertyNameOf(Method method) {
    if (REGISTERED_GETTERS.containsKey(method)) {
      return REGISTERED_GETTERS.get(method);
    }

    if (REGISTERED_SETTERS.containsKey(method)) {
      return REGISTERED_SETTERS.get(method);
    }

    String methodName = method.getName();
    Matcher m;
    if ((m = PATTERN_GETTER.matcher(methodName)).matches()) {
      return uncapitalizeFirstLetter(m.group(2));
    }

    if ((m = PATTERN_SETTER.matcher(methodName)).matches()) {
      return uncapitalizeFirstLetter(m.group(2));
    }

    return null;
  }

  /**
   * Attempts to invoke the given method.
   *
   * @param method The method
   * @param object The object to call the method on
   * @param args   The arguments to the method
   * @return The return value of the method call
   */
  public static Object invoke(Method method, Object object, Object... args) {
    try {
      method.setAccessible(true);
    } catch (Exception e) {
      // do nothing
    }

    try {
      return method.invoke(object, args);
    } catch (Exception e1) {
      throw new RuntimeException(String.format(MessageTemplates.ERROR_INVOKE_METHOD,
          method.getName(), object.getClass().getName()));
    }
  }

  /**
   * Attempts to invoke the given constructor.
   *
   * @param constructor The constructor
   * @param args        The arguments to <b>constructor</b>
   * @return The instance created by <b>constructor</b>
   */
  public static <T> T invoke(Constructor<T> constructor, Object... args) {
    try {
      constructor.setAccessible(true);
    } catch (Exception e) {
      // do nothing
    }

    try {
      return constructor.newInstance(args);
    } catch (Exception e1) {
      throw new RuntimeException(
          String.format(MessageTemplates.ERROR_INSTANTIATE_INVOKE_CONSTRUCTOR,
              constructor.getDeclaringClass().getName()));
    }
  }

  /**
   * Attempts to find the most appropriate setter for the given getter.
   *
   * @param getter  The getter
   * @param setters A list of potential setters
   * @return The most appropriate setter
   */
  public static Method selectSetter(Method getter, List<Method> setters) {
    Class<?> sourceClass = Utils.getRawClass(getter.getGenericReturnType());

    // Exact match
    for (Method setter : setters) {
      Class<?> targetClass = Utils.getRawClass(setter.getGenericParameterTypes()[0]);
      if (targetClass == sourceClass) {
        return setter;
      }
    }

    // Assignable type
    Method bestSetter = null;
    int minDistance = Integer.MAX_VALUE;
    for (Method setter : setters) {
      Class<?> targetClass = Utils.getRawClass(setter.getGenericParameterTypes()[0]);
      int distance = Utils.typeDistance(targetClass, sourceClass);
      if (distance >= 0 && distance < minDistance) {
        minDistance = distance;
        bestSetter = setter;
      }
    }

    if (bestSetter != null) {
      return bestSetter;
    }

    // Most mutual fields
    int maxMutualPropertyCount = 0;

    Map<String, PropertyDescriptor> sourcePropertyDescriptors = Utils.describeClass(sourceClass);
    for (Method setter : setters) {
      Class<?> targetClass = Utils.getRawClass(setter.getGenericParameterTypes()[0]);
      Map<String, PropertyDescriptor> targetPropertyDescriptors = Utils.describeClass(targetClass);
      int mutualPropertyCount = intersectionSize(
          sourcePropertyDescriptors.keySet(),
          targetPropertyDescriptors.keySet());
      if (mutualPropertyCount > maxMutualPropertyCount) {
        maxMutualPropertyCount = mutualPropertyCount;
        bestSetter = setter;
      }
    }

    if (bestSetter != null) {
      return bestSetter;
    }

    // Last resort - Levenshtein distance
    minDistance = Integer.MAX_VALUE;
    for (Method setter : setters) {
      Class<?> targetClass = Utils.getRawClass(setter.getGenericParameterTypes()[0]);
      int distance = Utils.levenshteinDistance(sourceClass.getName(), targetClass.getName());
      if (distance < minDistance) {
        distance = minDistance;
        bestSetter = setter;
      }
    }

    return bestSetter;
  }

  // ///////////////////////////////////////////////////////////
  //
  // Class utils
  //
  // ///////////////////////////////////////////////////////////

  /**
   * @param c The class
   * @return True if <b>c</b> is abstract
   */
  public static boolean isAbstract(Class<?> c) {
    return Modifier.isAbstract(c.getModifiers());
  }

  /**
   * @param c The class
   * @return True if <b>c</b> is an interface
   */
  public static boolean isInterface(Class<?> c) {
    return c.isInterface();
  }

  /**
   * @param c The class
   * @return True if <b>c</b> is concrete (not abstract or an interface)
   */
  public static boolean isConcrete(Class<?> c) {
    return isArray(c) || isPrimitive(c) || (!isAbstract(c) && !isInterface(c));
  }

  /**
   * @param c The class
   * @return True if <b>c</b> represents a primitive type
   */
  public static boolean isPrimitive(Class<?> c) {
    return c.isPrimitive();
  }

  /**
   * @param c The class
   * @return True if <b>c</b> is immutable (is primitive, an enum, or one of {@link #IMMUTABLE_CLASSES})
   */
  public static boolean isImmutable(Class<?> c) {
    return IMMUTABLE_CLASSES.contains(c) || isPrimitive(c) || isEnum(c);
  }

  /**
   * @param c The class
   * @return True if <b>c</b> represents an array
   */
  public static boolean isArray(Class<?> c) {
    return c.isArray();
  }

  /**
   * @param c The class
   * @return True if <b>c</b> is an enum
   */
  public static boolean isEnum(Class<?> c) {
    return c.isEnum();
  }

  /**
   * @param c The class
   * @return True if <b>c</b> is a subclass of {@link List}
   */
  public static boolean isList(Class<?> c) {
    return List.class.isAssignableFrom(c);
  }

  /**
   * @param c The class
   * @return True if <b>c</b> is a subclass of {@link Collection}
   */
  public static boolean isCollection(Class<?> c) {
    return Collection.class.isAssignableFrom(c);
  }

  /**
   * @param c The class
   * @return True if <b>c</b> is a subclass of {@link Map}
   */
  public static boolean isMap(Class<?> c) {
    return Map.class.isAssignableFrom(c);
  }

  /**
   * @param c The class
   * @return The no-arg constructor of <code>c</code> or <code>null</code> (if none exists)
   */
  @SuppressWarnings("unchecked")
  public static <T> Constructor<T> getDefaultConstructor(Class<T> c) {
    for (Constructor<?> constructor : c.getDeclaredConstructors()) {
      if (constructor.getParameterTypes().length == 0) {
        return (Constructor<T>) constructor;
      }
    }

    return null;
  }

  /**
   * @param c The class
   * @return The declared methods of <b>c</b> and all of its superclasses (except those declared by {@link Object})
   */
  public static List<Method> getMethods(Class<?> c) {
    List<Method> methods = new ArrayList<Method>();

    if (c == null) {
      return methods;
    }

    for (Method method : c.getDeclaredMethods()) {
      if (method.getDeclaringClass() != Object.class) {
        methods.add(method);
      }
    }

    for (Class<?> superInterface : c.getInterfaces()) {
      methods.addAll(getMethods(superInterface));
    }

    methods.addAll(getMethods(c.getSuperclass()));
    return methods;
  }

  /**
   * @param c The class
   * @return The public methods of <b>c</b> (except those declared by {@link Object})
   */
  public static List<Method> getPublicMethods(Class<?> c) {
    List<Method> methods = new ArrayList<Method>();

    if (c == null) {
      return methods;
    }

    for (Method method : c.getMethods()) {
      if (method.getDeclaringClass() != Object.class) {
        methods.add(method);
      }
    }

    return methods;
  }

  /**
   * @param c The class
   * @return The boxed class for <b>c</b> (or <b>c</b>, if it is not primitive)
   */
  public static Class<?> wrapperOf(Class<?> c) {
    if (!c.isPrimitive()) {
      return c;
    }

    if (c == byte.class) {
      return Byte.class;
    }

    if (c == short.class) {
      return Short.class;
    }

    if (c == int.class) {
      return Integer.class;
    }

    if (c == long.class) {
      return Long.class;
    }

    if (c == float.class) {
      return Float.class;
    }

    if (c == double.class) {
      return Double.class;
    }

    if (c == boolean.class) {
      return Boolean.class;
    }

    if (c == char.class) {
      return Character.class;
    }

    throw new RuntimeException();
  }

  /**
   * Casts the given object to the given type.
   *
   * @param c      The class
   * @param object The object
   * @return <b>object</b> cast to <b>c</b>
   */
  @SuppressWarnings("unchecked")
  public static <T> T cast(Class<T> c, Object object) {
    return (T) wrapperOf(c).cast(object);
  }

  /**
   * Attempts to instantiate the given class.
   *
   * @param c The class
   * @return A new instance of <b>c</b>
   */
  public static <T> T instantiate(Class<T> c) {
    try {
      return c.newInstance();
    } catch (Exception e) {
      Constructor<T> constructor = getDefaultConstructor(c);
      if (constructor == null) {
        throw new RuntimeException(String.format(
            MessageTemplates.ERROR_INSTANTIATE_MISSING_DEFAULT_CONSTRUCTOR, c.getName()));
      }

      return cast(c, invoke(constructor));
    }
  }

  /**
   * See {@link PropertyDescriptor}.
   *
   * @param c The class
   * @return A map from property names to property descriptors for <b>c</b>
   */
  public static Map<String, PropertyDescriptor> describeClass(Class<?> c) {
    Map<String, PropertyDescriptor> description = CACHE_DESCRIPTION.get(c);
    if (description == null) {
      description = describeClass0(c);
      CACHE_DESCRIPTION.put(c, description);
    }

    return description;
  }

  /**
   * Raw (not memorized) version of {@link #describeClass(Class)}.
   *
   * @param c The class.
   * @return A map from property names to property descriptors for <b>c</b>.
   */
  private static Map<String, PropertyDescriptor> describeClass0(Class<?> c) {
    Map<String, PropertyDescriptor> propertyDescriptors = new LinkedHashMap<String, PropertyDescriptor>();
    Map<String, Method> getters = new LinkedHashMap<String, Method>();
    Map<String, List<Method>> groupedSetters = new HashMap<String, List<Method>>();

    for (Method method : getPublicMethods(c)) {
      String propertyName = propertyNameOf(method);
      if (propertyName == null) {
        continue;
      }

      if (isGetter(method)) {
        getters.put(propertyName, method);
      } else if (isSetter(method)) {
        List<Method> setters = groupedSetters.get(propertyName);
        if (setters == null) {
          setters = new ArrayList<Method>();
          groupedSetters.put(propertyName, setters);
        }

        setters.add(method);
      }
    }

    for (String propertyName : getters.keySet()) {
      List<Method> setters = groupedSetters.get(propertyName);
      setters = setters != null ? setters : new ArrayList<Method>();
      propertyDescriptors.put(
          propertyName,
          new PropertyDescriptor(getters.get(propertyName), setters));
    }

    return propertyDescriptors;
  }

  /**
   * Calculates the distance from the parent to the child, which is the length of the shortest path from the child to
   * the parent on the type graph (a directed graph where an edge between the nodes (classes) <b>A</b> and <b>B</b>
   * exists if and only if <b>A</b> is assignable to <b>B</b>).
   * <p>
   * See {@link Class#isAssignableFrom(Class)}.
   *
   * @param parent The parent class
   * @param child  The child class
   * @return The distance from <b>child</b> to <b>parent</b> or <code>-1</code> <b>parent</b> is not an ancestor of
   * <b>child</b>
   */
  public static int typeDistance(Class<?> parent, Class<?> child) {
    TypeDistanceCacheKey key = new TypeDistanceCacheKey(parent, child);
    Integer typeDistance = CACHE_TYPE_DISTANCE.get(key);
    if (typeDistance == null) {
      typeDistance = Integer.valueOf(typeDistance0(parent, child));
      CACHE_TYPE_DISTANCE.put(key, typeDistance);
    }

    return typeDistance.intValue();
  }

  /**
   * Raw (not memorized) version of {@link #typeDistance(Class, Class)}.
   *
   * @param parent The parent class.
   * @param child  The child class.
   * @return The distance from <b>child</b> to <b>parent</b> or <code>-1</code> <b>parent</b> is
   * not an ancestor of <b>child</b>.
   */
  private static int typeDistance0(Class<?> parent, Class<?> child) {
    if (!parent.isAssignableFrom(child)) {
      return -1;
    }

    if (parent == child) {
      return 0;
    }

    List<Class<?>> superClasses = new ArrayList<Class<?>>();
    superClasses.addAll(Arrays.asList(child.getInterfaces()));

    Class<?> superClass = child.getSuperclass();
    if (superClass != null) {
      superClasses.add(superClass);
    }

    if (superClasses.isEmpty()) {
      return -1;
    }

    int minDistance = Integer.MAX_VALUE;
    for (Class<?> c : superClasses) {
      int distance = typeDistance(parent, c) + 1;
      if (distance > 0 && distance < minDistance) {
        minDistance = distance;
      }
    }

    return minDistance;
  }

  /**
   * Register a property with the given name, getter method and setter method. (the class is inferred from the method
   * instances). Example usage:
   *
   * <pre>
   * class A {
   *    private int n;
   *
   *    // Getter with name 'nn' instead of 'getN'
   *    public int nn() {
   *      return n;
   *    }
   *
   *    // Setter with name 'nnn' instead of 'setN'
   *    public void nnn(int n) {
   *      this.n = n;
   *    }
   * }
   *
   * ...
   * Utils.registerProperty(
   *   "n",
   *   A.class.getMethod("nn"),
   *   A.class.getMethod("nnn", int.class)
   * );
   * ...
   * </pre>
   * <p>
   * See also: {@link PropertyFilter}.
   *
   * @param propertyName The name of the property
   * @param getter       The getter method of the property
   * @param setter       The setter method of the property
   */
  public static void registerProperty(String propertyName, Method getter, Method setter) {
    REGISTERED_GETTERS.put(getter, propertyName);
    REGISTERED_SETTERS.put(setter, propertyName);
  }

  /**
   * If the first class is a boxed/primitive and the second class is its matching primitive/boxed, returns true.
   * Otherwise, returns the same as {@link Class#isAssignableFrom(Class)}.
   *
   * @param c1 The first class
   * @param c2 The second class
   * @return True if <b>c1</b> is assignable from <b>c2</b>
   */
  public static boolean isAssignableFrom(Class<?> c1, Class<?> c2) {
    return wrapperOf(c1).isAssignableFrom(wrapperOf(c2));
  }

  // ///////////////////////////////////////////////////////////
  //
  // Type utils
  //
  // ///////////////////////////////////////////////////////////

  /**
   * @param type The type
   * @return The component types of <b>type</b>
   */
  public static Type[] getComponentTypes(Type type) {
    if (type == null) {
      return new Type[0];
    }

    if (type instanceof Class<?>) {
      Class<?> c = (Class<?>) type;
      return c.isArray() ? new Type[]{c.getComponentType()} : new Type[0];
    }

    if (type instanceof ParameterizedType) {
      return ((ParameterizedType) type).getActualTypeArguments();
    }

    if (type instanceof GenericArrayType) {
      return new Type[]{((GenericArrayType) type).getGenericComponentType()};
    }

    if (type instanceof WildcardType) {
      Type[] upperBounds = ((WildcardType) type).getUpperBounds();
      return upperBounds.length == 1 ? getComponentTypes(upperBounds[0]) : new Type[0];
    }

    if (type instanceof TypeVariable<?>) {
      Type[] bounds = ((TypeVariable<?>) type).getBounds();
      return bounds.length == 1 ? getComponentTypes(bounds[0]) : new Type[0];
    }

    return new Type[0];
  }

  /**
   * @param type The type
   * @return The first component type, or <code>Object.class</code> if <b>type</b> has no component types.
   */
  public static Type getComponentType(Type type) {
    return getComponentType(type, 0, Object.class);
  }

  /**
   * @param type The type
   * @param n    The index of the component type
   * @return The <b>n</b>th component type, or <code>Object.class</code> if <b>type</b> has less then <b>n + 1</b>
   * component types.
   */
  public static Type getComponentType(Type type, int n) {
    return getComponentType(type, n, Object.class);
  }

  /**
   * @param type        The type
   * @param n           The index of the component type
   * @param defaultType The default type
   * @return The <b>n</b>th component type, or <code>defaultType</code> if <b>type</b> has less then <b>n + 1</b>
   * component types.
   */
  public static Type getComponentType(Type type, int n, Type defaultType) {
    Type[] componentTypes = getComponentTypes(type);
    return componentTypes.length <= n ? defaultType : componentTypes[n];
  }

  /**
   * @param type The type
   * @return The raw class of <b>type</b>
   */
  public static Class<?> getRawClass(Type type) {
    if (type instanceof Class<?>) {
      return (Class<?>) type;
    }

    if (type instanceof ParameterizedType) {
      return (Class<?>) ((ParameterizedType) type).getRawType();
    }

    if (type instanceof GenericArrayType) {
      return Array.newInstance(getRawClass(((GenericArrayType) type).getGenericComponentType()), 0)
          .getClass();
    }

    if (type instanceof WildcardType) {
      Type[] upperBounds = ((WildcardType) type).getUpperBounds();
      return upperBounds.length == 1 ? getRawClass(upperBounds[0]) : Object.class;
    }

    if (type instanceof TypeVariable) {
      Type[] bounds = ((TypeVariable<?>) type).getBounds();
      return bounds.length == 1 ? getRawClass(bounds[0]) : Object.class;
    }

    return Object.class;
  }

  /**
   * @param type The type
   * @return The formatted name of <b>type</b>
   */
  public static String formatTypeName(Type type) {
    if (!(type instanceof Class<?>)) {
      return type.toString().replaceAll("\\w*\\.", "").replace("class ", "");
    }

    Class<?> c = (Class<?>) type;
    return c.isArray()
        ? String.format("%s[]", formatTypeName(getComponentType(c)))
        : c.getSimpleName();
  }

  /**
   * Helper class for {@link Utils#typeDistance(Class, Class)}. Serves as a cache key.
   */
  private static class TypeDistanceCacheKey {
    private final Class<?> parent;
    private final Class<?> child;

    public TypeDistanceCacheKey(Class<?> parent, Class<?> child) {
      this.parent = parent;
      this.child = child;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((child == null) ? 0 : child.hashCode());
      result = prime * result + ((parent == null) ? 0 : parent.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      TypeDistanceCacheKey other = (TypeDistanceCacheKey) obj;
      if (child == null) {
        if (other.child != null) {
          return false;
        }
      } else if (!child.equals(other.child)) {
        return false;
      }
      if (parent == null) {
        if (other.parent != null) {
          return false;
        }
      } else if (!parent.equals(other.parent)) {
        return false;
      }
      return true;
    }
  }
}
