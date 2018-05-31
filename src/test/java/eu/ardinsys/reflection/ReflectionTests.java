package eu.ardinsys.reflection;

import eu.ardinsys.reflection.tool.BasicImplementationProvider;
import eu.ardinsys.reflection.tool.cloner.CustomCloner;
import eu.ardinsys.reflection.tool.cloner.ReflectionCloner;
import eu.ardinsys.reflection.tool.dumper.CustomDumper;
import eu.ardinsys.reflection.tool.dumper.ReflectionDumper;
import eu.ardinsys.reflection.tool.dumper.ReflectionDumper.DelimiterPair;
import eu.ardinsys.reflection.tool.initializer.ReflectionInitializer;
import eu.ardinsys.reflection.tool.proxy.ReflectionProxy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Tests for all reflection related utilities.
 */
@SuppressWarnings("unused")
public class ReflectionTests {
  private ReflectionInitializer initializer;
  private ReflectionCloner cloner;
  private ReflectionDumper dumper;

  private void assertEquals(Object a, Object b) {
    String s1 = dumper.dump(a);
    String s2 = dumper.dump(b);
    if (!s1.equals(s2)) {
      Assert.fail(String.format("Strings not equal: \n%s\n%s\n", s1, s2));
    }
  }

  private <T> void roundtrip(Class<T> c) {
    roundtrip(c, c);
  }

  private <T> void roundtrip(Class<T> c1, Class<?> c2) {
    T x = initializer.initialize(c1);
    assertEquals(x, cloner.clone(x, c2));
  }

  @Before
  public void beforeEach() {
    initializer = new ReflectionInitializer();
    cloner = new ReflectionCloner();
    dumper = new ReflectionDumper() {
      @Override
      public String dump(Object object) {
        return super.dump(object).replaceAll(System.getProperty("line.separator"), "");
      }
    };
    dumper.setIncludeId(false);
    dumper.setIncludeDeclaredType(false);
    dumper.setIncludeActualType(false);
    dumper.setIncludeName(false);
    dumper.setIndentationString(" ");
  }

  /**
   * Bean with deeply nested data structures (initialize + clone + dump)
   */
  @Test
  public void test01() {
    roundtrip(Bean1.class);
  }

  /**
   * Beans with compatible fields (initialize + clone + dump)
   */
  @Test
  public void test02() {
    roundtrip(Bean2A.class, Bean2B.class);
  }

  /**
   * Recursive bean #1 (initialize + clone + dump)
   */
  @Test
  public void test03() {
    roundtrip(Bean3.class);
  }

  /**
   * Recursive bean #2 (initialize + clone + dump)
   */
  @Test
  public void test04() {
    roundtrip(Bean4A.class);
  }

  /**
   * Bean with multiple setters for the same field (initialize + clone + dump)
   */
  @Test
  public void test05() {
    roundtrip(Bean5A.class);
  }

  /**
   * Bean with nested collection types (initialize + clone + dump)
   */
  @Test
  public void test06() {
    roundtrip(Bean6.class);
  }

  /**
   * Bean with nested wildcard types (initialize + clone + dump)
   */
  @Test
  public void test07() {
    Bean7<?, ?, ?> x = new Bean7<Integer, String, List<Integer>>();
    initializer.initialize(x);
    assertEquals(x, cloner.clone(x));
  }

  /**
   * Bean with complex type variables (initialize + clone + dump)
   */
  @Test
  public void test08() {
    Bean8<List<String>, Map<Integer, ArrayList<String>>> x = new Bean8<List<String>, Map<Integer, ArrayList<String>>>();
    initializer.initialize(x);
    assertEquals(x, cloner.clone(x));
  }

  /**
   * Bean with deeply nested data structures (proxy)
   */
  @Test
  public void test09() {
    Bean9A x = new Bean9A();
    ReflectionProxy xProxy = new ReflectionProxy(x);

    xProxy.set(".a[+][10][xbar].a[0][100][+].a[qux][7]", Integer.valueOf(10));
    Assert.assertEquals(xProxy.get(".a[-][-][xbar].a[-][-][-].a[qux][-]"), Integer.valueOf(10));
  }

