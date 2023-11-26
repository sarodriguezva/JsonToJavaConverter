package org.example;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONToJavaClassConverterBackup {
    private String JSONFilePath;
    private final Map<Integer, List<Attribute>> attributesByDepth;
    private final Stack<String> classNames;

    public JSONToJavaClassConverterBackup(String JSONFilePath) {
        this.JSONFilePath = JSONFilePath;
        this.attributesByDepth = new HashMap<>();
        this.classNames = new Stack<>();
    }

    public String getJSONFilePath() {
        return JSONFilePath;
    }

    public void setJSONFilePath(String JSONFilePath) {
        this.JSONFilePath = JSONFilePath;
    }

    public boolean isAValidJSON() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(new File(JSONFilePath));
            
            classNames.push("Root");
            traverseJson(rootNode, 0);

            int depth = 0;
            for(List<Attribute> list : attributesByDepth.values()) {
                System.out.println("Nivel de anidamiento: " + depth);
                for(Attribute attr: list) {
                    System.out.println(attr.toString());
                }
                depth++;
            }
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

    private void traverseJson(JsonNode node, int depth) {
        // Recorrer los nodos hijos
        for (JsonNode childNode : node) {
            traverseJson(childNode, depth + 1);
            // System.out.println(childNode);
        }

        // Se obtienen todos los atributos del nodo
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();

            String name = field.getKey(); // Nombre del atributo
            JsonNode value = field.getValue(); // Valor del atributo
            String type = value.getNodeType().toString();
            System.out.println("Nombre: " + name + " Value: " + value.toString() + " Tipo: " + type);// Tipo de dato del valor del atributo  
            if (!"OBJECT".equals(type)) {
                // Se agrega el nombre actual a la pila
                classNames.push(name);
                Attribute attr = new Attribute(name, type, classNames.toString());
                if (attributesByDepth.containsKey(depth)) {
                    List<Attribute> temp = attributesByDepth.get(depth);
                    // System.out.println(temp);
                    temp.add(attr);
                    attributesByDepth.put(depth, temp);
                } else {
                    List<Attribute> temp = new LinkedList<>(); 
                    temp.add(attr);
                    attributesByDepth.put(depth, temp); 
                }
                // Se elimina el nombre de la pila dado que ya se salió de ese nivel
                classNames.pop();
            } else {
                classNames.push(name);
            }
        }
    }
}

