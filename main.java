import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class ASTNode {
    String type;
    Object children;

    public ASTNode(String myType, Object children) {
        this.type = myType;
        this.children = children;
    }

    @Override
    public String toString() {
        return type + "(" + children + ")";
    }
}

class Parser {
    private String line;
    private List<String[]> tokens;
    private int brackets;
    private int pos;
    private int groupsCount;
    private List<ASTNode> defineGroups;
    private List<Integer> expectedGroups;
    private int inGroup;
    private int inLookahead;

    public Parser(String line) {
        this.line = line;
        this.tokens = new ArrayList<>();
        this.brackets = 0;
        this.pos = 0;
        this.groupsCount = 0;
        this.defineGroups = new ArrayList<>();
        this.expectedGroups = new ArrayList<>();
        this.inGroup = 0;
        this.inLookahead = 0;
    }

    public String makeTokens() {
        int i = 0;
        String num = "123456789";
        String alf = "abcdefghijklmnopqrstuvwxyz";

        while (i < line.length()) {
            char currentChar = line.charAt(i);
            if (alf.indexOf(currentChar) != -1) {
                tokens.add(new String[]{"CHAR", String.valueOf(currentChar)});
                i++;
            } else if (currentChar == '*') {
                tokens.add(new String[]{"STAR", "-"});
                i++;
            } else if (currentChar == '|') {
                tokens.add(new String[]{"ALTERNATIVE", "-"});
                i++;
            } else if (currentChar == '(') {
                brackets++;
                if (i + 1 < line.length()) {
                    if (line.charAt(i + 1) == '?') {
                        if (i + 2 < line.length()) {
                            if (line.charAt(i + 2) == '=') {
                                tokens.add(new String[]{"LOOKAHEAD_OPEN", "-"});
                                i += 3;
                            } else if (line.charAt(i + 2) == ':') {
                                tokens.add(new String[]{"COLON_OPEN", "-"});
                                i += 3;
                            } else if (num.indexOf(line.charAt(i + 2)) != -1) {
                                tokens.add(new String[]{"EXPR_OPEN", String.valueOf(line.charAt(i + 2))});
                                i += 3;
                            } else {
                                return "ERROR";
                            }
                        } else {
                            return "ERROR";
                        }
                    } else {
                        tokens.add(new String[]{"GROUP_OPEN", "-"});
                        i++;
                    }
                } else {
                    return "ERROR";
                }
            } else if (currentChar == ')') {
                brackets--;
                if (brackets < 0) {
                    return "ERROR: UNMATCHED CLOSE PARENTHESES";
                }
                tokens.add(new String[]{"CLOSE", "-"});
                i++;
            } else {
                return "ERROR";
            }
        }
        return "OK";
    }

    private String[] getToken() {
        if (pos >= tokens.size()) {
            return null;
        } else {
            return tokens.get(pos);
        }
    }

    private void advance() {
        pos++;
    }

