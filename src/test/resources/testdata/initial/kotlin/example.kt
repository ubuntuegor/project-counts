// Define a class for a person
class Person(val name: String, var age: Int) {
    fun sayHello() {
        println("Hello, my name is $name and I am $age years old.")
    }
}

// Define a class for a car
class Car(val make: String, val model: String, var year: Int) {
    fun start() {
        println("$make $model started!")
    }

    fun stop() {
        println("$make $model stopped.")
    }

    fun drive(distance: Double) {
        println("$make $model drove $distance miles.")
    }
}

// Define a function to calculate the area of a circle
fun calculateCircleArea(radius: Double): Double {
    return Math.PI * radius * radius
}

// Define a function to check if a number is even
fun isEven(number: Int): Boolean {
    return number % 2 == 0
}
