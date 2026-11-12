# Cliente e servidor multiplataforma com ZeroC Ice (Python e Java)

Exemplo de RPC orientado a objetos com o middleware **ZeroC Ice**, feito para a disciplina de
Sistemas Distribuídos (UFG). O código original é o Exemplo 3.21 do livro de Maarten van Steen
(template `professorfabio/ice-demo`). A ASR 07 acrescentou três métodos novos. Esta versão
(ASR 08) implementa o mesmo serviço também em Java, para os dois cenários multiplataforma:

1. **Cliente em Python e servidor em Java**
2. **Cliente em Java e servidor em Python**

As duas linguagens usam a **mesma interface**, [`Printer.ice`](Printer.ice), sem duplicação.
A discussão sobre o uso de middleware nesses cenários está em [`DISCUSSAO.md`](DISCUSSAO.md).

## Estrutura do repositório

```
Printer.ice              interface Slice (fonte única para Python e Java)
python/
  server.py, client.py   servidor e cliente Python
  server2.py, client2.py variante original com dois servants (só printString)
  Printer_ice.py, Demo/  código gerado pelo slice2py (versionado; não editar)
java/
  pom.xml                build Maven: roda slice2java e compila
  src/main/java/
    Server.java          servidor Java (equivalente ao server.py)
    PrinterI.java        servant Java (mesma semântica do PrinterI do Python)
    Client.java          cliente Java (equivalente ao client.py)
    PyFormat.java        reproduz %g / repr do Python para saídas idênticas
DISCUSSAO.md             texto para o campo da tarefa
```

## Como o Ice funciona neste exemplo

- **Slice** (`Printer.ice`): linguagem de definição da interface remota (`Demo::Printer`),
  com tipos, structs, sequences e exceções. O `slice2py` e o `slice2java` geram, a partir
  dela, os stubs e skeletons de cada linguagem.
- **Servant** (`PrinterI`): objeto que implementa de fato os métodos, um em Python e outro em Java.
- **Object adapter** (`SimpleAdapter`): escuta na porta 5678 e encaminha cada requisição ao
  servant registrado com a identidade `SimplePrinter`. Os dois servidores usam o mesmo adapter,
  a mesma porta e a mesma identidade, então os dois clientes funcionam com qualquer um deles.
- **Proxy** (`PrinterPrx`): representante local do objeto remoto. Chamar um método no proxy
  envia a requisição pela rede e devolve o resultado (ou a exceção), seja qual for a linguagem
  do servidor.
- **Comunicador** (`Ice.initialize` / `Util.initialize`): ponto de entrada do runtime do Ice.

## Pré-requisitos e instalação

Os dois lados usam a **série 3.7** do Ice (testado com 3.7.10, Python 3.12 e OpenJDK 21 no
Ubuntu 24.04 / WSL).

| Componente | Para quê | Ubuntu 24.04 |
|---|---|---|
| `python3-zeroc-ice` | runtime Ice para Python | apt |
| `zeroc-ice-compilers` | `slice2py` e `slice2java` | apt |
| JDK 17 ou 21 | compilar e rodar o lado Java | apt (`openjdk-21-jdk-headless`) |
| Maven | build do lado Java; baixa `com.zeroc:ice:3.7.10` do Maven Central | apt (`maven`) |

```
sudo apt-get update
sudo apt-get install python3-zeroc-ice zeroc-ice-compilers openjdk-21-jdk-headless maven
```

No Amazon Linux, o lado Python se instala com:
```
sudo dnf install https://download.zeroc.com/ice/3.7/amzn2023/ice-repo-3.7.amzn2023.noarch.rpm
sudo dnf install python3-ice ice-compilers
```

Verificação:
```
slice2py --version        # 3.7.10
slice2java --version      # 3.7.10
python3 -c "import Ice; print(Ice.stringVersion())"
java -version
mvn -v
```

> As instruções do template original para Ubuntu instalavam o Ice 3.8, que tem um mapeamento
> Python diferente. Este exemplo fixa a série 3.7 nos dois lados (no Java, a versão está no
> `pom.xml`).

## Gerar o código e compilar

