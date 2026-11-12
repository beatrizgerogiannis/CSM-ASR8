# Exemplo cliente-servidor com ZeroC Ice (Python)

Exemplo de RPC orientado a objetos com o middleware **ZeroC Ice**, estendido para a disciplina
de Sistemas Distribuídos (UFG). O código original é o Exemplo 3.21 do livro de Maarten van Steen
(template `professorfabio/ice-demo`); a esta versão foram acrescentados três métodos novos.

## Como o Ice funciona neste exemplo

- **Slice** (`Printer.ice`): linguagem de definição da interface remota (`Demo::Printer`),
  com tipos, structs, sequences e exceções. O `slice2py` gera o código Python a partir dela.
- **Servant** (`PrinterI` em `server.py`): objeto Python que implementa de fato os métodos.
- **Object adapter** (`SimpleAdapter`): escuta na porta 5678 e encaminha cada requisição ao
  servant registrado com a identidade `SimplePrinter`.
- **Proxy** (`PrinterPrx` em `client.py`): representante local do objeto remoto. Chamar um
  método no proxy envia a requisição pela rede e devolve o resultado (ou a exceção).
- **Comunicador** (`Ice.initialize`): ponto de entrada do runtime do Ice, que cria adapters e
  proxies e gerencia conexões e threads.

## Pré-requisitos e instalação

Python 3 e Ice **3.7** (runtime Python e compilador `slice2py`). Testado com Ice 3.7.10 e
Python 3.12 no Ubuntu 24.04.

### Ubuntu 24.04 (pacotes do próprio Ubuntu, série 3.7)
```
sudo apt-get update
sudo apt-get install python3-zeroc-ice zeroc-ice-compilers
```

### Amazon Linux
```
sudo dnf install https://download.zeroc.com/ice/3.7/amzn2023/ice-repo-3.7.amzn2023.noarch.rpm
sudo dnf install python3-ice ice-compilers
```

Verificação:
```
slice2py --version
python3 -c "import Ice; print(Ice.stringVersion())"
```

> As instruções do template original para Ubuntu instalavam o Ice 3.8, que tem um mapeamento
> Python diferente. Este exemplo usa a série 3.7.

## Gerar o código, compilar e rodar

1. **Gerar o código a partir do Slice** (só é necessário após alterar `Printer.ice`):
   ```
   slice2py Printer.ice
   ```
   Isso gera `Printer_ice.py` e o pacote `Demo/`. Eles estão versionados no repositório e
   **não devem ser editados à mão**.

2. Não há etapa de compilação: Python é interpretado.

3. **Servidor** (num terminal):
   ```
   python3 server.py
   ```

4. **Cliente** (em outro terminal):
   ```
   python3 client.py                 # servidor em localhost
   python3 client.py 34.203.80.210   # servidor em outra máquina
   ```

Para encerrar o servidor, use Ctrl+C.

`server2.py` e `client2.py` são a variante original com dois servants no mesmo adapter
(`SimplePrinter1` e `SimplePrinter2`). Eles usam apenas `printString` e não foram alterados.

## Métodos da interface `Demo::Printer`

Tipos auxiliares definidos no Slice:

```
sequence<string> StringSeq;
struct TextStats { int wordCount; int charCount; StringSeq words; string longestWord; }
exception DivisionByZero { string reason; }
```

| Método (Slice) | Descrição | Exceções |
|---|---|---|
| `string printString(string s)` | *(original)* Imprime `s` no servidor e devolve `s + "*"`. | — |
| `double printCost(int pages, int copies, double pricePerPage)` | *(novo)* Devolve o custo da impressão: `pages * copies * pricePerPage`. | — |
| `TextStats analyzeText(string s)` | *(novo)* Separa o texto em palavras e devolve uma struct com o nº de palavras, o nº de caracteres, a lista de palavras (sequence) e a palavra mais longa. | — |
| `double divide(double a, double b)` | *(novo)* Devolve `a / b`. | `DivisionByZero` se `b == 0` |

## Exemplo de execução

Saída do cliente:
```
printString('Hello World!') -> Hello World!*
printCost(pages=10, copies=3, pricePerPage=0.25) -> R$ 7.50
analyzeText(...) -> palavras=5, caracteres=35, mais longa='distribuidos'
    lista de palavras: ['Sistemas', 'distribuidos', 'com', 'ZeroC', 'Ice']
divide(10, 4) -> 2.5
divide(1, 0) -> exceção Demo.DivisionByZero capturada: divisão de 1 por zero
```

Saída do servidor (log de cada chamada):
```
Hello World!
[printCost] pages=10 copies=3 pricePerPage=0.25 -> 7.50
[analyzeText] 'Sistemas distribuidos com ZeroC Ice' -> 5 palavras, 35 caracteres
[divide] 10 / 4 -> 2.5
[divide] 1 / 0 -> lançando DivisionByZero
```

## Referência

- Documentação do Ice: https://docs.zeroc.com/ice/3.8/cpp/get-started
- Ice 3.7 para Python: https://doc.zeroc.com/ice/3.7/language-mappings/python-mapping
