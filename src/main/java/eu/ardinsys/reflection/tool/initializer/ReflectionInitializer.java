package eu.ardinsys.reflection.tool.initializer;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import eu.ardinsys.reflection.ClassHashMap;
import eu.ardinsys.reflection.PropertyDescriptor;
import eu.ardinsys.reflection.PropertyFilter;
import eu.ardinsys.reflection.Utils;
import eu.ardinsys.reflection.tool.ReflectionBase;

/**
 * Utility to initialize objects with default values.
 * <p>
 * Public API:
 * <ul>
 * <li>{@link #initialize(Class)}</li>
 * <li>{@link #initialize(Object)}</li>
 * </ul>
 * <p>
 * Customize with:
 * <ul>
 * <li>{@link #setRecursionLimit(int)}</li>
 * <li>{@link #addCustomInitializer(Class, CustomInitializer)}</li>
 * <li>{@link #removeCustomInitializer(Class)}</li>
 * <li>{@link #getCustomInitializer(Class)}</li>
 * </ul>
 */
public class ReflectionInitializer extends ReflectionBase {
	private static final Object INVALID_OBJECT = new Object();

	private final Map<Type, Integer> recursionLimits = new HashMap<Type, Integer>();
	private final ClassHashMap<CustomInitializer<?>> customInitializers = new ClassHashMap<CustomInitializer<?>>();

	/**
	 * The recursion limit. If a class recursively contains itself, the nested instances past this limit will not be
	 * initialized (in order to prevent stack overflow).<br>
	 * Default value: <code>3</code>.
	 */
	private int recursionLimit = 3;

	/**
	 * @return The current value of {@link #recursionLimit}
	 */
	public int getRecursionLimit() {
		return recursionLimit;
	}

	/**
	 * @param recursionLimit
	 *          The new value of {@link #recursionLimit}
	 */
	public void setRecursionLimit(int recursionLimit) {
		this.recursionLimit = recursionLimit;
	}

	/**
	 * @param c
	 *          The class
	 * @return The registered custom initializer for <code>c</code>
	 */
	@SuppressWarnings("unchecked")
	public <T> CustomInitializer<? super T> getCustomInitializer(Class<T> c) {
		return (CustomInitializer<? super T>) customInitializers.get(c);
	}

	/**
	 * Registers a custom initializer for the given class.<br>
	 * See {@link CustomInitializer}.
	 *
	 * @param c
	 *          The class
	 * @param initializer
	 *          The custom initializer
	 */
	public <T> void addCustomInitializer(Class<T> c, CustomInitializer<? super T> initializer) {
		customInitializers.put(c, initializer);
	}

	/**
	 * Unregisters a custom initializer for the given class.
	 *
	 * @param c
	 *          The class
	 */
	public void removeCustomInitializer(Class<?> c) {
		customInitializers.remove(c);
	}

	// ///////////////////////////////////////////////////////////
	//
	// Internals
	//
	// ///////////////////////////////////////////////////////////

