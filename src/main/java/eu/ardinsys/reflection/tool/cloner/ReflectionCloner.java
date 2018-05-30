package eu.ardinsys.reflection.tool.cloner;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import eu.ardinsys.reflection.ClassHashMap;
import eu.ardinsys.reflection.MessageTemplates;
import eu.ardinsys.reflection.PropertyDescriptor;
import eu.ardinsys.reflection.PropertyFilter;
import eu.ardinsys.reflection.Utils;
import eu.ardinsys.reflection.tool.ReflectionBase;

/**
 * Utility to produce deep copies of arbitrary objects.
 * <p>
 * Public API:
 * <ul>
 * <li>{@link #clone(Object)}</li>
 * <li>{@link #clone(Object, Class)}</li>
 * <li>{@link #clone(Object, Object)}</li>
 * </ul>
 * <p>
 * Customize with:
 * <ul>
 * <li>{@link #addCustomCloner(Class, Class, CustomCloner)}</li>
 * <li>{@link #removeCustomCloner(Class, Class)}</li>
 * <li>{@link #getCustomCloner(Class, Class)}</li>
 * </ul>
 */
public class ReflectionCloner extends ReflectionBase {
	private static final ClassHashMap<ClassHashMap<CustomCloner<?, ?>>> DEFAULT_CUSTOM_CLONERS;
	static {
		// TODO redundant! needs rework

		DEFAULT_CUSTOM_CLONERS = new ClassHashMap<>();

		ClassHashMap<CustomCloner<?, ?>> numberCloners = new ClassHashMap<CustomCloner<?, ?>>();
		numberCloners.put(Byte.class, new NumberToByteObjectCloner());
		numberCloners.put(byte.class, new NumberToByteCloner());
		numberCloners.put(Short.class, new NumberToShortObjectCloner());
		numberCloners.put(short.class, new NumberToShortCloner());
		numberCloners.put(Integer.class, new NumberToIntegerObjectCloner());
		numberCloners.put(int.class, new NumberToIntegerCloner());
		numberCloners.put(Long.class, new NumberToLongObjectCloner());
		numberCloners.put(long.class, new NumberToLongCloner());
		numberCloners.put(Float.class, new NumberToFloatObjectCloner());
		numberCloners.put(float.class, new NumberToFloatCloner());
		numberCloners.put(Double.class, new NumberToDoubleObjectCloner());
		numberCloners.put(double.class, new NumberToDoubleCloner());
		numberCloners.put(Boolean.class, new NumberToBooleanCloner());
		numberCloners.put(boolean.class, new NumberToBooleanCloner());
		numberCloners.put(BigInteger.class, new NumberToBigIntegerCloner());
		numberCloners.put(BigDecimal.class, new NumberToBigDecimalCloner());
		numberCloners.put(String.class, new NumberToStringCloner());
		DEFAULT_CUSTOM_CLONERS.put(Number.class, numberCloners);
		DEFAULT_CUSTOM_CLONERS.put(byte.class, numberCloners);
		DEFAULT_CUSTOM_CLONERS.put(short.class, numberCloners);
		DEFAULT_CUSTOM_CLONERS.put(int.class, numberCloners);
		DEFAULT_CUSTOM_CLONERS.put(long.class, numberCloners);
		DEFAULT_CUSTOM_CLONERS.put(float.class, numberCloners);
		DEFAULT_CUSTOM_CLONERS.put(double.class, numberCloners);

		ClassHashMap<CustomCloner<?, ?>> stringCloners = new ClassHashMap<CustomCloner<?, ?>>();
		stringCloners.put(Byte.class, new StringToByteCloner());
		stringCloners.put(byte.class, new StringToByteCloner());
		stringCloners.put(Short.class, new StringToShortCloner());
		stringCloners.put(short.class, new StringToShortCloner());
		stringCloners.put(Integer.class, new StringToIntegerCloner());
		stringCloners.put(int.class, new StringToIntegerCloner());
		stringCloners.put(Long.class, new StringToLongCloner());
		stringCloners.put(long.class, new StringToLongCloner());
		stringCloners.put(Float.class, new StringToFloatCloner());
		stringCloners.put(float.class, new StringToFloatCloner());
		stringCloners.put(Double.class, new StringToDoubleCloner());
		stringCloners.put(double.class, new StringToDoubleCloner());
		stringCloners.put(BigInteger.class, new StringToBigIntegerCloner());
		stringCloners.put(BigDecimal.class, new StringToBigDecimalCloner());
		DEFAULT_CUSTOM_CLONERS.put(String.class, stringCloners);

		ClassHashMap<CustomCloner<?, ?>> booleanCloners = new ClassHashMap<CustomCloner<?, ?>>();
		booleanCloners.put(Byte.class, new BooleanToByteCloner());
		booleanCloners.put(byte.class, new BooleanToByteCloner());
		booleanCloners.put(Short.class, new BooleanToShortCloner());
		booleanCloners.put(short.class, new BooleanToShortCloner());
		booleanCloners.put(Integer.class, new BooleanToIntegerCloner());
		booleanCloners.put(int.class, new BooleanToIntegerCloner());
		booleanCloners.put(Long.class, new BooleanToLongCloner());
		booleanCloners.put(long.class, new BooleanToLongCloner());
		booleanCloners.put(Float.class, new BooleanToFloatCloner());
		booleanCloners.put(float.class, new BooleanToFloatCloner());
		booleanCloners.put(Double.class, new BooleanToDoubleCloner());
		booleanCloners.put(double.class, new BooleanToDoubleCloner());
		booleanCloners.put(BigInteger.class, new BooleanToBigIntegerCloner());
		booleanCloners.put(BigDecimal.class, new BooleanToBigDecimalCloner());
		DEFAULT_CUSTOM_CLONERS.put(Boolean.class, booleanCloners);
		DEFAULT_CUSTOM_CLONERS.put(boolean.class, booleanCloners);

		ClassHashMap<CustomCloner<?, ?>> enumCloners = new ClassHashMap<CustomCloner<?, ?>>();
		enumCloners.put(String.class, new EnumToStringCloner());
		// TODO enum <-> ordinals?
		DEFAULT_CUSTOM_CLONERS.put(Enum.class, enumCloners);
	}

