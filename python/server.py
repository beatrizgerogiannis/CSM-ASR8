import sys, Ice
import Demo

class PrinterI(Demo.Printer):
    def printString(self, s, current=None):
        print(s)
        return s + "*"

    # NOVO 1: calcula o custo de uma impressão (páginas x cópias x preço por página).
    def printCost(self, pages, copies, pricePerPage, current=None):
        cost = pages * copies * pricePerPage
        print("[printCost] pages=%d copies=%d pricePerPage=%.2f -> %.2f"
              % (pages, copies, pricePerPage, cost))
        return cost

    # NOVO 2: separa o texto em palavras e devolve a struct TextStats
    # (o campo "words" é a sequence StringSeq, que no Python é uma lista).
    def analyzeText(self, s, current=None):
        words = s.split()
        longest = max(words, key=len) if words else ""
        stats = Demo.TextStats(wordCount=len(words), charCount=len(s),
                               words=words, longestWord=longest)
        print("[analyzeText] %r -> %d palavras, %d caracteres"
              % (s, stats.wordCount, stats.charCount))
        return stats

    # NOVO 3: divisão; se b == 0, lança a exceção de usuário definida no Slice.
    # O Ice transporta a exceção pela rede e ela é relançada no cliente.
    def divide(self, a, b, current=None):
        if b == 0:
            print("[divide] %g / %g -> lançando DivisionByZero" % (a, b))
            raise Demo.DivisionByZero(reason="divisão de %g por zero" % a)
        result = a / b
        print("[divide] %g / %g -> %g" % (a, b, result))
        return result

communicator = Ice.initialize(sys.argv)

adapter = communicator.createObjectAdapterWithEndpoints("SimpleAdapter", "default -p 5678")
object = PrinterI()
adapter.add(object, Ice.Identity("SimplePrinter"))
adapter.activate()

communicator.waitForShutdown()