  /**
   * Random bean (initialize + clone + dump)
   */
  @Test
  public void test10() {
    AsmClassGenerator generator = new AsmClassGenerator();
    generator.setClassCount(2);
    generator.setMaxTypeDepth(5);
    generator.setPropertyCount(2);
    Class<?>[] randomClasses = generator.generateClasses();
    roundtrip(randomClasses[randomClasses.length - 1]);
  }

  /**
   * Circular reference (dump)
   */
  @Test
  public void test11() {
    Bean11 x1 = new Bean11();
    x1.setA(1);

    Bean11 x2 = new Bean11();
    x2.setA(2);

    Bean11 x3 = new Bean11();
    x3.setA(3);

    Bean11 x4 = new Bean11();
    x4.setA(4);

    x1.setB(x2);
    x2.setB(x3);
    x3.setB(x4);
    x4.setB(x1);

    Assert.assertEquals("<1<2<3<4@@@>>>>", dumper.dump(x1).replaceAll(" ", ""));
  }

  /**
   * Custom formatter (dump)
   */
  @Test
  public void test12() {
    Bean12A x = new Bean12A();
    x.setA(10);
    x.setB(new Bean12B());
    x.setC("asdf");

    dumper.addCustomDumper(Bean12B.class, new CustomDumper<Bean12B>() {
      @Override
      public String dump(Bean12B object) {
        return "xxx";
      }
    });

    Assert.assertEquals("< 10 xxx asdf>", dumper.dump(x));
  }

  /**
   * Collection cloning (clone)
   */
  @Test
  public void test13() {
    List<String> stringList = new ArrayList<String>();
    stringList.addAll(Arrays.asList(new String[]{"aa", "bb", "cc", "dd"}));

    assertEquals(stringList, cloner.clone(stringList));
    assertEquals(stringList, cloner.clone(stringList, List.class));
    assertEquals(stringList, cloner.clone(stringList, ArrayList.class));
    assertEquals(stringList, cloner.clone(stringList, ArrayDeque.class));
    assertEquals(stringList, cloner.clone(stringList, SortedSet.class));
    assertEquals(stringList, cloner.clone(stringList, new ArrayList<String>()));
    assertEquals(stringList,
        cloner.clone(stringList, new ArrayList<String>(Arrays.asList(new String[]{"a"}))));

    String[] stringArray = new String[]{"a", "b", "c"};
    assertEquals(stringArray, cloner.clone(stringArray, String[].class));
    assertEquals(stringArray, cloner.clone(stringArray, new String[3]));

    // The type is erased, but since Strings are immutable the cloner does not
    // care
    DelimiterPair oldCollectionDelimiter = dumper.getCollectionDelimiters();
    dumper.setCollectionDelimiters(dumper.getArrayDelimiters());
    assertEquals(stringArray, cloner.clone(stringArray, List.class));
    assertEquals(stringArray, cloner.clone(stringArray, ArrayList.class));
    assertEquals(stringArray, cloner.clone(stringArray, new ArrayList<String>()));
    dumper.setCollectionDelimiters(oldCollectionDelimiter);

    Bean2A[] beanArray = initializer.initialize(Bean2A[].class);
    assertEquals(beanArray, cloner.clone(beanArray, Bean2B[].class));
    assertEquals(beanArray, cloner.clone(beanArray, new Bean2B[beanArray.length]));

    // Does not work due to type erasure
    // assertEquals(beanArray, cloner.clone(beanArray, ArrayList.class));
    // assertEquals(beanArray, cloner.clone(beanArray, new
    // ArrayList<Bean2B>()));
  }

  /**
   * Bean with a getter which throws when invoked (dump)
   */
  @Test
  public void test14() {
    Assert.assertEquals("< ???>", dumper.dump(new Bean14()));
  }

