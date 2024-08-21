package br.ufscar.dc.compiladores.lagerador;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;

/**
 * @author vinij
 */
public class LaSemanticoUtils {
    public static final List<String> errosSemanticos = new ArrayList<>(); // Lista para armazenar erros semânticos

    // Adiciona um erro semântico à lista com a linha e mensagem apropriada
    public static void addErroSemantico(Token token, String mensagem) {
        int linha = token.getLine();
        errosSemanticos.add(String.format("Linha %d: %s", linha, mensagem));
    }

    // Verifica o tipo de uma expressão lógica
    public static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.ExpressaoContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;
        for (LAParser.Termo_logicoContext termoLogicoContext : ctx.termo_logico()) {
            TabelaDeSimbolos.TipoLA tipoAux = verificarTipo(escopos, termoLogicoContext);
            ret = atribuirTipoLA(ret, tipoAux);
        }
        return ret;
    }

    // Verifica o tipo de um termo lógico
    private static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.Termo_logicoContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;

        for (LAParser.Fator_logicoContext ta : ctx.fator_logico()) {
            TabelaDeSimbolos.TipoLA tipoAux = verificarTipo(escopos, ta);
            ret = atribuirTipoLA(ret, tipoAux);
        }

        return ret;
    }

    // Verifica o tipo de um fator lógico
    private static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.Fator_logicoContext ctx) {
        return verificarTipo(escopos, ctx.parcela_logica());
    }

    // Verifica o tipo de uma parcela lógica
    private static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.Parcela_logicaContext ctx) {
        TabelaDeSimbolos.TipoLA ret;

        if (ctx.exp_relacional() != null) {
            ret = verificarTipo(escopos, ctx.exp_relacional());
        } else {
            ret = TabelaDeSimbolos.TipoLA.LOGICO;
        }

        return ret;
    }

    // Verifica o tipo de uma expressão relacional
    private static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.Exp_relacionalContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;

        if (ctx.op_relacional() != null) {
            for (LAParser.Exp_aritmeticaContext ta : ctx.exp_aritmetica()) {
                TabelaDeSimbolos.TipoLA tipoAux = verificarTipo(escopos, ta);
                ret = atribuirTipoLAAuxNumerico(ret, tipoAux);
            }
            if (ret != TabelaDeSimbolos.TipoLA.INVALIDO) {
                ret = TabelaDeSimbolos.TipoLA.LOGICO;
            }
        } else {
            ret = verificarTipo(escopos, ctx.exp_aritmetica(0));
        }

        return ret;
    }

    // Verifica o tipo de uma expressão aritmética
    private static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.Exp_aritmeticaContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;
        for (LAParser.TermoContext ta : ctx.termo()) {
            TabelaDeSimbolos.TipoLA tipoAux = verificarTipo(escopos, ta);
            ret = atribuirTipoLA(ret, tipoAux);
        }
        return ret;
    }

    // Verifica o tipo de um termo
    private static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.TermoContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;
        for (LAParser.FatorContext fa : ctx.fator()) {
            TabelaDeSimbolos.TipoLA tipoAux = verificarTipo(escopos, fa);
            ret = atribuirTipoLAAuxNumerico(ret, tipoAux);
        }
        return ret;
    }

    // Atribui o tipo de retorno com base em comparações de tipos
    private static TabelaDeSimbolos.TipoLA atribuirTipoLA(TabelaDeSimbolos.TipoLA ret, TabelaDeSimbolos.TipoLA tipoAux) {
        TabelaDeSimbolos.TipoLA result = ret;
        if (ret == null) {
            result = tipoAux;
        } else if (ret != tipoAux && tipoAux != TabelaDeSimbolos.TipoLA.INVALIDO) {
            result = TabelaDeSimbolos.TipoLA.INVALIDO;
        }
        return result;
    }

    // Atribui tipo com verificação adicional para tipos numéricos
    private static TabelaDeSimbolos.TipoLA atribuirTipoLAAuxNumerico(
            TabelaDeSimbolos.TipoLA ret,
            TabelaDeSimbolos.TipoLA tipoAux) {

        TabelaDeSimbolos.TipoLA result = ret;
        Boolean auxNumeric = tipoAux == TabelaDeSimbolos.TipoLA.REAL || tipoAux == TabelaDeSimbolos.TipoLA.INTEIRO;
        Boolean retNumeric = ret == TabelaDeSimbolos.TipoLA.REAL || ret == TabelaDeSimbolos.TipoLA.INTEIRO;
        if (ret == null) {
            result = tipoAux;
        } else if (!(auxNumeric && retNumeric) && tipoAux != ret) {
            result = TabelaDeSimbolos.TipoLA.INVALIDO;
        }
        return result;
    }

    // Verifica o tipo de um fator
    private static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.FatorContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;

        for (LAParser.ParcelaContext fa : ctx.parcela()) {
            TabelaDeSimbolos.TipoLA tipoAux = verificarTipo(escopos, fa);
            ret = atribuirTipoLA(ret, tipoAux);
        }
        return ret;
    }

    // Verifica o tipo de uma parcela
    private static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.ParcelaContext ctx) {
        TabelaDeSimbolos.TipoLA ret;
        if (ctx.parcela_nao_unario() != null) {
            ret = verificarTipo(escopos, ctx.parcela_nao_unario());
        } else {
            ret = verificarTipo(escopos, ctx.parcela_unario());
        }
        return ret;
    }

    // Verifica o tipo de uma parcela não unária
    private static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.Parcela_nao_unarioContext ctx) {
        if (ctx.identificador() != null) {
            return verificarTipo(escopos, ctx.identificador());
        }
        return TabelaDeSimbolos.TipoLA.CADEIA;
    }

    // Verifica o tipo de um identificador
    private static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.IdentificadorContext ctx) {
        StringBuilder nomeVar = new StringBuilder();
        TabelaDeSimbolos.TipoLA ret = TabelaDeSimbolos.TipoLA.INVALIDO;

        for (int i = 0; i < ctx.IDENT().size(); i++) {
            nomeVar.append(ctx.IDENT(i).getText());
            if (i != ctx.IDENT().size() - 1) {
                nomeVar.append(".");
            }
        }

        for (TabelaDeSimbolos tabelaSimbolos : escopos.percorrerEscoposAninhados()) {
            if (tabelaSimbolos.isExiste(nomeVar.toString())) {
                ret = verificarTipo(escopos, nomeVar.toString());
            }
        }
        return ret;
    }

    // Verifica o tipo de uma parcela unária
    public static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.Parcela_unarioContext ctx) {
        if (ctx.NUM_INT() != null) {
            return TabelaDeSimbolos.TipoLA.INTEIRO;
        }
        if (ctx.NUM_REAL() != null) {
            return TabelaDeSimbolos.TipoLA.REAL;
        }
        if (ctx.identificador() != null) {
            return verificarTipo(escopos, ctx.identificador());
        }
        if (ctx.IDENT() != null) {
            return verificarTipo(escopos, ctx.IDENT().getText());
        } else {
            TabelaDeSimbolos.TipoLA ret = null;
            for (LAParser.ExpressaoContext fa : ctx.expressao()) {
                TabelaDeSimbolos.TipoLA tipoAux = verificarTipo(escopos, fa);
                ret = atribuirTipoLA(ret, tipoAux);
            }
            return ret;
        }
    }

    // Verifica o tipo de uma variável pelo nome
    public static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, String nomeVar) {
        TabelaDeSimbolos.TipoLA type = TabelaDeSimbolos.TipoLA.INVALIDO;
        for (TabelaDeSimbolos tabelaSimbolos : escopos.percorrerEscoposAninhados()) {
            if (tabelaSimbolos.isExiste(nomeVar)) {
                return tabelaSimbolos.verificar(nomeVar);
            }
        }

        return type;
    }

    // Retorna o tipo com base em uma string de valor
    public static TabelaDeSimbolos.TipoLA getTipo(String val) {
        TabelaDeSimbolos.TipoLA tipo = null;
        switch (val) {
            case "literal":
                tipo = TabelaDeSimbolos.TipoLA.CADEIA;
                break;
            case "inteiro":
                tipo = TabelaDeSimbolos.TipoLA.INTEIRO;
                break;
            case "real":
                tipo = TabelaDeSimbolos.TipoLA.REAL;
                break;
            case "logico":
                tipo = TabelaDeSimbolos.TipoLA.LOGICO;
                break;
            default:
                break;
        }
        return tipo;
    }

    public static String getCTypeSymbol(TabelaDeSimbolos.TipoLA tipo) {
        String type = null;
        switch (tipo) {
            case CADEIA:
                type = "s";
                break;
            case INTEIRO:
                type = "d";
                break;
            case REAL:
                type = "f";
                break;
            default:
                break;
        }
        return type;
    }

    public static String getCType(String val) {
        String tipo = null;
        switch (val) {
            case "literal":
                tipo = "char";
                break;
            case "inteiro":
                tipo = "int";
                break;
            case "real":
                tipo = "float";
                break;
            default:
                break;
        }
        return tipo;
    }


}
