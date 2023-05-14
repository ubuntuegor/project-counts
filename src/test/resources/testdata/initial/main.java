public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, world!");

        // Create a new instance of the Person class
        Person person = new Person("John", 30);

        // Call the sayHello() method on the person instance
        person.sayHello();

        // Create a new instance of the Car class
        Car car = new Car("Honda", "Civic", 2021);

        // Call the start(), drive(), and stop() methods on the car instance
        car.start();
        car.drive(10.5);
        car.stop();

        // Call the calculateCircleArea() function and print the result
        double radius = 5.0;
        double area = calculateCircleArea(radius);
        System.out.println("The area of a circle with radius " + radius + " is " + area);

        // Call the isEven() function and print the result
        int number = 7;
        boolean isEven = isEven(number);
        System.out.println(number + " is even: " + isEven);
    }

    // Define a class for a person
    static class Person {
        String name;
        int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        void sayHello() {
            System.out.println("Hello, my name is " + name + " and I am " + age + " years old.");
        }
    }

    // Define a class for a car
    static class Car {
        String make;
        String model;
        int year;

        Car(String make, String model, int year) {
            this.make = make;
            this.model = model;
            this.year = year;
        }

        void start() {
            System.out.println(make + " " + model + " started!");
        }

        void stop() {
            System.out.println(make + " " + model + " stopped.");
        }

        void drive(double distance) {
            System.out.println(make + " " + model + " drove " + distance + " miles.");
        }
    }

    // Define a function to calculate the area of a circle
    static double calculateCircleArea(double radius) {
        return Math.PI * radius * radius;
    }

    // Define a function to check if a number is even
    static boolean isEven(int number) {
        return number % 2 == 0;
    }
}
