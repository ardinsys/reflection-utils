package eu.ardinsys.reflection.tool;

import eu.ardinsys.reflection.ClassHashMap;
import eu.ardinsys.reflection.MessageTemplates;
import eu.ardinsys.reflection.Utils;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;
import java.util.UUID;

/**
 * Default {@link InstanceProvider} implementation. Primitive or boxed values are 'cycled' (ie. successive
 * <code>byte</code> instances will be 0, 1, ..., 127, 0, ...).
 */
public class BasicInstanceProvider implements InstanceProvider {
  private final ClassHashMap<TypedInstanceProvider> typedInstanceProviders = new ClassHashMap<TypedInstanceProvider>();
  private int compositeSize = 2;

  public BasicInstanceProvider() {
    typedInstanceProviders.put(Object.class, new ObjectInstanceProvider());
    typedInstanceProviders.put(Byte.class, new ByteInstanceProvider());
    typedInstanceProviders.put(byte.class, new ByteInstanceProvider());
    typedInstanceProviders.put(Short.class, new ShortInstanceProvider());
    typedInstanceProviders.put(short.class, new ShortInstanceProvider());
    typedInstanceProviders.put(Integer.class, new IntegerInstanceProvider());
    typedInstanceProviders.put(int.class, new IntegerInstanceProvider());
    typedInstanceProviders.put(Long.class, new LongInstanceProvider());
    typedInstanceProviders.put(long.class, new LongInstanceProvider());
    typedInstanceProviders.put(Float.class, new FloatInstanceProvider());
    typedInstanceProviders.put(float.class, new FloatInstanceProvider());
    typedInstanceProviders.put(Double.class, new DoubleInstanceProvider());
    typedInstanceProviders.put(double.class, new DoubleInstanceProvider());
    typedInstanceProviders.put(Character.class, new CharacterInstanceProvider());
    typedInstanceProviders.put(char.class, new CharacterInstanceProvider());
    typedInstanceProviders.put(Boolean.class, new BooleanInstanceProvider());
    typedInstanceProviders.put(boolean.class, new BooleanInstanceProvider());
    typedInstanceProviders.put(BigInteger.class, new BigIntegerInstanceProvider());
    typedInstanceProviders.put(BigDecimal.class, new BigDecimalInstanceProvider());
    typedInstanceProviders.put(String.class, new StringInstanceProvider());
    typedInstanceProviders.put(Date.class, new DateInstanceProvider());
    typedInstanceProviders.put(UUID.class, new UUIDInstanceProvider());
    typedInstanceProviders.put(Timestamp.class, new TimestampInstanceProvider());
    typedInstanceProviders.put(XMLGregorianCalendar.class,
        new XMLGregorianCalendarInstanceProvider());
  }

  /**
   * @see InstanceProvider#getCompositeSize()
   */
  @Override
  public int getCompositeSize() {
    return compositeSize;
  }

  /**
   * @see InstanceProvider#setCompositeSize(int)
   */
  @Override
  public void setCompositeSize(int compositeSize) {
    this.compositeSize = compositeSize;
  }

  /**
   * @see InstanceProvider#provideInstance(Class)
   */
  @Override
  public <T> T provideInstance(Class<T> c) {
    if (c.isArray()) {
      return Utils.cast(c, Array.newInstance(c.getComponentType(), compositeSize));
    }

    if (c.isEnum()) {
      T[] enumConstants = c.getEnumConstants();
      if (enumConstants.length == 0) {
        throw new RuntimeException(String.format(
            MessageTemplates.ERROR_INSTANTIATE_EMPTY_ENUM, c.getName()));
      }

      return enumConstants[0];
    }

    Set<TypedInstanceProvider> localProviders = typedInstanceProviders.getSubValues(c);
    if (localProviders.iterator().hasNext()) {
      return Utils.cast(c, localProviders.iterator().next().provideInstance());
    }

    return Utils.instantiate(c);
  }

  private interface TypedInstanceProvider {
    Object provideInstance();
  }

  private static class ObjectInstanceProvider implements TypedInstanceProvider {
    @Override
    public Object provideInstance() {
      return new Object();
    }
  }

  private static abstract class NumberInstanceProvider implements TypedInstanceProvider {
    private final BigDecimal minValue;
    private final BigDecimal maxValue;
    private BigDecimal value;

