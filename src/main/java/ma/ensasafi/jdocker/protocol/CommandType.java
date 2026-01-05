package ma.ensasafi.jdocker.protocol;

public enum CommandType {
    LIST_IMAGES,
    PULL_IMAGE,
    LIST_CONTAINERS,
    CREATE_CONTAINER,
    START_CONTAINER,
    STOP_CONTAINER,
    DELETE_CONTAINER,
    CONTAINER_STATUS,
    PING,
    EXIT
}
