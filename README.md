# t5-compiladores

## Alunos:
Vitor Kasai Tanoue, 801904<br>
Vinícius de Oliveira Guimarães, 802431<br>
Karen Barcelos, 799657

## Pré-requisitos:
```
Java JDK 17
Apache Maven 3.9.1
```

## Como rodar?

### Rodar o gerador de código passando um arquivo de teste para input

- Entre na pasta LaGerador
- Crie um arquivo entrada.txt e insira nele o conteúdo que o gerador de código vai ler.

(Opcional) Você pode escolher compilar o projeto do zero ou não (Nós já compilamos e deixamos o arquivo JAR do projeto pronto aqui no GitHub)
```
// compilando o projeto do zero
mvn clean
mvn generate-sources
mvn package
```

Agora, no terminal, execute:
```
java -jar ./target/LaGerador-1.0-SNAPSHOT-jar-with-dependencies.jar  entrada.txt saida.txt
```
Pronto, o resultado criado pelo gerador estará no arquivo saida.txt dentro da pasta LaGerador.

## Rodando o corretor automático
(Obs: Todos os resultados gerados pelo corretor automático já estão dentro do diretório correcaoAutomatica/temp/saidaProduzida/saida_t5)

- Entre na pasta correcaoAutomatica

Com terminal aberto na pasta correcaoAutomatica insira no terminal: <br>
```
java -jar compiladores-corretor-automatico-1.0-SNAPSHOT-jar-with-dependencies.jar "java -jar ../LaGerador/target/LaGerador-1.0-SNAPSHOT-jar-with-dependencies.jar" gcc temp casos-de-teste "802431 801904 799657" t5
```

O resultado gerado pelo corretor automático vai estar na pasta /temp/saidaProduzida/saida_t5
