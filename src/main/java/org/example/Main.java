package org.example;

public class Main {
    public static void main(String[] args) {
        JSONToJavaClassConverter json = new JSONToJavaClassConverter(
            System.getProperty("user.dir") +
            "\\src\\main\\java\\org\\example\\prueba.json"
        );

        if (json.isAValidJSON()) {
            System.out.println("Execution was successful");
        }else {
            System.out.println("Execution failed");
        }
    }
}