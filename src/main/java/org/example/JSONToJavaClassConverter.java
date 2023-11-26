package org.example;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONToJavaClassConverter {
    private String JSONFilePath;
    private ConcurrentHashMap<String, JsonNode> elements = new ConcurrentHashMap<>();
    private LinkedHashMap<String, LinkedList<String>> finalClasses = new LinkedHashMap<>();

    public LinkedHashMap<String, LinkedList<String>> getFinalClasses() {
        return finalClasses;
    }

    public JSONToJavaClassConverter(String JSONFilePath) {
        this.JSONFilePath = JSONFilePath;
    }

    public boolean isAValidJSON() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(new File(JSONFilePath));

            HashMap<String, JsonNode> attributes = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> it = rootNode.fields();
            attributes = updateAttributes(it, attributes);
            createClass("MyJson", attributes);
            iterateAttributes(attributes, rootNode);

            elements.forEach((key, value) -> {
                inspectClass(key, value, true);
            });
        } catch (FileNotFoundException e) {
            System.err.println("File not found");
            return false;
        } catch (IOException e) {
            System.err.println("The file is not a valid JSON");
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void iterateAttributes(HashMap<String, JsonNode> attributes, JsonNode parent) {
        attributes.forEach((className, node) -> {
            String nodeType = getNodeTypeName(node);
            if (nodeType.equals("Object") || nodeType.equals("ArrayList")) {
                inspectClass(className, parent, false);
            }
        });
    }

    private void inspectClass(String className, JsonNode node, boolean isArrayElement) {
        HashMap<String, JsonNode> attrs = new HashMap<>();
        JsonNode classNode = isArrayElement ? node : node.get(className);
        if (!classNode.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> it = classNode.fields();
        attrs = updateAttributes(it, attrs);
        createClass(className, attrs);
        iterateAttributes(attrs, classNode);
    }

    private HashMap<String, JsonNode> updateAttributes(Iterator<Map.Entry<String, JsonNode>> it, HashMap<String, JsonNode> attrs) {
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> attr = it.next();
            String attrName = attr.getKey();
            attrs.put(attrName, attr.getValue());
        }
        return attrs;
    }

    private String getNodeTypeName(JsonNode node) {
        String typeName = node.getNodeType().name().toLowerCase();
        if (typeName.equals("array")) {
            typeName = "ArrayList";
        }
        return capitalize(typeName);
    }

    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private boolean isEqual(JsonNode element, JsonNode addedElement) {
        Iterator<Map.Entry<String, JsonNode>> it = element.fields();
        Iterator<Map.Entry<String, JsonNode>> it2 = addedElement.fields();
        boolean areEqual = true;
        while (it.hasNext() && it2.hasNext()) {
            Map.Entry<String, JsonNode> attr = it.next();
            Map.Entry<String, JsonNode> mainAttr = it2.next();

            String attrName = attr.getKey();
            String mainAttrName = mainAttr.getKey();
            String attrType = getNodeTypeName(attr.getValue());
            String mainAttrType = getNodeTypeName(mainAttr.getValue());

            if (!attrName.equals(mainAttrName) || !attrType.equals(mainAttrType)) {
                areEqual = false;
                break;
            }
        }
        return !it.hasNext() && !it2.hasNext() && areEqual;
    }

    private String setArrayType(JsonNode arrayNode, String mainType, int cnt) {
        /* Si los elementos son Object, hay que mirar si todos son MyObjectElement por comparación de atributos.
        * Si un elemento es distinto se crea el nuevo objeto y el array sería de tipo Object.
        */
        for (JsonNode element : arrayNode) {
            String elementType = getNodeTypeName(element);

            if (elementType.equals("Object")) {
                elementType = getObjectType(elementType, mainType, element, cnt);
            }

            if (elementType.equals("Number")) {
                elementType = getNumberType(element);
                if (mainType.equals("Number")) {
                    mainType = getNumberType(arrayNode.get(0));
                }
            }

            if (!elementType.equals(mainType)) {
                //Caso en el que se tiene un array de elementos de distinto tipo
                boolean setLongDouble = 
                    (mainType.equals("Long Integer") && elementType.equals("Long Double")) || 
                    (mainType.equals("Long Double") && elementType.equals("Long Integer"));

                if (setLongDouble){
                    mainType = "Long Double";
                    break;
                }
                mainType = "Object";
                break;
            }
        }
        return mainType;
    }

    private String getAttributeType(String name, JsonNode node) {
        String type = getNodeTypeName(node);
        if (type.equals("ArrayList")) {
            int objectsCount = 0;
            //mainType representa el tipo de objetos en el ArrayList
            //Por ejemplo String es el mainType de ArrayList<String> 
            String mainType = getNodeTypeName(node.get(0));
            if (mainType.equals("Object")) {
                //En principio mainType será de tipo MyObjectElement
                objectsCount++;
                mainType = capitalize(name) + "Element";
                elements.put(mainType, node.get(0));
            }

            mainType = setArrayType(node, mainType, objectsCount);
            type = "ArrayList<" + mainType + ">";
        }

        if (type.equals("Number")) {
            type = getNumberType(node);
        }

        if (type.equals("Object") || type.equals("Pojo")) {
            type = capitalize(name);
        }

        if (type.equals("Null")) {
            type = "Object";
        }
        return type;
    }

    private String getObjectType(String initial, String mainType, JsonNode element, int cnt) {
        String elementType = initial;
        boolean alreadyParsed = false;
        for (Map.Entry<String, JsonNode> type : elements.entrySet()) {
            JsonNode addedElement = elements.get(type.getKey());
            if (isEqual(element, addedElement)) {
                alreadyParsed = true;
                elementType = type.getKey();
                break;
            }
        }
        //Si el elemento es nuevo, se añade el nuevo tipo para parsearlo
        if (!alreadyParsed) {
            cnt++;
            elementType = mainType + cnt;
            elements.put(elementType, element);
        }
        return elementType;
    }

    private String getNumberType(JsonNode node) {
        String number = node.toString();
        if (number.contains(".") && number.split("\\.")[0].length() < 19) {
            return "Long Double";
        } else {
            return "Long Integer";
        }
    }

    private void createClass(String className, HashMap<String, JsonNode> attributes) {
        LinkedList<String> attributesList = new LinkedList<>();
        
        attributes.forEach((attributeName, attributeNode) -> {
            attributesList.add("private " + getAttributeType(attributeName, attributeNode) + " " + attributeName + ";");
            System.out.println("Attr: " + getAttributeType(attributeName, attributeNode) + " " + attributeName + ";");
        });
        System.out.println();
        
        this.finalClasses.put(capitalize(className), attributesList);
    }
}

