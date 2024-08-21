package br.ufscar.dc.compiladores.lagerador;


import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author vinij
 */
public class LaSemantico extends LABaseVisitor {

    Escopos escopos = new Escopos(TabelaDeSimbolos.TipoLA.VOID); // Gerenciador de escopos de variáveis e funções
    // Mensagens de erro semântico
    private static final String jaDeclaradoMessage = " ja declarado anteriormente";
    private static final String duasVezesDeclaradoMessage = " declarado duas vezes num mesmo escopo";
    private static final String atribuicaoNaoCompativelMessage = "atribuicao nao compativel para ";
    private static final String comandoNaoPermitidoMessage = "comando retorne nao permitido nesse escopo";
    private static final String incompatibilidadeMessage = "incompatibilidade de parametros na chamada de ";

    // Verifica e adiciona uma constante no escopo atual
    @Override
    public Object visitDeclaracao_constante(LAParser.Declaracao_constanteContext ctx) {
        TabelaDeSimbolos escopoCorrente = escopos.obterEscopoAtual();
        if (escopoCorrente.isExiste(ctx.IDENT().getText())) {
            LaSemanticoUtils.addErroSemantico(ctx.start, "constante" + ctx.IDENT().getText() + jaDeclaradoMessage);
        } else {
            TabelaDeSimbolos.TipoLA tipo = TabelaDeSimbolos.TipoLA.INTEIRO;
            TabelaDeSimbolos.TipoLA tipoAux = LaSemanticoUtils.getTipo(ctx.tipo_basico().getText());
            if (tipoAux != null)
                tipo = tipoAux;
            escopoCorrente.adicionar(ctx.IDENT().getText(), tipo, TabelaDeSimbolos.Structure.CONST);
        }
        return super.visitDeclaracao_constante(ctx);
    }

    // Verifica e adiciona um novo tipo no escopo atual
    @Override
    public Object visitDeclaracao_tipo(LAParser.Declaracao_tipoContext ctx) {
        TabelaDeSimbolos escopoCorrente = escopos.obterEscopoAtual();
        String identificador = ctx.IDENT().getText();

        if (escopoCorrente.isExiste(identificador)) {
            LaSemanticoUtils.addErroSemantico(ctx.start, "tipo " + identificador + duasVezesDeclaradoMessage);
            return super.visitDeclaracao_tipo(ctx);
        }

        TabelaDeSimbolos.TipoLA tipoContext = LaSemanticoUtils.getTipo(ctx.tipo().getText());
        if (tipoContext != null) {
            escopoCorrente.adicionar(identificador, tipoContext, TabelaDeSimbolos.Structure.TIPO);
        } else if (ctx.tipo().registro() != null) {
            adicionarRegistro(ctx, escopoCorrente, identificador);
        }

        return super.visitDeclaracao_tipo(ctx);
    }

    // Verifica e adiciona variáveis no escopo atual
    @Override
    public Object visitDeclaracao_variavel(LAParser.Declaracao_variavelContext ctx) {
        TabelaDeSimbolos escopoCorrente = escopos.obterEscopoAtual();
        // Itera sobre cada identificador e realiza as validações necessárias
        for (LAParser.IdentificadorContext identificadorContext : ctx.variavel().identificador()) {
            String nomeIdentificador = appendTerminalNodes(identificadorContext.IDENT());

            if (escopoCorrente.isExiste(nomeIdentificador)) {
                LaSemanticoUtils.addErroSemantico(identificadorContext.start,
                        "identificador " + nomeIdentificador + jaDeclaradoMessage);
                continue;
            }

            TabelaDeSimbolos.TipoLA tipo = LaSemanticoUtils.getTipo(ctx.variavel().tipo().getText());
            if (tipo != null) {
                escopoCorrente.adicionar(nomeIdentificador, tipo, TabelaDeSimbolos.Structure.VAR);
            } else {
                TerminalNode identTipo = getIdentTipo(ctx);
                if (identTipo != null) {
                    ArrayList<TabelaDeSimbolos.EntradaTabelaDeSimbolos> regVars = buscarRegistroVars(identTipo.getText());
                    if (escopoCorrente.isExiste(nomeIdentificador)) {
                        LaSemanticoUtils.addErroSemantico(identificadorContext.start,
                                "identificador " + nomeIdentificador + jaDeclaradoMessage);
                    } else {
                        adicionarRegistro(escopoCorrente, nomeIdentificador, regVars);
                    }
                } else if (ctx.variavel().tipo().registro() != null) {
                    ArrayList<TabelaDeSimbolos.EntradaTabelaDeSimbolos> varReg = getRegistroVariaveis(ctx, escopoCorrente);
                    adicionarRegistroComVariaveis(escopoCorrente, identificadorContext, nomeIdentificador, varReg);
                } else {
                    escopoCorrente.adicionar(nomeIdentificador, TabelaDeSimbolos.TipoLA.INTEIRO, TabelaDeSimbolos.Structure.VAR);
                }
            }
        }
        return super.visitDeclaracao_variavel(ctx);
    }

