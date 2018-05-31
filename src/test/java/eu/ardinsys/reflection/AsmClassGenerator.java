package eu.ardinsys.reflection;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Random;

/**
 * Utility which generates, compiles and loads bean-like classes with random properties.
 * <p>
 * Public API:
 * <ul>
 * <li>{@link #generateClasses()}</li>
 * </ul>
 * <p>
 * Customize with:
 * <ul>
 * <li>{@link #setPropertyCount(int)}</li>
 * <li>{@link #setMaxTypeDepth(int)}</li>
 * <li>{@link #setClassCount(int)}</li>
 * </ul>
 */
public class AsmClassGenerator {
  private static final String[][] JNI_SIGNATURE_PRIMITIVE = new String[][]{
      {"B", "Ljava/lang/Byte;"},
      {"S", "Ljava/lang/Short;"},
      {"I", "Ljava/lang/Integer;"},
      {"J", "Ljava/lang/Long;"},
      {"F", "Ljava/lang/Float;"},
      {"D", "Ljava/lang/Double;"},
      {"C", "Ljava/lang/Character;"},
      {"Z", "Ljava/lang/Boolean;"}
  };
  private static final String[] JNI_SIGNATURE_IMMUTABLE = new String[]{
      "Ljava/math/BigInteger;",
      "Ljava/math/BigDecimal;",
      "Ljava/lang/String;",
      "Ljava/util/UUID;"
  };
  private static final String[] JNI_SIGNATURE_COLLECTION = new String[]{
      "Ljava/util/Collection",
      "Ljava/util/Set",
      "Ljava/util/HashSet",
      "Ljava/util/List",
      "Ljava/util/Queue",
      "Ljava/util/LinkedList",
      "Ljava/util/ArrayList"
  };
  private static final String[] JNI_SIGNATURE_MAP = new String[]{
      "Ljava/util/Map",
      "Ljava/util/LinkedHashMap",
      "Ljava/util/HashMap"
  };
  private static final Random RANDOM = new Random();

  private final ByteClassLoader classLoader = new ByteClassLoader();
  private int currentId = 0;

  /**
   * Number of classes to generate.<br>
   * Default value: <code>4</code>.
   */
  private int classCount = 4;

  /**
   * Number of properties each generated class should have.<br>
   * Default value: <code>4</code>.
   */
  private int propertyCount = 4;

  /**
   * Maximum type depth of the property types.<br>
   * Default value: <code>5</code>.
   */
  private int maxTypeDepth = 5;

  /**
   * Determines whether the generated code should allow circular or selfreferences.<br>
   * Default value: <code>false</code>
   */
  private boolean allowCircularReferences = false;

  /**
   * @return The current value of {@link #classCount}.
   */
  public int getClassCount() {
    return classCount;
  }

  /**
   * @param classCount The new value of {@link #classCount}
   */
  public void setClassCount(int classCount) {
    this.classCount = classCount;
  }

  /**
   * @return The current value of {@link #propertyCount}.
   */
  public int getPropertyCount() {
    return propertyCount;
  }

  /**
   * @param propertyCount The new value of {@link #propertyCount}
   */
  public void setPropertyCount(int propertyCount) {
    this.propertyCount = propertyCount;
  }

  /**
   * @return The current value of {@link #maxTypeDepth}.
   */
  public int getMaxTypeDepth() {
    return maxTypeDepth;
  }

  /**
   * @param maxTypeDepth The new value of {@link #maxTypeDepth}
   */
  public void setMaxTypeDepth(int maxTypeDepth) {
    this.maxTypeDepth = maxTypeDepth;
  }

  /**
   * @return The current value of {@link #allowCircularReferences}
   */
  public boolean isAllowCircularReferences() {
    return allowCircularReferences;
  }

  /**
   * @param allowCircularReferences The new value of {@link #allowCircularReferences}
   */
  public void setAllowCircularReferences(boolean allowCircularReferences) {
    this.allowCircularReferences = allowCircularReferences;
  }

  private String generateIdentifier() {
    return Integer.toString(currentId++);
  }

  private String generateTypeSignature(String[] classNames) {
    return generateTypeSignature(classNames, true, 0);
  }

  private String generateTypeSignature(String[] classNames, boolean primitiveAllowed, int depth) {
    double r = RANDOM.nextDouble();

    if (r < 0.05 || depth == maxTypeDepth) {
      return r < 0.04
          ? JNI_SIGNATURE_PRIMITIVE[RANDOM.nextInt(JNI_SIGNATURE_PRIMITIVE[0].length)][primitiveAllowed ? 0 : 1]
          : JNI_SIGNATURE_IMMUTABLE[RANDOM.nextInt(JNI_SIGNATURE_IMMUTABLE.length)];
    }

    if (r < 0.1 && classNames.length > 0) {
      return String.format("L%s;", classNames[RANDOM.nextInt(classNames.length)]);
    }

    if (r < 0.4) {
      return String.format("[%s", generateTypeSignature(classNames, true, depth + 1));
    }

    if (r < 0.7) {
      return String.format("%s<%s>;",
          JNI_SIGNATURE_COLLECTION[RANDOM.nextInt(JNI_SIGNATURE_COLLECTION.length)],
          generateTypeSignature(classNames, false, depth + 1));
    }

    return String.format("%s<%s%s>;",
        JNI_SIGNATURE_MAP[RANDOM.nextInt(JNI_SIGNATURE_MAP.length)],
        generateTypeSignature(classNames, false, depth + 1),
        generateTypeSignature(classNames, false, depth + 1));
  }