	private void initializeArray(Object array, Type type) {
		if (array == null) {
			return;
		}

		Type componentType = Utils.getComponentType(type, 0, Utils.getComponentType(array.getClass()));
		int size = Array.getLength(array);
		for (int i = 0; i < size; i++) {
			try {
				Object item = initializeAny(componentType);
				if (item != INVALID_OBJECT) {
					Array.set(array, i, item);
				}
			} catch (Exception e) {
				log(Level.WARNING, e.toString(), e);
				if (throwOnError) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private void initializeCollection(Collection<Object> collection, Type type) {
		if (collection == null) {
			return;
		}

		collection.clear();
		Type componentType = Utils.getComponentType(type);
		int size = instanceProvider.getCompositeSize();
		for (int i = 0; i < size; i++) {
			try {
				Object item = initializeAny(componentType);
				if (item != INVALID_OBJECT) {
					collection.add(item);
				}
			} catch (Exception e) {
				log(Level.WARNING, e.toString(), e);
				if (throwOnError) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private void initializeMap(Map<Object, Object> map, Type type) {
		if (map == null) {
			return;
		}

		map.clear();
		Type keyType = Utils.getComponentType(type, 0);
		Type valueType = Utils.getComponentType(type, 1);
		int size = instanceProvider.getCompositeSize();
		for (int i = 0; i < size; i++) {
			try {
				Object key = initializeAny(keyType);
				Object value = initializeAny(valueType);
				if (key != INVALID_OBJECT && value != INVALID_OBJECT) {
					map.put(key, value);
				}
			} catch (Exception e) {
				log(Level.WARNING, e.toString(), e);
				if (throwOnError) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void initializeBeanProperty(Object bean, PropertyDescriptor descriptor) {
		for (PropertyFilter localPropertyFilter : propertyFilters.getSuperValues(bean.getClass())) {
			if (localPropertyFilter.excludeProperty(descriptor.getName())) {
				return;
			}
		}

		try {
			List<Method> setters = descriptor.getSetters();
			if (!setters.isEmpty()) {
				Method setter = Utils.selectSetter(descriptor.getGetter(), setters);
				Object value = initializeAny(setter.getGenericParameterTypes()[0]);
				if (value != INVALID_OBJECT) {
					if (descriptor.getType() == String.class) {
						value = descriptor.getName() + "_" + value;
					}

					Utils.invoke(setter, bean, value);
				}
			} else {
				Method getter = descriptor.getGetter();
				if (Utils.isCollectionGetter(getter)) {
					initializeCollection(
							(Collection<Object>) Utils.invoke(getter, bean),
							getter.getGenericReturnType());
				} else if (Utils.isMapGetter(getter)) {
					initializeMap(
							(Map<Object, Object>) Utils.invoke(getter, bean),
							getter.getGenericReturnType());
				}
			}
		} catch (Exception e) {
			log(Level.WARNING, e.toString(), e);
			if (throwOnError) {
				throw new RuntimeException(e);
			}
		}
	}

	private void initializeBean(Object bean) {
		if (bean == null) {
			return;
		}

		for (PropertyDescriptor descriptor : Utils.describeClass(bean.getClass()).values()) {
			initializeBeanProperty(bean, descriptor);
		}
	}

	private Object initializeAny(Type type) {
		Integer typeRecursionLimit = recursionLimits.get(type);
		if (typeRecursionLimit == null) {
			typeRecursionLimit = Integer.valueOf(0);
		}

		if (typeRecursionLimit.intValue() > recursionLimit) {
			return INVALID_OBJECT;
		}

		Object object = null;
		try {
			recursionLimits.put(type, Integer.valueOf(typeRecursionLimit.intValue() + 1));
			object = instantiateType(type);
			initializeAny0(object, type);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			recursionLimits.put(type, Integer.valueOf(typeRecursionLimit.intValue()));
		}

		return object;
	}

	@SuppressWarnings("unchecked")
	private void initializeAny0(Object object, Type type) {
		Class<?> c = object.getClass();

		Set<CustomInitializer<?>> localCustomInitializers = customInitializers.getSuperValues(c);
		if (!localCustomInitializers.isEmpty()) {
			((CustomInitializer<Object>) localCustomInitializers.iterator().next()).initialize(object);
			return;
		}

		if (isImmutable(c)) {
			// do nothing
		} else if (Utils.isArray(c)) {
			initializeArray(object, type);
		} else if (Utils.isCollection(c)) {
			initializeCollection((Collection<Object>) object, type);
		} else if (Utils.isMap(c)) {
			initializeMap((Map<Object, Object>) object, type);
		} else {
			initializeBean(object);
		}
	}

	// ///////////////////////////////////////////////////////////
	//
	// Public API
	//
	// ///////////////////////////////////////////////////////////

	/**
	 * Initializes the given object. Does nothing if the object is immutable.
	 *
	 * @param object
	 *          The object to initialize
	 * @return The initialized object.
	 * @see Utils#isImmutable(Class)
	 */
	public <T> T initialize(T object) {
		if (object != null) {
			initializeAny0(object, object.getClass());
		}

		return object;
	}

	/**
	 * Instantiates the given class and initializes the instance.
	 *
	 * @param c
	 *          The class
	 * @return The initialized instance of <b>c</b>
	 */
	public <T> T initialize(Class<T> c) {
		return c == null ? null : Utils.cast(c, initializeAny(c));
	}
}
