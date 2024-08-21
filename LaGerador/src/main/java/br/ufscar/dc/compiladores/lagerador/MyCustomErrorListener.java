package br.ufscar.dc.compiladores.lagerador;

import java.io.PrintWriter;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;

import java.util.BitSet;


public class MyCustomErrorListener implements ANTLRErrorListener {
    PrintWriter pw;

    static boolean isErrorFound = false;

    public MyCustomErrorListener(PrintWriter pw) {
        this.pw = pw;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> arg0, Object arg1, int arg2, int arg3, String arg4, RecognitionException arg5) {

        Token t = (Token) arg1;
        String text = t.getText();
        text = (text.equals("<EOF>")) ? "EOF" : text;

        String aType = LALexer.VOCABULARY.getDisplayName(t.getType());
        if (!isErrorFound) {
            MyCustomErrorListener.isErrorFound = true;

            if (aType == "Nao_Fechado") {
                pw.println("Linha " + t.getLine() + ": " + "comentario nao fechado");
            } else if (aType == "Literal_Nao_Fechada") {
                pw.println("Linha " + t.getLine() + ": " + "cadeia literal nao fechada");
            } else if (aType == "ERR") {
                pw.println("Linha " + t.getLine() + ": " + text + " - simbolo nao identificado");
            } else {
                pw.println("Linha " + arg2 + ": erro sintatico proximo a " + text);
            }
        }
    }

    @Override
    public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact, BitSet ambigAlts, ATNConfigSet configs) {
    }

    @Override
    public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex, BitSet conflictingAlts, ATNConfigSet configs) {

    }

    @Override
    public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs) {

    }
}