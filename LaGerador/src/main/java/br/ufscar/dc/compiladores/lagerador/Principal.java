package br.ufscar.dc.compiladores.lagerador;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.PrintWriter;

public class Principal {

    public static void main(String args[]) {
        try (PrintWriter pw = new PrintWriter(args[1])) {
            CharStream c = CharStreams.fromFileName(args[0]);
            LALexer lex = new LALexer(c);
            CommonTokenStream cs = new CommonTokenStream(lex);
            LAParser parser = new LAParser(cs);
            LAParser.ProgramaContext arvore = parser.programa();
            LaSemantico as = new LaSemantico();
            as.visitPrograma(arvore);
            for (String error : LaSemanticoUtils.errosSemanticos) {
                pw.println(error);
            }
            if (LaSemanticoUtils.errosSemanticos.isEmpty()) {
                LaGerador agc = new LaGeradorSelecao();
                agc.visitPrograma(arvore);
                try (PrintWriter pw2 = new PrintWriter(args[1])) {
                    pw2.print(agc.saida.toString());
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}