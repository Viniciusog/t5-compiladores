package br.ufscar.dc.compiladores.lagerador;

import java.util.ArrayList;
import java.util.Arrays;

public class LaGeradorSelecao extends LaGeradorCmd{

    @Override
    public Void visitSelecao(LAParser.SelecaoContext ctx) {
        // vamos visitar cada item de seleçã dentro do caso de seleção
        ctx.item_selecao().forEach(this::visitItem_selecao);
        return null;
    }

    @Override
    public Void visitItem_selecao(LAParser.Item_selecaoContext ctx) {
        // separando os intervalos de constantes
        ArrayList<String> intervalo = new ArrayList<>(Arrays.asList(ctx.constantes().getText().split("\\.\\.")));
        // primeiro e ultimo valor do intervalo
        String first = !intervalo.isEmpty() ? intervalo.get(0) : ctx.constantes().getText();
        String last = intervalo.size() > 1 ? intervalo.get(1) : intervalo.get(0);
        
        // geração de código para o intervalo definido
        for (int i = Integer.parseInt(first); i <= Integer.parseInt(last); i++) {
            saida.append("case ").append(i).append(":\n");
            ctx.cmd().forEach(this::visitCmd);
            saida.append("break;\n");
        }
        return null;
    }

}
