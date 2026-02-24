Struttura minimale, senza reflection né annotazioni.

## 1️⃣ Eccezione dedicata

```java
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
```

---

## 2️⃣ Validator centrale

```java
public final class Validator {

    private Validator() {}

    public static String required(String value, String name) {
        if (value == null || value.isBlank())
            throw new ValidationException(name + " is required");
        return value;
    }

    public static String maxLength(String value, int max, String name) {
        if (value != null && value.length() > max)
            throw new ValidationException(name + " too long");
        return value;
    }

    public static String pattern(String value, String regex, String name) {
        if (value != null && !value.matches(regex))
            throw new ValidationException(name + " invalid format");
        return value;
    }

    public static int positiveInt(String value, String name) {
        try {
            int v = Integer.parseInt(value);
            if (v <= 0) throw new Exception();
            return v;
        } catch (Exception e) {
            throw new ValidationException(name + " must be positive integer");
        }
    }

    public static int range(int value, int min, int max, String name) {
        if (value < min || value > max)
            throw new ValidationException(name + " out of range");
        return value;
    }

    public static boolean bool(String value, String name) {
        if (!"true".equalsIgnoreCase(value) && 
            !"false".equalsIgnoreCase(value))
            throw new ValidationException(name + " must be boolean");
        return Boolean.parseBoolean(value);
    }

    public static String email(String value, String name) {
        return pattern(value,
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$",
            name);
    }

    public static String oneOf(String value, String name, String... allowed) {
        for (String a : allowed)
            if (a.equals(value))
                return value;
        throw new ValidationException(name + " invalid value");
    }
}
```

---

## 3️⃣ Uso tipico

```java
String username = Validator.maxLength(
    Validator.required(params.get("username"), "username"),
    50,
    "username"
);

int page = Validator.range(
    Validator.positiveInt(params.get("page"), "page"),
    1,
    100,
    "page"
);
```

---

## Opzioni comuni da includere

* `required`
* `notBlank`
* `maxLength / minLength`
* `pattern`
* `positiveInt`
* `range`
* `email`
* `uuid`
* `oneOf`
* `optional(...)` helper
* `custom(Predicate<T>)`

Struttura semplice, statica, nessuna dipendenza, facilmente estendibile.
