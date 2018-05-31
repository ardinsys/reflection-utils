package eu.ardinsys.reflection.tool;

import eu.ardinsys.reflection.ClassHashMap;

import java.util.*;

/**
 * Default {@link ImplementationProvider} implementation. Provides implementations for the most basic interfaces of the
 * Java Collection API.
 */
public class BasicImplementationProvider implements ImplementationProvider {
  protected ClassHashMap<Class<?>> defaultImplementations = new ClassHashMap<Class<?>>();

  public BasicImplementationProvider() {
    defaultImplementations.put(Collection.class, ArrayList.class);
    defaultImplementations.put(List.class, ArrayList.class);
    defaultImplementations.put(Deque.class, LinkedList.class);
    defaultImplementations.put(Set.class, HashSet.class);
    defaultImplementations.put(NavigableSet.class, TreeSet.class);
    defaultImplementations.put(Map.class, HashMap.class);
    defaultImplementations.put(NavigableMap.class, TreeMap.class);
  }

  /**
   * @see ImplementationProvider#provideImplementations(Class)
   */
  @Override
  public Set<Class<?>> provideImplementations(Class<?> superClass) {
    return defaultImplementations.getSubValues(superClass);
  }
}
