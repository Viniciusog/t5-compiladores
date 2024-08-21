package br.ufscar.dc.compiladores.lagerador;

import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Objects;

/**
 *  Essa nossa classe fica sendo responsável pro gerar o código em C 
 * chamando funções dela própria e também de outras classes de geração de códigos
 * mais específicos
 */
public class LaGerador extends LABaseVisitor<Void> {
    StringBuilder saida;
    TabelaDeSimbolos tabela;

    public LaGerador() {
        this.saida = new StringBuilder();
        this.tabela = new TabelaDeSimbolos();
    }

    @Override
    public Void visitPrograma(LAParser.ProgramaContext ctx) {
        saida.append("#include <stdio.h>\n");
        saida.append("#include <stdlib.h>\n");
        saida.append("\n");
        ctx.declaracoes().decl_local_global().forEach(this::visitDecl_local_global);
        saida.append("\n");
        saida.append("int main() {\n");
        visitCorpo(ctx.corpo());
        saida.append("return 0;\n");
        saida.append("}\n");
        return null;
    }

    @Override
    public Void visitCorpo(LAParser.CorpoContext ctx) {
        // Nossa função visitCorpo vai ter seu conteúdo incluindo dentro da parte main do código C
        for (LAParser.Declaracao_localContext dec : ctx.declaracao_local()) {
            visitDeclaracao_local(dec);
        }
        for (LAParser.CmdContext com : ctx.cmd()) {
            visitCmd(com);
        }
        return null;
    }

    @Override
    public Void visitIdentificador(LAParser.IdentificadorContext ctx) {
        saida.append(" ");
        int i = 0;
        for (TerminalNode id : ctx.IDENT()) {
            if (i++ > 0)
                saida.append(".");
            saida.append(id.getText());
        }
        visitDimensao(ctx.dimensao());
        return null;
    }

    @Override
    public Void visitDimensao(LAParser.DimensaoContext ctx) {
        // Para cada contexto de dimensão, vamos colocar o símbolo de []
        // que indica tamanho de array e o valor do tamanho vai ser gerado com base 
        // na chamda da função visitExp_aritmetica
        for (LAParser.Exp_aritmeticaContext exp : ctx.exp_aritmetica()) {
            saida.append("[");
            visitExp_aritmetica(exp);
            saida.append("]");
        }
        return null;
    }

  
    @Override
    public Void visitParametro(LAParser.ParametroContext ctx) {
        int i = 0;
        String cTipo = LaSemanticoUtils.getCType(ctx.tipo_estendido().getText().replace("^", ""));
        TabelaDeSimbolos.TipoLA tipo = LaSemanticoUtils.getTipo(ctx.tipo_estendido().getText());
        // Vamos percorrer cada um dos identificadores, colocando virgula para separar os parâmetros
        // e chamando visitação de tipo estendido e do identificador
        for (LAParser.IdentificadorContext id : ctx.identificador()) {
            if (i++ > 0)
                saida.append(",");
            visitTipo_estendido(ctx.tipo_estendido());
            visitIdentificador(id);
            if (cTipo.equals("char")) {
                saida.append("[80]");
            }
            tabela.adicionar(id.getText(), tipo, TabelaDeSimbolos.Structure.VAR);
        }
        return null;
    }

    @Override
    public Void visitVariavel(LAParser.VariavelContext ctx) {
        // Vamos gerar o código em C para as variáveis, incluindo array, char, registro e outros valores
        
        String cTipo = LaSemanticoUtils.getCType(ctx.tipo().getText().replace("^", ""));
        TabelaDeSimbolos.TipoLA tipo = LaSemanticoUtils.getTipo(ctx.tipo().getText());
        
        for (LAParser.IdentificadorContext id : ctx.identificador()) {
            if (ctx.tipo().getText().contains("registro")) {
                adicionarCamposRegistro(id, ctx.tipo().registro());
            } else if (cTipo == null && tipo == null) {
                adicionarCamposCustomizados(id, ctx.tipo().getText());
            } else if (id.getText().contains("[")) {
                adicionarCamposArray(id, tipo);
            } else {
                tabela.adicionar(id.getText(), tipo, TabelaDeSimbolos.Structure.VAR);
            }

            visitTipo(ctx.tipo());
            visitIdentificador(id);

            // Caso especial de texto, é um array de caracteres em C
            if (Objects.equals(cTipo, "char")) {
                saida.append("[80]");
            }

            saida.append(";\n");
        }
        return null;
    }