    private ASTNode parseSingleRG() {
        String[] currentToken = getToken();

        switch (currentToken[0]) {
            case "EXPR_OPEN":
                String node = currentToken[1];
                String[] token = getToken();
                if (token == null) {
                    throw new RuntimeException("ERROR: UNEXPECTED END");
                } else if (!token[0].equals("EXPR_OPEN")) {
                    throw new RuntimeException("ERROR: UNEXPECTED TOKEN");
                }
                expectedGroups.add(Integer.parseInt(node) - 1);
                advance();
                String[] tokenClose = getToken();
                if (tokenClose == null) {
                    throw new RuntimeException("ERROR: UNEXPECTED END");
                } else if (!tokenClose[0].equals("CLOSE")) {
                    throw new RuntimeException("ERROR: UNEXPECTED TOKEN");
                }
                advance();
                return new ASTNode("EXPR_NODE", node);

            case "CHAR":
                token = getToken();
                if (token == null) {
                    throw new RuntimeException("ERROR: UNEXPECTED END");
                } else if (!token[0].equals("CHAR")) {
                    throw new RuntimeException("ERROR: UNEXPECTED TOKEN");
                }
                advance();
                node = currentToken[1];
                return new ASTNode("CHAR_NODE", node);

            case "LOOKAHEAD_OPEN":
                token = getToken();
                if (token == null) {
                    throw new RuntimeException("ERROR: UNEXPECTED END");
                } else if (!token[0].equals("LOOKAHEAD_OPEN")) {
                    throw new RuntimeException("ERROR: UNEXPECTED TOKEN");
                }
                advance();
                if (inLookahead == 1) {
                    throw new RuntimeException("ERROR: LOOKAHEAD IN LOOKAHEAD");
                }
                inLookahead = 1;
                ASTNode lookaheadNode = parseAlt();
                if (Objects.equals(lookaheadNode.children.toString(), "[]")) {
                    throw new RuntimeException("ERROR: EMPTY LOOKAHEAD");
                }
                inLookahead = 0;
                tokenClose = getToken();
                if (tokenClose == null) {
                    throw new RuntimeException("ERROR: UNEXPECTED END");
                } else if (!tokenClose[0].equals("CLOSE")) {
                    throw new RuntimeException("ERROR: UNEXPECTED TOKEN");
                }
                advance();
                return new ASTNode("LOOKAHEAD_NODE", lookaheadNode);

            case "COLON_OPEN":
                token = getToken();
                if (token == null) {
                    throw new RuntimeException("ERROR: UNEXPECTED END");
                } else if (!token[0].equals("COLON_OPEN")) {
                    throw new RuntimeException("ERROR: UNEXPECTED TOKEN");
                }
                advance();
                ASTNode colonNode = parseAlt();
                if (Objects.equals(colonNode.children.toString(), "[]")) {
                    throw new RuntimeException("ERROR: EMPTY COLON");
                }
                tokenClose = getToken();
                if (tokenClose == null) {
                    throw new RuntimeException("ERROR: UNEXPECTED END");
                } else if (!tokenClose[0].equals("CLOSE")) {
                    throw new RuntimeException("ERROR: UNEXPECTED TOKEN");
                }
                advance();
                return new ASTNode("COLON_NODE", colonNode);

            case "GROUP_OPEN":
                if (inLookahead == 1) {
                    throw new RuntimeException("ERROR: GROUP IN LOOKAHEAD");
                }

                groupsCount++;
                if (groupsCount == 10) {
                    throw new RuntimeException("ERROR: TOO MANY GROUPS");
                }

                token = getToken();
                if (token == null) {
                    throw new RuntimeException("ERROR: UNEXPECTED END");
                } else if (!token[0].equals("GROUP_OPEN")) {
                    throw new RuntimeException("ERROR: UNEXPECTED TOKEN");
                }
                advance();

                ASTNode groupNode = parseAlt();
                if (Objects.equals(groupNode.children.toString(), "[]")) {
                    throw new RuntimeException("ERROR: EMPTY GROUP");
                }

                defineGroups.add(groupNode);
                tokenClose = getToken();
                if (tokenClose == null) {
                    throw new RuntimeException("ERROR: UNEXPECTED END");
                } else if (!tokenClose[0].equals("CLOSE")) {
                    throw new RuntimeException("ERROR: UNEXPECTED TOKEN");
                }
                advance();
                return new ASTNode("GROUP_NODE", groupNode);

            default:
                throw new RuntimeException("ERROR: UNEXPECTED TOKEN");
        }
    }

    private ASTNode parseConcat() {
        List<ASTNode> nodes = new ArrayList<>();
        while (getToken() != null && !getToken()[0].equals("CLOSE") && !getToken()[0].equals("ALTERNATIVE")) {
            ASTNode node = parseSingleRG();
            if (node != null) {
                nodes.add(node);
            }

            while (getToken() != null && getToken()[0].equals("STAR")) {
                String[] token = getToken();
                if (token == null) {
                    throw new RuntimeException("ERROR: UNEXPECTED END");
                } else if (!token[0].equals("STAR")) {
                    throw new RuntimeException("ERROR: UNEXPECTED TOKEN");
                }
                advance();
                if (!nodes.isEmpty()) {
                    ASTNode lastNode = nodes.remove(nodes.size() - 1);
                    node = new ASTNode("STAR_NODE", lastNode);
                    nodes.add(node);
                }
            }
        }

        if (nodes.size() == 1) {
            return nodes.get(0);
        }
        return new ASTNode("CONCAT_NODE", nodes);
    }