	private final Map<ClonerCacheKey, Object> cache = new HashMap<ClonerCacheKey, Object>();
	private final ClassHashMap<ClassHashMap<CustomCloner<?, ?>>> customCloners = new ClassHashMap<ClassHashMap<CustomCloner<?, ?>>>();

	/**
	 * Registers a custom cloner for the given classes.<br>
	 * See {@link CustomCloner}.
	 *
	 * @param sourceClass
	 *          The class of the object to clone
	 * @param targetClass
	 *          The class of the cloned object
	 * @param cloner
	 *          The custom cloner
	 */
	public <T, U> void addCustomCloner(Class<T> sourceClass, Class<U> targetClass,
			CustomCloner<? super T, ? extends U> cloner) {
		ClassHashMap<CustomCloner<?, ?>> sourceCustomCloners = customCloners.get(sourceClass);
		if (sourceCustomCloners == null) {
			sourceCustomCloners = new ClassHashMap<CustomCloner<?, ?>>();
			customCloners.put(sourceClass, sourceCustomCloners);
		}

		sourceCustomCloners.put(targetClass, cloner);
	}

	/**
	 * Unregisters a custom cloner for the given classes.
	 *
	 * @param sourceClass
	 *          The class of the object to clone
	 * @param targetClass
	 *          The class of the cloned object
	 */
	public void removeCustomCloner(Class<?> sourceClass, Class<?> targetClass) {
		ClassHashMap<CustomCloner<?, ?>> sourceCustomCloners = customCloners.get(sourceClass);
		if (sourceCustomCloners != null) {
			sourceCustomCloners.remove(targetClass);
		}
	}