    // Verifica e adiciona uma declaração global (função ou procedimento) no escopo
    @Override
    public Object visitDeclaracao_global(LAParser.Declaracao_globalContext ctx) {
        TabelaDeSimbolos escopoCorrente = escopos.obterEscopoAtual();

        if (escopoCorrente.isExiste(ctx.IDENT().getText())) {
            LaSemanticoUtils.addErroSemantico(ctx.start, ctx.IDENT().getText() + jaDeclaradoMessage);
            return super.visitDeclaracao_global(ctx);
        }

        configurarEscopoGlobal(ctx, escopoCorrente);

        if (ctx.parametros() != null) {
            processarParametros(ctx, escopoCorrente);
        }

        Object ret = super.visitDeclaracao_global(ctx);
        escopos.abandonarEscopo();

        return ret;
    }

    // Verifica se um tipo básico identificado foi declarado
    @Override
    public Object visitTipo_basico_ident(LAParser.Tipo_basico_identContext identContext) {
        if (identContext.IDENT() != null) {
            boolean exists = false;
            for (TabelaDeSimbolos tabelaDeSimbolos : escopos.percorrerEscoposAninhados()) {
                if (tabelaDeSimbolos.isExiste(identContext.IDENT().getText())) {
                    exists = true;
                }
            }
            if (!exists) {
                LaSemanticoUtils.addErroSemantico(identContext.start, "tipo " + identContext.IDENT().getText() + " nao declarado");
            }
        }
        return super.visitTipo_basico_ident(identContext);
    }

    // Verifica se um identificador foi declarado
    @Override
    public Object visitIdentificador(LAParser.IdentificadorContext ctx) {
        String nomeVar = appendTerminalNodes(ctx.IDENT());

        boolean isErro = true;
        for (TabelaDeSimbolos tabelaDeSimbolos : escopos.percorrerEscoposAninhados()) {

            if (tabelaDeSimbolos.isExiste(nomeVar)) {
                isErro = false;
            }
        }
        if (isErro)
            LaSemanticoUtils.addErroSemantico(ctx.start, "identificador " + nomeVar + " nao declarado");
        return super.visitIdentificador(ctx);
    }

    // Verifica se a atribuição é válida
    @Override
    public Object visitCmdAtribuicao(LAParser.CmdAtribuicaoContext ctx) {
        TabelaDeSimbolos.TipoLA tipoExpressao = LaSemanticoUtils.verificarTipo(escopos, ctx.expressao());
        boolean isErro = false;
        String ponteiro = ctx.getText().charAt(0) == '^' ? "^" : "";
        String nomeVar = appendTerminalNodes(ctx.identificador().IDENT());
        if (tipoExpressao != TabelaDeSimbolos.TipoLA.INVALIDO) {
            boolean found = false;
            for (TabelaDeSimbolos escopo : escopos.percorrerEscoposAninhados()) {
                if (escopo.isExiste(nomeVar) && !found) {
                    found = true;
                    TabelaDeSimbolos.TipoLA tipoVariavel = LaSemanticoUtils.verificarTipo(escopos, nomeVar);
                    Boolean varNumeric = tipoVariavel == TabelaDeSimbolos.TipoLA.REAL || tipoVariavel == TabelaDeSimbolos.TipoLA.INTEIRO;
                    Boolean expNumeric = tipoExpressao == TabelaDeSimbolos.TipoLA.REAL || tipoExpressao == TabelaDeSimbolos.TipoLA.INTEIRO;
                    if (!(varNumeric && expNumeric) && tipoVariavel != tipoExpressao) {
                        isErro = true;
                    }
                }
            }
        } else {
            isErro = true;
        }

        if (isErro) {
            nomeVar = ctx.identificador().getText();
            LaSemanticoUtils.addErroSemantico(ctx.identificador().start, atribuicaoNaoCompativelMessage + ponteiro + nomeVar);
        }

        return super.visitCmdAtribuicao(ctx);
    }

