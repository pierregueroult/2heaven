class Good {
    boolean same(String a, String b) {
        return a != null && a.equals(b);
    }

    boolean isString(java.lang.reflect.Type type) {
        return type == String.class;
    }

    boolean sameClass(Class<?> left, Class<?> right) {
        return left == right;
    }
}
