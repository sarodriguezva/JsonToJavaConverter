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
            // Se lee el archivo JSON y se genera su representación como árbol
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(new File(JSONFilePath));
            
            // Atributos del JSON principal
            HashMap<String, JsonNode> attributes = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> it = rootNode.fields();
            attributes = updateAttributes(it, attributes);
            // Crear la clase principal "MyJson"
            createClass("MyJson", attributes);
            
            // Iterar sobre los atributos del JSON principal y sus elementos anidados
            iterateAttributes(attributes, rootNode);
            
            // Inspeccionar cada elemento y generar clases según la estructura
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

    /*
    * Método para iterar sobre los atributos de una clase y sus elementos anidados
    */
    private void iterateAttributes(HashMap<String, JsonNode> attributes, JsonNode parent) {
        attributes.forEach((className, node) -> {
            String nodeType = getNodeTypeName(node);
            // Si el atributo es un objeto o una lista, inspeccionar la clase correspondiente
            if (nodeType.equals("Object") || nodeType.equals("ArrayList")) {
                inspectClass(className, parent, false);
            }
        });
    }
    
    /*
    * Método para inspeccionar una clase y generar las clases correspondientes
    */
    private void inspectClass(String className, JsonNode node, boolean isArrayElement) {
        // Atributos de la clase que se está inspeccionando
        HashMap<String, JsonNode> attrs = new HashMap<>();
        // Obtener el nodo correspondiente a la clase, considerando si es un elemento de una lista
        JsonNode classNode = isArrayElement ? node : node.get(className);
        if (!classNode.isObject()) {
            return;
        }
        // Iterador sobre los atributos del nodo
        Iterator<Map.Entry<String, JsonNode>> it = classNode.fields();
        attrs = updateAttributes(it, attrs);
        createClass(className, attrs);
        iterateAttributes(attrs, classNode);
    }
    
    /*
    * Actualiza la lista de atributos de una clase con los atributos proporcionados por un iterador
    */
    private HashMap<String, JsonNode> updateAttributes(Iterator<Map.Entry<String, JsonNode>> it, HashMap<String, JsonNode> attrs) {
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> attr = it.next();
            String attrName = attr.getKey();
            attrs.put(attrName, attr.getValue());
        }
        return attrs;
    }
    
    /*
    * Obtiene el nombre del tipo de dato de un nodo JSON.
    */
    private String getNodeTypeName(JsonNode node) {
        String typeName = node.getNodeType().name().toLowerCase();
        if (typeName.equals("array")) {
            typeName = "ArrayList";
        }
        return capitalize(typeName);
    }
    
    /*
    * Convierte la primer letra de un string a mayúscula
    */
    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
    
    /*
    * Compara dos nodos JSON para determinar si son iguales en términos de sus atributos
    */
    private boolean isEqual(JsonNode element, JsonNode addedElement) {
        // Obtener iteradores sobre los atributos de ambos nodos
        Iterator<Map.Entry<String, JsonNode>> it = element.fields();
        Iterator<Map.Entry<String, JsonNode>> it2 = addedElement.fields();
        boolean areEqual = true;
        
        while (it.hasNext() && it2.hasNext()) {
            Map.Entry<String, JsonNode> attr = it.next();
            Map.Entry<String, JsonNode> mainAttr = it2.next();
            
            // Obtener información sobre el atributo del primer nodo
            String attrName = attr.getKey();
            String mainAttrName = mainAttr.getKey();
            String attrType = getNodeTypeName(attr.getValue());
            String mainAttrType = getNodeTypeName(mainAttr.getValue());
            
            // Verificar si los atributos son iguales en nombre y tipo
            if (!attrName.equals(mainAttrName) || !attrType.equals(mainAttrType)) {
                areEqual = false;
                break;
            }
        }
        // Devolver true si ambos iteradores están vacíos y todos los atributos coinciden
        return !it.hasNext() && !it2.hasNext() && areEqual;
    }
    
    /*
    * Determina y establece el tipo de un array en función de los tipos de sus elementos
    */
    private String setArrayType(JsonNode arrayNode, String mainType, int cnt) {
        /* Si los elementos son Object, hay que mirar si todos son MyObjectElement por comparación de atributos.
        * Si un elemento es distinto se crea el nuevo objeto y el array sería de tipo Object.
        */
        for (JsonNode element : arrayNode) {
            // Obtener el tipo del elemento del array
            String elementType = getNodeTypeName(element);
            
            // Si el elemento del array es un objeto, determinar el tipo del objeto
            if (elementType.equals("Object")) {
                elementType = getObjectType(elementType, mainType, element, cnt);
            }
            
            // Si el elemento del array es un número, determinar el tipo del número
            if (elementType.equals("Number")) {
                elementType = getNumberType(element);
                // Si el tipo principal es "Number", actualizarlo según el tipo del primer elemento
                if (mainType.equals("Number")) {
                    mainType = getNumberType(arrayNode.get(0));
                }
            }
            
            // Manejar el caso en el que los elementos del array son de tipos diferentes
            if (!elementType.equals(mainType)) {
                //Caso en el que se tiene un array de elementos de distinto tipo
                boolean setDouble = 
                    (mainType.equals("Integer") && elementType.equals("Double")) || 
                    (mainType.equals("Double") && elementType.equals("Integer"));
                
                // Si se detecta un cambio de tipo, actualizar el tipo principal del array
                if (setDouble){
                    mainType = "Double";
                    break;
                }
                mainType = "Object";
                break;
            }
        }
        return mainType;
    }
    
    /*
    * Obtiene el tipo de un atributo en función de su nombre y nodo JSON asociado
    */
    private String getAttributeType(String name, JsonNode node) {
        // Obtener el tipo base del nodo JSON
        String type = getNodeTypeName(node);
        // Si el tipo es "ArrayList", determinar el tipo de objetos contenidos en la lista
        if (type.equals("ArrayList")) {
            int objectsCount = 0;
            //mainType representa el tipo de objetos en el ArrayList
            //Por ejemplo String es el mainType de ArrayList<String> 
            String mainType = getNodeTypeName(node.get(0));
            
            // Si el tipo principal es "Object", actualizarlo y añadir el tipo del primer objeto
            if (mainType.equals("Object")) {
                //En principio mainType será de tipo MyObjectElement
                objectsCount++;
                mainType = capitalize(name) + "Element";
                elements.put(mainType, node.get(0));
            }
            
            // Determinar el tipo final del ArrayList
            mainType = setArrayType(node, mainType, objectsCount);
            type = "ArrayList<" + mainType + ">";
        }
        
        // Si el tipo es "Number", determinar si es "Integer" o "Double"
        if (type.equals("Number")) {
            type = getNumberType(node);
        }
        
        // Si el tipo es "Object" o "Pojo", capitalizar el nombre del atributo
        if (type.equals("Object") || type.equals("Pojo")) {
            type = capitalize(name);
        }
        
        // Si el tipo es "Null", considerarlo como "Object"
        if (type.equals("Null")) {
            type = "Object";
        }
        return type;
    }
    
    /*
    * Obtiene el tipo de un objeto a partir de su tipo inicial, el tipo principal, el elemento y un contador
    */
    private String getObjectType(String initial, String mainType, JsonNode element, int cnt) {
        String elementType = initial;
        boolean alreadyParsed = false;
        
        // Iterar sobre los elementos previamente analizados
        for (Map.Entry<String, JsonNode> type : elements.entrySet()) {
            JsonNode addedElement = elements.get(type.getKey());
            
            // Verificar si el elemento es igual a uno ya analizado
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
    
    /*
    * Obtiene el tipo de un objeto a partir de su tipo inicial, el tipo principal, el elemento y un contador
    */
    private String getNumberType(JsonNode node) {
        if (node.isDouble() || node.isFloat()) {
            return "Double";
        }
        return "Integer";
    }
    
    /*
    * Crea una clase Java a partir de los atributos de una clase JSON y la agrega
    * a la lista final de clases generadas
    */
    private void createClass(String className, HashMap<String, JsonNode> attributes) {
        LinkedList<String> attributesList = new LinkedList<>();
        // Mapeo de nombres de atributos a sus tipos
        Map<String, String> map = new HashMap<String, String>();
        // Añadir la declaración de la clase Java
        attributesList.add("class " + capitalize(className) + " {");
        
        // Iterar sobre los atributos y generar declaraciones de atributos en la clase Java
        attributes.forEach((attributeName, attributeNode) -> {
            // Obtener el tipo del atributo
            String attributeType = getAttributeType(attributeName, attributeNode);
            // Añadir el atributo a la lista de atributos de la clase Java
            map.put(attributeName, attributeType);
            // Añadir el atributo al mapeo de nombres de atributos a tipos
            attributesList.add("    private " + attributeType + " " + attributeName + ";");
        });
        
        // Añadir métodos getters y setters
        attributesList.add("");
        createget(map, attributesList);
        createset(map, attributesList);
        attributesList.add("}\n");
        
        // Agregar la clase Java a la lista final de clases generadas
        this.finalClasses.put(capitalize(className), attributesList);
    }
    
    /*
    * Se crean los getters de todos los atributos
    */
    private void createget(Map<String, String> map, LinkedList<String> attributesList) {
        for (Map.Entry<String, String> entry : map.entrySet()){
            attributesList.add("    public " + entry.getValue() + " get" + capitalize(entry.getKey()) + "() {\n        return " + entry.getKey() + ";\n    }\n");
        }
      
    }
    
    /*
    * Se crean los setters de todos los atributos
    */
    private void createset(Map<String, String> map, LinkedList<String> attributesList) {
        for (Map.Entry<String, String> entry : map.entrySet()){
            attributesList.add("    public void set" + capitalize(entry.getKey()) + "(" + entry.getValue() + " " + entry.getKey() + ") {\n        this." + entry.getKey() + " = " + entry.getKey() + ";\n    }\n");
        }
    }
}