	/**
	 * @param sourceClass
	 *          The class of the object to clone
	 * @param targetClass
	 *          The class of the cloned object
	 * @return The registered custom cloner for <code>c1</code> and <code>c2</code>
	 */
	@SuppressWarnings("unchecked")
	public <T, U> CustomCloner<? super T, ? extends U> getCustomCloner(
			Class<T> sourceClass, Class<U> targetClass) {
		ClassHashMap<CustomCloner<?, ?>> sourceCustomCloners = customCloners.get(sourceClass);
		return sourceCustomCloners == null
				? null
				: (CustomCloner<? super T, ? extends U>) sourceCustomCloners.get(targetClass);
	}

	// ///////////////////////////////////////////////////////////
	//
	// Internals
	//
	// ///////////////////////////////////////////////////////////

	@SuppressWarnings("unchecked")
	private Object instantiateAny(Object sourceObject, Type targetType) {
		if (sourceObject == null) {
			return null;
		}

		Class<?> sourceClass = sourceObject.getClass();
		Class<?> targetClass = Utils.getRawClass(targetType);

		if (isImmutable(sourceClass) && Utils.isAssignableFrom(targetClass, sourceClass)) {
			return sourceObject;
		}

		if (Utils.isArray(sourceClass) && !Utils.isCollection(targetClass)) {
			return Array.newInstance(
					Utils.getRawClass(Utils.getComponentType(targetType)),
					Array.getLength(sourceObject));
		}

		if (Utils.isCollection(sourceClass)) {
			return Utils.isArray(targetClass)
					? Array.newInstance(Utils.getRawClass(Utils.getComponentType(targetType)),
							((Collection<Object>) sourceObject).size())
					: instantiateForAbstraction(Collection.class, sourceClass, targetType);
		}

		if (Utils.isMap(sourceClass)) {
			return instantiateForAbstraction(Map.class, sourceClass, targetType);
		}

		return instantiateType(targetType);
	}