    // Verifica se o comando 'retorne' está em um escopo adequado
    @Override
    public Object visitCmdRetorne(LAParser.CmdRetorneContext ctx) {
        if (escopos.obterEscopoAtual().tipo == TabelaDeSimbolos.TipoLA.VOID) {
            LaSemanticoUtils.addErroSemantico(ctx.start, comandoNaoPermitidoMessage);
        }
        return super.visitCmdRetorne(ctx);
    }

    // Verifica a compatibilidade de parâmetros em chamadas de função/procedimento
    @Override
    public Object visitParcela_unario(LAParser.Parcela_unarioContext ctx) {
        TabelaDeSimbolos escopoCorrente = escopos.obterEscopoAtual();

        if (ctx.IDENT() != null) {
            String nomeIdentificador = ctx.IDENT().getText();

            if (escopoCorrente.isExiste(nomeIdentificador)) {
                List<TabelaDeSimbolos.EntradaTabelaDeSimbolos> parametrosEsperados = escopoCorrente.retornaTipo(nomeIdentificador);
                List<LAParser.ExpressaoContext> expressoes = ctx.expressao();

                if (verificarIncompatibilidade(parametrosEsperados, expressoes)) {
                    LaSemanticoUtils.addErroSemantico(ctx.start, incompatibilidadeMessage + nomeIdentificador);
                }
            }
        }

        return super.visitParcela_unario(ctx);
    }

    // Concatena os textos dos nós terminais em um único identificador
    private String appendTerminalNodes(List<TerminalNode> terminalNodes) {
        int i = 0;
        StringBuilder nomeVar = new StringBuilder();
        for (TerminalNode id : terminalNodes) {
            if (i++ > 0) {
                nomeVar.append(".");
            }
            nomeVar.append(id.getText());
        }
        return nomeVar.toString();
    }

    // Adiciona um novo registro ao escopo corrente
    private void adicionarRegistro(LAParser.Declaracao_tipoContext ctx, TabelaDeSimbolos escopoCorrente, String identificador) {
        ArrayList<TabelaDeSimbolos.EntradaTabelaDeSimbolos> varReg = new ArrayList<>();

        for (LAParser.VariavelContext va : ctx.tipo().registro().variavel()) {
            TabelaDeSimbolos.TipoLA tipoReg = LaSemanticoUtils.getTipo(va.tipo().getText());
            for (LAParser.IdentificadorContext identificadorContext : va.identificador()) {
                varReg.add(escopoCorrente.new EntradaTabelaDeSimbolos(identificadorContext.getText(), tipoReg, TabelaDeSimbolos.Structure.TIPO));
            }
        }

        escopoCorrente.adicionar(identificador, TabelaDeSimbolos.TipoLA.REG, TabelaDeSimbolos.Structure.TIPO);

        for (TabelaDeSimbolos.EntradaTabelaDeSimbolos entradaTS : varReg) {
            String nomeVar = identificador + '.' + entradaTS.nome;
            if (escopoCorrente.isExiste(nomeVar)) {
                LaSemanticoUtils.addErroSemantico(ctx.start, "identificador " + nomeVar + jaDeclaradoMessage);
            } else {
                escopoCorrente.adicionar(entradaTS);
                escopoCorrente.adicionar(identificador, entradaTS);
            }
        }
    }

    // Retorna o nó terminal do tipo de uma variável
    private TerminalNode getIdentTipo(LAParser.Declaracao_variavelContext ctx) {
        return ctx.variavel().tipo() != null
                && ctx.variavel().tipo().tipo_estendido() != null
                && ctx.variavel().tipo().tipo_estendido().tipo_basico_ident() != null
                && ctx.variavel().tipo().tipo_estendido().tipo_basico_ident().IDENT() != null
                ? ctx.variavel().tipo().tipo_estendido().tipo_basico_ident().IDENT()
                : null;
    }

