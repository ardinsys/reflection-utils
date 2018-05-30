package eu.ardinsys.reflection.tool.proxy;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import eu.ardinsys.reflection.PropertyDescriptor;
import eu.ardinsys.reflection.MessageTemplates;
import eu.ardinsys.reflection.Utils;
import eu.ardinsys.reflection.tool.ReflectionBase;
import eu.ardinsys.reflection.tool.proxy.Selector.SelectorType;

/**
 * Utility to conveniently get or set values in nested data structures.
 * <p>
 * Public API:
 * <ul>
 * <li>{@link #ReflectionProxy(Object)}</li>
 * <li>{@link #set(String, Object)}</li>
 * <li>{@link #get(String)}</li>
 * </ul>
 */
public class ReflectionProxy extends ReflectionBase {
	/** The selector parser. */
	private final SelectorParser selectorParser;

	/** The object upon which the proxy acts. */
	private final Object rootObject;

	/**
	 * Creates a new {@link #ReflectionProxy} and sets the value of {@link #rootObject}.
	 * 
	 * @param rootObject
	 *          The {@link #rootObject}
	 */
	public ReflectionProxy(Object rootObject) {
		this(rootObject, new BasicSelectorParser());
	}

	/**
	 * Creates a new {@link ReflectionProxy} and sets the value of {@link #rootObject} and {@link #selectorParser}.
	 * 
	 * @param rootObject
	 *          The {@link #rootObject}
	 * @param selectorParser
	 *          The {@link #selectorParser}
	 */
	public ReflectionProxy(Object rootObject, SelectorParser selectorParser) {
		this.rootObject = rootObject;
		this.selectorParser = selectorParser;
		instanceProvider.setCompositeSize(0);
	}

	// ///////////////////////////////////////////////////////////
	//
	// Util
	//
	// ///////////////////////////////////////////////////////////

	private int calculateIndex(Selector selector, Object object) {
		SelectorType selectorType = selector.getType();
		if (object == null) {
			switch (selectorType) {
			case APPEND_INDEX:
			case LAST_INDEX:
				return 0;
			default:
				return selector.getNumericValue();
			}
		}

		Class<?> c = object.getClass();
		boolean isArray = Utils.isArray(c);
		boolean isList = Utils.isList(c);
		if (!isArray && !isList) {
			throw new RuntimeException(String.format(MessageTemplates.ERROR_TYPE_NOT_SEQUENCE, c));
		}

		int size = isArray ? Array.getLength(object) : ((List<?>) object).size();
		switch (selectorType) {
		case LAST_INDEX:
			return size - 1;
		case APPEND_INDEX:
			return size;
		default:
			return selector.getNumericValue();
		}
	}

	// ///////////////////////////////////////////////////////////
	//
	// Set
	//
	// ///////////////////////////////////////////////////////////

