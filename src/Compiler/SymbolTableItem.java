package Compiler;

public class SymbolTableItem {
    int line;
    String value;


    public SymbolTableItem(int line, String value) {
        this.line = line;
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
