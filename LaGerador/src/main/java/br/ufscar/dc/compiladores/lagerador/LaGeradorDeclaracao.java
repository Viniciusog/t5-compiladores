package br.ufscar.dc.compiladores.lagerador;

import java.util.Objects;

// Fica sendo responsável pela geração do código C das partes de declaração
public class LaGeradorDeclaracao extends LaGerador {

    @Override
    public Void visitDecl_local_global(LAParser.Decl_local_globalContext ctx) {
        if (ctx.declaracao_local() != null) {
            visitDeclaracao_local(ctx.declaracao_local());
        } else if (ctx.declaracao_global() != null) {
            visitDeclaracao_global(ctx.declaracao_global());
        }
        return null;
    }

    @Override
    public Void visitDeclaracao_global(LAParser.Declaracao_globalContext ctx) {
        // vamos gerar código para declarações globais, que podem ser funções/procedimentos ou variáveis
        if (ctx.getText().contains("procedimento")) {
            // no caso de procedimento consideramos o tipo como 'void'
            saida.append("void ").append(ctx.IDENT().getText()).append("(");
        } else {
            // nos casos de variáveis globais ou funções, vamos determinar o tipo e adicionar na tabela de simbolos
            String cTipo = LaSemanticoUtils.getCType(ctx.tipo_estendido().getText().replace("^", ""));
            TabelaDeSimbolos.TipoLA tipo = LaSemanticoUtils.getTipo(ctx.tipo_estendido().getText());
            visitTipo_estendido(ctx.tipo_estendido());
            if (Objects.equals(cTipo, "char")) {
                saida.append("[80]");
            }
            saida.append(" ").append(ctx.IDENT().getText()).append("(");
            tabela.adicionar(ctx.IDENT().getText(), tipo, TabelaDeSimbolos.Structure.FUNC);
        }
        ctx.parametros().parametro().forEach(this::visitParametro);
        saida.append("){\n");
        ctx.declaracao_local().forEach(this::visitDeclaracao_local);
        ctx.cmd().forEach(this::visitCmd);
        saida.append("}\n");
        return null;
    }

    @Override
    public Void visitDeclaracao_local(LAParser.Declaracao_localContext ctx) {
        if (ctx.declaracao_variavel() != null) {
            visitDeclaracao_variavel(ctx.declaracao_variavel());
        }
        if (ctx.declaracao_constante() != null) {
            visitDeclaracao_constante(ctx.declaracao_constante());
        } else if (ctx.declaracao_tipo() != null) {
            visitDeclaracao_tipo(ctx.declaracao_tipo());
        }
        return null;
    }

    @Override
    public Void visitDeclaracao_tipo(LAParser.Declaracao_tipoContext ctx) {
        saida.append("typedef ");
        TabelaDeSimbolos.TipoLA tipo = LaSemanticoUtils.getTipo(ctx.tipo().getText());

        if (ctx.tipo().getText().contains("registro")) {
            for (LAParser.VariavelContext sub : ctx.tipo().registro().variavel()) {
                for (LAParser.IdentificadorContext idIns : sub.identificador()) {
                    TabelaDeSimbolos.TipoLA tipoIns = LaSemanticoUtils.getTipo(sub.tipo().getText());
                    tabela.adicionar(ctx.IDENT().getText() + "." + idIns.getText(), tipoIns,
                            TabelaDeSimbolos.Structure.VAR);
                    tabela.adicionar(ctx.IDENT().getText(), tabela.new EntradaTabelaDeSimbolos(idIns.getText(), tipoIns,
                            TabelaDeSimbolos.Structure.TIPO));
                }
            }
        }
        tabela.adicionar(ctx.IDENT().getText(), tipo, TabelaDeSimbolos.Structure.VAR);
        visitTipo(ctx.tipo());
        saida.append(ctx.IDENT()).append(";\n");
        return null;
    }

    @Override
    public Void visitDeclaracao_variavel(LAParser.Declaracao_variavelContext ctx) {
        visitVariavel(ctx.variavel());
        return null;
    }

    @Override
    public Void visitDeclaracao_constante(LAParser.Declaracao_constanteContext ctx) {
        String type = LaSemanticoUtils.getCType(ctx.tipo_basico().getText());
        TabelaDeSimbolos.TipoLA typeVar = LaSemanticoUtils.getTipo(ctx.tipo_basico().getText());
        tabela.adicionar(ctx.IDENT().getText(), typeVar, TabelaDeSimbolos.Structure.VAR);
        saida.append("const ").append(type).append(" ").append(ctx.IDENT().getText()).append(" = ");
        visitValor_constante(ctx.valor_constante());
        saida.append(";\n");
        return null;
    }
}
