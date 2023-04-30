import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class LL1 {
    private List<String> nonTerminalKeys;
    private List<String> terminalKeys;
    private Map<String, List<List<String>>> production_rules;
    private Map<String, List<String>> production_rules_numbered; // only for parsing purpose, to lookup reduced production
    public Map<String, Set<String>> firstPos;
    public Map<String, Set<String>> followPos;
    private Map<String, Map<String, String>> parsingTable;
    public static final String EPSILON = "EPSILON";
    public static final String DOLLAR = "$";
    public static final String SEPARATOR = "#";
    public static String START_SYMBOL = null;

    public LL1() {
        nonTerminalKeys = new ArrayList<>();
        terminalKeys = new ArrayList<>();
        production_rules = new HashMap<>();
        production_rules_numbered = new HashMap<>();
        firstPos = new HashMap<>();
        followPos = new HashMap<>();
        parsingTable = new HashMap<>();
    }

    public void readProductions(Path path) throws Exception {
        List<String> raw_lines = Files.readAllLines(path);
        for(String line : raw_lines) {
            String[] parts = line.split("->");
            String nonTerminal = parts[0].trim();
            nonTerminalKeys.add(nonTerminal);
            List<List<String>> productions = new ArrayList<>();
            String[] production_parts = parts[1].trim().split("\\|");
            for (String production_part : production_parts) {
                String[] symbols = production_part.trim().split(" ");
                List<String> tmp = Arrays.asList(symbols).stream()
                        .map(String::trim)
                        .filter(x -> !x.isEmpty())
                        .collect(Collectors.toList());
                productions.add(tmp);
            }
            production_rules.put(nonTerminal, productions);
        }

        // generate the numbered productions
        for(String symbol : production_rules.keySet()) {
            List<List<String>> productions = production_rules.get(symbol);
            for (int i = 0; i < productions.size(); i++) {
                List<String> production = productions.get(i);
                if(production.size() == 1 && production.get(0).equals(EPSILON)){
                    production = new ArrayList<>();
                }
                production_rules_numbered.put(symbol+SEPARATOR+i, production);
            }
        }
    }

    public boolean isNonTerminal(String symbol) {
        return production_rules.containsKey(symbol);
    }

    public boolean isTerminal(String symbol) {
        return !symbol.equals(EPSILON) && !isNonTerminal(symbol);
    }

    public Set<String> findFirst(String symbol) {
        Set<String> first = new HashSet<>();
        // symbol is a terminal, return symbol
        if(!isNonTerminal(symbol)) {
            first.add(symbol);
            return first;
        }
        // symbol is epsilon, return EPSILON
        if(isNullable(symbol)) {
            first.add(EPSILON);
            return first;
        }
        // already computed, return firstPos
        // if(firstPos.containsKey(symbol)) {
        //     return firstPos.get(symbol);
        // }
        // Fetch productions of symbol
        List<List<String>> productions = production_rules.get(symbol);
        // For each production, find first of first symbol
        for (List<String> production : productions) {
            for (int i = 0; i < production.size(); i++) {
                Set<String> firstSymbolFirst = findFirst(production.get(i));
                if(isNullable(production.get(i)) || !firstSymbolFirst.contains(EPSILON)) {
                    first.addAll(firstSymbolFirst);
                    break;
                }else{
                    // remove only if it is not the last symbol
                    if(i + 1 < production.size()) {
                        firstSymbolFirst.remove(EPSILON);
                    }
                    first.addAll(firstSymbolFirst);
                }
            }
        }
        // Store result in firstPos against symbol
        firstPos.put(symbol, new HashSet<>(first));
        return first;
    }

    public Set<String> findFollow(String symbol) {
        if(followPos.containsKey(symbol)) {
            return followPos.get(symbol);
        }
        HashSet<String> follow = new HashSet<>();
        if(isNullable(symbol)) {
            return follow;
        }

        // If symbol is start symbol, add $ to follow list
        if(START_SYMBOL.equals(symbol)) {
            follow.add(DOLLAR);
        }

        // Fetch all productions
        for (String nonTerminal : production_rules.keySet()) {
            // For each production parts, find follow of symbol
            List<List<String>> productions = production_rules.get(nonTerminal);
            for (List<String> production : productions) {
                for (int i = 0; i < production.size(); i++) {
                    String currentSymbol = production.get(i);
                    String nextSymbol = i + 1 < production.size() ? production.get(i + 1) : null;
                    if(currentSymbol.equals(symbol)) {
                        if(nextSymbol == null) {
                            // last symbol
                            if(!nonTerminal.equals(symbol)){
                                follow.addAll(findFollow(nonTerminal));
                            }
                        }else{
                            if(!isNonTerminal(nextSymbol)) {
                                // terminal
                                follow.add(nextSymbol);
                            }else{
                                Set<String> first = findFirst(nextSymbol);
                                // if first has epsilon then go for next symbol in production
                                if(first.contains(EPSILON)){
                                    first.remove(EPSILON);
                                    follow.addAll(first);
                                    if(i + 2 < production.size()) {
                                        follow.addAll(findFirst(production.get(i + 2)));
                                    }else{
                                        follow.addAll(findFollow(nonTerminal));
                                    }
                                }else{
                                    follow.addAll(first);
                                }
                            }
                        }
                    }
                }
            }
        }

        followPos.put(symbol, follow);
        return follow;

    }

    public boolean isNullable(String symbol) {
        return symbol.equals(EPSILON);
    }

    public void computeFirstPos() {
        for (String nonTerminal : production_rules.keySet()) {
            findFirst(nonTerminal);
        }
    }

    public void computeFollowPos() {
        for (String nonTerminal : production_rules.keySet()) {
            findFollow(nonTerminal);
        }
    }

    public void generateTerminalKeys() {
        terminalKeys.clear();
        HashSet<String> terminals = new HashSet<>();
        for(List<List<String>> productions: production_rules.values()) {
            for(List<String> production: productions) {
                for(String symbol: production)  {
                    if(isTerminal(symbol)) {
                        terminals.add(symbol);
                    }
                }
            }
        }
        terminalKeys.addAll(terminals);
    }

    public void generateParsingTable() {
//        (production_rules) S-> a A | B :: S -> [[a, A], [B]]
//        rows -> Non terminals
//        columns -> terminals
        generateTerminalKeys();
        computeFirstPos();
        computeFollowPos();
        for(String nonTerminal: nonTerminalKeys) {
            parsingTable.put(nonTerminal, new HashMap<>());
        }

        for(String symbol: production_rules.keySet()) {
            List<List<String>> productions = production_rules.get(symbol);
            Map<String, String> parsingRow = new HashMap<>();
            int idx = -1;
//            TODO: FIX Prod_Rule_id not correctly incremented check for F_0 and F_1 --done
            for(List<String> production: productions) {
                idx++;
                if(isTerminal(production.get(0)))   {
                    if(parsingRow.containsKey(production.get(0))) {
                        System.out.println("Grammar is not LL(1) parsable");
                        System.exit(1);
                    }
                    parsingRow.put(production.get(0), symbol+SEPARATOR+idx);
                    continue;
                }else if(production.get(0).equals(EPSILON)) {
                    Set<String> last_of_symbol = followPos.get(symbol);
                    for(String elem: last_of_symbol) {
                        if(parsingRow.containsKey(elem)) {
                            System.out.println("Grammar is not LL(1) parsable");
                            System.exit(1);
                        }
                        parsingRow.put(elem, EPSILON);
                    }
                    continue;
                }
                Set<String> first_of_symbol = firstPos.get(production.get(0));
                for(String ele: first_of_symbol) {
                    if (ele.equals(EPSILON)) {
//                        TODO: fill elements of FOLLOWPOS(symbol) column with EPSILON --done
                        Set<String> last_of_symbol = followPos.get(symbol);
                        for(String elem: last_of_symbol) {
                            if(parsingRow.containsKey(elem)) {
                                System.out.println("Grammar is not LL(1) parsable");
                                System.exit(1);
                            }
                            parsingRow.put(elem, EPSILON);
                        }
                    }else {
                        if(parsingRow.containsKey(ele)) {
                            System.out.println("Grammar is not LL(1) parsable");
                            System.exit(1);
                        }
                        parsingRow.put(ele, symbol+SEPARATOR+idx);
                    }
                }
            }
            parsingTable.put(symbol, parsingRow);
        }

    }

//  TODO: check about null productions and EPSILON in stack, etc.
    public void parseInput(String input) {
        ArrayList<String> inputList = new ArrayList<>(Arrays.asList(input.split(" ")));
        inputList.add(DOLLAR);
        System.out.println(inputList);
        Stack<String> parseStack = new Stack<>();
        Queue<String> inputQueue = new LinkedList<>(inputList);
        parseStack.push(DOLLAR);
        parseStack.push(START_SYMBOL);

        // print header
        int width = 30;
        System.out.print(centerText("ACTION", width));
        System.out.print(centerText("STACK", width));
        System.out.println("");

        while(!parseStack.empty()) {
            String curr_symbol = parseStack.peek();
            String curr_token = inputQueue.peek();
            if(isTerminal(curr_symbol) || curr_symbol.equals(DOLLAR)) {
                if(curr_symbol.equals(curr_token)) {
//                    Match found
                    parseStack.pop();
                    inputQueue.poll();

                    System.out.print(centerText("MATCHED "+curr_token, width));
                    System.out.print(centerText(String.valueOf(parseStack), width));
                }else {
                    System.out.println("Parsing Failed ! Unexpected Token.\t Found "+curr_token+" instead of "+curr_symbol);
                    return;
                }
            }else if(isNonTerminal(curr_symbol)) {
//                top of stack is a non-terminal, appropriate production-rules are pushed to the stack.
                if(parsingTable.get(curr_symbol).containsKey(curr_token)) {
                    String production_rule_id = parsingTable.get(curr_symbol).get(curr_token);
                    parseStack.pop();
                    if(!production_rule_id.equals(EPSILON)) {
//                        If production rule doesn't produce EPSILON, push RHS of production into stack
                        List<String> elements = production_rules_numbered.get(production_rule_id);
                        ListIterator<String> itr = elements.listIterator(elements.size());
                        while(itr.hasPrevious()) {
                            String nextSymbol = itr.previous();
                            if(nextSymbol.equals(EPSILON)) {
                                continue;
                            }
                            parseStack.push(nextSymbol);
                        }
                    }

                    System.out.print(centerText("PUSHED "+production_rule_id, width));
                    System.out.print(centerText(String.valueOf(parseStack), width));
                }else {
                    System.out.println("Parsing Failed ! No production rule found for this conversion\t"+curr_symbol+" -> "+curr_token);
                    return;
                }
            }
            System.out.println();
        }

        System.out.println("Parsing Successful ! Code is Accepted");
    }

    // Display functions
    public void displayProductionRules(){
        for (String nonTerminal : production_rules.keySet()) {
            for (List<String> rule : production_rules.get(nonTerminal)) {
                System.out.print(nonTerminal+" -> ");
                for (String symbol : rule) {
                    System.out.print(symbol+" ");
                }
                System.out.println();
            }
        }
    }

    public void displayParsingTable(){
        HashSet<String> symbols = new HashSet<>();
        symbols.addAll(terminalKeys);
        symbols.remove(EPSILON);
        symbols.add(DOLLAR);
        int width = 30;
        // print header
        System.out.print(centerText("Non-Terminals", width));
        for (String symbol : symbols) {
            System.out.print(centerText(symbol, width));
        }
        // print rows
        for (String non_terminals : parsingTable.keySet()) {
            System.out.println();
            System.out.print(centerText(non_terminals.toString(), width));
            for (String symbol : symbols) {
                if(parsingTable.get(non_terminals).containsKey(symbol)){
                    System.out.print(centerText(parsingTable.get(non_terminals).get(symbol), width));
                }else{
                    System.out.print(centerText("", width));
                }
            }
        }
        System.out.println();
    }

    public void displayFirstAndFollowPosTable(){
        System.out.println(centerText("Symbol", 10) + centerText("FIRST", 200) + centerText("FOLLOW", 200));
        for (String nonTerminal : nonTerminalKeys) {
            System.out.println(centerText(nonTerminal, 10)+centerText(findFirst(nonTerminal).toString(), 200)+centerText(findFollow(nonTerminal).toString(), 200));
        }
        System.out.println();
    }

    public void displayNumberedProductionRules(){
        for (String nonTerminal : production_rules_numbered.keySet()) {
            System.out.print(nonTerminal+" -> ");
            for (String symbol : production_rules_numbered.get(nonTerminal)) {
                System.out.print(symbol+" ");
            }
            System.out.println();
        }
    }

    public static String centerText(String input, int width) {
        if (input.length() >= width) {
            return input; // if the input is already wider than the desired width, return it as-is
        }

        int padding = width - input.length();
        int leftPadding = padding / 2;
        int rightPadding = padding - leftPadding;

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < leftPadding; i++) {
            builder.append(" "); // add left padding
        }
        builder.append(input); // add the input text
        for (int i = 0; i < rightPadding; i++) {
            builder.append(" "); // add right padding
        }

        return builder.toString();
    }

    public static void main(String[] args) throws Exception {
        LL1 ll1 = new LL1();
        // Read production rules
        Path fileName = Path.of("./CFG.txt");
        ll1.readProductions(fileName);
        System.out.println("> Production Rules : \n");
        // Display production rules
        ll1.START_SYMBOL = "S";
        ll1.displayProductionRules();
        ll1.generateParsingTable();
        ll1.displayFirstAndFollowPosTable();
        ll1.displayNumberedProductionRules();
        ll1.displayParsingTable();
        String input = "id * id";
//        String input = "a d b";
        ll1.parseInput(input);
    }
}
