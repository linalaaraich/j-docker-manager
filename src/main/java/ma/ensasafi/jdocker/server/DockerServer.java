package ma.ensasafi.jdocker.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DockerServer {
    private static final int DEFAULT_PORT = 9999;
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private DockerManager dockerManager;
    private AtomicInteger clientCounter;
    private volatile boolean running;

    public DockerServer(int port) {
        try {
            this.serverSocket = new ServerSocket(port);
            this.executorService = Executors.newCachedThreadPool();
            this.dockerManager = new DockerManager();
            this.clientCounter = new AtomicInteger(0);
            this.running = true;

            System.out.println("╔════════════════════════════════════════════════════════╗");
            System.out.println("║     J-DOCKER REMOTE MANAGER - SERVER STARTED           ║");
            System.out.println("╟────────────────────────────────────────────────────────╢");
            System.out.println("║  Port: " + port + "                                            ║");
            System.out.println("║  Docker: Connected                                     ║");
            System.out.println("║  Status: Waiting for clients...                        ║");
            System.out.println("╚════════════════════════════════════════════════════════╝");

        } catch (IOException e) {
            System.err.println("✗ Failed to start server on port " + port + ": " + e.getMessage());
            throw new RuntimeException("Server initialization failed", e);
        }
    }

    public void start() {
        // Shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                int clientNum = clientCounter.incrementAndGet();
                ClientHandler handler = new ClientHandler(clientSocket, dockerManager, clientNum);
                executorService.execute(handler);
            } catch (IOException e) {
                if (running) {
                    System.err.println("✗ Error accepting client connection: " + e.getMessage());
                }
            }
        }
    }

    public void shutdown() {
        System.out.println("\n⚠ Shutting down server...");
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            executorService.shutdown();
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }

            if (dockerManager != null) {
                dockerManager.close();
            }

            System.out.println("✓ Server stopped cleanly");
        } catch (Exception e) {
            System.err.println("✗ Error during shutdown: " + e.getMessage());
            executorService.shutdownNow();
        }
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default: " + DEFAULT_PORT);
            }
        }

        DockerServer server = new DockerServer(port);
        server.start();
    }
}