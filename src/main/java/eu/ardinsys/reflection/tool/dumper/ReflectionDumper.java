package eu.ardinsys.reflection.tool.dumper;

import eu.ardinsys.reflection.ClassHashMap;
import eu.ardinsys.reflection.PropertyDescriptor;
import eu.ardinsys.reflection.PropertyFilter;
import eu.ardinsys.reflection.Utils;
import eu.ardinsys.reflection.tool.ReflectionBase;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * Turns any object into a compact string representation. The output has a different notation for arrays
 * (<code>[...]</code>), collections (<code>(...)</code>), maps (<code>{...}</code>) and bean-like objects
 * (<code>&lt;...&gt;</code>).
 * <p>
 * Public API:
 * <ul>
 * <li>{@link #dump(Object)}</li>
 * </ul>
 * <p>
 * Customize with:
 * <ul>
 * <li>{@link #setIncludeId(boolean)}</li>
 * <li>{@link #setIncludeName(boolean)}</li>
 * <li>{@link #setIncludeDeclaredType(boolean)}</li>
 * <li>{@link #setIncludeActualType(boolean)}</li>
 * <li>{@link #getArrayDelimiters()} / {@link #setArrayDelimiters(DelimiterPair)}</li>
 * <li>{@link #getCollectionDelimiters()} / {@link #setCollectionDelimiters(DelimiterPair)}</li>
 * <li>{@link #getMapDelimiters()} / {@link #setMapDelimiters(DelimiterPair)}</li>
 * <li>{@link #getBeanDelimiters()} / {@link #setBeanDelimiters(DelimiterPair)}</li>
 * <li>{@link #addCustomDumper(Class, CustomDumper)}</li>
 * <li>{@link #removeCustomDumper(Class)}</li>
 * <li>{@link #getCustomDumper(Class)}</li>
 * </ul>
 */
public class ReflectionDumper extends ReflectionBase {
  private static final String STR_LINE_SEPARATOR = System.getProperty("line.separator");
  private static final String STR_UNKNOWN = "???";
  private static final String STR_NOT_SHOWN = "...";
  private static final String STR_REFERENCE = "@@@";
  private static final String STR_NULL = "null";
  private static final Pattern PATTERN_LINE_SEPARATOR = Pattern.compile("(\r\n|\n|\r)");
  private static final Object INVALID_OBJECT = new Object();

  private final Map<Object, Long> idStack = new HashMap<Object, Long>();
  private final ClassHashMap<CustomDumper<?>> customDumpers = new ClassHashMap<CustomDumper<?>>();
  private long currentId = 0;
  /**
   * Determines whether the output should include generated object ids.<br>
   * Default value: <code>true</code>.
   */
  private boolean includeId = true;
  /**
   * Determines whether the output should include the declared types.<br>
   * Default value: <code>true</code>.
   */
  private boolean includeDeclaredType = true;
  /**
   * Determines whether the output should include the actual types.<br>
   * Default value: <code>true</code>.
   */
  private boolean includeActualType = true;
  /**
   * Determines whether the output should include the property names.<br>
   * Default value: <code>true</code>.
   */
  private boolean includeName = true;
  /**
   * Determines whether the output should use simple classnames.<br>
   * Default value: <code>true</code>.
   */
  private boolean useSimpleNames = true;
  /**
   * The string to use for indentations.<br>
   * Default value: <code>".  "</code> (dot + space + space).
   */
  private String indentationString = ".  ";
  /**
   * The maximum depth when traversing the data structures.<br>
   * Default value: <code>10</code>.
   */
  private int maxDepth = 10;
  /**
   * Array begin and end delimiters.<br>
   * Default value: <code>[]</code>.
   */
  private DelimiterPair arrayDelimiters = new DelimiterPair("[", "]");
  /**
   * Collection begin and end delimiters.<br>
   * Default value: <code>()</code>.
   */
  private DelimiterPair collectionDelimiters = new DelimiterPair("(", ")");
  /**
   * Map begin and end delimiters.<br>
   * Default value: <code>{}</code>.
   */
  private DelimiterPair mapDelimiters = new DelimiterPair("{", "}");
  /**
   * Bean begin and end delimiters.<br>
   * Default value: <code><></code>.
   */
  private DelimiterPair beanDelimiters = new DelimiterPair("<", ">");

  /**
   * @return The current value of {@link #includeId}
   */
  public boolean getIncludeId() {
    return includeId;
  }

  /**
   * @param includeId The new value of {@link #includeId}
   */
  public void setIncludeId(boolean includeId) {
    this.includeId = includeId;
  }

  /**
   * @return The current value of {@link #includeDeclaredType}
   */
  public boolean getIncludeDeclaredType() {
    return includeDeclaredType;
  }

  /**
   * @param includeDeclaredType The new value of {@link #includeDeclaredType}
   */
  public void setIncludeDeclaredType(boolean includeDeclaredType) {
    this.includeDeclaredType = includeDeclaredType;
  }

  /**
   * @return The current value of {@link #includeActualType}
   */
  public boolean getIncludeActualType() {
    return includeActualType;
  }

  /**
   * @param includeActualType The new value of {@link #includeActualType}
   */
  public void setIncludeActualType(boolean includeActualType) {
    this.includeActualType = includeActualType;
  }

  /**
   * @return The current value of {@link #includeName}
   */
  public boolean getIncludeName() {
    return includeName;
  }

  /**
   * @param includeName The new value of {@link #includeName}
   */
  public void setIncludeName(boolean includeName) {
    this.includeName = includeName;
  }

  /**
   * @return The current value of {@link #useSimpleNames}
   */
  public boolean getUseSimpleNames() {
    return useSimpleNames;
  }

  /**
   * @param useSimpleNames The new value of {@link #useSimpleNames}
   */
  public void setUseSimpleNames(boolean useSimpleNames) {
    this.useSimpleNames = useSimpleNames;
  }

  /**
   * @return The current value of {@link #indentationString}
   */
  public String getIndentationString() {
    return indentationString;
  }

  /**
   * @param indentationString The new value of {@link #indentationString}
   */
  public void setIndentationString(String indentationString) {
    this.indentationString = indentationString;
  }

  /**
   * @return The current value of {@link #arrayDelimiters}
   */
  public DelimiterPair getArrayDelimiters() {
    return arrayDelimiters;
  }

  /**
   * @param arrayDelimiters The new value of {@link #arrayDelimiters}
   */
  public void setArrayDelimiters(DelimiterPair arrayDelimiters) {
    this.arrayDelimiters = arrayDelimiters;
  }

  /**
   * @return The current value of {@link #collectionDelimiters}
   */
  public DelimiterPair getCollectionDelimiters() {
    return collectionDelimiters;
  }

  /**
   * @param collectionDelimiters The new value of {@link #collectionDelimiters}
   */
  public void setCollectionDelimiters(DelimiterPair collectionDelimiters) {
    this.collectionDelimiters = collectionDelimiters;
  }

  /**
   * @return The current value of {@link #mapDelimiters}
   */
  public DelimiterPair getMapDelimiters() {
    return mapDelimiters;
  }

  /**
   * @param mapDelimiters The new value of {@link #mapDelimiters}
   */
  public void setMapDelimiters(DelimiterPair mapDelimiters) {
    this.mapDelimiters = mapDelimiters;
  }

  /**
   * @return The current value of {@link #beanDelimiters}
   */
  public DelimiterPair getBeanDelimiters() {
    return beanDelimiters;
  }

  /**
   * @param beanDelimiters The new value of {@link #beanDelimiters}
   */
  public void setBeanDelimiters(DelimiterPair beanDelimiters) {
    this.beanDelimiters = beanDelimiters;
  }

  /**
   * @return The current value of {@link #maxDepth}
   */
  public int getMaxDepth() {
    return maxDepth;
  }

  /**
   * @param maxDepth The new value of {@link #maxDepth}
   */
  public void setMaxDepth(int maxDepth) {
    this.maxDepth = maxDepth;
  }

  /**
   * @param c   The class
   * @param <T> The type variable of the class
   * @return The registered custom dumper for <code>c</code>
   */
  @SuppressWarnings("unchecked")
  public <T> CustomDumper<? super T> getCustomDumper(Class<T> c) {
    return (CustomDumper<? super T>) customDumpers.get(c);
  }

  /**
   * Registers a custom dumper for the given class.<br>
   * See {@link CustomDumper}.
   *
   * @param c      The class
   * @param <T>    The type variable of the class
   * @param dumper The custom dumper
   */
  public <T> void addCustomDumper(Class<T> c, CustomDumper<? super T> dumper) {
    customDumpers.put(c, dumper);
  }

  /**
   * Unregisters a custom dumper for the given class.
   *
   * @param c The class
   */
  public void removeCustomDumper(Class<?> c) {
    customDumpers.remove(c);
  }

  // ///////////////////////////////////////////////////////////
  //
  // Internals
  //
  // ///////////////////////////////////////////////////////////

  private CharSequence dumpArray(Object array, Type type, int depth, String indentation) {
    StringBuilder builder = new StringBuilder().append(arrayDelimiters.getBeginDelimiter());

    if (depth == maxDepth) {
      return builder.append(STR_NOT_SHOWN).append(arrayDelimiters.getEndDelimiter());
    }

    int size;
    if ((size = Array.getLength(array)) == 0) {
      return builder.append(arrayDelimiters.getEndDelimiter());
    }

    builder.append(STR_LINE_SEPARATOR);

    Type componentType = Utils.getComponentType(type, 0, Utils.getComponentType(array.getClass()));
    for (int i = 0; i < size; i++) {
      builder
          .append(dumpAny(Array.get(array, i), componentType, "",
              depth + 1, indentation + indentationString))
          .append(STR_LINE_SEPARATOR);
    }

    return builder.append(indentation).append(arrayDelimiters.getEndDelimiter());
  }

  private CharSequence dumpCollection(Collection<?> collection, Type type, int depth,
                                      String indentation) {
    StringBuilder builder = new StringBuilder().append(collectionDelimiters.getBeginDelimiter());

    if (depth == maxDepth) {
      return builder.append(STR_NOT_SHOWN).append(collectionDelimiters.getEndDelimiter());
    }

    if (collection.isEmpty()) {
      return builder.append(collectionDelimiters.getEndDelimiter());
    }

    builder.append(STR_LINE_SEPARATOR);

    Type componentType = Utils.getComponentType(type);
    List<String> dumpedCollectionItems = new ArrayList<String>();
    for (Object item : collection) {
      dumpedCollectionItems.add(
          dumpAny(item, componentType, "", depth + 1, indentation + indentationString).toString());
    }

    if (collection instanceof Set && !(collection instanceof SortedSet)) {
      Collections.sort(dumpedCollectionItems);
    }

    for (String dumpedCollectionItem : dumpedCollectionItems) {
      builder.append(dumpedCollectionItem).append(STR_LINE_SEPARATOR);
    }

    return builder.append(indentation).append(collectionDelimiters.getEndDelimiter());
  }

  private CharSequence dumpMap(Map<?, ?> map, Type type, int depth, String indentation) {
    StringBuilder builder = new StringBuilder().append(mapDelimiters.getBeginDelimiter());

    if (depth == maxDepth) {
      return builder.append(STR_NOT_SHOWN).append(mapDelimiters.getEndDelimiter());
    }

    if (map.isEmpty()) {
      return builder.append(mapDelimiters.getEndDelimiter());
    }

    builder.append(STR_LINE_SEPARATOR);

    String nextIndentation = indentation + indentationString;
    Type keyType = Utils.getComponentType(type, 0);
    Type valueType = Utils.getComponentType(type, 1);

    List<Entry<String, String>> tempList = new ArrayList<Entry<String, String>>();
    for (Entry<?, ?> entry : map.entrySet()) {
      tempList.add(new SimpleEntry<String, String>(
          dumpAny(entry.getKey(), keyType, "", depth + 1, nextIndentation).toString(),
          dumpAny(entry.getValue(), valueType, "", depth + 1, nextIndentation).toString()));
    }

    if (!(map instanceof SortedMap)) {
      Collections.sort(tempList, new Comparator<Entry<String, String>>() {
        @Override
        public int compare(Entry<String, String> o1, Entry<String, String> o2) {
          int keysEqual = o1.getKey().compareTo(o2.getKey());
          return keysEqual != 0 ? keysEqual : o1.getValue().compareTo(o2.getValue());
        }
      });
    }

    for (Entry<String, String> tempListEntry : tempList) {
      builder
          .append(tempListEntry.getKey()).append(STR_LINE_SEPARATOR)
          .append(tempListEntry.getValue()).append(STR_LINE_SEPARATOR)
          .append(nextIndentation).append(STR_LINE_SEPARATOR);
    }

    if (tempList.size() > 0) {
      builder.setLength(
          builder.length() - STR_LINE_SEPARATOR.length() - nextIndentation.length());
    }

    return builder.append(indentation).append(mapDelimiters.getEndDelimiter());
  }

  private CharSequence dumpBeanProperty(Object bean, PropertyDescriptor descriptor, int depth,
                                        String indentation) {
    String name = descriptor.getName();
    for (PropertyFilter localPropertyFilter : propertyFilters.getSuperValues(bean.getClass())) {
      if (localPropertyFilter.excludeProperty(name)) {
        return "";
      }
    }

    StringBuilder builder = new StringBuilder();
    Object propertyValue = null;
    try {
      propertyValue = Utils.invoke(descriptor.getGetter(), bean);
    } catch (Exception e) {
      propertyValue = INVALID_OBJECT;
    }

    return builder.append(dumpAny(propertyValue, descriptor.getType(), name, depth, indentation));
  }

  private CharSequence dumpBean(Object bean, int depth, String indentation) {
    StringBuilder builder = new StringBuilder().append(beanDelimiters.getBeginDelimiter());

    if (depth == maxDepth) {
      return builder.append(STR_NOT_SHOWN).append(beanDelimiters.getEndDelimiter());
    }

    Collection<PropertyDescriptor> descriptors;
    if ((descriptors = Utils.describeClass(bean.getClass()).values()).isEmpty()) {
      return builder.append(beanDelimiters.getEndDelimiter());
    }

    builder.append(STR_LINE_SEPARATOR);
    for (PropertyDescriptor descriptor : descriptors) {
      builder
          .append(dumpBeanProperty(bean, descriptor, depth + 1, indentation + indentationString))
          .append(STR_LINE_SEPARATOR);
    }

    return builder.append(indentation).append(beanDelimiters.getEndDelimiter());
  }

  private CharSequence dumpAny(Object object, Type type, String name, int depth, String indentation) {
    StringBuilder builder = new StringBuilder().append(indentation);

    Class<?> c = object == null ? null : object.getClass();

    Long id = idStack.get(object);
    if (id == null) {
      id = Long.valueOf(currentId++);
    }

    if (includeId) {
      builder.append(String.format("%d", id)).append(" ");
    }

    if (includeName && name != null && !name.isEmpty()) {
      builder.append(name).append(" ");
    }

    if (includeDeclaredType && type != null) {
      builder.append(useSimpleNames ? Utils.formatTypeName(type) : type.toString()).append(" ");
    }

    if (includeActualType && c != null) {
      builder
          .append(object == INVALID_OBJECT
              ? STR_UNKNOWN
              : useSimpleNames ? c.getSimpleName() : c.getName())
          .append(" ");
    }

    if (object == INVALID_OBJECT) {
      return builder.append(STR_UNKNOWN);
    }

    if (idStack.containsKey(object)) {
      return builder.append(STR_REFERENCE);
    }

    idStack.put(object, id);
    builder.append(dumpAny0(object, type, depth, indentation));
    idStack.remove(object);
    return builder;
  }

  @SuppressWarnings("unchecked")
  private CharSequence dumpAny0(Object object, Type type, int depth, String indentation) {
    Class<?> c = object == null ? null : object.getClass();

    if (object == null) {
      return STR_NULL;
    }

    Set<CustomDumper<?>> localCustomDumpers = customDumpers.getSuperValues(c);
    if (!localCustomDumpers.isEmpty()) {
      return PATTERN_LINE_SEPARATOR.matcher(
          ((CustomDumper<Object>) localCustomDumpers.iterator().next()).dump(object))
          .replaceAll("$1" + indentation);
    }

    if (isImmutable(c)) {
      return object.toString();
    }

    if (Utils.isArray(c)) {
      return dumpArray(object, type, depth, indentation);
    }

    if (Utils.isCollection(c)) {
      return dumpCollection((Collection<?>) object, type, depth, indentation);
    }

    if (Utils.isMap(c)) {
      return dumpMap((Map<?, ?>) object, type, depth, indentation);
    }

    return dumpBean(object, depth, indentation);
  }

  // ///////////////////////////////////////////////////////////
  //
  // Public API
  //
  // ///////////////////////////////////////////////////////////

  /**
   * Dumps the given object to a string.
   *
   * @param object The object to dump
   * @return The dumped <b>object</b>
   */
  public String dump(Object object) {
    return dumpAny(object, null, "", 0, "").toString();
  }

  public static class DelimiterPair {
    private final String beginDelimiter;
    private final String endDelimiter;

    public DelimiterPair(String beginDelimiter, String endDelimiter) {
      this.beginDelimiter = beginDelimiter;
      this.endDelimiter = endDelimiter;
    }

    public String getBeginDelimiter() {
      return beginDelimiter;
    }

    public String getEndDelimiter() {
      return endDelimiter;
    }
  }
}
