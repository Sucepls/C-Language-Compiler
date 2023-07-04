package Compiler;

import java.util.LinkedHashMap;
import java.util.Map;

public class Scope {
    private final LinkedHashMap<String, SymbolTableItem> symbol_table;
    private final String name;
    private final int index;
    private final int scope_number;
    private final Type scope_type;
    private Scope parent;

    public Scope(String name, int scope_number, Type scope_type, int index) {
        this.name = name;
        this.scope_number = scope_number;
        this.scope_type = scope_type;
        this.index = index;
        this.symbol_table = new LinkedHashMap<>();
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public int getScope_number() {
        return scope_number;
    }

    public Type getScope_type() {
        return scope_type;
    }

    public Scope getParent() {
        return parent;
    }

    public void setParent(Scope parent) {
        this.parent = parent;
    }

    public void insert(String Key, SymbolTableItem Value) {
        symbol_table.put(Key, Value);
    }
    public SymbolTableItem lookup(String Key) {
        return symbol_table.get(Key);
    }

    public LinkedHashMap<String, SymbolTableItem> getSymbol_table() {
        return symbol_table;
    }

    @Override
    public String toString() {
        return "------------- " + this.name + " : " + this.scope_number + " -------------\n" +
                this.printItems() +
                "-----------------------------------------\n";
    }

    public String printItems() {
        StringBuilder itemsStr = new StringBuilder();
        for (Map.Entry<String, SymbolTableItem> entry : this.symbol_table.entrySet()
        ) {
            itemsStr.append("Key = ").append(entry.getKey()).append(" | Value = ").append(entry.getValue()).append("\n");
        }
        return itemsStr.toString();
    }
}
