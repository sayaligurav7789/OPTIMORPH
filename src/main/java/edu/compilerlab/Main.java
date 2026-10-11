
package edu.compilerlab;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("--cli")) {
            System.out.println("For CLI mode, use your existing CLI implementation.");
            return;
        }

        CompilerLabUI.launch();
    }
}