  /**
   * Manual property registration + property filter test (initialize + clone + dump)
   */
  @Test
  public void test15() {
    // Register d
    try {
      Utils.registerProperty(
          "d",
          Bean15B.class.getMethod("dd"),
          Bean15B.class.getMethod("ddd", byte.class));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // Do not initialize a
    initializer.addPropertyFilter(Bean15A.class, new PropertyFilter() {
      @Override
      public boolean excludeProperty(String propertyName) {
        return "a".equals(propertyName);
      }
    });
    Bean15B x = initializer.initialize(Bean15B.class);
    Assert.assertEquals(0, x.getA());

    // Do not clone b
    x.setB((byte) 100);
    cloner.addPropertyFilter(Bean15A.class, new PropertyFilter() {
      @Override
      public boolean excludeProperty(String propertyName) {
        return "b".equals(propertyName);
      }
    });
    Bean15B y = cloner.clone(x);
    Assert.assertEquals(0, y.getB());

    // Do not dump c
    dumper.addPropertyFilter(Bean15A.class, new PropertyFilter() {
      @Override
      public boolean excludeProperty(String propertyName) {
        return "c".equals(propertyName);
      }
    });
    Assert.assertEquals("< 0 0 3>", dumper.dump(y));
  }

  /**
   * Array, collection, map initializing (initialize)
   */
  @Test
  public void test16() {
    Assert.assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, initializer.initialize(new byte[5]));
    Assert.assertEquals(initializer.getCompositeSize(), initializer.initialize(List.class).size());
    Assert.assertEquals(initializer.getCompositeSize(), initializer.initialize(Map.class).size());
  }

  /**
   * Property filter test (initialize)
   */
  @Test
  public void test17() {
    // Parent excludes but child includes = included
    initializer.addPropertyFilter(Bean17A.class, new PropertyFilter() {
      @Override
      public boolean excludeProperty(String propertyName) {
        return true;
      }
    });
    initializer.addPropertyFilter(Bean17B.class, new PropertyFilter() {
      @Override
      public boolean excludeProperty(String propertyName) {
        return false;
      }
    });
    Assert.assertNotEquals(0, initializer.initialize(Bean17B.class).getA());
    // Parent includes but child excludes = excluded
    initializer = new ReflectionInitializer();
    initializer.addPropertyFilter(Bean17A.class, new PropertyFilter() {
      @Override
      public boolean excludeProperty(String propertyName) {
        return false;
      }
    });
    initializer.addPropertyFilter(Bean17B.class, new PropertyFilter() {
      @Override
      public boolean excludeProperty(String propertyName) {
        return true;
      }
    });
    Assert.assertEquals(0, initializer.initialize(Bean17B.class).getA());

    // One parent includes, one parent excludes = excluded
    initializer = new ReflectionInitializer();
    initializer.addPropertyFilter(Interface17A.class, new PropertyFilter() {
      @Override
      public boolean excludeProperty(String propertyName) {
        return true;
      }
    });
    initializer.addPropertyFilter(Interface17B.class, new PropertyFilter() {
      @Override
      public boolean excludeProperty(String propertyName) {
        return false;
      }
    });
    Assert.assertEquals(0, initializer.initialize(Bean17C.class).getA());
  }

  /**
   * Random bean with potential circular references (initialize + clone + dump)
   */
  @Test
  public void test18() {
    AsmClassGenerator generator = new AsmClassGenerator();
    generator.setClassCount(5);
    generator.setMaxTypeDepth(3);
    generator.setPropertyCount(3);
    generator.setAllowCircularReferences(false);
    Class<?>[] randomClasses = generator.generateClasses();
    roundtrip(randomClasses[randomClasses.length - 1]);
  }

  /**
   * Beans with "almost-compatible" fields (i.e. (T[] vs. List<T>)) (initialize + clone)
   */
  @Test
  public void test19() {
    dumper.setCollectionDelimiters(new DelimiterPair("[", "]"));
    roundtrip(Bean19A.class, Bean19B.class);
    roundtrip(Bean19B.class, Bean19A.class);
  }