    @Override
    public Void visitTipo(LAParser.TipoContext ctx) {
        String cTipo = LaSemanticoUtils.getCType(ctx.getText().replace("^", ""));
        boolean pointer = ctx.getText().contains("^");
        if (cTipo != null) {
            saida.append(cTipo);
        } else if (ctx.registro() != null) {
            visitRegistro(ctx.registro());
        } else {
            visitTipo_estendido(ctx.tipo_estendido());
        }
        // Caso especial, onde os ponteiros em C tem o *
        if (pointer)
            saida.append("*");
        saida.append(" ");
        return null;
    }

    @Override
    public Void visitTipo_estendido(LAParser.Tipo_estendidoContext ctx) {
        visitTipo_basico_ident(ctx.tipo_basico_ident());
        if (ctx.getText().contains("^"))
            saida.append("*");
        return null;
    }

    @Override
    public Void visitTipo_basico_ident(LAParser.Tipo_basico_identContext ctx) {
        // Gera o tipo básico ou identificador
        if (ctx.IDENT() != null) {
            saida.append(ctx.IDENT().getText());
        } else {
            saida.append(LaSemanticoUtils.getCType(ctx.getText().replace("^", "")));
        }
        return null;
    }

    @Override
    public Void visitRegistro(LAParser.RegistroContext ctx) {
        // Gerando a definição de um registro (struct)
        saida.append("struct {\n");
        ctx.variavel().forEach(this::visitVariavel);
        saida.append("} ");
        return null;
    }

    @Override
    public Void visitValor_constante(LAParser.Valor_constanteContext ctx) {
        // Adicionamos o valor constante (verdadeiro, falso ou literal)
        if (ctx.getText().equals("verdadeiro")) {
            saida.append("true");
        } else if (ctx.getText().equals("falso")) {
            saida.append("false");
        } else {
            saida.append(ctx.getText());
        }
        return null;
    }

    @Override
    public Void visitExpressao(LAParser.ExpressaoContext ctx) {
        // Geramos a expressão lógica (OR)
        if (ctx.termo_logico() != null) {
            visitTermo_logico(ctx.termo_logico(0));
            for (int i = 1; i < ctx.termo_logico().size(); i++) {
                LAParser.Termo_logicoContext termo = ctx.termo_logico(i);
                saida.append(" || ");
                visitTermo_logico(termo);
            }
        }
        return null;
    }

    @Override
    public Void visitTermo_logico(LAParser.Termo_logicoContext ctx) {
        // Caso do termo lógico (AND)
        visitFator_logico(ctx.fator_logico(0));
        for (int i = 1; i < ctx.fator_logico().size(); i++) {
            LAParser.Fator_logicoContext fator = ctx.fator_logico(i);
            saida.append(" && ");
            visitFator_logico(fator);
        }
        return null;
    }

    @Override
    public Void visitFator_logico(LAParser.Fator_logicoContext ctx) {
        // Esse é o caso do fator lógico (NOT)
        if (ctx.getText().startsWith("nao")) {
            saida.append("!");
        }
        visitParcela_logica(ctx.parcela_logica());
        return null;
    }

    @Override
    public Void visitParcela_logica(LAParser.Parcela_logicaContext ctx) {
        // Geramos a parcela lógica, que pode ser uma expressão relacional ou literal
        if (ctx.exp_relacional() != null) {
            visitExp_relacional(ctx.exp_relacional());
        } else {
            if (ctx.getText().equals("verdadeiro")) {
                saida.append("true");
            } else {
                saida.append("false");
            }
        }
        return null;
    }

    @Override
    public Void visitExp_relacional(LAParser.Exp_relacionalContext ctx) {
        // Geramos a expressão relacional (comparação)
        visitExp_aritmetica(ctx.exp_aritmetica(0));
        for (int i = 1; i < ctx.exp_aritmetica().size(); i++) {
            LAParser.Exp_aritmeticaContext termo = ctx.exp_aritmetica(i);
            if (ctx.op_relacional().getText().equals("=")) {
                saida.append(" == ");
            } else {
                saida.append(ctx.op_relacional().getText());
            }
            visitExp_aritmetica(termo);
        }
        return null;
    }

    @Override
    public Void visitExp_aritmetica(LAParser.Exp_aritmeticaContext ctx) {
        // processando expressão aritmética
        visitTermo(ctx.termo(0));
        for (int i = 1; i < ctx.termo().size(); i++) {
            LAParser.TermoContext termo = ctx.termo(i);
            saida.append(ctx.op1(i - 1).getText());
            visitTermo(termo);
        }
        return null;
    }

