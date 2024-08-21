package br.ufscar.dc.compiladores.lagerador;

public class LaGeradorCmd extends LaGeradorDeclaracao {
    @Override
    public Void visitCmd(LAParser.CmdContext ctx) {
        if (ctx.cmdLeia() != null) return visitCmdLeia(ctx.cmdLeia());
        if (ctx.cmdEscreva() != null) return visitCmdEscreva(ctx.cmdEscreva());
        if (ctx.cmdAtribuicao() != null) return visitCmdAtribuicao(ctx.cmdAtribuicao());
        if (ctx.cmdSe() != null) return visitCmdSe(ctx.cmdSe());
        if (ctx.cmdCaso() != null) return visitCmdCaso(ctx.cmdCaso());
        if (ctx.cmdPara() != null) return visitCmdPara(ctx.cmdPara());
        if (ctx.cmdEnquanto() != null) return visitCmdEnquanto(ctx.cmdEnquanto());
        if (ctx.cmdFaca() != null) return visitCmdFaca(ctx.cmdFaca());
        if (ctx.cmdChamada() != null) return visitCmdChamada(ctx.cmdChamada());
        if (ctx.cmdRetorne() != null) return visitCmdRetorne(ctx.cmdRetorne());
        return null;
    }

    @Override
    public Void visitCmdRetorne(LAParser.CmdRetorneContext ctx) {
        saida.append("return ");
        visitExpressao(ctx.expressao());
        saida.append(";\n");
        return null;
    }

    @Override
    public Void visitCmdChamada(LAParser.CmdChamadaContext ctx) {
        saida.append(ctx.IDENT().getText()).append("(");
        int i = 0;
        for (LAParser.ExpressaoContext exp : ctx.expressao()) {
            if (i++ > 0)
                saida.append(",");
            visitExpressao(exp);
        }
        saida.append(");\n");
        return null;
    }

    @Override
    public Void visitCmdLeia(LAParser.CmdLeiaContext ctx) {
        for (LAParser.IdentificadorContext id : ctx.identificador()) {
            TabelaDeSimbolos.TipoLA idType = tabela.verificar(id.getText());
            if (idType != TabelaDeSimbolos.TipoLA.CADEIA) {
                saida.append("scanf(\"%").append(LaSemanticoUtils.getCTypeSymbol(idType)).append("\", &");
                saida.append(id.getText()).append(");\n");
            } else {
                saida.append("gets(");
                visitIdentificador(id);
                saida.append(");\n");
            }
        }
        return null;
    }

    @Override
    public Void visitCmdEscreva(LAParser.CmdEscrevaContext ctx) {
        for (LAParser.ExpressaoContext exp : ctx.expressao()) {
            Escopos escopo = new Escopos(tabela);
            String cType = LaSemanticoUtils.getCTypeSymbol(LaSemanticoUtils.verificarTipo(escopo, exp));
            if (tabela.isExiste(exp.getText())) {
                TabelaDeSimbolos.TipoLA tip = tabela.verificar(exp.getText());
                cType = LaSemanticoUtils.getCTypeSymbol(tip);
            }
            saida.append("printf(\"%").append(cType).append("\", ");
            saida.append(exp.getText());
            saida.append(");\n");
        }
        return null;
    }

    @Override
    public Void visitCmdAtribuicao(LAParser.CmdAtribuicaoContext ctx) {
        if (ctx.getText().contains("^"))
            saida.append("*");
        try {
            TabelaDeSimbolos.TipoLA tip = tabela.verificar(ctx.identificador().getText());
            if (tip == TabelaDeSimbolos.TipoLA.CADEIA) {
                saida.append("strcpy(");
                visitIdentificador(ctx.identificador());
                saida.append(",").append(ctx.expressao().getText()).append(");\n");
            } else {
                visitIdentificador(ctx.identificador());
                saida.append(" = ").append(ctx.expressao().getText()).append(";\n");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    @Override
    public Void visitCmdSe(LAParser.CmdSeContext ctx) {
        saida.append("if(");
        visitExpressao(ctx.expressao());
        saida.append(") {\n");
        for (LAParser.CmdContext cmd : ctx.cmd()) {
            visitCmd(cmd);
        }
        saida.append("}\n");
        if (ctx.cmdSenao() != null) {
            saida.append("else {\n");
            for (LAParser.CmdContext cmd : ctx.cmdSenao().cmd()) {
                visitCmd(cmd);
            }
            saida.append("}\n");
        }
        return null;
    }

    @Override
    public Void visitCmdSenao(LAParser.CmdSenaoContext ctx) {
        saida.append("default:\n");
        ctx.cmd().forEach(this::visitCmd);
        saida.append("break;\n");
        return null;
    }

    @Override
    public Void visitCmdPara(LAParser.CmdParaContext ctx) {
        String id = ctx.IDENT().getText();
        saida.append("for(").append(id).append(" = ");
        visitExp_aritmetica(ctx.exp_aritmetica(0));
        saida.append("; ").append(id).append(" <= ");
        visitExp_aritmetica(ctx.exp_aritmetica(1));
        saida.append("; ").append(id).append("++){\n");
        ctx.cmd().forEach(this::visitCmd);
        saida.append("}\n");
        return null;
    }

    @Override
    public Void visitCmdEnquanto(LAParser.CmdEnquantoContext ctx) {
        saida.append("while(");
        visitExpressao(ctx.expressao());
        saida.append("){\n");
        ctx.cmd().forEach(this::visitCmd);
        saida.append("}\n");
        return null;
    }

    @Override
    public Void visitCmdFaca(LAParser.CmdFacaContext ctx) {
        saida.append("do{\n");
        ctx.cmd().forEach(this::visitCmd);
        saida.append("} while(");
        visitExpressao(ctx.expressao());
        saida.append(");\n");
        return null;
    }

    @Override
    public Void visitCmdCaso(LAParser.CmdCasoContext ctx) {
        saida.append("switch(");
        visit(ctx.exp_aritmetica());
        saida.append("){\n");
        visit(ctx.selecao());
        if (ctx.cmdSenao() != null) {
            visit(ctx.cmdSenao());
        }
        saida.append("}\n");
        return null;
    }
}