	private void cloneArray(Object sourceArray, Type sourceType, Object targetArray, Type targetType) {
		Type sourceComponentType = Utils.getComponentType(sourceType, 0, Utils.getComponentType(sourceArray.getClass()));
		Type targetComponentType = Utils.getComponentType(targetType, 0, Utils.getComponentType(targetArray.getClass()));
		int length = Math.min(Array.getLength(sourceArray), Array.getLength(targetArray));
		for (int i = 0; i < length; i++) {
			try {
				Array.set(targetArray, i, cloneAny(Array.get(sourceArray, i), sourceComponentType, targetComponentType));
			} catch (Exception e) {
				log(Level.WARNING, e.toString(), e);
				if (throwOnError) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private void cloneCollection(Collection<?> sourceCollection, Type sourceType, Collection<Object> targetCollection,
			Type targetType) {
		targetCollection.clear();

		Type sourceComponentType = Utils.getComponentType(sourceType);
		Type targetComponentType = Utils.getComponentType(targetType);
		for (Object sourceCollectionItem : sourceCollection) {
			try {
				targetCollection.add(cloneAny(sourceCollectionItem, sourceComponentType, targetComponentType));
			} catch (Exception e) {
				log(Level.WARNING, e.toString(), e);
				if (throwOnError) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private void cloneMap(Map<?, ?> sourceMap, Type sourceType, Map<Object, Object> targetMap, Type targetType) {
		targetMap.clear();

		Type sourceKeyType = Utils.getComponentType(sourceType, 0);
		Type sourceValueType = Utils.getComponentType(sourceType, 1);
		Type targetKeyType = Utils.getComponentType(targetType, 0);
		Type targetValueType = Utils.getComponentType(targetType, 1);
		for (Entry<?, ?> sourceMapEntry : sourceMap.entrySet()) {
			try {
				targetMap.put(
						cloneAny(sourceMapEntry.getKey(), sourceKeyType, targetKeyType),
						cloneAny(sourceMapEntry.getValue(), sourceValueType, targetValueType));
			} catch (Exception e) {
				log(Level.WARNING, e.toString(), e);
				if (throwOnError) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private boolean cloneBeanProperty(Object sourceBean, Object targetBean,
			PropertyDescriptor descriptor, Map<String, PropertyDescriptor> targetPropertyDescriptors) {
		// Check if target property exists
		String propertyName = descriptor.getName();
		PropertyDescriptor targetPropertyDescriptor = targetPropertyDescriptors.get(propertyName);
		if (targetPropertyDescriptor == null) {
			return false;
		}

		// Check if the property is excluded
		for (PropertyFilter localPropertyFilter : propertyFilters.getSuperValues(sourceBean.getClass())) {
			if (localPropertyFilter.excludeProperty(propertyName)) {
				return false;
			}
		}

		// Invoke the getter
		Method sourceGetter = descriptor.getGetter();
		Type sourcePropertyType = sourceGetter.getGenericReturnType();
		Object sourcePropertyValue = null;
		try {
			sourcePropertyValue = Utils.invoke(sourceGetter, sourceBean);
		} catch (Exception e) {
			log(Level.WARNING, e.toString(), e);
			if (throwOnError) {
				throw new RuntimeException(e);
			}

			return false;
		}

		// Invoke setter as 'normal' setter
		Method targetSetter = Utils.selectSetter(sourceGetter, targetPropertyDescriptor.getSetters());
		if (targetSetter != null) {
			try {
				Utils.invoke(
						targetSetter,
						targetBean,
						cloneAny(
								sourcePropertyValue,
								sourcePropertyType,
								targetSetter.getGenericParameterTypes()[0]));
				return true;
			} catch (Exception e) {
				log(Level.WARNING, e.toString(), e);
				if (throwOnError) {
					throw new RuntimeException(e);
				}

				return false;
			}
		}

		// Otherwise, check if it is a collection or map getter
		Method targetGetter = targetPropertyDescriptor.getGetter();
		boolean isCollectionGetter = Utils.isCollectionGetter(targetGetter);
		boolean isMapGetter = Utils.isMapGetter(targetGetter);
		if (!isCollectionGetter && !isMapGetter) {
			log(Level.INFO, String.format(MessageTemplates.INFO_NO_MATCHING_SETTER_FOUND,
					sourceBean.getClass().getName(), sourceGetter.getName()));
			return false;
		}

		// Invoke the getter
		Object targetPropertyValue = null;
		try {
			targetPropertyValue = Utils.invoke(targetGetter, targetBean);
		} catch (Exception e) {
			log(Level.WARNING, e.toString(), e);
			if (throwOnError) {
				throw new RuntimeException(e);
			}

			return false;
		}

		// Check if the collection/map is null
		if (targetPropertyValue == null) {
			return false;
		}

		// Fill the collection/map
		try {
			if (sourcePropertyValue != null) {
				if (isCollectionGetter) {
					((Collection<Object>) targetPropertyValue).addAll(
							(Collection<?>) cloneAny(sourcePropertyValue, sourcePropertyType, targetGetter.getGenericReturnType()));
				} else if (isMapGetter) {
					((Map<Object, Object>) targetPropertyValue).putAll(
							(Map<?, ?>) cloneAny(sourcePropertyValue, sourcePropertyType, targetGetter.getGenericReturnType()));
				}
			}

			return true;
		} catch (Exception e) {
			log(Level.WARNING, e.toString(), e);
			if (throwOnError) {
				throw new RuntimeException(e);
			}
		}

		return false;
	}

	private void cloneBean(Object sourceBean, Object targetBean) {
		Map<String, PropertyDescriptor> targetPropertyDescriptors = Utils.describeClass(targetBean.getClass());

		boolean anyPropertiesCloned = false;
		for (PropertyDescriptor descriptor : Utils.describeClass(sourceBean.getClass()).values()) {
			anyPropertiesCloned |= cloneBeanProperty(sourceBean, targetBean, descriptor, targetPropertyDescriptors);
		}

		if (!anyPropertiesCloned) {
			log(Level.INFO, String.format(MessageTemplates.INFO_NO_PROPERTIES_CLONED,
					sourceBean.getClass().getName(), targetBean.getClass().getName()));
		}
	}

	private Object cloneAny(Object sourceObject, Type sourceType, Type targetType) {
		ClonerCacheKey cacheKey = new ClonerCacheKey(sourceObject, sourceType, targetType);
		if (cache.containsKey(cacheKey)) {
			return cache.get(cacheKey);
		}

		Object targetObject = instantiateAny(sourceObject, targetType);
		targetObject = cloneAny0(sourceObject, sourceType, targetObject, targetType);
		cache.put(cacheKey, targetObject);
		return targetObject;
	}

	@SuppressWarnings("unchecked")
	private Object cloneAny0(Object sourceObject, Type sourceType, Object targetObject, Type targetType) {
		Class<?> sourceClass = sourceObject == null
				? Utils.getRawClass(sourceType)
				: sourceObject.getClass();
		Class<?> targetClass = Utils.getRawClass(targetType);

		// Do not attempt to find cloners for Object types
		if (targetClass == Object.class) {
			targetClass = targetObject.getClass();
		}

		for (ClassHashMap<CustomCloner<?, ?>> sourceCustomCloners : customCloners
				.getSuperValues(sourceClass)) {
			Set<CustomCloner<?, ?>> targetCustomCloners = sourceCustomCloners.getSubValues(targetClass);
			if (!targetCustomCloners.isEmpty()) {
				return ((CustomCloner<Object, Object>) targetCustomCloners.iterator().next())
						.clone(sourceObject, targetClass);
			}
		}

		for (ClassHashMap<CustomCloner<?, ?>> sourceCustomCloners : DEFAULT_CUSTOM_CLONERS
				.getSuperValues(sourceClass)) {
			Set<CustomCloner<?, ?>> targetCustomCloners = sourceCustomCloners.getSubValues(targetClass);
			if (!targetCustomCloners.isEmpty()) {
				return ((CustomCloner<Object, Object>) targetCustomCloners.iterator().next())
						.clone(sourceObject, targetClass);
			}
		}

		// Special case, not possible with the existing cloning hierarchy
		if (sourceClass == String.class && targetClass.isEnum()) {
			for (Object enumConstant : targetClass.getEnumConstants()) {
				if (((Enum<?>) enumConstant).name().equals(sourceObject)) {
					return enumConstant;
				}
			}

			return null;
		}

		if (sourceObject == null) {
			return null;
		}

		if (isImmutable(sourceClass)) {
			// do nothing
		} else if (Utils.isArray(sourceClass)) {
			if (Utils.isArray(targetClass)) {
				cloneArray(sourceObject, sourceType, targetObject, targetType);
			} else if (Utils.isCollection(targetClass)) {
				int length = Array.getLength(sourceObject);
				Object[] sourceArray = new Object[length];
				for (int i = 0; i < length; i++) {
					sourceArray[i] = Array.get(sourceObject, i);
				}

				cloneCollection(Arrays.asList(sourceArray), sourceType, (Collection<Object>) targetObject, targetType);
			}
		} else if (Utils.isCollection(sourceClass)) {
			if (Utils.isCollection(targetClass)) {
				cloneCollection((Collection<?>) sourceObject, sourceType, (Collection<Object>) targetObject, targetType);
			} else if (Utils.isArray(targetClass)) {
				cloneArray(
						((Collection<Object>) sourceObject).toArray(new Object[0]),
						sourceType,
						targetObject,
						targetType);
			}
		} else if (Utils.isMap(sourceClass)) {
			cloneMap((Map<?, ?>) sourceObject, sourceType, (Map<Object, Object>) targetObject, targetType);
		} else {
			cloneBean(sourceObject, targetObject);
		}

		return targetObject;
	}

	// ///////////////////////////////////////////////////////////
	//
	// Public API
	//
	// ///////////////////////////////////////////////////////////

	/**
	 * Clones an object into another.
	 *
	 * @param sourceObject
	 *          The source object to clone from
	 * @param targetObject
	 *          The target object to clone into
	 * @return <b>targetObject</b> with the contents of <b>sourceObject</b>
	 */
	public Object clone(Object sourceObject, Object targetObject) {
		cache.clear();
		return sourceObject == null || targetObject == null
				? null
				: cloneAny0(sourceObject, sourceObject.getClass(), targetObject, null);
	}

	/**
	 * Clones an object.
	 *
	 * @param sourceObject
	 *          The source object to clone
	 * @return A clone of <b>sourceObject</b>
	 */
	@SuppressWarnings("unchecked")
	public <T> T clone(T sourceObject) {
		cache.clear();
		return sourceObject == null
				? null
				: clone(sourceObject, (Class<T>) sourceObject.getClass());
	}

	/**
	 * Clones an object into an object of given class.
	 *
	 * @param sourceObject
	 *          The source object to copy from
	 * @param targetClass
	 *          The target class to instantiate and copy into
	 * @return An instance of <b>targetClass</b> with the contents of <b>sourceObject</b>.
	 */
	public <T> T clone(Object sourceObject, Class<T> targetClass) {
		cache.clear();
		return sourceObject == null
				? null
				: Utils.cast(targetClass, cloneAny(sourceObject, sourceObject.getClass(), targetClass));
	}

	private static class ClonerCacheKey {
		private final Object sourceObject;
		private final Type sourceType;
		private final Type targetType;

		public ClonerCacheKey(Object sourceObject, Type sourceType, Type targetType) {
			this.sourceObject = sourceObject;
			this.sourceType = sourceType;
			this.targetType = targetType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((sourceObject == null) ? 0 : sourceObject.hashCode());
			result = prime * result + ((sourceType == null) ? 0 : sourceType.hashCode());
			result = prime * result + ((targetType == null) ? 0 : targetType.hashCode());
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

			ClonerCacheKey other = (ClonerCacheKey) obj;

			if (sourceObject == null) {
				if (other.sourceObject != null) {
					return false;
				}
			} else if (!sourceObject.equals(other.sourceObject)) {
				return false;
			}

			if (sourceType == null) {
				if (other.sourceType != null) {
					return false;
				}
			} else if (!sourceType.equals(other.sourceType)) {
				return false;
			}

			if (targetType == null) {
				if (other.targetType != null) {
					return false;
				}
			} else if (!targetType.equals(other.targetType)) {
				return false;
			}

			return true;
		}
	}

	// ///////////////////////////////////////////////////////////
	//
	// Custom cloners
	//
	// ///////////////////////////////////////////////////////////

	private static class NumberToByteObjectCloner implements CustomCloner<Number, Byte> {
		@Override
		public Byte clone(Number object, Class<? extends Byte> targetClass) {
			return object == null
					? null
					: Byte.valueOf(object.byteValue());
		}
	}

	private static class NumberToByteCloner implements CustomCloner<Number, Byte> {
		@Override
		public Byte clone(Number object, Class<? extends Byte> targetClass) {
			return Byte.valueOf(object == null
					? 0
					: object.byteValue());
		}
	}

	private static class NumberToShortObjectCloner implements CustomCloner<Number, Short> {
		@Override
		public Short clone(Number object, Class<? extends Short> targetClass) {
			return object == null
					? null
					: Short.valueOf(object.shortValue());
		}
	}

	private static class NumberToShortCloner implements CustomCloner<Number, Short> {
		@Override
		public Short clone(Number object, Class<? extends Short> targetClass) {
			return Short.valueOf(object == null
					? 0
					: object.shortValue());
		}
	}

	private static class NumberToIntegerObjectCloner implements CustomCloner<Number, Integer> {
		@Override
		public Integer clone(Number object, Class<? extends Integer> targetClass) {
			return object == null
					? null
					: Integer.valueOf(object.intValue());
		}
	}

	private static class NumberToIntegerCloner implements CustomCloner<Number, Integer> {
		@Override
		public Integer clone(Number object, Class<? extends Integer> targetClass) {
			return Integer.valueOf(object == null
					? 0
					: object.intValue());
		}
	}

	private static class NumberToLongObjectCloner implements CustomCloner<Number, Long> {
		@Override
		public Long clone(Number object, Class<? extends Long> targetClass) {
			return object == null
					? null
					: Long.valueOf(object.longValue());
		}
	}

	private static class NumberToLongCloner implements CustomCloner<Number, Long> {
		@Override
		public Long clone(Number object, Class<? extends Long> targetClass) {
			return Long.valueOf(object == null
					? 0
					: object.longValue());
		}
	}

	private static class NumberToFloatObjectCloner implements CustomCloner<Number, Float> {
		@Override
		public Float clone(Number object, Class<? extends Float> targetClass) {
			return object == null
					? null
					: Float.valueOf(object.floatValue());
		}
	}

	private static class NumberToFloatCloner implements CustomCloner<Number, Float> {
		@Override
		public Float clone(Number object, Class<? extends Float> targetClass) {
			return Float.valueOf(object == null
					? 0
					: object.floatValue());
		}
	}

	private static class NumberToDoubleObjectCloner implements CustomCloner<Number, Double> {
		@Override
		public Double clone(Number object, Class<? extends Double> targetClass) {
			return object == null
					? null
					: Double.valueOf(object.doubleValue());
		}
	}

	private static class NumberToDoubleCloner implements CustomCloner<Number, Double> {
		@Override
		public Double clone(Number object, Class<? extends Double> targetClass) {
			return Double.valueOf(object == null
					? 0
					: object.doubleValue());
		}
	}

	private static class NumberToBooleanCloner implements CustomCloner<Number, Boolean> {
		@Override
		public Boolean clone(Number object, Class<? extends Boolean> targetClass) {
			return Boolean.valueOf(object == null
					? false
					: (object.doubleValue() != 0));
		}
	}

	private static class NumberToBigIntegerCloner implements CustomCloner<Number, BigInteger> {
		@Override
		public BigInteger clone(Number object, Class<? extends BigInteger> targetClass) {
			return object == null
					? null
					: BigInteger.valueOf(object.longValue());
		}
	}

	private static class NumberToBigDecimalCloner implements CustomCloner<Number, BigDecimal> {
		@Override
		public BigDecimal clone(Number object, Class<? extends BigDecimal> targetClass) {
			return object == null
					? null
					: BigDecimal.valueOf(object.doubleValue());
		}
	}

	private static class NumberToStringCloner implements CustomCloner<Number, String> {
		@Override
		public String clone(Number object, Class<? extends String> targetClass) {
			return object == null
					? "0"
					: object.toString();
		}
	}

	private static class StringToByteCloner implements CustomCloner<String, Byte> {
		@Override
		public Byte clone(String object, Class<? extends Byte> targetClass) {
			return Byte.valueOf(Double.valueOf(object).byteValue());
		}
	}

	private static class StringToShortCloner implements CustomCloner<String, Short> {
		@Override
		public Short clone(String object, Class<? extends Short> targetClass) {
			return Short.valueOf(Double.valueOf(object).shortValue());
		}
	}

	private static class StringToIntegerCloner implements CustomCloner<String, Integer> {
		@Override
		public Integer clone(String object, Class<? extends Integer> targetClass) {
			return Integer.valueOf(Double.valueOf(object).intValue());
		}
	}

	private static class StringToLongCloner implements CustomCloner<String, Long> {
		@Override
		public Long clone(String object, Class<? extends Long> targetClass) {
			return Long.valueOf(new BigDecimal(object).longValue());
		}
	}

	private static class StringToFloatCloner implements CustomCloner<String, Float> {
		@Override
		public Float clone(String object, Class<? extends Float> targetClass) {
			return Float.valueOf(object);
		}
	}

	private static class StringToDoubleCloner implements CustomCloner<String, Double> {
		@Override
		public Double clone(String object, Class<? extends Double> targetClass) {
			return Double.valueOf(object);
		}
	}

	private static class StringToBigIntegerCloner implements CustomCloner<String, BigInteger> {
		@Override
		public BigInteger clone(String object, Class<? extends BigInteger> targetClass) {
			return new BigDecimal(object).toBigInteger();
		}
	}

	private static class StringToBigDecimalCloner implements CustomCloner<String, BigDecimal> {
		@Override
		public BigDecimal clone(String object, Class<? extends BigDecimal> targetClass) {
			return new BigDecimal(object);
		}
	}

	private static class BooleanToByteCloner implements CustomCloner<Boolean, Byte> {
		@Override
		public Byte clone(Boolean object, Class<? extends Byte> targetClass) {
			return Byte.valueOf(object == null
					? 0
					: (byte) (object.booleanValue() ? 1 : 0));
		}
	}

	private static class BooleanToShortCloner implements CustomCloner<Boolean, Short> {
		@Override
		public Short clone(Boolean object, Class<? extends Short> targetClass) {
			return Short.valueOf(object == null
					? 0
					: (short) (object.booleanValue() ? 1 : 0));
		}
	}

	private static class BooleanToIntegerCloner implements CustomCloner<Boolean, Integer> {
		@Override
		public Integer clone(Boolean object, Class<? extends Integer> targetClass) {
			return Integer.valueOf(object == null
					? 0
					: (object.booleanValue() ? 1 : 0));
		}
	}

	private static class BooleanToLongCloner implements CustomCloner<Boolean, Long> {
		@Override
		public Long clone(Boolean object, Class<? extends Long> targetClass) {
			return Long.valueOf(object == null
					? 0
					: (object.booleanValue() ? 1 : 0));
		}
	}

	private static class BooleanToFloatCloner implements CustomCloner<Boolean, Float> {
		@Override
		public Float clone(Boolean object, Class<? extends Float> targetClass) {
			return Float.valueOf(object == null
					? 0
					: (object.booleanValue() ? 1 : 0));
		}
	}

	private static class BooleanToDoubleCloner implements CustomCloner<Boolean, Double> {
		@Override
		public Double clone(Boolean object, Class<? extends Double> targetClass) {
			return Double.valueOf(object == null
					? 0
					: (object.booleanValue() ? 1 : 0));
		}
	}

	private static class BooleanToBigIntegerCloner implements CustomCloner<Boolean, BigInteger> {
		@Override
		public BigInteger clone(Boolean object, Class<? extends BigInteger> targetClass) {
			return BigInteger.valueOf(object == null
					? 0
					: (object.booleanValue() ? 1 : 0));
		}
	}

	private static class BooleanToBigDecimalCloner implements CustomCloner<Boolean, BigDecimal> {
		@Override
		public BigDecimal clone(Boolean object, Class<? extends BigDecimal> targetClass) {
			return BigDecimal.valueOf(object == null
					? 0
					: (object.booleanValue() ? 1 : 0));
		}
	}

	private static class EnumToStringCloner implements CustomCloner<Enum<?>, String> {
		@Override
		public String clone(Enum<?> object, Class<? extends String> targetClass) {
			return object == null
					? null
					: object.name();
		}
	}
}