    // Adiciona um registro e suas variáveis ao escopo corrente
    private void adicionarRegistro(TabelaDeSimbolos escopoCorrente, String nomeIdentificador,
                                   ArrayList<TabelaDeSimbolos.EntradaTabelaDeSimbolos> regVars) {
        escopoCorrente.adicionar(nomeIdentificador, TabelaDeSimbolos.TipoLA.REG, TabelaDeSimbolos.Structure.VAR);
        for (TabelaDeSimbolos.EntradaTabelaDeSimbolos s : regVars) {
            escopoCorrente.adicionar(nomeIdentificador + "." + s.nome, s.tipo, TabelaDeSimbolos.Structure.VAR);
        }
    }

    // Retorna as variáveis de um registro
    private ArrayList<TabelaDeSimbolos.EntradaTabelaDeSimbolos> getRegistroVariaveis(LAParser.Declaracao_variavelContext ctx, TabelaDeSimbolos escopoCorrente) {
        ArrayList<TabelaDeSimbolos.EntradaTabelaDeSimbolos> varReg = new ArrayList<>();
        for (LAParser.VariavelContext va : ctx.variavel().tipo().registro().variavel()) {
            TabelaDeSimbolos.TipoLA tipoReg = LaSemanticoUtils.getTipo(va.tipo().getText());
            for (LAParser.IdentificadorContext id2 : va.identificador()) {
                varReg.add(escopoCorrente.new EntradaTabelaDeSimbolos(id2.getText(), tipoReg, TabelaDeSimbolos.Structure.VAR));
            }
        }
        return varReg;
    }

    // Adiciona um registro com suas variáveis ao escopo corrente
    private void adicionarRegistroComVariaveis(TabelaDeSimbolos escopoCorrente, LAParser.IdentificadorContext identificadorContext,
                                               String nomeIdentificador, ArrayList<TabelaDeSimbolos.EntradaTabelaDeSimbolos> varReg) {
        escopoCorrente.adicionar(nomeIdentificador, TabelaDeSimbolos.TipoLA.REG, TabelaDeSimbolos.Structure.VAR);
        for (TabelaDeSimbolos.EntradaTabelaDeSimbolos re : varReg) {
            String nameVar = nomeIdentificador + '.' + re.nome;
            if (escopoCorrente.isExiste(nameVar)) {
                LaSemanticoUtils.addErroSemantico(identificadorContext.start,
                        "identificador " + nameVar + jaDeclaradoMessage);
            } else {
                escopoCorrente.adicionar(re);
                escopoCorrente.adicionar(nameVar, re.tipo, TabelaDeSimbolos.Structure.VAR);
            }
        }
    }

    // Configura o escopo para uma função ou procedimento global
    private void configurarEscopoGlobal(LAParser.Declaracao_globalContext ctx, TabelaDeSimbolos escopoCorrente) {
        TabelaDeSimbolos.TipoLA returnTypeFunc;

        if (ctx.getText().startsWith("funcao")) {
            returnTypeFunc = LaSemanticoUtils.getTipo(ctx.tipo_estendido().getText());
            escopoCorrente.adicionar(ctx.IDENT().getText(), returnTypeFunc, TabelaDeSimbolos.Structure.FUNC);
        } else {
            returnTypeFunc = TabelaDeSimbolos.TipoLA.VOID;
            escopoCorrente.adicionar(ctx.IDENT().getText(), returnTypeFunc, TabelaDeSimbolos.Structure.PROC);
        }

        escopos.criarNovoEscopo(returnTypeFunc);
    }

    // Processa os parâmetros de uma função ou procedimento global
    private void processarParametros(LAParser.Declaracao_globalContext ctx, TabelaDeSimbolos escopoAntigo) {
        TabelaDeSimbolos escopoCorrente = escopos.obterEscopoAtual();

        for (LAParser.ParametroContext p : ctx.parametros().parametro()) {
            for (LAParser.IdentificadorContext id : p.identificador()) {
                String nomeIdentificador = appendTerminalNodes(id.IDENT());

                if (escopoCorrente.isExiste(nomeIdentificador)) {
                    LaSemanticoUtils.addErroSemantico(id.start, "identificador " + nomeIdentificador + jaDeclaradoMessage);
                } else {
                    processarParametroTipo(p, escopoCorrente, escopoAntigo, nomeIdentificador, ctx.IDENT().getText());
                }
            }
        }
    }

