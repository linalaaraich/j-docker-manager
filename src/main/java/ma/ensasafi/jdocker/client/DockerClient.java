package ma.ensasafi.jdocker.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ma.ensasafi.jdocker.models.ContainerInfo;
import ma.ensasafi.jdocker.models.ImageInfo;
import ma.ensasafi.jdocker.protocol.Command;
import ma.ensasafi.jdocker.protocol.CommandType;
import ma.ensasafi.jdocker.protocol.Response;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class DockerClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson;
    private Scanner scanner;

    public DockerClient(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.gson = new Gson();
        this.scanner = new Scanner(System.in);
    }

    public void start() throws IOException {
        // Read welcome message
        String welcomeJson = in.readLine();
        Response welcome = gson.fromJson(welcomeJson, Response.class);

        printBanner();
        System.out.println("✓ " + welcome.getMessage());
        System.out.println("\nType 'help' to see available commands\n");

        boolean running = true;
        while (running) {
            System.out.print("j-docker> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            String[] parts = input.split("\\s+");
            String commandStr = parts[0].toLowerCase();

            try {
                switch (commandStr) {
                    case "help":
                        printHelp();
                        break;

                    case "ping":
                        sendCommand(new Command(CommandType.PING, null));
                        break;

                    case "images":
                    case "list-images":
                        handleListImages();
                        break;

                    case "pull":
                        if (parts.length < 2) {
                            System.out.println("Usage: pull <image-name>");
                        } else {
                            handlePullImage(parts[1]);
                        }
                        break;

                    case "ps":
                    case "containers":
                        boolean showAll = parts.length > 1 && parts[1].equals("-a");
                        handleListContainers(showAll);
                        break;

                    case "create":
                        if (parts.length < 3) {
                            System.out.println("Usage: create <image> <container-name>");
                        } else {
                            handleCreateContainer(parts[1], parts[2]);
                        }
                        break;

                    case "start":
                        if (parts.length < 2) {
                            System.out.println("Usage: start <container-id>");
                        } else {
                            handleStartContainer(parts[1]);
                        }
                        break;

                    case "stop":
                        if (parts.length < 2) {
                            System.out.println("Usage: stop <container-id>");
                        } else {
                            handleStopContainer(parts[1]);
                        }
                        break;

                    case "rm":
                    case "delete":
                        if (parts.length < 2) {
                            System.out.println("Usage: rm <container-id>");
                        } else {
                            handleDeleteContainer(parts[1]);
                        }
                        break;

                    case "status":
                        if (parts.length < 2) {
                            System.out.println("Usage: status <container-id>");
                        } else {
                            handleContainerStatus(parts[1]);
                        }
                        break;

                    case "exit":
                    case "quit":
                        sendCommand(new Command(CommandType.EXIT, null));
                        running = false;
                        break;

                    case "clear":
                        clearScreen();
                        break;

                    default:
                        System.out.println("Unknown command: " + commandStr);
                        System.out.println("Type 'help' for available commands");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        cleanup();
    }

    private void handleListImages() throws IOException {
        Response response = sendCommand(new Command(CommandType.LIST_IMAGES, null));
        if (response.isSuccess() && response.getData() != null) {
            List<ImageInfo> images = gson.fromJson(
                    gson.toJson(response.getData()),
                    new TypeToken<List<ImageInfo>>(){}.getType()
            );

            if (images.isEmpty()) {
                System.out.println("No images found");
            } else {
                System.out.println("\n" + String.format("%-12s %-30s %-15s %-15s",
                        "IMAGE ID", "REPOSITORY:TAG", "SIZE", "CREATED"));
                System.out.println("─".repeat(75));
                for (ImageInfo img : images) {
                    System.out.println(img.toString());
                }
                System.out.println("\nTotal: " + images.size() + " image(s)\n");
            }
        }
    }

    private void handlePullImage(String imageName) throws IOException {
        System.out.println("Pulling image: " + imageName + " (this may take a while...)");
        Response response = sendCommand(new Command(CommandType.PULL_IMAGE,
                Map.of("image", imageName)));
        System.out.println(response.getMessage());
    }

    private void handleListContainers(boolean all) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("all", String.valueOf(all));

        Response response = sendCommand(new Command(CommandType.LIST_CONTAINERS, params));
        if (response.isSuccess() && response.getData() != null) {
            List<ContainerInfo> containers = gson.fromJson(
                    gson.toJson(response.getData()),
                    new TypeToken<List<ContainerInfo>>(){}.getType()
            );

            if (containers.isEmpty()) {
                System.out.println("No containers found" + (all ? "" : " (use 'ps -a' to see all)"));
            } else {
                System.out.println("\n" + String.format("%-12s %-20s %-25s %-10s %-30s",
                        "CONTAINER ID", "NAME", "IMAGE", "STATE", "STATUS"));
                System.out.println("─".repeat(100));
                for (ContainerInfo container : containers) {
                    System.out.println(container.toString());
                }
                System.out.println("\nTotal: " + containers.size() + " container(s)\n");
            }
        }
    }

    private void handleCreateContainer(String image, String name) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("image", image);
        params.put("name", name);

        Response response = sendCommand(new Command(CommandType.CREATE_CONTAINER, params));
        System.out.println(response.getMessage());
    }

    private void handleStartContainer(String containerId) throws IOException {
        Response response = sendCommand(new Command(CommandType.START_CONTAINER,
                Map.of("id", containerId)));
        System.out.println(response.getMessage());
    }

    private void handleStopContainer(String containerId) throws IOException {
        Response response = sendCommand(new Command(CommandType.STOP_CONTAINER,
                Map.of("id", containerId)));
        System.out.println(response.getMessage());
    }

    private void handleDeleteContainer(String containerId) throws IOException {
        Response response = sendCommand(new Command(CommandType.DELETE_CONTAINER,
                Map.of("id", containerId)));
        System.out.println(response.getMessage());
    }

    private void handleContainerStatus(String containerId) throws IOException {
        Response response = sendCommand(new Command(CommandType.CONTAINER_STATUS,
                Map.of("id", containerId)));
        System.out.println(response.getMessage());
    }

    private Response sendCommand(Command command) throws IOException {
        String json = gson.toJson(command);
        out.println(json);

        String responseJson = in.readLine();
        if (responseJson == null) {
            throw new IOException("Connection lost to server");
        }

        Response response = gson.fromJson(responseJson, Response.class);

        if (!response.isSuccess()) {
            System.err.println("✗ Error: " + response.getMessage());
        }

        return response;
    }

    private void printBanner() {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║     J-DOCKER REMOTE MANAGER - CLIENT                   ║");
        System.out.println("╟────────────────────────────────────────────────────────╢");
        System.out.println("║  ENSA Safi - GTR 3 Project                             ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
    }

    private void printHelp() {
        System.out.println("\n╔══════════════════ AVAILABLE COMMANDS ═══════════════════╗");
        System.out.println("║                                                          ║");
        System.out.println("║  IMAGE MANAGEMENT:                                       ║");
        System.out.println("║    images, list-images    - List all Docker images       ║");
        System.out.println("║    pull <image>          - Pull image from Docker Hub    ║");
        System.out.println("║                                                          ║");
        System.out.println("║  CONTAINER MANAGEMENT:                                   ║");
        System.out.println("║    ps                    - List running containers       ║");
        System.out.println("║    ps -a                 - List all containers           ║");
        System.out.println("║    create <img> <name>   - Create new container          ║");
        System.out.println("║    start <id>            - Start a container             ║");
        System.out.println("║    stop <id>             - Stop a container              ║");
        System.out.println("║    rm <id>               - Delete a container            ║");
        System.out.println("║    status <id>           - Get container status          ║");
        System.out.println("║                                                          ║");
        System.out.println("║  GENERAL:                                                ║");
        System.out.println("║    ping                  - Test server connection        ║");
        System.out.println("║    help                  - Show this help                ║");
        System.out.println("║    clear                 - Clear screen                  ║");
        System.out.println("║    exit, quit            - Disconnect from server        ║");
        System.out.println("║                                                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        printBanner();
    }

    private void cleanup() {
        try {
            if (scanner != null) scanner.close();
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("\n✓ Disconnected from server");
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 9999;

        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default: 9999");
            }
        }

        try {
            DockerClient client = new DockerClient(host, port);
            client.start();
        } catch (IOException e) {
            System.err.println("✗ Failed to connect to server at " + host + ":" + port);
            System.err.println("Error: " + e.getMessage());
            System.err.println("\nMake sure the server is running and accessible.");
        }
    }
}