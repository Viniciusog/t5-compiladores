package br.ufscar.dc.compiladores.lagerador;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author vinij
 */
public class Escopos {

    private final LinkedList<TabelaDeSimbolos> pilhaDeTabelas;

    public Escopos(TabelaDeSimbolos.TipoLA tipo) {
        pilhaDeTabelas = new LinkedList<>();
        criarNovoEscopo(tipo);
    }

    public Escopos(TabelaDeSimbolos tabelaDeSimbolos){
        pilhaDeTabelas = new LinkedList<>();
        pilhaDeTabelas.push(tabelaDeSimbolos);
    }

    public void criarNovoEscopo(TabelaDeSimbolos.TipoLA tipo) {
        pilhaDeTabelas.push(new TabelaDeSimbolos(tipo));
    }

    public TabelaDeSimbolos obterEscopoAtual() {
        return pilhaDeTabelas.peek();
    }

    public List<TabelaDeSimbolos> percorrerEscoposAninhados() {
        return pilhaDeTabelas;
    }

    public void abandonarEscopo() {
        pilhaDeTabelas.pop();
    }
}