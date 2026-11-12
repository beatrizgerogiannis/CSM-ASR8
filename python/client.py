import sys, Ice
import Demo

communicator = Ice.initialize(sys.argv)

# Host do servidor: "localhost" por padrão, ou o primeiro argumento
# (ex.: python3 client.py 34.203.80.210). Ice.initialize já removeu de
# sys.argv as opções --Ice.*, então sobra só o host, se houver.
host = sys.argv[1] if len(sys.argv) > 1 else "localhost"

base = communicator.stringToProxy("SimplePrinter:tcp -h %s -p 5678" % host)
printer = Demo.PrinterPrx.checkedCast(base)
if not printer:
    raise RuntimeError("Invalid proxy")

# Método original.
rep = printer.printString("Hello World!")
print("printString('Hello World!') ->", rep)

# NOVO 1: vários parâmetros numéricos -> valor calculado no servidor.
cost = printer.printCost(10, 3, 0.25)
print("printCost(pages=10, copies=3, pricePerPage=0.25) -> R$ %.2f" % cost)

# NOVO 2: a struct TextStats chega como objeto Python; o campo words
# (sequence<string> no Slice) chega como lista.
stats = printer.analyzeText("Sistemas distribuidos com ZeroC Ice")
print("analyzeText(...) -> palavras=%d, caracteres=%d, mais longa=%r"
      % (stats.wordCount, stats.charCount, stats.longestWord))
print("    lista de palavras:", stats.words)

# NOVO 3: chamada normal e chamada que dispara a exceção de usuário.
print("divide(10, 4) ->", printer.divide(10, 4))
try:
    printer.divide(1, 0)
except Demo.DivisionByZero as e:
    # A exceção lançada no servant chega aqui com o campo "reason" preenchido.
    print("divide(1, 0) -> exceção Demo.DivisionByZero capturada:", e.reason)

communicator.destroy()
