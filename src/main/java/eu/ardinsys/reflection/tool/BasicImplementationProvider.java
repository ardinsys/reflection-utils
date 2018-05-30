package eu.ardinsys.reflection.tool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import eu.ardinsys.reflection.ClassHashMap;

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
