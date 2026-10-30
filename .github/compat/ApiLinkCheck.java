import java.io.File;
import java.lang.classfile.*;
import java.lang.classfile.constantpool.*;
import java.lang.classfile.instruction.*;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Emulates JVM link-time resolution of every external class/method/field reference found in the
 * plugin's compiled classes against a given API classpath. Reports references that would throw
 * NoClassDefFoundError / NoSuchMethodError / NoSuchFieldError / IncompatibleClassChangeError.
 *
 * The plugin is compiled against one API version but shipped as a single jar for every supported
 * server version. A clean compile does not prove the jar links on other versions (for example,
 * 26.3 changed the return type of EntityPotionEffectEvent#getEntity), so CI runs this check
 * against each supported Spigot and Paper API version.
 *
 * Requires JDK 24+ (java.lang.classfile).
 * Usage: java ApiLinkCheck.java <classDirs> <classpathFile> <ownPackagePrefix>
 * where classDirs is a list of class directories separated by the platform path separator.
 */
public class ApiLinkCheck {

    record Ref(String kind, String owner, String name, String desc, boolean isStatic, Boolean isInterface) {}

    public static void main(String[] args) throws Exception {
        String cp = Files.readString(Path.of(args[1])).trim();
        String ownPrefix = args[2];

        List<URL> urls = new ArrayList<>();
        for (String entry : cp.split(File.pathSeparator)) {
            if (!entry.isBlank()) urls.add(Path.of(entry).toUri().toURL());
        }
        ClassLoader loader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader());

        Map<Ref, Set<String>> refs = new TreeMap<>(Comparator.comparing(Ref::toString));
        for (String classesDir : args[0].split(File.pathSeparator)) {
            collectClasses(Path.of(classesDir), refs);
        }

        int problems = 0;
        int checked = 0;
        for (Map.Entry<Ref, Set<String>> e : refs.entrySet()) {
            Ref ref = e.getKey();
            String owner = ref.owner().replaceFirst("^\\[+L?", "");
            if (owner.startsWith(ownPrefix) || owner.startsWith("java/") || owner.startsWith("javax/") || owner.startsWith("jdk/") || owner.startsWith("sun/")) {
                continue;
            }
            checked++;
            String error = check(ref, loader);
            if (error != null) {
                problems++;
                System.out.println("PROBLEM: " + error + "\n    ref=" + ref + "\n    used in " + e.getValue());
            }
        }
        System.out.println("Checked " + checked + " external API references, problems: " + problems);
        System.exit(problems == 0 ? 0 : 1);
    }

    static void collectClasses(Path classesDir, Map<Ref, Set<String>> refs) throws Exception {
        try (Stream<Path> files = Files.walk(classesDir)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".class")).toList()) {
                ClassModel model = ClassFile.of().parse(file);
                String self = model.thisClass().asInternalName();
                model.superclass().ifPresent(s -> add(refs, new Ref("class", s.asInternalName(), "", "", false, null), self));
                model.interfaces().forEach(i -> add(refs, new Ref("class", i.asInternalName(), "", "", false, null), self));
                for (MethodModel method : model.methods()) {
                    method.code().ifPresent(code -> {
                        for (CodeElement element : code) {
                            collect(element, refs, self + "#" + method.methodName().stringValue());
                        }
                    });
                }
                // Bootstrap method arguments (lambdas / method references)
                for (PoolEntry entry : model.constantPool()) {
                    if (entry instanceof MethodHandleEntry handle) {
                        MemberRefEntry ref = handle.reference();
                        int kind = handle.kind();
                        boolean isStatic = kind == 6 || kind == 2 || kind == 4; // invokeStatic, getStatic, putStatic
                        String refKind = (kind >= 1 && kind <= 4) ? "field" : "method";
                        Boolean iface = refKind.equals("method") ? (ref instanceof InterfaceMethodRefEntry) : null;
                        add(refs, new Ref(refKind, ref.owner().asInternalName(), ref.name().stringValue(),
                                ref.type().stringValue(), isStatic, iface), self + "#<methodhandle>");
                    }
                }
            }
        }
    }

    static void add(Map<Ref, Set<String>> refs, Ref ref, String where) {
        refs.computeIfAbsent(ref, k -> new TreeSet<>()).add(where);
    }

    static void collect(CodeElement element, Map<Ref, Set<String>> refs, String where) {
        switch (element) {
            case InvokeInstruction inv -> {
                boolean isStatic = inv.opcode() == Opcode.INVOKESTATIC;
                add(refs, new Ref("method", inv.owner().asInternalName(), inv.name().stringValue(),
                        inv.type().stringValue(), isStatic, inv.isInterface()), where);
            }
            case FieldInstruction field -> {
                boolean isStatic = field.opcode() == Opcode.GETSTATIC || field.opcode() == Opcode.PUTSTATIC;
                add(refs, new Ref("field", field.owner().asInternalName(), field.name().stringValue(),
                        field.type().stringValue(), isStatic, null), where);
            }
            case TypeCheckInstruction type -> add(refs, new Ref("class", type.type().asInternalName(), "", "", false, null), where);
            case NewObjectInstruction obj -> add(refs, new Ref("class", obj.className().asInternalName(), "", "", false, null), where);
            case NewReferenceArrayInstruction arr -> add(refs, new Ref("class", arr.componentType().asInternalName(), "", "", false, null), where);
            case ExceptionCatch cat -> cat.catchType().ifPresent(t -> add(refs, new Ref("class", t.asInternalName(), "", "", false, null), where));
            case ConstantInstruction.LoadConstantInstruction ldc when ldc.constantEntry() instanceof ClassEntry ce ->
                    add(refs, new Ref("class", ce.asInternalName(), "", "", false, null), where);
            default -> { }
        }
    }

    static Class<?> load(String internalName, ClassLoader loader) throws ClassNotFoundException {
        if (internalName.startsWith("[")) {
            return MethodType.fromMethodDescriptorString("(" + internalName + ")V", loader).parameterType(0);
        }
        return Class.forName(internalName.replace('/', '.'), false, loader);
    }

    static String check(Ref ref, ClassLoader loader) {
        Class<?> owner;
        try {
            owner = load(ref.owner(), loader);
        } catch (Throwable t) {
            return "missing class " + ref.owner() + " (" + t + ")";
        }
        try {
            switch (ref.kind()) {
                case "class" -> {
                    return null;
                }
                case "field" -> {
                    Class<?> type = MethodType.fromMethodDescriptorString("(" + ref.desc() + ")V", loader).parameterType(0);
                    Field f = findField(owner, ref.name(), type);
                    if (f == null) return "missing field " + ref.owner() + "." + ref.name() + ":" + ref.desc();
                    if (Modifier.isStatic(f.getModifiers()) != ref.isStatic()) return "static mismatch on field " + ref.name();
                    return null;
                }
                case "method" -> {
                    if (ref.isInterface() != null && !owner.isArray() && owner.isInterface() != ref.isInterface()) {
                        return "IncompatibleClassChangeError: " + ref.owner() + " interface=" + owner.isInterface()
                                + " but referenced as interface=" + ref.isInterface();
                    }
                    MethodType mt = MethodType.fromMethodDescriptorString(ref.desc(), loader);
                    if (ref.name().equals("<init>")) {
                        try {
                            owner.getDeclaredConstructor(mt.parameterArray());
                            return null;
                        } catch (NoSuchMethodException e) {
                            return "missing constructor " + ref.owner() + ref.desc();
                        }
                    }
                    if (owner.isArray()) return null; // clone() etc.
                    Method m = findMethod(owner, ref.name(), mt, new HashSet<>());
                    if (m == null) return "missing method " + ref.owner() + "." + ref.name() + ref.desc();
                    if (Modifier.isStatic(m.getModifiers()) != ref.isStatic()) return "static mismatch on method " + ref.name();
                    return null;
                }
                default -> {
                    return "unknown ref kind";
                }
            }
        } catch (Throwable t) {
            return "resolution failure for " + ref + ": " + t;
        }
    }

    static Field findField(Class<?> c, String name, Class<?> type) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            for (Field f : k.getDeclaredFields()) {
                if (f.getName().equals(name) && f.getType() == type) return f;
            }
            for (Class<?> i : allInterfaces(k)) {
                for (Field f : i.getDeclaredFields()) {
                    if (f.getName().equals(name) && f.getType() == type) return f;
                }
            }
        }
        return null;
    }

    static Method findMethod(Class<?> c, String name, MethodType mt, Set<Class<?>> seen) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            Method m = declared(k, name, mt);
            if (m != null) return m;
        }
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            for (Class<?> i : allInterfaces(k)) {
                Method m = declared(i, name, mt);
                if (m != null) return m;
            }
        }
        if (c.isInterface()) {
            return declared(Object.class, name, mt);
        }
        return null;
    }

    static Method declared(Class<?> k, String name, MethodType mt) {
        for (Method m : k.getDeclaredMethods()) {
            if (m.getName().equals(name)
                    && m.getReturnType() == mt.returnType()
                    && Arrays.equals(m.getParameterTypes(), mt.parameterArray())) {
                return m;
            }
        }
        return null;
    }

    static Set<Class<?>> allInterfaces(Class<?> c) {
        Set<Class<?>> result = new LinkedHashSet<>();
        Deque<Class<?>> queue = new ArrayDeque<>(Arrays.asList(c.getInterfaces()));
        while (!queue.isEmpty()) {
            Class<?> i = queue.poll();
            if (result.add(i)) queue.addAll(Arrays.asList(i.getInterfaces()));
        }
        return result;
    }
}