    // Processa o tipo de um parâmetro
    private void processarParametroTipo(LAParser.ParametroContext p, TabelaDeSimbolos escopoCorrente, TabelaDeSimbolos escopoAntigo,
                                        String nomeIdentificador, String nomeFuncao) {
        TabelaDeSimbolos.TipoLA tipo = LaSemanticoUtils.getTipo(p.tipo_estendido().getText());

        if (tipo != null) {
            adicionarEntradaEscopo(escopoCorrente, escopoAntigo, nomeIdentificador, tipo, nomeFuncao);
        } else {
            TerminalNode identTipo = obterIdentTipo(p);
            if (identTipo != null) {
                processarIdentTipo(identTipo, escopoCorrente, escopoAntigo, nomeIdentificador, nomeFuncao);
            }
        }
    }

    // Retorna o nó terminal do tipo de um parâmetro
    private TerminalNode obterIdentTipo(LAParser.ParametroContext p) {
        return p.tipo_estendido().tipo_basico_ident() != null
                && p.tipo_estendido().tipo_basico_ident().IDENT() != null
                ? p.tipo_estendido().tipo_basico_ident().IDENT()
                : null;
    }

    // Processa o tipo de um identificador e adiciona as variáveis associadas ao escopo
    private void processarIdentTipo(TerminalNode identTipo, TabelaDeSimbolos escopoCorrente, TabelaDeSimbolos escopoAntigo,
                                    String nomeIdentificador, String nomeFuncao) {
        ArrayList<TabelaDeSimbolos.EntradaTabelaDeSimbolos> regVars = buscarRegistroVars(identTipo.getText());

        if (escopoCorrente.isExiste(nomeIdentificador)) {
            LaSemanticoUtils.addErroSemantico(identTipo.getSymbol(), "identificador " + nomeIdentificador + jaDeclaradoMessage);
        } else {
            adicionarEntradaEscopo(escopoCorrente, escopoAntigo, nomeIdentificador, TabelaDeSimbolos.TipoLA.REG, nomeFuncao);
            adicionarVariaveisRegistro(escopoCorrente, nomeIdentificador, regVars);
        }
    }

    // Busca as variáveis de um registro em escopos aninhados
    private ArrayList<TabelaDeSimbolos.EntradaTabelaDeSimbolos> buscarRegistroVars(String tipo) {
        for (TabelaDeSimbolos escopo : escopos.percorrerEscoposAninhados()) {
            if (escopo.isExiste(tipo)) {
                return escopo.retornaTipo(tipo);
            }
        }
        return new ArrayList<>();
    }

    // Adiciona uma entrada no escopo e as associações no escopo antigo
    private void adicionarEntradaEscopo(TabelaDeSimbolos escopoCorrente, TabelaDeSimbolos escopoAntigo,
                                        String nomeIdentificador, TabelaDeSimbolos.TipoLA tipo, String nomeFuncao) {
        TabelaDeSimbolos.EntradaTabelaDeSimbolos in = escopoCorrente.new EntradaTabelaDeSimbolos(nomeIdentificador, tipo, TabelaDeSimbolos.Structure.VAR);
        escopoCorrente.adicionar(in);
        escopoAntigo.adicionar(nomeFuncao, in);
    }

    // Adiciona as variáveis de um registro ao escopo
    private void adicionarVariaveisRegistro(TabelaDeSimbolos escopoCorrente, String nomeIdentificador,
                                            ArrayList<TabelaDeSimbolos.EntradaTabelaDeSimbolos> regVars) {
        for (TabelaDeSimbolos.EntradaTabelaDeSimbolos s : regVars) {
            escopoCorrente.adicionar(nomeIdentificador + "." + s.nome, s.tipo, TabelaDeSimbolos.Structure.VAR);
        }
    }

    // Verifica se há incompatibilidade de tipos entre parâmetros esperados e expressões fornecidas
    private boolean verificarIncompatibilidade(List<TabelaDeSimbolos.EntradaTabelaDeSimbolos> parametrosEsperados, List<LAParser.ExpressaoContext> expressoes) {
        if (parametrosEsperados.size() != expressoes.size()) {
            return true;
        }
        for (int i = 0; i < parametrosEsperados.size(); i++) {
            TabelaDeSimbolos.TipoLA tipoEsperado = parametrosEsperados.get(i).tipo;
            TabelaDeSimbolos.TipoLA tipoExpressao = LaSemanticoUtils.verificarTipo(escopos, expressoes.get(i));

            if (tipoEsperado != tipoExpressao) {
                return true;
            }
        }
        return false;
    }

}
