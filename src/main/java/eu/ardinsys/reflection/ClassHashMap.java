package eu.ardinsys.reflection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A specialization of {@link HashMap} where the key type is {@link Class}. Provides two additional operations,
 * {@link #getSuperValues(Class)} and {@link #getSubValues(Class)}.
 *
 * @param <V> The value type
 */
public class ClassHashMap<V> extends HashMap<Class<?>, V> {
  private static final long serialVersionUID = 1L;
  private final Map<Class<?>, Set<V>> superCache = new HashMap<Class<?>, Set<V>>();
  private final Map<Class<?>, Set<V>> subCache = new HashMap<Class<?>, Set<V>>();

  /**
   * Raw (not memorized) version of {@link #getValues(Class, boolean)}.
   *
   * @param c           The class.
   * @param superValues if <code>true</code> it searches all the values mapped to the most
   *                    concrete superclasses or interfaces of the given class, otherwise it searches most abstract
   *                    subclasses or interfaces.
   * @return The values.
   */
  private Set<V> getValues0(Class<?> c, boolean superValues) {
    // Gather all keys which are super (or sub) classes (or interfaces) of c
    Set<Class<?>> classes = new HashSet<Class<?>>();
    for (Class<?> key : keySet()) {
      if ((superValues && key.isAssignableFrom(c)) || (!superValues && c.isAssignableFrom(key))) {
        classes.add(key);
      }
    }

    // Retain only the most concrete ones if superValues = true, the most
    // abstract ones otherwise
    Set<Class<?>> filteredClasses = new HashSet<Class<?>>(classes);
    for (Class<?> c1 : classes) {
      for (Class<?> c2 : classes) {
        if (c1 != c2 && c1.isAssignableFrom(c2)) {
          filteredClasses.remove(superValues ? c1 : c2);
        }
      }
    }

    // Map to values
    Set<V> values = new HashSet<V>();
    for (Class<?> filteredClass : filteredClasses) {
      values.add(get(filteredClass));
    }

    return values;
  }

  /**
   * Common method for {@link #getSuperValues(Class)} and {@link #getSubValues(Class)}.
   *
   * @param c           The class
   * @param superValues if <code>true</code> it searches all the values mapped to the most
   *                    concrete superclasses or interfaces of the given class, otherwise it searches most abstract
   *                    subclasses or interfaces
   * @return The values
   */
  private Set<V> getValues(Class<?> c, boolean superValues) {
    Map<Class<?>, Set<V>> cache = superValues ? superCache : subCache;
    Set<V> values = cache.get(c);
    if (values == null) {
      values = getValues0(c, superValues);
      cache.put(c, values);
    }

    return values;
  }

  /**
   * Returns all the values mapped to the most concrete superclasses or interfaces of the given class.
   *
   * @param c The class
   * @return The values
   */
  public Set<V> getSuperValues(Class<?> c) {
    return getValues(c, true);
  }

  /**
   * Returns all the values mapped to the most abstract subclasses or interfaces of the given class.
   *
   * @param c The class
   * @return The values
   */
  public Set<V> getSubValues(Class<?> c) {
    return getValues(c, false);
  }

  /**
   * @see Map#put(Object, Object)
   */
  @Override
  public V put(Class<?> key, V value) {
    if (!containsKey(key) || get(key) != value) {
      subCache.clear();
      superCache.clear();
    }

    return super.put(key, value);
  }

  /**
   * @see Map#putAll(Map)
   */
  @Override
  public void putAll(Map<? extends Class<?>, ? extends V> m) {
    for (Map.Entry<? extends Class<?>, ? extends V> entry : m.entrySet()) {
      Class<?> key = entry.getKey();
      V value = entry.getValue();
      if (!containsKey(key) || get(key) != value) {
        subCache.clear();
        superCache.clear();
        break;
      }
    }

    super.putAll(m);
  }

  /**
   * @see Map#remove(Object)
   */
  @Override
  public V remove(Object key) {
    if (containsKey(key)) {
      subCache.clear();
      superCache.clear();
    }

    return super.remove(key);
  }

  /**
   * @see Map#clear()
   */
  @Override
  public void clear() {
    subCache.clear();
    superCache.clear();

    super.clear();
  }
}
