
package edu.compilerlab;

import java.util.*;
import java.util.regex.*;

final class Pipeline {

    record Assign(String name, String expr) {}

    private static final Pattern VARIABLE =
            Pattern.compile("[A-Za-z_][A-Za-z_0-9]*");

    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("\\s*(\\d+|[A-Za-z_][A-Za-z_0-9]*|[()+*/-])");

    private static final Pattern DIVISION_BY_ZERO =
            Pattern.compile("/\\s*0(?![0-9])");

    // PARSING

    List<Assign> parse(String source) {
        List<String> diagnostics = diagnose(source);

        boolean hasError = diagnostics.stream()
                .anyMatch(d -> d.startsWith("ERROR"));

        if (hasError) {
            throw new IllegalArgumentException(
                    String.join("\n", diagnostics)
            );
        }

        List<Assign> result = new ArrayList<>();

        for (String raw : source.split(";")) {
            String statement = raw.trim();

            if (statement.isEmpty()) {
                continue;
            }

            int eq = statement.indexOf('=');

            String name = statement.substring(0, eq).trim();
            String expr = statement.substring(eq + 1).trim();

            result.add(new Assign(name, expr));
        }

        return result;
    }

    // DIAGNOSTICS 

    List<String> diagnose(String source) {
        List<String> messages = new ArrayList<>();
        Set<String> defined = new HashSet<>();

        String[] lines = source.split("\\R", -1);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.isEmpty()) {
                continue;
            }

            int lineNo = i + 1;
            boolean endsWithSemicolon = line.endsWith(";");

            String[] statements = line.split(";", -1);

