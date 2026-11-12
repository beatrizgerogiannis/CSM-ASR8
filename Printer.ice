module Demo
{
    // Sequência de strings: vira uma lista Python no código gerado (slice2py).
    sequence<string> StringSeq;

    // Struct com estatísticas de um texto (usada como retorno de analyzeText).
    struct TextStats
    {
        int wordCount;      // número de palavras
        int charCount;      // número de caracteres (incluindo espaços)
        StringSeq words;    // lista de palavras, na ordem em que aparecem
        string longestWord; // palavra mais longa ("" se o texto estiver vazio)
    }

    // Exceção de usuário: lançada pelo servidor e recebida no cliente
    // como uma exceção Python (Demo.DivisionByZero).
    exception DivisionByZero
    {
        string reason;
    }

    interface Printer
    {
        // Método original do exemplo.
        string printString(string s);

        // NOVO 1: vários parâmetros numéricos -> valor calculado.
        // Custo de uma impressão: pages * copies * pricePerPage.
        double printCost(int pages, int copies, double pricePerPage);

        // NOVO 2: recebe uma string e devolve uma struct que contém uma sequence.
        TextStats analyzeText(string s);

        // NOVO 3: pode lançar a exceção de usuário DivisionByZero (quando b == 0).
        double divide(double a, double b) throws DivisionByZero;
    }
}
