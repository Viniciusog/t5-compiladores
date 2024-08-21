#include <stdio.h>
#include <stdlib.h>


int main() {
struct {
char  nome[80];
int  idade;
}   reg;
strcpy( reg.nome,"Maria");
 reg.idade = 24;
printf("%s", reg.nome);
printf("%s", " tem ");
printf("%d", reg.idade);
printf("%s", " anos");
return 0;
}
