`reflection-utils` is a utility library which consists of 4 tools: `ReflectionCloner`, `ReflectionInitializer`,
`ReflectionDumper`, and `ReflectionProxy` (all of which reside in the `eu.ardinsys.reflection.tool.*` packages).

# Quick start

## `ReflectionCloner`

This class can be used to deep clone arbitrary objects. Example:

```java
public class Bar1 {
  private long a;
  
  public long getA() {
    return a;
  }
  
  public void setA(long a) {
    this.a = a;
  } 
}
```

```java
public class Bar2 {
  private long a;
  
  public long getA() {
    return a;
  }
  
  public void setA(long a) {
    this.a = a;
  } 
}
```

```java
ReflectionCloner cloner = new ReflectionCloner();

Bar1 bar = new Bar1();
bar.setA(100);

Bar1 barCopy1 = cloner.clone(bar);             // Clones into a new instance of the same class
Bar1 barCopy2 = cloner.clone(bar, Bar1.class); // Same as above
Bar2 barCopy3 = cloner.clone(bar, Bar2.class); // Clones into a new instance of a nearly identical class
Bar2 barCopy4 = cloner.clone(bar, new Bar2()); // Clones into an existing object
```

`ReflectionCloner` works by finding getter/setter pairs with identical field names (it makes the assumption that all
accessor methods follow the `getX` / `isX` / `setX` naming convetion). `ReflectionCloner` also attempts to handle
conversions between similar types (list <-> array, numerical types, etc.).

## `ReflectionInitializer`

`ReflectionInitializer` can be used to initialize arbitrary objects (ie. recursively fill their fields with default values).
Example:

```java
ReflectionInitializer initializer = new ReflectionInitializer();

Bar1 bar1 = initializer.initialize(Bar1.class); // Initializes a new instance of the class
Bar2 bar2 = initializer.initialize(new Bar2()); // Initializes an existing instance of the class
```

## `ReflectionDumper`

`ReflectionDumper` can be used to pretty-print arbitrary objects. Example:

```java
System.out.println(new ReflectionDumper().dump(new Bar1()));
// 0 Bar1 <
// .  1 a long Long 0
// >

System.out.println(new ReflectionDumper().dump(new double[] {0, 1, 2}));
// 0 double[] [
// .  1 double Double 0.0
// .  2 double Double 1.0
// .  3 double Double 2.0
// ]
```

By default, `ReflectionDumper` includes most accessible information about values (declared type, actual type, value,
field name, etc.), this can be fully customized. Some specific types also have custom notations (sequential types
with `[]`, maps with `{}`, POJOs with `<>`).
