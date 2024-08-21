grammar LA;
// Algoritmo
ALGORITMO: 'algoritmo';
FIM_ALGORITMO: 'fim_algoritmo';

// Struct
REGISTRO: 'registro';
FIM_REGISTRO: 'fim_registro';

// Booleanos
FALSO: 'falso';
VERDADEIRO: 'verdadeiro';
NAO: 'nao';

// Manipulação de funções
FACA: 'faca';
FUNCAO: 'funcao';
FIM_FUNCAO: 'fim_funcao';
PROCEDIMENTO: 'procedimento';
FIM_PROCEDIMENTO: 'fim_procedimento';
RETORNE: 'retorne';

// Entrada e saída
LEIA: 'leia';
ESCREVA: 'escreva';

// Tipos de variáveis
DECLARE: 'declare';
TIPO: 'tipo';
LITERAL: 'literal';
VAR: 'var';
LOGICO: 'logico';
INTEIRO: 'inteiro';
CONSTANTE: 'constante';
REAL: 'real';

// Condições
SE: 'se';
SENAO: 'senao';
FIM_SE: 'fim_se';
ENTAO: 'entao';
CASO: 'caso';
FIM_CASO: 'fim_caso';

// Laços de repetição
PARA: 'para';
FIM_PARA: 'fim_para';
ATE: 'ate';
SEJA: 'seja';
ENQUANTO: 'enquanto';
FIM_ENQUANTO: 'fim_enquanto';

// Operadores lógicos
E: 'e';
OU: 'ou';
IGUAL: '=';
DIFERENTE: '<>';
ATRIBUICAO: '<-';
MENOR: '<';
MAIOR: '>';
MENOR_IGUAL: '<=';
MAIOR_IGUAL: '>=';

// Operadores aritméticos
SOMA: '+';
MOD: '%';
SUBTRACAO: '-';
DIVISAO: '/';
MULTIPLICACAO: '*';
ENDERECO:'&';
PONT: '^';

// Números
NUM_INT: ('0'..'9')+;
NUM_REAL: ('0'..'9')+ '.' ('0'..'9')+;

// Identificadores
IDENT: ('a'..'z'|'A'..'Z')('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;

// Cadeias
CADEIA: '"' ('\\\'' | '\\n' | ~('"' | '\\' | '\n'))* '"';
CADEIA_NAO_FECHADA: '"' ('\\\'' | '\\n' | ~('"' | '\\' | '\n'))* '\n';

/*
CADEIA: '"' ('\\"' | ~('"' | '\\' | '\n'))* '"';
CADEIA_NAO_FECHADA: '"' ('\\"' | ~('"' | '\\' | '\n'))* '\n';

*/

//CADEIA: '"' ( ESC_SEQ | ~('"'|'\\'|'\n'|'\r') )* '"';
//CADEIA_NAO_FECHADA: '"' ( ESC_SEQ | ~('"'|'\\') )* '\r'? '\n'?;


//fragment
//ESC_SEQ: '\\\'' | '\\n';
//ESC_SEQ	: '\\\'';


fragment
ESC_SEQ	: '\\\'';

// Simbolos
PONTO: '.';
INTERVALO: '..';
DOIS_PONTOS: ':';
ABREPAR: '(';
FECHAPAR: ')';
VIRGULA: ',';
ABRECOL: '[';
FECHACOL: ']';

// Comentários
COMENTARIO: '{' ~('\n' | '\r' | '}')* '}' -> skip;
COMENTARIO_NAO_FECHADO: '{' ~('}')* '\n';

// White-spaces
WS: (' ' | '\t' | '\r' | '\n') -> skip;

//ANALISE SINTATICA (PARSER)

programa: declaracoes 'algoritmo' corpo 'fim_algoritmo' EOF;
declaracoes: (decl_local_global)*;
decl_local_global: declaracao_local | declaracao_global;
declaracao_local: declaracao_variavel |
                declaracao_constante |
                declaracao_tipo;
declaracao_constante: 'constante' IDENT ':' tipo_basico '=' valor_constante;
declaracao_tipo: 'tipo' IDENT ':' tipo;
declaracao_variavel: 'declare' variavel;
variavel: identificador (',' identificador)* ':' tipo;
identificador: IDENT ( '.' IDENT)* dimensao;
dimensao: ('[' exp_aritmetica ']')*;
tipo: registro | tipo_estendido;
tipo_basico: 'literal' | 'inteiro' | 'real' | 'logico';
tipo_basico_ident: tipo_basico | IDENT;
tipo_estendido: ('^')? tipo_basico_ident;
valor_constante: CADEIA | NUM_INT | NUM_REAL | 'verdadeiro' | 'falso';
registro: 'registro' variavel* 'fim_registro';
declaracao_global: 'procedimento' IDENT '(' parametros? ')' declaracao_local* cmd* 'fim_procedimento' | 'funcao' IDENT '(' parametros? ')' ':' tipo_estendido declaracao_local* cmd* 'fim_funcao' ;
parametro: 'var'? identificador ( ',' identificador)* ':' tipo_estendido;
parametros: parametro (',' parametro)*;
corpo: declaracao_local* cmd*;
cmd: cmdLeia | cmdEscreva | cmdSe | cmdCaso | cmdPara | cmdEnquanto | cmdFaca | cmdAtribuicao | cmdChamada | cmdRetorne;
cmdLeia: 'leia' '(' '^'? identificador ( ',' '^'? identificador)* ')';
cmdEscreva: 'escreva' '(' expressao ( ',' expressao)* ')';
cmdSe: 'se' expressao 'entao' (cmd)* (cmdSenao)? 'fim_se';
cmdSenao: 'senao' (cmd)*;
cmdCaso: 'caso' exp_aritmetica 'seja' selecao (cmdSenao)? 'fim_caso';
cmdPara: 'para' IDENT '<-' exp_aritmetica 'ate' exp_aritmetica 'faca' cmd* 'fim_para';
cmdEnquanto: 'enquanto' expressao 'faca' cmd* 'fim_enquanto';
cmdFaca: 'faca' cmd* 'ate' expressao ;
cmdAtribuicao: '^'? identificador '<-' expressao ;
cmdChamada: IDENT '(' expressao (',' expressao)* ')';
cmdRetorne: 'retorne' expressao;
selecao: item_selecao*;
item_selecao: constantes ':' cmd*;
constantes: numero_intervalo ('..' numero_intervalo)*;
numero_intervalo: op_unario? NUM_INT ('. .' op_unario? NUM_INT)?;
op_unario: '-';
exp_aritmetica: termo (op1 termo)*;
termo: fator (op2 fator)*;
fator: parcela (op3 parcela)*;
op1: '+' | '-';
op2: '*' | '/';
op3: '%';
parcela: (op_unario)? parcela_unario | parcela_nao_unario;
parcela_unario: '^'? identificador | IDENT '(' expressao (',' expressao)* ')' |
            NUM_INT | NUM_REAL | parentesis_expressao;
parentesis_expressao: '(' expressao ')';
parcela_nao_unario: '&' identificador | CADEIA;
exp_relacional: exp_aritmetica (op_relacional exp_aritmetica)?;
op_relacional: '=' | '<>' | '>=' | '<=' | '>' | '<';
expressao: termo_logico (op_logico_1 termo_logico)*;
termo_logico: fator_logico (op_logico_2 fator_logico)*;
fator_logico: 'nao'? parcela_logica;
parcela_logica: ( 'verdadeiro' | 'falso' ) | exp_relacional ;
op_logico_1: 'ou';
op_logico_2: 'e';
