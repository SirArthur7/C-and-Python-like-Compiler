import java.util.*;

public class SymbolTable {
    
    private Map<String, List<Symbol>> symbols;
    private int currentScope;
    
    public SymbolTable() {
        symbols = new HashMap<>();
        currentScope = 0;
    }
    
    public void enterScope() {
        currentScope++;
    }
    
    public void exitScope() {
        if (currentScope == 0) {
            throw new RuntimeException("Cannot exit global scope");
        }
        currentScope--;
    }

    public void insert(String name, String value) {
        if (symbols.containsKey(name)) {
            List<Symbol> symbolList = symbols.get(name);
            // Check if symbol is already defined in current scope
            for (Symbol symbol : symbolList) {
                if (symbol.getScope() == currentScope) throw new RuntimeException("Symbol already defined in current scope: " + name);
            }
            symbolList.add(new Symbol(name, value, currentScope));
        } else {
            List<Symbol> symbolList = new ArrayList<>();
            symbolList.add(new Symbol(name, value, currentScope));
            symbols.put(name, symbolList);
        }
    }

    public void display() {
        System.out.println("Symbol Table:");
        for (String name : symbols.keySet()) {
            List<Symbol> symbolList = symbols.get(name);
            for (Symbol symbol : symbolList) {
                System.out.println(symbol.getName() + " = ( type : "+symbol.getType()+", value : " + symbol.getValue() + ", scope: " + symbol.getScope() + ")");
            }
        }
    }

    public static SymbolTable generateFromTokens(List<Token> tokens){
        SymbolTable symbolTable = new SymbolTable();

        int idx = 2;
        if(!tokens.get(1).text.equals("main") || !tokens.get(2).text.equals("(")) {
            System.out.println("Code must start with main()");
        }
        while(!tokens.get(idx).text.equals(")")) {
            if(tokens.get(idx).token_type.equals("identifier")) {
                symbolTable.insert(tokens.get(idx).value, "_PARAMS_");
            }
            idx++;
        }

        for (int i=idx+1; i<tokens.size(); i++) {
            if(tokens.get(i).token_type.equals("identifier")) {

                if(tokens.get(i+1).text.equals("=") && tokens.get(i+2).token_type.equals("constant")) {
                    symbolTable.insert(tokens.get(i).value, tokens.get(i+2).value);
                }

            }
            else if(tokens.get(i).token_type.equals("punctuator")){
                if(tokens.get(i).text.equals("{")) symbolTable.enterScope();
                else if(tokens.get(i).text.equals("}")) symbolTable.exitScope();
            }
        }

        return symbolTable;
    }
    
    private class Symbol {
        private String name;
        private String value;
        private String type;
        private int scope;
        
        String determineType(String input) {
            try {
                Integer.parseInt(input);
                return "Integer";
            } catch (NumberFormatException e) {}

            // Check if input is a float
            try {
                Float.parseFloat(input);
                return "Float";
            } catch (NumberFormatException e) {}

            // Check if input is a single character
            if (input.length() == 3) {
                return "Character";
            }
            return "String";
        }

        public Symbol(String name, String value, int scope) {
            this.name = name;
            this.value = value;
            this.scope = scope;
            if(value.equals("_PARAMS_")) {
                this.type = value;
            }else {
                this.type = determineType(value);
            }
        }

        public String getName() {
            return name;
        }
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
        
        public int getScope() {
            return scope;
        }   

        public String getType() {
            return type;
        }
    }
    
}
