package ma.ensasafi.jdocker.server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import ma.ensasafi.jdocker.protocol.Command;
import ma.ensasafi.jdocker.protocol.Response;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private DockerManager dockerManager;
    private Gson gson;
    private String clientId;

    public ClientHandler(Socket socket, DockerManager dockerManager, int clientNumber) {
        this.clientSocket = socket;
        this.dockerManager = dockerManager;
        this.gson = new Gson();
        this.clientId = "Client-" + clientNumber + " (" + socket.getInetAddress().getHostAddress() + ")";
    }

    @Override
    public void run() {
        System.out.println("✓ " + clientId + " connected");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Send welcome message
            Response welcome = Response.success("Connected to J-Docker Remote Manager");
            out.println(gson.toJson(welcome));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                try {
                    Command command = gson.fromJson(inputLine, Command.class);
                    Response response = handleCommand(command);
                    out.println(gson.toJson(response));

                    if (command.getType() != null && command.getType().name().equals("EXIT")) {
                        break;
                    }
                } catch (JsonSyntaxException e) {
                    Response error = Response.error("Invalid command format: " + e.getMessage());
                    out.println(gson.toJson(error));
                } catch (Exception e) {
                    Response error = Response.error("Error processing command: " + e.getMessage());
                    out.println(gson.toJson(error));
                    e.printStackTrace();
                }
            }
        } catch (SocketException e) {
            System.out.println("✗ " + clientId + " disconnected abruptly");
        } catch (IOException e) {
            System.err.println("✗ Error handling " + clientId + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private Response handleCommand(Command command) {
        try {
            if (command.getType() == null) {
                return Response.error("Command type is required");
            }

            switch (command.getType()) {
                case PING:
                    return Response.success("PONG");

                case LIST_IMAGES:
                    return Response.success("Images retrieved successfully", dockerManager.listImages());

                case PULL_IMAGE:
                    String imageName = command.getParameter("image");
                    if (imageName == null || imageName.trim().isEmpty()) {
                        return Response.error("Image name is required");
                    }
                    String pullResult = dockerManager.pullImage(imageName);
                    return Response.success(pullResult);

                case LIST_CONTAINERS:
                    boolean showAll = "true".equals(command.getParameter("all"));
                    return Response.success("Containers retrieved successfully",
                            dockerManager.listContainers(showAll));

                case CREATE_CONTAINER:
                    String image = command.getParameter("image");
                    String name = command.getParameter("name");

                    if (image == null || image.trim().isEmpty()) {
                        return Response.error("Image name is required");
                    }
                    if (name == null || name.trim().isEmpty()) {
                        return Response.error("Container name is required");
                    }

                    String containerId = dockerManager.createContainer(image, name);
                    return Response.success("Container created successfully with ID: " + containerId, containerId);

                case START_CONTAINER:
                    String startId = command.getParameter("id");
                    if (startId == null || startId.trim().isEmpty()) {
                        return Response.error("Container ID is required");
                    }
                    dockerManager.startContainer(startId);
                    return Response.success("Container started successfully");

                case STOP_CONTAINER:
                    String stopId = command.getParameter("id");
                    if (stopId == null || stopId.trim().isEmpty()) {
                        return Response.error("Container ID is required");
                    }
                    dockerManager.stopContainer(stopId);
                    return Response.success("Container stopped successfully");

                case DELETE_CONTAINER:
                    String deleteId = command.getParameter("id");
                    if (deleteId == null || deleteId.trim().isEmpty()) {
                        return Response.error("Container ID is required");
                    }
                    dockerManager.deleteContainer(deleteId);
                    return Response.success("Container deleted successfully");

                case CONTAINER_STATUS:
                    String statusId = command.getParameter("id");
                    if (statusId == null || statusId.trim().isEmpty()) {
                        return Response.error("Container ID is required");
                    }
                    String status = dockerManager.getContainerStatus(statusId);
                    return Response.success(status);

                case EXIT:
                    return Response.success("Goodbye!");

                default:
                    return Response.error("Unknown command: " + command.getType());
            }
        } catch (Exception e) {
            return Response.error("Command execution failed: " + e.getMessage());
        }
    }

    private void cleanup() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            System.out.println("✓ " + clientId + " disconnected cleanly");
        } catch (IOException e) {
            System.err.println("Error closing client socket: " + e.getMessage());
        }
    }
}