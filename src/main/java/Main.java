package reflect;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Set;
import java.util.*;
import java.lang.module.ModuleReference;
import java.lang.module.ModuleReader;
import java.lang.module.Configuration;
import java.lang.module.ResolvedModule;

public class Main {
    static Set<String> allPackagesSet = new HashSet<String>();
    static Set<String> allTypesSet = new HashSet<String>();
    static Map<String, List<String>> mapString = new HashMap<>();

    public static void main(String [] args) throws ClassNotFoundException {
        Integer n;
        firstQuestion();
        if (args.length == 0) {
            System.out.println("Null value isn't valid");
        } else {
            try{
                n = Integer.parseInt(args[0]);
                System.out.println("This takes a while");
                secondQuestion(n);
                thirdQuestion(n);
            } catch (NumberFormatException ex){
                System.out.println("Provide a valid integer number");
            }
        }
    }

    private static Stream<Module> javaSEModules() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        return bootLayer.modules().stream().filter(m -> m.getName().startsWith("java."));
    }

    private static void firstQuestion() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        Configuration bootConfig = bootLayer.configuration();
        AtomicInteger allModules = new AtomicInteger(0);
        javaSEModules().forEach( module -> {
            Optional<ResolvedModule> resolved = bootConfig.findModule(module.getName());
            Set<String> packages = module.getPackages();
            allPackagesSet.addAll(packages);
            resolved.ifPresent( rm -> {
                allModules.incrementAndGet();
                ModuleReference ref = rm.reference();
                try (ModuleReader reader = ref.open()) {
                    reader.list().forEach( file -> {
                        if (file.endsWith(".class") && !file.equals("module-info.class")) {
                            String packageName = file.substring(
                                    0, file.lastIndexOf('/')
                            ).replace('/', '.');
                            if (module.isExported(packageName)) {
                                allTypesSet.add(
                                        file.replace("/", ".")
                                                .replace(".class", "")
                                );
                            }
                        }
                    });
                }
                catch (IOException e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace(System.out);
                }
            });
        });
        System.out.println("Modules: " + allModules +
                ", Packages: " + allPackagesSet.size() +
                ", Types: " + allTypesSet.size()
        );
    }

    private static HashMap<String, Integer> sortMap(Map<String, Integer> map) {
        return map.entrySet()
                  .stream()
                  .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                  .collect(Collectors.toMap(
                          Map.Entry::getKey,
                          Map.Entry::getValue,
                          (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    private static void printTop(Map<String, Integer> map, Integer n) {
        for (int i = 0; i < n; i++) {
            System.out.println(sortMap(map).keySet().toArray()[i]);
        }
    }

    private static void secondQuestion(Integer n) {
        Map<String, Integer> map = new HashMap<>();
        allTypesSet.forEach( type -> {
            map.put(type, 0);
            mapString.put(type, new ArrayList<String>());
            allTypesSet.forEach( nested_type -> {
                try {
                    if (!Objects.equals(type, nested_type) &&
                            Class.forName(nested_type).isAssignableFrom(type.getClass())) {
                        map.computeIfPresent(type, (k, v) -> v + 1);
                        mapString.computeIfAbsent(type, v -> new ArrayList<>()).add(nested_type);

                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            });
        });
        System.out.println("\nThe top " + n + " polymorphic types are:");
        printTop(map, n);
    }

    private static void thirdQuestion(Integer n) {
        Map<String, Integer> mapOverload = new HashMap<>();

        mapString.forEach( (parentClass, children) -> {
            Method[] parentMethods = parentClass.getClass().getDeclaredMethods();
            children.forEach( child -> {
                Method[] childMethods = child.getClass().getDeclaredMethods();
                for (Method parentMethod : parentMethods) {
                    mapOverload.put(parentMethod.getName(), 0);
                    for (Method childMethod: childMethods) {
                        if (parentMethod.getName().equals(childMethod.getName()) &&
                                (parentMethod.getParameterCount() != childMethod.getParameterCount() ||
                                        parentMethod.getGenericParameterTypes() != childMethod.getGenericParameterTypes())
                        ) {
                            mapOverload.computeIfPresent(parentMethod.getName(), (k, v) -> v + 1);
                        }
                    }
                }
            });
        });
        System.out.println("\nThe top " + n + " overloaded methods are:");
        printTop(mapOverload, n);
    }
}