  /**
   * Type conversion (initialize + clone + dump)
   */
  @Test
  public void test20() {
    cloner.addCustomCloner(Number.class, BigDecimal2.class,
        new CustomCloner<Number, BigDecimal2>() {
          @Override
          public BigDecimal2 clone(Number object, Class<? extends BigDecimal2> targetClass) {
            return new BigDecimal2(object.doubleValue());
          }
        });
    // TODO
    // roundtrip(Bean20A.class, Bean20B.class);
  }

  /**
   * Primitive type conversion (initialize + clone + dump)
   */
  @Test
  public void test21() {
    dumper.addPropertyFilter(Bean21A.class, new PropertyFilter() {
      @Override
      public boolean excludeProperty(String propertyName) {
        return propertyName.equals("j");
      }
    });
    roundtrip(Bean21B.class, Bean21C.class);
  }

  /**
   * Numerical roundtrip
   */
  @Test
  public void test22() {
    List<Class<?>> classes = new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{
        byte.class, short.class, int.class, long.class,
        Byte.class, Short.class, Integer.class, Long.class,
        BigInteger.class, BigDecimal.class,
        float.class, double.class, Float.class, Double.class,
        String.class
    }));

    for (int i = 0; i < 6; i++) {
      classes.addAll(classes);
    }

    Collections.shuffle(classes);

    Object originalValue = Byte.valueOf((byte) (Math.random() * 127));
    Object value = originalValue;
    for (Class<?> c : classes) {
      value = cloner.clone(value, c);
    }

    value = cloner.clone(value, Byte.class);
    Assert.assertEquals(originalValue, value);
  }

  /**
   * WSDL test, Array (get, set) <-> List (get)
   */
  @Test
  public void test23() {
    dumper.setCollectionDelimiters(new DelimiterPair("[", "]"));
    roundtrip(Bean23A.class, Bean23B.class);
    roundtrip(Bean23B.class, Bean23A.class);
  }

  /**
   * Number -> Boolean conversion.
   */
  @Test
  public void test24() {
    for (Object falsyValue : Arrays.asList(new Object[]{
        Byte.valueOf((byte) 0),
        Short.valueOf((short) 0),
        Integer.valueOf(0),
        Long.valueOf(0),
        BigInteger.valueOf(0),
        BigDecimal.valueOf(0),
        Float.valueOf(0),
        Double.valueOf(0)})) {
      Assert.assertEquals(Boolean.FALSE, cloner.clone(falsyValue, Boolean.class));
    }

    for (Object truthyValue : Arrays.asList(new Object[]{
        Byte.valueOf((byte) 1),
        Short.valueOf((short) 2),
        Integer.valueOf(3),
        Long.valueOf(4),
        BigInteger.valueOf(5),
        BigDecimal.valueOf(0.3),
        Float.valueOf(0.1f),
        Double.valueOf(0.2)})) {
      Assert.assertEquals(Boolean.TRUE, cloner.clone(truthyValue, Boolean.class));
    }
  }

  /**
   * Expects "No properties cloned"
   */
  @Test
  public void test25() {
    final boolean[] helper = new boolean[1];

    cloner.getLogger().addHandler(new Handler() {
      @Override
      public void publish(LogRecord record) {
        if (record.getMessage().contains("No properties cloned")) {
          helper[0] = true;
        }
      }

      @Override
      public void flush() {

      }

      @Override
      public void close() throws SecurityException {

      }
    });

    cloner.clone(new Bean25(), new Bean25());

    if (!helper[0]) {
      Assert.fail();
    }
  }

  /**
   * XMLGregorianCalendar
   */
  @Test
  public void test26() {
    cloner.addCustomCloner(Date.class, XMLGregorianCalendar.class,
        new CustomCloner<Date, XMLGregorianCalendar>() {
          @Override
          public XMLGregorianCalendar clone(Date object, Class<? extends XMLGregorianCalendar> targetClass) {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(object);
            try {
              return DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
            } catch (DatatypeConfigurationException e) {
              throw new RuntimeException(e);
            }
          }
        });
    cloner.setImplementationProvider(new ImplementationProvider());

    Date date = new Date();
    Bean26A source = new Bean26A();
    source.setX(date);
    Assert.assertEquals(date.getTime(),
        cloner.clone(source, Bean26B.class).getX().toGregorianCalendar().getTime().getTime());
  }

  @Test
  public void test27() {
    Assert.assertEquals(
        Boolean.FALSE, Boolean.valueOf(cloner.clone(new Bean27A(), Bean27B.class).isA()));
  }

  /**
   * null Double -> Double | double
   */
  @Test
  public void test28() {
    Bean28A a = new Bean28A();
    a.setX(null);
    a.setY(null);

    Bean28B b = cloner.clone(a, Bean28B.class);
    Assert.assertEquals(b.getX(), 0, 0);
    Assert.assertEquals(b.getY(), null);
  }

  /**
   * String <-> Enum
   */
  @Test
  public void test29() {
    Assert.assertEquals("CAT", cloner.clone(Enum29.CAT, String.class));
    Assert.assertEquals(Enum29.DOG, cloner.clone("DOG", Enum29.class));
  }

  /**
   * null BigDecimal -> null
   */
  @Test
  public void test30() {
    Assert.assertNull(cloner.clone(new Bean30A()).getX());
  }

  private static enum Enum29 {
    CAT, DOG
  }

  private static interface Interface17A {

  }

  private static interface Interface17B {

  }

  private static interface Bean21A {

  }

  private static class Bean1 {
    private Map<List<String[]>, Map<Integer, List<List<Integer>>[]>>[][] a;

    public Map<List<String[]>, Map<Integer, List<List<Integer>>[]>>[][] getA() {
      return a;
    }

    public void setA(
        Map<List<String[]>, Map<Integer, List<List<Integer>>[]>>[][] a) {
      this.a = a;
    }
  }

  private static class Bean2A {
    private Integer a;

    public Integer getA() {
      return a;
    }

    public void setA(Integer a) {
      this.a = a;
    }
  }

  private static class Bean2B {
    private Object a;

    public Object getA() {
      return a;
    }

    public void setA(Object a) {
      this.a = a;
    }
  }

  private static class Bean3 {
    private Bean3 a;

    public Bean3 getA() {
      return a;
    }

    public void setA(Bean3 a) {
      this.a = a;
    }
  }

  private static class Bean4A {
    private Bean4B b;

    public Bean4B getB() {
      return b;
    }

    public void setB(Bean4B b) {
      this.b = b;
    }
  }

  private static class Bean4B {
    private Bean4A a;

    public Bean4A getA() {
      return a;
    }

    public void setA(Bean4A a) {
      this.a = a;
    }
  }

  private static class Bean5A {
    private Bean5B a;

    public Bean5B getA() {
      return a;
    }

    public void setA(Bean5C a) {

    }

    public void setA(Object a) {

    }

    public void setA(Bean5B a) {
      this.a = a;
    }
  }

  private static class Bean5B {
    private int a;

    public int getA() {
      return a;
    }

    public void setA(int a) {
      this.a = a;
    }
  }

  private static class Bean5C extends Bean5B {

  }

  private static class Bean6 {
    private Set<Collection<ArrayDeque<Set<Queue<HashSet<List<Integer>>>>>>> a;

    public Set<Collection<ArrayDeque<Set<Queue<HashSet<List<Integer>>>>>>> getA() {
      return a;
    }

    public void setA(
        Set<Collection<ArrayDeque<Set<Queue<HashSet<List<Integer>>>>>>> a) {
      this.a = a;
    }
  }

  private static class Bean7<T, U, W> {
    private List<? extends Set<? extends Map<Set<? extends Boolean>, ? extends Integer>>> a;

    public List<? extends Set<? extends Map<Set<? extends Boolean>, ? extends Integer>>> getA() {
      return a;
    }

    public void setA(List<? extends Set<? extends Map<Set<? extends Boolean>, ? extends Integer>>> a) {
      this.a = a;
    }
  }

  private static class Bean8<T extends List<String>, U extends Map<Integer, ? extends T>> {
    private T t;
    private U u;

    public T getT() {
      return t;
    }

    public void setT(T t) {
      this.t = t;
    }

    public U getU() {
      return u;
    }

    public void setU(U u) {
      this.u = u;
    }
  }

  private static class Bean9A {
    private List<List<Map<String, Bean9B>>> a;

    public List<List<Map<String, Bean9B>>> getA() {
      return a;
    }

    public void setA(List<List<Map<String, Bean9B>>> a) {
      this.a = a;
    }
  }

  private static class Bean9B {
    private Bean9C[][][] a;

    public Bean9C[][][] getA() {
      return a;
    }

    public void setA(Bean9C[][][] a) {
      this.a = a;
    }
  }

  private static class Bean9C {
    private Map<String, int[]> a;

    public Map<String, int[]> getA() {
      return a;
    }

    public void setA(Map<String, int[]> a) {
      this.a = a;
    }
  }

  private static class Bean11 {
    private int a;
    private Bean11 b;

    public int getA() {
      return a;
    }

    public void setA(int a) {
      this.a = a;
    }

    public Bean11 getB() {
      return b;
    }

    public void setB(Bean11 b) {
      this.b = b;
    }
  }

  private static class Bean12A {
    private int a;
    private Bean12B b;
    private String c;

    public int getA() {
      return a;
    }

    public void setA(int a) {
      this.a = a;
    }

    public Bean12B getB() {
      return b;
    }

    public void setB(Bean12B b) {
      this.b = b;
    }

    public String getC() {
      return c;
    }

    public void setC(String c) {
      this.c = c;
    }
  }

  private static class Bean12B {

  }

  private static class Bean14 {
    public int getA() {
      throw new RuntimeException();
    }
  }

  private static class Bean15A {
    private byte a;
    private byte b;
    private byte c;

    public byte getA() {
      return a;
    }

    public void setA(byte a) {
      this.a = a;
    }

    public byte getB() {
      return b;
    }

    public void setB(byte b) {
      this.b = b;
    }

    public byte getC() {
      return c;
    }

    public void setC(byte c) {
      this.c = c;
    }
  }

  private static class Bean15B extends Bean15A {
    private byte d;

    public byte dd() {
      return d;
    }

    public void ddd(byte d) {
      this.d = d;
    }
  }

  private static class Bean17A {
    private int a;

    public int getA() {
      return a;
    }

    public void setA(int a) {
      this.a = a;
    }
  }

  private static class Bean17B extends Bean17A {

  }

  private static class Bean17C extends Bean17A
      implements Interface17A, Interface17B {

  }

  private static class Bean19A {
    private List<Integer[]>[][] ints;

    public List<Integer[]>[][] getInts() {
      return ints;
    }

    public void setInts(List<Integer[]>[][] ints) {
      this.ints = ints;
    }
  }

  private static class Bean19B {
    private List<List<List<Integer>>[]> ints;

    public List<List<List<Integer>>[]> getInts() {
      return ints;
    }

    public void setInts(List<List<List<Integer>>[]> ints) {
      this.ints = ints;
    }
  }

  private static class Bean20A {
    private double value;

    public double getValue() {
      return value;
    }

    public void setValue(double value) {
      this.value = value;
    }
  }

  private static class Bean20B {
    private BigDecimal value;

    public BigDecimal getValue() {
      return value;
    }

    public void setValue(BigDecimal value) {
      this.value = value;
    }
  }

  private static class BigDecimal2 extends BigDecimal {
    private static final long serialVersionUID = 1L;

    public BigDecimal2(double val) {
      super(Double.toString(val));
    }
  }

  private static class Bean21B implements Bean21A {
    private byte a;
    private short b;
    private int c;
    private long d;
    private Byte e;
    private Short f;
    private Integer g;
    private Long h;
    private BigInteger i;
    private Double j;

    public byte getA() {
      return a;
    }

    public void setA(byte a) {
      this.a = a;
    }

    public short getB() {
      return b;
    }

    public void setB(short b) {
      this.b = b;
    }

    public int getC() {
      return c;
    }

    public void setC(int c) {
      this.c = c;
    }

    public long getD() {
      return d;
    }

    public void setD(long d) {
      this.d = d;
    }

    public Byte getE() {
      return e;
    }

    public void setE(Byte e) {
      this.e = e;
    }

    public Short getF() {
      return f;
    }

    public void setF(Short f) {
      this.f = f;
    }

    public Integer getG() {
      return g;
    }

    public void setG(Integer g) {
      this.g = g;
    }

    public Long getH() {
      return h;
    }

    public void setH(Long h) {
      this.h = h;
    }

    public BigInteger getI() {
      return i;
    }

    public void setI(BigInteger i) {
      this.i = i;
    }

    public Double getJ() {
      return j;
    }

    public void setJ(Double j) {
      this.j = j;
    }
  }

  private static class Bean21C implements Bean21A {
    private long a;
    private long b;
    private long c;
    private long d;
    private long e;
    private long f;
    private long g;
    private long h;
    private long i;
    private boolean j;

    public long getA() {
      return a;
    }

    public void setA(long a) {
      this.a = a;
    }

    public long getB() {
      return b;
    }

    public void setB(long b) {
      this.b = b;
    }

    public long getC() {
      return c;
    }

    public void setC(long c) {
      this.c = c;
    }

    public long getD() {
      return d;
    }

    public void setD(long d) {
      this.d = d;
    }

    public long getE() {
      return e;
    }

    public void setE(long e) {
      this.e = e;
    }

    public long getF() {
      return f;
    }

    public void setF(long f) {
      this.f = f;
    }

    public long getG() {
      return g;
    }

    public void setG(long g) {
      this.g = g;
    }

    public long getH() {
      return h;
    }

    public void setH(long h) {
      this.h = h;
    }

    public long getI() {
      return i;
    }

    public void setI(long i) {
      this.i = i;
    }

    public boolean isJ() {
      return j;
    }

    public void setJ(boolean j) {
      this.j = j;
    }
  }

  private static class Bean23A {
    private int[] a;

    public int[] getA() {
      return a;
    }

    public void setA(int[] a) {
      this.a = a;
    }
  }

  private static class Bean23B {
    private List<Integer> a;

    public List<Integer> getA() {
      if (a == null) {
        a = new ArrayList<Integer>();
      }

      return a;
    }
  }

  private static class Bean25 {

  }

  private static class ImplementationProvider extends BasicImplementationProvider {
    public ImplementationProvider() {
      super();
      try {
        defaultImplementations.put(
            XMLGregorianCalendar.class,
            DatatypeFactory.newInstance().newXMLGregorianCalendar().getClass());
      } catch (DatatypeConfigurationException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class Bean26A {
    private Date x;

    public Date getX() {
      return x;
    }

    public void setX(Date x) {
      this.x = x;
    }
  }

  private static class Bean26B {
    private XMLGregorianCalendar x;

    public XMLGregorianCalendar getX() {
      return x;
    }

    public void setX(XMLGregorianCalendar x) {
      this.x = x;
    }
  }

  private static class Bean27A {
    private Double a;

    public Double getA() {
      return a;
    }

    public void setA(Double a) {
      this.a = a;
    }
  }

  private static class Bean27B {
    private boolean a;

    public boolean isA() {
      return a;
    }

    public void setA(boolean a) {
      this.a = a;
    }
  }

  private static class Bean28A {
    private Double x;

    private Double y;

    public Double getX() {
      return x;
    }

    public void setX(Double x) {
      this.x = x;
    }

    public Double getY() {
      return y;
    }

    public void setY(Double y) {
      this.y = y;
    }
  }

  private static class Bean28B {
    private double x;

    private Double y;

    public double getX() {
      return x;
    }

    public void setX(double x) {
      this.x = x;
    }

    public Double getY() {
      return y;
    }

    public void setY(Double y) {
      this.y = y;
    }
  }

  private static class Bean30A {
    private BigDecimal x;

    public BigDecimal getX() {
      return x;
    }

    public void setX(BigDecimal x) {
      this.x = x;
    }
  }
}
