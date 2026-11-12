import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;
import java.io.FileOutputStream;
import java.io.FileDescriptor;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Servidor Java: equivalente a python/server.py (mesmo adapter, porta e identidade),
 * de modo que tanto client.py quanto o Client Java conseguem usá-lo sem mudança alguma.
 */
public class Server {
    public static void main(String[] args) {
        // Saída em UTF-8 com flush automático (acentos corretos e log imediato mesmo
        // quando a saída é redirecionada para um arquivo).
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true,
                                      StandardCharsets.UTF_8));

        // try-with-resources: o communicator é destruído ao sair do bloco.
        try (Communicator communicator = Util.initialize(args)) {
            // Ctrl+C (ou SIGTERM) destrói o communicator, o que libera waitForShutdown().
            Runtime.getRuntime().addShutdownHook(new Thread(communicator::destroy));

            ObjectAdapter adapter =
                communicator.createObjectAdapterWithEndpoints("SimpleAdapter", "default -p 5678");
            adapter.add(new PrinterI(), Util.stringToIdentity("SimplePrinter"));
            adapter.activate();

            communicator.waitForShutdown();
        }
    }
}
