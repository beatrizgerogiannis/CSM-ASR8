import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import Demo.DivisionByZero;
import Demo.PrinterPrx;
import Demo.TextStats;

/**
 * Cliente Java: faz as mesmas chamadas que python/client.py e imprime no mesmo formato,
 * funcionando igualmente contra o servidor Python ou o servidor Java.
 */
public class Client {
    public static void main(String[] args) {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true,
                                      StandardCharsets.UTF_8));

        // Util.initialize consome as opções --Ice.* e devolve o resto em "rest";
        // o primeiro argumento restante (se houver) é o host do servidor.
        List<String> rest = new ArrayList<>();
        // try-with-resources: o communicator é destruído ao final, mesmo em caso de erro.
        try (Communicator communicator = Util.initialize(args, rest)) {
            String host = rest.isEmpty() ? "localhost" : rest.get(0);

            ObjectPrx base = communicator.stringToProxy("SimplePrinter:tcp -h " + host + " -p 5678");
            PrinterPrx printer = PrinterPrx.checkedCast(base);
            if (printer == null) {
                throw new Error("Invalid proxy");
            }

            // Método original.
            String rep = printer.printString("Hello World!");
            System.out.println("printString('Hello World!') -> " + rep);

            // NOVO 1: vários parâmetros numéricos -> valor calculado no servidor.
            // Locale.ROOT garante o ponto decimal (7.50), igual ao Python.
            double cost = printer.printCost(10, 3, 0.25);
            System.out.println(String.format(Locale.ROOT,
                    "printCost(pages=10, copies=3, pricePerPage=0.25) -> R$ %.2f", cost));

            // NOVO 2: a struct TextStats chega como objeto Java; o campo words
            // (sequence<string> no Slice) chega como String[].
            TextStats stats = printer.analyzeText("Sistemas distribuidos com ZeroC Ice");
            System.out.println(String.format("analyzeText(...) -> palavras=%d, caracteres=%d, mais longa=%s",
                    stats.wordCount, stats.charCount, PyFormat.repr(stats.longestWord)));
            System.out.println("    lista de palavras: " + PyFormat.reprList(Arrays.asList(stats.words)));

            // NOVO 3: chamada normal e chamada que dispara a exceção de usuário.
            // Double.toString(2.5) == "2.5", como print() do Python.
            System.out.println("divide(10, 4) -> " + printer.divide(10, 4));
            try {
                printer.divide(1, 0);
            } catch (DivisionByZero e) {
                // A exceção lançada no servant (em Java ou em Python) chega aqui com o
                // campo "reason" preenchido.
                System.out.println("divide(1, 0) -> exceção Demo.DivisionByZero capturada: " + e.reason);
            }
        } catch (DivisionByZero e) {
            // divide(10, 4) declara a exceção checada; não deve ocorrer aqui.
            throw new RuntimeException(e);
        }
    }
}