### Python
O código gerado já está versionado em `python/`. Só é preciso regerá-lo se `Printer.ice` mudar
(a partir da raiz do repositório):
```
slice2py --output-dir python Printer.ice
```
Não há etapa de compilação.

### Java
Um único comando gera o código Slice e compila:
```
cd java
mvn -q package
```
O Maven roda o `slice2java` sobre `../Printer.ice` (gerando em `java/target/generated-sources/slice`),
compila e cria `java/target/printer-java.jar`, com o runtime do Ice incluído. Nada disso é
versionado.

## Como rodar os 4 cenários

Todos os comandos abaixo são executados a partir da raiz do repositório. Suba o servidor num
terminal e rode o cliente em outro. O servidor é encerrado com Ctrl+C.

| Cenário | Terminal 1 (servidor) | Terminal 2 (cliente) |
|---|---|---|
| **1. Python → Java** | `java -cp java/target/printer-java.jar Server` | `python3 python/client.py` |
| **2. Java → Python** | `python3 python/server.py` | `java -cp java/target/printer-java.jar Client` |
| Python → Python | `python3 python/server.py` | `python3 python/client.py` |
| Java → Java | `java -cp java/target/printer-java.jar Server` | `java -cp java/target/printer-java.jar Client` |

Os dois clientes recebem o host do servidor como argumento opcional (padrão `localhost`):
```
python3 python/client.py 34.203.80.210
java -cp java/target/printer-java.jar Client 34.203.80.210
```

`python/server2.py` e `python/client2.py` são a variante original com dois servants no mesmo
adapter (`SimplePrinter1` e `SimplePrinter2`). Eles usam apenas `printString` e não foram
alterados.

## Métodos da interface `Demo::Printer`

Tipos auxiliares definidos no Slice e como aparecem em cada linguagem:

| Slice | Python | Java |
|---|---|---|
| `sequence<string> StringSeq` | `list` de `str` | `String[]` |
| `struct TextStats { int wordCount; int charCount; StringSeq words; string longestWord; }` | classe `Demo.TextStats` | classe `Demo.TextStats` |
| `exception DivisionByZero { string reason; }` | `Demo.DivisionByZero` (exceção) | `Demo.DivisionByZero` (exceção checada) |

| Método (Slice) | Descrição | Exceções |
|---|---|---|
| `string printString(string s)` | *(original)* Imprime `s` no servidor e devolve `s + "*"`. | — |
| `double printCost(int pages, int copies, double pricePerPage)` | Devolve o custo da impressão: `pages * copies * pricePerPage`. | — |
| `TextStats analyzeText(string s)` | Separa o texto em palavras e devolve uma struct com o nº de palavras, o nº de caracteres, a lista de palavras (sequence) e a palavra mais longa. | — |
| `double divide(double a, double b)` | Devolve `a / b`. | `DivisionByZero` se `b == 0` |

## Exemplo de execução

A saída do cliente é **idêntica nos 4 cenários**. As saídas foram comparadas com `diff`, e a
exceção `DivisionByZero` atravessa a fronteira de linguagem nos dois sentidos:
```
printString('Hello World!') -> Hello World!*
printCost(pages=10, copies=3, pricePerPage=0.25) -> R$ 7.50
analyzeText(...) -> palavras=5, caracteres=35, mais longa='distribuidos'
    lista de palavras: ['Sistemas', 'distribuidos', 'com', 'ZeroC', 'Ice']
divide(10, 4) -> 2.5
divide(1, 0) -> exceção Demo.DivisionByZero capturada: divisão de 1 por zero
```

O log do servidor também é igual no Python e no Java:
```
Hello World!
[printCost] pages=10 copies=3 pricePerPage=0.25 -> 7.50
[analyzeText] 'Sistemas distribuidos com ZeroC Ice' -> 5 palavras, 35 caracteres
[divide] 10 / 4 -> 2.5
[divide] 1 / 0 -> lançando DivisionByZero
```

## Referência

- Ice 3.7 para Python: https://doc.zeroc.com/ice/3.7/language-mappings/python-mapping
- Ice 3.7 para Java: https://doc.zeroc.com/ice/3.7/language-mappings/java-mapping
- Runtime Java no Maven Central: https://central.sonatype.com/artifact/com.zeroc/ice/3.7.10
