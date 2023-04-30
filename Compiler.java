import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class Compiler {
    public static void main(String[] args) throws Exception{
        // Code input from code.txt
        Path fileName = Path.of("./code.txt");
        String code = Files.readString(fileName);
       System.out.println(code);
       ArrayList<Token> tokens = Tokenizer.tokenize(code);
       System.out.println("\n> Tokens: ");
       Tokenizer.displayTokens(tokens);
       System.out.println("\n--------------------\n");
        // Generate symbol table
       SymbolTable symbolTable = SymbolTable.generateFromTokens(tokens);
       symbolTable.display();
       System.out.println("--------------------\n");

       // Create instance of LL1
        LL1 ll1 = new LL1();
        // Set start symbol
        ll1.START_SYMBOL = "program";
        // Read production rules from CFG_latest.txt
        fileName = Path.of("./CFG_latest.txt");
        ll1.readProductions(fileName);
        System.out.println("> Production Rules : \n");
        ll1.displayProductionRules();
        System.out.println();
        System.out.println("> First / Follow Set \n");
        ll1.displayFirstAndFollowPosTable();
        System.out.println("\n> Production Numbers : \n");
        ll1.displayNumberedProductionRules();
        ll1.generateParsingTable();
        System.out.println("\n> LL(1) Parsing Table : \n");
        ll1.displayParsingTable();
       StringBuilder output = new StringBuilder();
       for (Token token : tokens) {
           output.append(token.text);
           output.append(" ");
       }
       System.out.println("\n> Parsing Tokens : \n");
       ll1.parseInput(output.toString());
    }
}
