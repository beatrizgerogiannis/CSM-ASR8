# Discussão: middleware (ZeroC Ice) em cenários multiplataforma

Nesta atividade, o mesmo serviço `Demo::Printer` passou a ter um cliente e um servidor em Python
e outro par em Java, e as quatro combinações funcionaram com saída idêntica no cliente
(Python→Java, Java→Python, Python→Python e Java→Java). O ponto central é que o cliente não
precisa saber em que linguagem o servidor foi escrito. Trocar o servidor Python pelo Java não
exigiu mudar nenhuma linha dos clientes, só qual processo estava escutando na porta 5678.

**IDL única e geração de código.** A interface existe em um só lugar, o `Printer.ice`, que fica
na raiz do repositório. O `slice2py` gera os stubs e skeletons Python e o `slice2java` gera os
Java (no build do Maven). Nas ASR 04/05, com sockets puros, o "contrato" era um protocolo JSON
que eu inventei e que só existia implicitamente no código e na documentação. Se eu quisesse um
lado em Java, teria que reescrever esse contrato à mão e torcer para os dois lados concordarem
em nomes de campos, tipos e códigos de erro. Com o Slice, uma divergência vira erro de
compilação (Java) ou de execução já na chamada, e não um bug silencioso.

**Marshalling e representação de dados.** O Ice define como cada tipo é serializado e como ele
aparece em cada linguagem. A `struct TextStats` vira uma classe nos dois lados. A
`sequence<string>` vira `list` no Python e `String[]` no Java. `int` é 32 bits e `double` é
IEEE 754 nas duas pontas. Sem middleware, eu teria que escolher a codificação (JSON, como antes),
converter tipos em cada lado (no JSON não há distinção entre int e double, nem structs
nomeadas), validar campos ausentes e, em Java, ainda trazer uma biblioteca JSON e escrever o
mapeamento para classes.

**Exceções entre linguagens.** `DivisionByZero` é lançada com `raise` no servant Python ou com
`throw` no servant Java e chega ao cliente da outra linguagem como exceção nativa, com o campo
`reason` preenchido. No Java, ela é uma exceção checada, então o compilador obriga o `try/catch`.
Nas ASR 04/05, o erro era um campo no JSON de resposta, e cada cliente tinha que verificar esse
campo e decidir o que fazer. Seria preciso repetir isso em cada linguagem.

**Transparência de acesso e de localização.** O proxy (`PrinterPrx`) faz a chamada remota
parecer uma chamada de método local (`printer.divide(10, 4)`). A localização fica concentrada
numa string (`SimplePrinter:tcp -h <host> -p 5678`), e o host é um argumento dos clientes. A
transparência não é total: o endereço ainda está explícito no proxy, e falhas de rede aparecem
como exceções do Ice. Um serviço de localização (IceGrid) eliminaria esse ponto, mas não foi
usado aqui.

**O que eu teria que reimplementar à mão, em Python e em Java, sem o middleware:** abrir e aceitar
conexões TCP; enquadrar mensagens (o `"\n"` das ASR 04/05); serializar e desserializar cada tipo;
uma tabela de despacho do nome do método para a função; identificar o objeto alvo (aqui o adapter
faz isso pela identidade `SimplePrinter`); um formato de erro e a conversão de erro em exceção;
e a concorrência no servidor. Tudo isso duas vezes, uma por linguagem, mantendo as duas versões
compatíveis.

**Custos e limitações.**
- *Dependência de framework e de versões.* Foi preciso o runtime Python (apt), o runtime Java
  (`com.zeroc:ice:3.7.10` do Maven Central) e os compiladores Slice, todos da série 3.7. As
  instruções do template original instalavam o Ice 3.8, que tem outro mapeamento de Python, e por
  isso fixei a série 3.7 nos dois lados.
- *Curva de aprendizado.* Communicator, object adapter, identidade, servant, proxy, `checkedCast`
  e o mapeamento de cada linguagem são conceitos a mais em relação a um socket.
- *Build mais complexo.* O lado Java precisa de uma etapa de geração de código (Maven chamando o
  `slice2java`) antes da compilação.
- *Overhead.* Há o cabeçalho do protocolo Ice em cada mensagem, e o `checkedCast` faz uma ida e
  volta extra ao servidor para confirmar o tipo do objeto. Não medi esse custo. Para chamadas
  simples como as deste exemplo, ele não foi perceptível.
- *Depuração e controle.* O protocolo é binário. Com o JSON por linha eu podia testar o servidor
  com `nc` e ler o tráfego diretamente. Com o Ice, preciso das ferramentas e do tracing dele
  (`--Ice.Trace.Network`). Também perco o controle sobre o formato, o enquadramento e a gestão de
  conexões.
- *Acoplamento ao ecossistema.* Os dois lados precisam falar o protocolo Ice. Um cliente em uma
  linguagem sem runtime Ice, ou um navegador sem a biblioteca, não conversa diretamente com esse
  servidor, enquanto um protocolo JSON simples seria acessível de qualquer lugar.
- *O middleware não garante semântica igual.* O Ice transporta os dados, mas cada servant é
  código próprio. Para o servidor Java devolver exatamente a mesma mensagem de exceção que o
  Python (`"divisão de 1 por zero"`, formatada com `%g` no Python), precisei reproduzir em Java a
  formatação do Python (classe `PyFormat`). Manter dois servants equivalentes continua sendo
  responsabilidade de quem desenvolve.

**Conclusão.** Para um sistema com partes em linguagens diferentes, o middleware elimina
justamente o trabalho repetitivo e propenso a erro que fiz à mão nas ASR 04/05 (protocolo,
serialização, despacho e erros), e o elimina para as duas linguagens ao mesmo tempo a partir de
uma única interface. Em troca, o sistema fica preso a um framework, às suas versões e ao seu
protocolo binário, o que vale a pena quando a interface é rica e há várias linguagens envolvidas,
mas pode ser exagero para um serviço pequeno que precisa ser acessível por qualquer cliente.
