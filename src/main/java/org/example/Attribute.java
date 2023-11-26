package org.example;

public class Attribute {
    private String name;
    private String type;
    private String className;

    public Attribute(String name, String type, String className) {
        this.name = name;
        this.type = type;
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getClassName() { 
        return className; 
    }

    public void setClassName(String className) { 
        this.className = className; 
    }
    
    @Override
    public String toString() {
        return "Name: " + name + ", " + "Type: " + type + ", " + "Class name: " + className;
    }
}
