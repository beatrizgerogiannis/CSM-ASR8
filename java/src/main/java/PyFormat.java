import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * Funções auxiliares que reproduzem a formatação do Python usada em server.py e client.py,
 * para que o lado Java produza exatamente o mesmo texto (logs, saída do cliente e, mais
 * importante, a mensagem da exceção DivisionByZero, que trafega até o cliente).
 */
final class PyFormat {
    private PyFormat() {}

    /** Equivalente a "%g" % x no Python (6 algarismos significativos, sem zeros à direita). */
    static String g(double x) {
        if (Double.isNaN(x)) return "nan";
        if (Double.isInfinite(x)) return x > 0 ? "inf" : "-inf";
        if (x == 0) return (1 / x < 0) ? "-0" : "0";

        BigDecimal bd = new BigDecimal(x).round(new MathContext(6, RoundingMode.HALF_EVEN));
        int exp = bd.precision() - bd.scale() - 1; // expoente decimal após o arredondamento
        if (exp < -4 || exp >= 6) {
            // Notação científica, como 1e+06 ou 1.5e-05.
            String mant = bd.movePointLeft(exp).stripTrailingZeros().toPlainString();
            return mant + (exp < 0 ? "e-" : "e+") + String.format("%02d", Math.abs(exp));
        }
        return bd.stripTrailingZeros().toPlainString();
    }

    /** Equivalente a repr(s) para strings (aspas simples, ou duplas se s contém ' e não "). */
    static String repr(String s) {
        char q = (s.indexOf('\'') >= 0 && s.indexOf('"') < 0) ? '"' : '\'';
        StringBuilder sb = new StringBuilder().append(q);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c == q) sb.append('\\');
                    sb.append(c);
            }
        }
        return sb.append(q).toString();
    }

    /** Equivalente a str(lista_de_strings): ['a', 'b', 'c']. */
    static String reprList(List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(repr(items.get(i)));
        }
        return sb.append(']').toString();
    }
}