            for (int j = 0; j < statements.length; j++) {
                String statement = statements[j].trim();

                if (statement.isEmpty()) {
                    continue;
                }

                boolean missingSemicolon =
                        j == statements.length - 1
                                && !endsWithSemicolon;

                if (missingSemicolon) {
                    addError(
                            messages,
                            lineNo,
                            "Missing semicolon ';' at the end of the assignment."
                    );
                }

                validateStatement(
                        statement,
                        lineNo,
                        defined,
                        messages
                );
            }
        }

        if (messages.isEmpty()) {
            messages.add("OK | No syntax or semantic issues found.");
        }

        return messages;
    }

    private void validateStatement(
            String statement,
            int lineNo,
            Set<String> defined,
            List<String> messages) {

        int eq = statement.indexOf('=');

        if (eq < 1 || statement.indexOf('=', eq + 1) >= 0) {
            addError(
                    messages,
                    lineNo,
                    "Expected an assignment such as: result = 10 + 5;"
            );
            return;
        }

        String name = statement.substring(0, eq).trim();
        String expr = statement.substring(eq + 1).trim();

        if (!VARIABLE.matcher(name).matches()) {
            addError(
                    messages,
                    lineNo,
                    "Invalid variable name '" + name + "'."
            );
            return;
        }

        List<String> tokens;

        try {
            tokens = tokenize(expr);
        } catch (IllegalArgumentException ex) {
            addError(messages, lineNo, ex.getMessage());
            return;
        }

        boolean undefinedVariable = false;

        for (String token : tokens) {
            if (VARIABLE.matcher(token).matches()
                    && !defined.contains(token)) {

                addError(
                        messages,
                        lineNo,
                        "Variable '" + token
                                + "' is used before it is defined."
                );

                undefinedVariable = true;
            }
        }

        Matcher divisionMatcher = DIVISION_BY_ZERO.matcher(expr);

        if (divisionMatcher.find()) {
            messages.add(
                    "WARNING | Line " + lineNo
                            + " | Possible division by zero."
            );
        }

        // Do not define the destination when its expression
        // contains an undefined variable.
        if (!undefinedVariable) {
            defined.add(name);
        }
    }

    private void addError(
            List<String> messages,
            int lineNo,
            String message) {

        messages.add("ERROR | Line " + lineNo + " | " + message);
    }
    // TAC

    
    List<String> generateTac(List<Assign> assignments) {
        List<String> instructions = new ArrayList<>();
        int tempCounter = 1;

        for (Assign assignment : assignments) {
            List<String> tokens = tokenize(assignment.expr());
            List<String> postfixTokens = postfix(tokens);
            Deque<String> stack = new ArrayDeque<>();

            for (String token : postfixTokens) {
                if (isOperator(token)) {
                    String right = stack.pop();
                    String left = stack.pop();

                    String temp = "t" + tempCounter++;
                    instructions.add(
                        temp + " = " + left + " " + token + " " + right
                    );

                    stack.push(temp);
                } else {
                    stack.push(token);
                }
            }

            String result = stack.pop();
            instructions.add(assignment.name() + " = " + result);
        }

        return instructions;
    }



    // DISPLAY 

    String show(List<Assign> list) {
        if (list.isEmpty()) {
            return "(no instructions)";
        }

        StringJoiner result = new StringJoiner("\n");

        for (Assign assignment : list) {
            result.add(
                    assignment.name() + " = " + assignment.expr()
            );
        }

        return result.toString();
    }

    // CONSTANT FOLDING

    List<Assign> foldAll(List<Assign> input) {
        List<Assign> result = new ArrayList<>();

        for (Assign assignment : input) {
            result.add(new Assign(
                    assignment.name(),
                    simplify(assignment.expr(), Map.of(), true)
            ));
        }

        return result;
    }

    // CONSTANT PROPAGATION

    List<Assign> propagate(List<Assign> input) {
        Map<String, String> constants = new HashMap<>();
        List<Assign> result = new ArrayList<>();

        for (Assign assignment : input) {
            String expr = simplify(
                    assignment.expr(),
                    constants,
                    true
            );

            // Assignment overwrites any previous value.
            constants.remove(assignment.name());

            if (expr.matches("-?\\d+")) {
                constants.put(assignment.name(), expr);
            }

            result.add(new Assign(assignment.name(), expr));
        }

        return result;
    }

    // DEAD-CODE ELIMINATION

    List<Assign> eliminateDead(List<Assign> input) {
        if (input.isEmpty()) {
            return input;
        }

        Set<String> live = new HashSet<>();

        // The final assignment is treated as the program result.
        live.add(input.get(input.size() - 1).name());

        List<Assign> reversed = new ArrayList<>();

        for (int i = input.size() - 1; i >= 0; i--) {
            Assign assignment = input.get(i);

            if (live.remove(assignment.name())) {
                reversed.add(assignment);

                Matcher matcher =
                        VARIABLE.matcher(assignment.expr());

                while (matcher.find()) {
                    live.add(matcher.group());
                }
            }
        }

        Collections.reverse(reversed);
        return reversed;
    }

    // EXPRESSION SIMPLIFICATION

    private String simplify(
            String expr,
            Map<String, String> constants,
            boolean fold) {

        List<String> tokens = tokenize(expr);
        List<String> replaced = new ArrayList<>();

        for (String token : tokens) {
            replaced.add(
                    constants.getOrDefault(token, token)
            );
        }

        List<String> postfix = postfix(replaced);
        Deque<String> stack = new ArrayDeque<>();

        for (String token : postfix) {
            if (isOperator(token)) {
                String b = stack.pop();
                String a = stack.pop();

                if (fold
                        && a.matches("-?\\d+")
                        && b.matches("-?\\d+")) {

                    long x = Long.parseLong(a);
                    long y = Long.parseLong(b);

                    // Never evaluate division by zero.
                    if (!token.equals("/") || y != 0) {
                        long value = switch (token) {
                            case "+" -> x + y;
                            case "-" -> x - y;
                            case "*" -> x * y;
                            case "/" -> x / y;
                            default -> throw new IllegalStateException();
                        };

                        stack.push(Long.toString(value));
                        continue;
                    }
                }

                stack.push("(" + a + " " + token + " " + b + ")");
            } else {
                stack.push(token);
            }
        }

        return stack.isEmpty() ? "" : stack.pop();
    }

    // TOKENIZATION

    private List<String> tokenize(String expr) {
        List<String> tokens = new ArrayList<>();

        Matcher matcher = TOKEN_PATTERN.matcher(expr);
        int position = 0;

        while (position < expr.length()) {
            matcher.region(position, expr.length());

            if (!matcher.lookingAt()) {
                throw new IllegalArgumentException(
                        "Unexpected expression text near: "
                                + expr.substring(position)
                );
            }

            tokens.add(matcher.group(1));
            position = matcher.end();
        }

        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Empty expression.");
        }

        postfix(tokens);
        return tokens;
    }

    private boolean isOperator(String token) {
        return token.equals("+")
                || token.equals("-")
                || token.equals("*")
                || token.equals("/");
    }

    private int precedence(String operator) {
        return (operator.equals("*") || operator.equals("/"))
                ? 2 : 1;
    }

    // INFIX TO POSTFIX

    private List<String> postfix(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Deque<String> operators = new ArrayDeque<>();

        boolean expectValue = true;

        for (String token : tokens) {
            if (token.matches("\\d+")
                    || VARIABLE.matcher(token).matches()) {

                if (!expectValue) {
                    throw new IllegalArgumentException(
                            "Missing operator near '" + token + "'."
                    );
                }

                output.add(token);
                expectValue = false;

            } else if (token.equals("(")) {

                if (!expectValue) {
                    throw new IllegalArgumentException(
                            "Missing operator before '('."
                    );
                }

                operators.push(token);
                expectValue = true;

            } else if (token.equals(")")) {

                if (expectValue) {
                    throw new IllegalArgumentException(
                            "Unexpected ')'."
                    );
                }

                while (!operators.isEmpty()
                        && !operators.peek().equals("(")) {
                    output.add(operators.pop());
                }

                if (operators.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Unmatched ')'."
                    );
                }

                operators.pop();
                expectValue = false;

            } else if (isOperator(token)) {

                if (expectValue) {
                    throw new IllegalArgumentException(
                            "Unexpected operator '" + token + "'."
                    );
                }

                while (!operators.isEmpty()
                        && isOperator(operators.peek())
                        && precedence(operators.peek())
                        >= precedence(token)) {
                    output.add(operators.pop());
                }

                operators.push(token);
                expectValue = true;

            } else {
                throw new IllegalArgumentException(
                        "Unexpected token '" + token + "'."
                );
            }
        }

        if (expectValue) {
            throw new IllegalArgumentException(
                    "Expression ends unexpectedly."
            );
        }

        while (!operators.isEmpty()) {
            String operator = operators.pop();

            if (operator.equals("(")) {
                throw new IllegalArgumentException(
                        "Unmatched '('."
                );
            }

            output.add(operator);
        }

        return output;
    }
}