	@SuppressWarnings("unchecked")
	private void setProperty(Object object, Selector selector, Object value) {
		String selectorValue = selector.getValue();
		Class<?> c = object.getClass();
		PropertyDescriptor descriptor = Utils.describeClass(c).get(selectorValue);
		if (descriptor == null) {
			throw new RuntimeException(
					String.format(MessageTemplates.ERROR_MISSING_PROPERTY, object.getClass(), selectorValue));
		}

		List<Method> setters = descriptor.getSetters();
		if (!setters.isEmpty()) {
			Utils.invoke(setters.get(0), object, value);
		} else {
			Method getter = descriptor.getGetter();
			if (Utils.isCollectionGetter(getter)) {
				((Collection<Object>) Utils.invoke(getter, object))
						.addAll((Collection<Object>) value);
			} else if (Utils.isMapGetter(getter)) {
				((Map<Object, Object>) Utils.invoke(getter, object))
						.putAll((Map<Object, Object>) value);
			} else {
				throw new RuntimeException(
						String.format(MessageTemplates.ERROR_MISSING_PROPERTY_SETTER,
								selectorValue, c));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void setSequenceItem(Object object, Selector selector, Object value) {
		int index = calculateIndex(selector, object);
		if (selector.getType() == SelectorType.APPEND_INDEX) {
			index--;
		}

		Class<?> c = object.getClass();
		if (Utils.isList(c)) {
			((List<Object>) object).set(index, value);
		} else if (Utils.isArray(c)) {
			Array.set(object, index, value);
		} else {
			throw new RuntimeException(String.format(MessageTemplates.ERROR_TYPE_NOT_SEQUENCE, c));
		}
	}

	@SuppressWarnings("unchecked")
	private void setMapValue(Object object, Selector selector, Object value) {
		Class<?> c = object.getClass();
		if (!Utils.isMap(c)) {
			throw new RuntimeException(String.format(MessageTemplates.ERROR_TYPE_NOT_MAP, c));

		}

		((Map<String, Object>) object).put(selector.getValue(), value);
	}

	private void set(Object object, Selector selector, Object value) {
		if (object == null) {
			return;
		}

		switch (selector.getType()) {
		case PROPERTY:
			setProperty(object, selector, value);
			return;
		case INDEX:
		case APPEND_INDEX:
		case LAST_INDEX:
			setSequenceItem(object, selector, value);
			return;
		case KEY:
			setMapValue(object, selector, value);
			return;
		default:
			throw new RuntimeException();
		}
	}

	private void set(Object object, List<Selector> selectors, Object value) {
		if (object == null) {
			return;
		}

		get(object, selectors, true);
		set(get(object, selectors.subList(0, selectors.size() - 1), false),
				selectors.get(selectors.size() - 1),
				value);
	}

	// ///////////////////////////////////////////////////////////
	//
	// Get
	//
	// ///////////////////////////////////////////////////////////

	private Object getProperty(Object object, Selector selector) {
		String selectorValue = selector.getValue();
		Class<?> c = object.getClass();

		Map<String, PropertyDescriptor> descriptors = Utils.describeClass(c);
		PropertyDescriptor descriptor = descriptors.get(selectorValue);
		if (descriptor == null) {
			throw new RuntimeException(String.format(
					MessageTemplates.ERROR_MISSING_PROPERTY_GETTER, selectorValue, c));
		}

		return Utils.invoke(descriptor.getGetter(), object);
	}

	@SuppressWarnings("unchecked")
	private Object getSequenceItem(Object object, Selector selector) {
		int index = calculateIndex(selector, object);
		if (selector.getType() == SelectorType.APPEND_INDEX) {
			index--;
		}

		Class<?> c = object.getClass();
		if (Utils.isList(c)) {
			return ((List<Object>) object).get(index);
		}

		if (Utils.isArray(c)) {
			return Array.get(object, index);
		}

		throw new RuntimeException(String.format(MessageTemplates.ERROR_TYPE_NOT_SEQUENCE, c));
	}

	@SuppressWarnings("unchecked")
	private Object getMapValue(Object object, Selector selector) {
		Class<?> c = object.getClass();
		if (!Utils.isMap(c)) {
			throw new RuntimeException(String.format(MessageTemplates.ERROR_TYPE_NOT_MAP, c));
		}

		return ((Map<String, Object>) object).get(selector.getValue());
	}

	private Object get(Object object, Selector selector) {
		if (object == null) {
			return null;
		}

		switch (selector.getType()) {
		case PROPERTY:
			return getProperty(object, selector);
		case INDEX:
		case APPEND_INDEX:
		case LAST_INDEX:
			return getSequenceItem(object, selector);
		case KEY:
			return getMapValue(object, selector);
		default:
			throw new RuntimeException();
		}
	}

	private Object get(Object object, List<Selector> selectors, boolean byForce) {
		if (object == null) {
			return null;
		}

		if (byForce) {
			reallocate(object.getClass(), object, selectors);
		}

		Object value = object;
		for (Selector selector : selectors) {
			value = get(value, selector);
		}

		return value;
	}

	// ///////////////////////////////////////////////////////////
	//
	// Type
	//
	// ///////////////////////////////////////////////////////////

	private Type getPropertyType(Type type, Selector selector) {
		String selectorValue = selector.getValue();
		Class<?> c = Utils.getRawClass(type);

		PropertyDescriptor descriptor = Utils.describeClass(c).get(selectorValue);
		if (descriptor == null) {
			throw new RuntimeException(String.format(
					MessageTemplates.ERROR_MISSING_PROPERTY_GETTER, selectorValue, c));
		}

		return descriptor.getType();
	}

	@SuppressWarnings("unused")
	private Type getSequenceItemType(Type type, Selector selector) {
		Class<?> c = Utils.getRawClass(type);
		if (!Utils.isList(c) && !Utils.isArray(c)) {
			throw new RuntimeException(String.format(MessageTemplates.ERROR_TYPE_NOT_SEQUENCE, c));
		}

		return Utils.getComponentType(type);
	}

	@SuppressWarnings("unused")
	private Type getMapValueType(Type type, Selector selector) {
		Class<?> c = Utils.getRawClass(type);
		if (!Utils.isMap(c)) {
			throw new RuntimeException(String.format(MessageTemplates.ERROR_TYPE_NOT_MAP, c));
		}

		return Utils.getComponentType(type, 1);
	}

	private Type getType(Type type, Selector selector) {
		switch (selector.getType()) {
		case PROPERTY:
			return getPropertyType(type, selector);
		case INDEX:
		case APPEND_INDEX:
		case LAST_INDEX:
			return getSequenceItemType(type, selector);
		case KEY:
			return getMapValueType(type, selector);
		default:
			throw new RuntimeException();
		}
	}

	// ///////////////////////////////////////////////////////////
	//
	// Reallocate
	//
	// ///////////////////////////////////////////////////////////

	@SuppressWarnings("unused")
	private Object reallocateProperty(Type type, Object object, Selector selector) {
		return object;
	}

	@SuppressWarnings("unchecked")
	private Object reallocateSequenceItem(Type type, Object object, Selector selector) {
		int index = calculateIndex(selector, object);

		Class<?> c = Utils.getRawClass(type);
		if (Utils.isList(c)) {
			List<Object> list = (List<Object>) object;
			int size = list.size();
			if (index >= size) {
				for (int i = 0; i < index - size + 1; i++) {
					list.add(null);
				}
			}

			return object;
		}

		if (Utils.isArray(c)) {
			int length = Array.getLength(object);
			if (index >= length) {
				Object newArray = Array.newInstance(
						Utils.getRawClass(Utils.getComponentType(type)),
						index + 1);
				for (int i = 0; i < length; i++) {
					Array.set(newArray, i, Array.get(object, i));
				}

				return newArray;
			}

			return object;
		}

		throw new RuntimeException(String.format(MessageTemplates.ERROR_TYPE_NOT_SEQUENCE, c));
	}

	@SuppressWarnings("unused")
	private Object reallocateMapValue(Type type, Object object, Selector selector) {
		Class<?> c = Utils.getRawClass(type);
		if (!Utils.isMap(c)) {
			throw new RuntimeException(String.format(MessageTemplates.ERROR_TYPE_NOT_MAP, c));
		}

		return object;
	}

	private Object reallocate(Type type, Object object, Selector selector) {
		if (object == null) {
			object = instantiateType(type);
		}

		switch (selector.getType()) {
		case PROPERTY:
			return reallocateProperty(type, object, selector);
		case INDEX:
		case APPEND_INDEX:
		case LAST_INDEX:
			return reallocateSequenceItem(type, object, selector);
		case KEY:
			return reallocateMapValue(type, object, selector);
		default:
			throw new RuntimeException();
		}
	}

	private Object reallocate(Type type, Object object, List<Selector> selectors) {
		Selector selector = selectors.get(0);
		object = reallocate(type, object, selector);

		selectors = selectors.subList(1, selectors.size());
		if (!selectors.isEmpty()) {
			set(object, selector, reallocate(getType(type, selector), get(object, selector), selectors));
		}

		return object;
	}

	// ///////////////////////////////////////////////////////////
	//
	// Public API
	//
	// ///////////////////////////////////////////////////////////

	/**
	 * Sets a value in the {@link #rootObject}.<br>
	 * Note: <code>set</code> attempts to reallocate objects along its path if necessary (ie. null property, over-indexed
	 * array or list).<br>
	 * See {@link Selector}.
	 * 
	 * @param path
	 *          A concatenated string of selectors
	 * @param value
	 *          The value to set in the {@link #rootObject} at the location specified by the <b>path</b> parameter
	 */
	public void set(String path, Object value) {
		set(rootObject, selectorParser.parsePath(path), value);
	}

	/**
	 * Gets a value from the {@link #rootObject}.<br>
	 * See {@link Selector}.
	 * 
	 * @param path
	 *          A concatenated string of selectors
	 * @return The value in the {@link #rootObject} at the location specified by the <b>path</b> parameter
	 */
	public Object get(String path) {
		return get(rootObject, selectorParser.parsePath(path), false);
	}
}
