import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class FinanceServer {

    private static FinanceFileHandler fileHandler = new FinanceFileHandler();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        // Endpoints de API
        server.createContext("/api/transactions", new TransactionsHandler());
        
        // Servir arquivos estáticos (Front-end)
        server.createContext("/", new StaticFileHandler());
        
        server.setExecutor(null);
        System.out.println("Servidor iniciado em http://localhost:8080");
        server.start();
    }

    // Handler para ler e salvar transações
    static class TransactionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                List<TransactionEntry> transactions = fileHandler.getAllTransactions();
                // Converte lista de objetos Java para uma String JSON manual
                String json = "[" + transactions.stream()
                        .map(t -> String.format("{\"id\":%d, \"date\":\"%s\", \"category\":\"%s\", \"type\":\"%s\", \"amount\":%.2f}", 
                            t.hashCode(), t.getDate(), t.getCategory(), t.getType().toString().toLowerCase(), t.getAmount()))
                        .collect(Collectors.joining(",")) + "]";
                
                byte[] response = json.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();

            } else if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                InputStream is = exchange.getRequestBody();
                String body = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n"));
                
                // Parser de JSON manual simplificado para pegar os campos
                try {
                    String date = extractJsonValue(body, "date");
                    String category = extractJsonValue(body, "category");
                    String typeStr = extractJsonValue(body, "type").toUpperCase();
                    double amount = Double.parseDouble(extractJsonValue(body, "amount"));

                    TransactionEntry.Type type = TransactionEntry.Type.valueOf(typeStr);
                    TransactionEntry entry = new TransactionEntry(date, category, type, amount);
                    
                    fileHandler.appendTransaction(entry);
                    
                    String response = "{\"status\":\"success\"}";
                    exchange.sendResponseHeaders(200, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(400, 0);
                }
                exchange.getResponseBody().close();
            }
        }

        private String extractJsonValue(String json, String key) {
            String pattern = "\"" + key + "\":\"";
            int start = json.indexOf(pattern);
            if (start != -1) {
                start += pattern.length();
                int end = json.indexOf("\"", start);
                return json.substring(start, end);
            }
            // Para números (sem aspas)
            pattern = "\"" + key + "\":";
            start = json.indexOf(pattern);
            start += pattern.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return json.substring(start, end).trim();
        }
    }

    // Handler para servir o Dashboard (HTML, CSS, JS)
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";
            
            Path file = Paths.get("frontend" + path);
            if (Files.exists(file)) {
                String contentType = "text/plain";
                if (path.endsWith(".html")) contentType = "text/html";
                else if (path.endsWith(".css")) contentType = "text/css";
                else if (path.endsWith(".js")) contentType = "application/javascript";

                byte[] response = Files.readAllBytes(file);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, response.length);
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
            } else {
                exchange.sendResponseHeaders(404, 0);
                exchange.getResponseBody().close();
            }
        }
    }
}