  private String toTypeDescriptor(String typeSignature) {
    return typeSignature.replaceAll("<.*>", "");
  }

  private int toReturnType(String typeDescriptor) {
    if (typeDescriptor.length() != 1) {
      return Opcodes.ARETURN;
    }

    switch (typeDescriptor.charAt(0)) {
      case 'Z':
      case 'C':
      case 'B':
      case 'S':
      case 'I':
        return Opcodes.IRETURN;
      case 'J':
        return Opcodes.LRETURN;
      case 'F':
        return Opcodes.FRETURN;
      case 'D':
        return Opcodes.DRETURN;
      default:
        return Opcodes.ARETURN;
    }
  }

  private int toLoadType(String typeDescriptor) {
    if (typeDescriptor.length() != 1) {
      return Opcodes.ALOAD;
    }

    switch (typeDescriptor.charAt(0)) {
      case 'Z':
      case 'C':
      case 'B':
      case 'S':
      case 'I':
        return Opcodes.ILOAD;
      case 'J':
        return Opcodes.LLOAD;
      case 'F':
        return Opcodes.FLOAD;
      case 'D':
        return Opcodes.DLOAD;
      default:
        return Opcodes.ALOAD;
    }
  }

  private void generateConstructor(ClassWriter classWriter) {
    MethodVisitor methodVisitor = classWriter.visitMethod(
        Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
    methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V",
        false);
    methodVisitor.visitInsn(Opcodes.RETURN);
    methodVisitor.visitMaxs(0, 0);
    methodVisitor.visitEnd();
  }

  private void generateProperty(ClassWriter classWriter, String className,
                                String[] referenceableClassNames) {
    String propertyName = "property_" + generateIdentifier();
    String propertyTypeSignature = generateTypeSignature(referenceableClassNames);
    String propertyTypeDescriptor = toTypeDescriptor(propertyTypeSignature);

    FieldVisitor fieldVisitor = classWriter.visitField(
        Opcodes.ACC_PUBLIC,
        propertyName,
        propertyTypeDescriptor,
        propertyTypeSignature,
        null);
    fieldVisitor.visitEnd();

    MethodVisitor getterMethodVisitor = classWriter.visitMethod(
        Opcodes.ACC_PUBLIC,
        "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1),
        "()" + propertyTypeDescriptor,
        "()" + propertyTypeSignature,
        null);
    getterMethodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
    getterMethodVisitor.visitFieldInsn(Opcodes.GETFIELD, className, propertyName,
        propertyTypeDescriptor);
    getterMethodVisitor.visitInsn(toReturnType(propertyTypeDescriptor));
    getterMethodVisitor.visitMaxs(0, 0);
    getterMethodVisitor.visitEnd();

    MethodVisitor setterMethodVisitor = classWriter.visitMethod(
        Opcodes.ACC_PUBLIC,
        "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1),
        "(" + propertyTypeDescriptor + ")V",
        "(" + propertyTypeSignature + ")V",
        null);
    setterMethodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
    setterMethodVisitor.visitVarInsn(toLoadType(propertyTypeDescriptor), 1);
    setterMethodVisitor.visitFieldInsn(Opcodes.PUTFIELD, className, propertyName,
        propertyTypeDescriptor);
    setterMethodVisitor.visitInsn(Opcodes.RETURN);
    setterMethodVisitor.visitMaxs(0, 0);
    setterMethodVisitor.visitEnd();
  }

  private Class<?> generateClass(String className, String[] referenceableClassNames) {
    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    classWriter.visit(
        Opcodes.V1_5,
        Opcodes.ACC_PUBLIC,
        className,
        null,
        "java/lang/Object",
        null);

    generateConstructor(classWriter);

    for (int i = 0; i < propertyCount; i++) {
      generateProperty(classWriter, className, referenceableClassNames);
    }

    classWriter.visitEnd();
    return classLoader.define(className, classWriter.toByteArray());
  }

  public Class<?>[] generateClasses() {
    String[] classNames = new String[classCount];
    for (int i = 0; i < classCount; i++) {
      classNames[i] = "Class_" + generateIdentifier();
    }

    Class<?>[] classes = new Class<?>[classCount];
    for (int i = 0; i < classCount; i++) {
      String[] referenceableClassNames = Arrays.copyOf(
          classNames, allowCircularReferences ? classCount : i);
      classes[i] = generateClass(classNames[i], referenceableClassNames);
    }

    return classes;
  }

  private static class ByteClassLoader extends ClassLoader {
    public Class<?> define(String className, byte[] classBody) {
      return defineClass(className, classBody, 0, classBody.length);
    }
  }
}
