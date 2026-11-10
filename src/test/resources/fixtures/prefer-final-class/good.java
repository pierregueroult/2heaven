final class Closed {
}

abstract class Base {
}

class ExtendedInSameFile {
}

final class Child extends ExtendedInSameFile {
}

sealed class Shape permits Circle {
}

final class Circle extends Shape {
}

enum Color {
    RED, GREEN, BLUE
}

record Point(int x, int y) {
}

interface Service {
}
