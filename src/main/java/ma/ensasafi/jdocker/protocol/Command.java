package ma.ensasafi.jdocker.protocol;

import java.util.Map;

public class Command {
    private CommandType type;
    private Map<String, String> parameters;

    public Command() {}

    public Command(CommandType type, Map<String, String> parameters) {
        this.type = type;
        this.parameters = parameters;
    }

    public CommandType getType() {
        return type;
    }

    public void setType(CommandType type) {
        this.type = type;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public String getParameter(String key) {
        return parameters != null ? parameters.get(key) : null;
    }
}