    private ASTNode parseAlt() {
        List<ASTNode> alternatives = new ArrayList<>();
        ASTNode firstPart = parseConcat();
        alternatives.add(firstPart);
        while (getToken() != null && getToken()[0].equals("ALTERNATIVE")) {
            String[] token = getToken();
            if (token == null) {
                throw new RuntimeException("ERROR: UNEXPECTED END");
            } else if (!token[0].equals("ALTERNATIVE")) {
                throw new RuntimeException("ERROR: UNEXPECTED TOKEN");
            }
            advance();
            ASTNode nextNode = parseConcat();
            if (Objects.equals(nextNode.children.toString(), "[]")) {
                throw new RuntimeException("ERROR: EMPTY ALTERNATIVE");
            }
            alternatives.add(nextNode);
        }

        if (alternatives.size() == 1) {
            return alternatives.get(0);
        }
        return new ASTNode("ALTERNATIVE_NODE", alternatives);
    }

    public ASTNode parseWithCheckOption() {
        ASTNode node = parseAlt();
        boolean allExist = true;
        for (int index : expectedGroups) {
            if (index >= defineGroups.size()) {
                allExist = false;
                break;
            }
        }
        if (!allExist) {
            throw new RuntimeException("ERROR: UNEXPECTED EXPR");
        }
        return node;
    }
}

public class Main {
    public static void main(String[] args) {

        String[] testInputs = {
            "(aa|bb)(?1)",
            "(a|(bb))(a|(?2))",
            "(a|(bb))(a|(?3))",
            "(a|(b|c))d",
            "((a|b)(c|s))a*",
            "(a(?=b))c",
            "(a*|(?:b|a))a",
            "(?=a)b",
            "a(?=b|c)d|(abf|q)",
            "(a(?1)b|c)",
            "(?1)(a|(b|c))",
            "(a|bb|(aa|(aa)))(a|(?1)(?2)(?4))",
            "(?1)(a|(b|c))",
            "aa****a*",
        };

        String[] testInputsERR = {
            "(a|b(",
            "(a|b",
            "a|b)",
            "a|b)(",
            "a|b)()",
            "(?3)(a|(b|c))",
            "((a)(a)(a)(a)(a)(a)(a)(a)(a)(a))",
            "(a)(?2)",
            "(?=(asdc))",
            "(?=a(?=c))",
            "(a|bb|(aa|(aa)))(a|(?1)(?2)(?9))",
            "(a|bb|(aa|(aa)))(a|(?6)(?2)(?3))",
            "(?1)(a|(b|)",
            "(a|bb|(aa|(aa)))(a|(?1)(?2)(?4)))",
            "(?=)",
            "()",
            "a(?=b|c)d|(abf||)",
            "(?)",
            "(?:)",
        };
        System.out.println("____________________________________________________");
        System.out.println("POSITIVE TESTS");
        System.out.println("____________________________________________________");

        for (String text : testInputs) {
            System.out.println("=====================================");
            System.out.println("Testing: " + text);
            Parser parser = new Parser(text);
            String OKorNOT = parser.makeTokens();
            if (OKorNOT.equals("OK")) {
                try {
                    System.out.println(parser.parseWithCheckOption());
                } catch (RuntimeException e) {
                    System.out.println("ERROR: " + e.getMessage());
                }
            } else {
                System.out.println("ERROR: SINTAXIS");
            }
            System.out.println("=====================================");
        }
        System.out.println("____________________________________________________");
        System.out.println("NEGATIVE TESTS");
        System.out.println("____________________________________________________");

        for (String text : testInputsERR) {
            System.out.println("=====================================");
            System.out.println("Testing: " + text);
            Parser parser = new Parser(text);
            String OKorNOT = parser.makeTokens();
            if (OKorNOT.equals("OK")) {
                try {
                    System.out.println(parser.parseWithCheckOption());
                } catch (RuntimeException e) {
                    System.out.println("ERROR: " + e.getMessage());
                }
            } else {
                System.out.println("ERROR: SINTAXIS");
            }
            System.out.println("=====================================");
        }
    }
}