    @Override
    public Void visitTermo(LAParser.TermoContext ctx) {
        // gerando código para os termos
        visitFator(ctx.fator(0));
        for (int i = 1; i < ctx.fator().size(); i++) {
            LAParser.FatorContext fator = ctx.fator(i);
            // adicionando o operador do termo
            saida.append(ctx.op2(i - 1).getText());
            visitFator(fator);
        }
        return null;
    }

    @Override
    public Void visitFator(LAParser.FatorContext ctx) {
        // processando o fator (operações mais básicas como números e variáveis)
        // visitamos a primeira parcela
        visitParcela(ctx.parcela(0));
        // visitamos as parcelas seguintes
        for (int i = 1; i < ctx.parcela().size(); i++) {
            LAParser.ParcelaContext parcela = ctx.parcela(i);
            saida.append(ctx.op3(i - 1).getText());
            visitParcela(parcela);
        }
        return null;
    }

    @Override
    public Void visitParcela(LAParser.ParcelaContext ctx) {
        // vai processar a parcela (pode ser uma variável, constante, ou chamada de função)
        if (ctx.parcela_unario() != null) {
            if (ctx.op_unario() != null) {
                saida.append(ctx.op_unario().getText());
            }
            visitParcela_unario(ctx.parcela_unario());
        } else {
            visitParcela_nao_unario(ctx.parcela_nao_unario());
        }
        return null;
    }

    @Override
    public Void visitParcela_unario(LAParser.Parcela_unarioContext ctx) {
        if (ctx.IDENT() != null) {
            saida.append(ctx.IDENT().getText());
            // início da lista de argumentos
            saida.append("(");
            // Visita cada expressão e organiza elas separando por virgula
            for (int i = 0; i < ctx.expressao().size(); i++) {
                visitExpressao(ctx.expressao(i));
                if (i < ctx.expressao().size() - 1) {
                    saida.append(", ");
                }
            }
            // fim da lista de argumentos (está fechando os argumentos)
            saida.append(")");
        } else if (ctx.parentesis_expressao() != null) {
            saida.append("(");
            visitExpressao(ctx.parentesis_expressao().expressao());
            saida.append(")");
        } else {
            saida.append(ctx.getText());
        }
        return null;
    }

    @Override
    public Void visitParcela_nao_unario(LAParser.Parcela_nao_unarioContext ctx) {
        saida.append(ctx.getText());
        return null;
    }

    private void adicionarCamposRegistro(LAParser.IdentificadorContext id, LAParser.RegistroContext registro) {
        // Para cada identificador para cada variável do registro, vamos adicionar na tabela de símbolos
        for (LAParser.VariavelContext sub : registro.variavel()) {
            for (LAParser.IdentificadorContext idIns : sub.identificador()) {
                TabelaDeSimbolos.TipoLA tipoIns = LaSemanticoUtils.getTipo(sub.tipo().getText());
                tabela.adicionar(id.getText() + "." + idIns.getText(), tipoIns, TabelaDeSimbolos.Structure.VAR);
            }
        }
    }

    private void adicionarCamposCustomizados(LAParser.IdentificadorContext id, String tipoTexto) {
        ArrayList<TabelaDeSimbolos.EntradaTabelaDeSimbolos> arg = tabela.retornaTipo(tipoTexto);
        if (arg != null) {
            for (TabelaDeSimbolos.EntradaTabelaDeSimbolos val : arg) {
                tabela.adicionar(id.getText() + "." + val.nome, val.tipo, TabelaDeSimbolos.Structure.VAR);
            }
        }
    }

    private void adicionarCamposArray(LAParser.IdentificadorContext id, TabelaDeSimbolos.TipoLA tipo) {
        // Vamos adicionar os campos do array na tabela de símbolos
        int ini = id.getText().indexOf("[");
        int end = id.getText().indexOf("]");
        
        // pega o tamanho do array
        String tam = (end - ini == 2) ? String.valueOf(id.getText().charAt(ini + 1))
                : id.getText().substring(ini + 1, end);
        // nome do array
        String nome = id.IDENT().get(0).getText();
        for (int i = 0; i < Integer.parseInt(tam); i++) {
            tabela.adicionar(nome + "[" + i + "]", tipo, TabelaDeSimbolos.Structure.VAR);
        }
    }

}
