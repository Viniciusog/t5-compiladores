package br.ufscar.dc.compiladores.lagerador;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author vinij
 */
public class TabelaDeSimbolos {

    public TabelaDeSimbolos.TipoLA tipo;

    public enum TipoLA {
        INTEIRO,
        REAL,
        CADEIA,
        LOGICO,
        INVALIDO,
        REG,
        VOID
    }

    public enum Structure {
        VAR,
        CONST,
        PROC,
        FUNC,
        TIPO
    }

    class EntradaTabelaDeSimbolos {
        TipoLA tipo;
        String nome;
        Structure structure;

        public EntradaTabelaDeSimbolos(String nome, TipoLA tipo, Structure structure) {
            this.tipo = tipo;
            this.nome = nome;
            this.structure = structure;
        }
    }

    private final Map<String, EntradaTabelaDeSimbolos> tabela;
    private final HashMap<String, ArrayList<EntradaTabelaDeSimbolos>> tipoTabela;


    public boolean isExiste(String nome) {
        return tabela.containsKey(nome);
    }

    public TipoLA verificar(String nome) {
        if (tabela.containsKey(nome))
            return tabela.get(nome).tipo;
        return null;
    }

    public TabelaDeSimbolos(){
        tabela = new HashMap<>();
        tipoTabela = new HashMap<>();
    }

    public TabelaDeSimbolos(TabelaDeSimbolos.TipoLA tipo) {
        tabela = new HashMap<>();
        tipoTabela = new HashMap<>();
        this.tipo = tipo;
    }

    public void adicionar(String nome, TipoLA tipo, Structure structure) {
        EntradaTabelaDeSimbolos entrada = new EntradaTabelaDeSimbolos(nome, tipo, structure);
        tabela.put(nome, entrada);
    }

    public void adicionar(EntradaTabelaDeSimbolos entrada) {
        tabela.put(entrada.nome, entrada);
    }

    public void adicionar(String tipoNome, EntradaTabelaDeSimbolos entrada) {
        if (tipoTabela.containsKey(tipoNome)) {
            tipoTabela.get(tipoNome).add(entrada);
        } else {
            ArrayList<EntradaTabelaDeSimbolos> list = new ArrayList<>();
            list.add(entrada);
            tipoTabela.put(tipoNome, list);
        }
    }

    public ArrayList<EntradaTabelaDeSimbolos> retornaTipo(String nome) {
        return tipoTabela.get(nome);
    }


}