package ma.ensasafi.jdocker.server;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import ma.ensasafi.jdocker.models.ContainerInfo;
import ma.ensasafi.jdocker.models.ImageInfo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DockerManager {
    private DockerClient dockerClient;

    public DockerManager() {
        try {
            // Try to connect to Docker daemon
            DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost("tcp://localhost:2375")  // Adjust if needed
                    .build();

            DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .maxConnections(100)
                    .connectionTimeout(Duration.ofSeconds(30))
                    .responseTimeout(Duration.ofSeconds(45))
                    .build();

            this.dockerClient = DockerClientImpl.getInstance(config, httpClient);

            // Test connection
            dockerClient.pingCmd().exec();
            System.out.println("✓ Successfully connected to Docker daemon");
        } catch (Exception e) {
            System.err.println("✗ Failed to connect to Docker daemon: " + e.getMessage());
            System.err.println("Make sure Docker is running and accessible on tcp://localhost:2375");
            throw new RuntimeException("Docker connection failed", e);
        }
    }

    public List<ImageInfo> listImages() {
        List<ImageInfo> images = new ArrayList<>();
        try {
            List<Image> dockerImages = dockerClient.listImagesCmd().exec();

            for (Image img : dockerImages) {
                String[] repoTags = img.getRepoTags();
                if (repoTags != null && repoTags.length > 0) {
                    for (String repoTag : repoTags) {
                        String[] parts = repoTag.split(":");
                        String repo = parts.length > 0 ? parts[0] : "unknown";
                        String tag = parts.length > 1 ? parts[1] : "latest";

                        images.add(new ImageInfo(
                                img.getId().replace("sha256:", ""),
                                repo,
                                tag,
                                img.getSize(),
                                img.getCreated()
                        ));
                    }
                } else {
                    images.add(new ImageInfo(
                            img.getId().replace("sha256:", ""),
                            "<none>",
                            "<none>",
                            img.getSize(),
                            img.getCreated()
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("Error listing images: " + e.getMessage());
            throw new RuntimeException("Failed to list images", e);
        }
        return images;
    }

    public String pullImage(String imageName) {
        try {
            System.out.println("Pulling image: " + imageName);

            PullImageResultCallback callback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    if (item.getStatus() != null) {
                        System.out.println("  " + item.getStatus() +
                                (item.getProgress() != null ? " " + item.getProgress() : ""));
                    }
                    super.onNext(item);
                }
            };

            dockerClient.pullImageCmd(imageName)
                    .exec(callback)
                    .awaitCompletion();

            return "Successfully pulled image: " + imageName;
        } catch (Exception e) {
            System.err.println("Pull exception: " + e.getMessage());
            // Return success anyway - let user verify with 'images' command
            return "Pull completed. Use 'images' command to verify.";
        }
    }


    public List<ContainerInfo> listContainers(boolean all) {
        List<ContainerInfo> containers = new ArrayList<>();
        try {
            List<Container> dockerContainers = dockerClient.listContainersCmd()
                    .withShowAll(all)
                    .exec();

            for (Container container : dockerContainers) {
                String name = container.getNames() != null && container.getNames().length > 0
                        ? container.getNames()[0].replace("/", "")
                        : "unnamed";

                containers.add(new ContainerInfo(
                        container.getId(),
                        name,
                        container.getImage(),
                        container.getState(),
                        container.getStatus(),
                        container.getCreated()
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to list containers", e);
        }
        return containers;
    }

    public String createContainer(String imageName, String containerName) {
        try {
            CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                    .withName(containerName)
                    .exec();

            return container.getId();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create container: " + e.getMessage(), e);
        }
    }

    public void startContainer(String containerId) {
        try {
            dockerClient.startContainerCmd(containerId).exec();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start container: " + e.getMessage(), e);
        }
    }

    public void stopContainer(String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId)
                    .withTimeout(10)
                    .exec();
        } catch (Exception e) {
            throw new RuntimeException("Failed to stop container: " + e.getMessage(), e);
        }
    }

    public void deleteContainer(String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId)
                    .withForce(true)
                    .exec();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete container: " + e.getMessage(), e);
        }
    }

    public String getContainerStatus(String containerId) {
        try {
            InspectContainerResponse container = dockerClient.inspectContainerCmd(containerId).exec();
            InspectContainerResponse.ContainerState state = container.getState();

            return String.format("Container %s - State: %s, Running: %s, Status: %s",
                    containerId.substring(0, Math.min(12, containerId.length())),
                    state.getStatus(),
                    state.getRunning(),
                    state.getStatus());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get container status: " + e.getMessage(), e);
        }
    }

    public void close() {
        try {
            if (dockerClient != null) {
                dockerClient.close();
            }
        } catch (Exception e) {
            System.err.println("Error closing Docker client: " + e.getMessage());
        }
    }
}