    protected NumberInstanceProvider(double minValue, double maxValue) {
      this(BigDecimal.valueOf(minValue), BigDecimal.valueOf(maxValue));
    }

    protected NumberInstanceProvider(BigDecimal minValue, BigDecimal maxValue) {
      this.minValue = minValue;
      this.maxValue = maxValue;
      this.value = minValue;
    }

    protected BigDecimal provideInstance0() {
      BigDecimal nextValue = value.add(BigDecimal.ONE);
      return value = (nextValue.compareTo(maxValue) == -1 ? nextValue : minValue);
    }
  }

  private static class ByteInstanceProvider extends NumberInstanceProvider {
    public ByteInstanceProvider() {
      super(0, Byte.MAX_VALUE);
    }

    @Override
    public Object provideInstance() {
      return Byte.valueOf(provideInstance0().byteValue());
    }
  }

  private static class ShortInstanceProvider extends NumberInstanceProvider {
    public ShortInstanceProvider() {
      super(Byte.MAX_VALUE + 1, Short.MAX_VALUE);
    }

    @Override
    public Object provideInstance() {
      return Short.valueOf(provideInstance0().shortValue());
    }
  }

  private static class IntegerInstanceProvider extends NumberInstanceProvider {
    public IntegerInstanceProvider() {
      super(Short.MAX_VALUE + 1, Integer.MAX_VALUE);
    }

    @Override
    public Object provideInstance() {
      return Integer.valueOf(provideInstance0().intValue());
    }
  }

  private static class LongInstanceProvider extends NumberInstanceProvider {
    public LongInstanceProvider() {
      super(Integer.MAX_VALUE + 1, Long.MAX_VALUE);
    }

    @Override
    public Object provideInstance() {
      return Long.valueOf(provideInstance0().longValue());
    }
  }

  private static class FloatInstanceProvider extends NumberInstanceProvider {
    public FloatInstanceProvider() {
      super(0.1, Float.MAX_VALUE);
    }

    @Override
    public Object provideInstance() {
      return Float.valueOf(provideInstance0().floatValue());
    }
  }

  private static class DoubleInstanceProvider extends NumberInstanceProvider {
    public DoubleInstanceProvider() {
      super(0.2, Double.MAX_VALUE);
    }

    @Override
    public Object provideInstance() {
      return Double.valueOf(provideInstance0().doubleValue());
    }
  }

  private static class BigIntegerInstanceProvider extends NumberInstanceProvider {
    public BigIntegerInstanceProvider() {
      super(Math.pow(10, 10), Math.pow(10, 20));
    }

    @Override
    public Object provideInstance() {
      return provideInstance0().toBigInteger();
    }
  }

  private static class BigDecimalInstanceProvider extends NumberInstanceProvider {
    public BigDecimalInstanceProvider() {
      super(0.3, Double.MAX_VALUE);
    }

    @Override
    public Object provideInstance() {
      return provideInstance0();
    }
  }

  private static class CharacterInstanceProvider extends NumberInstanceProvider {
    public CharacterInstanceProvider() {
      super(0, Integer.MAX_VALUE);
    }

    @Override
    public Object provideInstance() {
      return Character.valueOf((char) provideInstance0().intValue());
    }
  }

  private static class BooleanInstanceProvider implements TypedInstanceProvider {
    private Boolean value = Boolean.FALSE;

    @Override
    public Object provideInstance() {
      return (value = Boolean.valueOf(!value.booleanValue()));
    }
  }

  private static class StringInstanceProvider implements TypedInstanceProvider {
    private final SecureRandom random = new SecureRandom();

    @Override
    public Object provideInstance() {
      return new BigInteger(130, random).toString(32);
    }
  }

  private static class DateInstanceProvider implements TypedInstanceProvider {
    @Override
    public Object provideInstance() {
      return new Date();
    }
  }

  private static class UUIDInstanceProvider implements TypedInstanceProvider {
    @Override
    public Object provideInstance() {
      return UUID.randomUUID();
    }
  }

  private static class TimestampInstanceProvider implements TypedInstanceProvider {
    @Override
    public Object provideInstance() {
      return new Timestamp(new Date().getTime());
    }
  }

  private static class XMLGregorianCalendarInstanceProvider implements TypedInstanceProvider {
    @Override
    public Object provideInstance() {
      try {
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
      } catch (DatatypeConfigurationException e) {
        return null;
      }
    }
  }
}
