import com.zeroc.Ice.Current;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import Demo.DivisionByZero;
import Demo.TextStats;

/**
 * Servant: implementa a interface Demo::Printer gerada pelo slice2java a partir do mesmo
 * Printer.ice usado pelo Python. A semântica (resultados, mensagem da exceção e log de cada
 * chamada) é a mesma do PrinterI de python/server.py.
 */
public class PrinterI implements Demo.Printer {

    @Override
    public String printString(String s, Current current) {
        System.out.println(s);
        return s + "*";
    }

    // NOVO 1: custo da impressão (páginas x cópias x preço por página).
    // pages * copies é feito em long para não estourar int (no Python o int não tem limite).
    @Override
    public double printCost(int pages, int copies, double pricePerPage, Current current) {
        double cost = (long) pages * copies * pricePerPage;
        System.out.println(String.format(Locale.ROOT,
                "[printCost] pages=%d copies=%d pricePerPage=%.2f -> %.2f",
                pages, copies, pricePerPage, cost));
        return cost;
    }

    // NOVO 2: separa o texto em palavras (como str.split() do Python) e devolve a struct
    // TextStats. No Java, a sequence<string> StringSeq é mapeada para String[].
    @Override
    public TextStats analyzeText(String s, Current current) {
        List<String> words = new ArrayList<>();
        for (String w : s.split("(?U)\\s+")) {
            if (!w.isEmpty()) words.add(w);
        }
        // Primeira palavra de maior tamanho, como max(words, key=len); tamanhos e o total de
        // caracteres contados em code points, como len() do Python.
        String longest = "";
        for (String w : words) {
            if (w.codePointCount(0, w.length()) > longest.codePointCount(0, longest.length())) {
                longest = w;
            }
        }
        TextStats stats = new TextStats(words.size(), s.codePointCount(0, s.length()),
                                        words.toArray(new String[0]), longest);
        System.out.println(String.format("[analyzeText] %s -> %d palavras, %d caracteres",
                PyFormat.repr(s), stats.wordCount, stats.charCount));
        return stats;
    }

    // NOVO 3: divisão; se b == 0, lança a exceção de usuário definida no Slice.
    // O Ice a serializa e o cliente (Python ou Java) a recebe como Demo.DivisionByZero.
    @Override
    public double divide(double a, double b, Current current) throws DivisionByZero {
        if (b == 0) {
            System.out.println(String.format("[divide] %s / %s -> lançando DivisionByZero",
                    PyFormat.g(a), PyFormat.g(b)));
            throw new DivisionByZero("divisão de " + PyFormat.g(a) + " por zero");
        }
        double result = a / b;
        System.out.println(String.format("[divide] %s / %s -> %s",
                PyFormat.g(a), PyFormat.g(b), PyFormat.g(result)));
        return result;
    }
}
