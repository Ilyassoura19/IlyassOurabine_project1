package ma.project.model;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/*
 Loads the network configuration (server IP address + port number) at RUNTIME
 from a standard .properties file, so the deployment can change without recompiling.

 Lookup order:
   1. ./config.properties  (a file next to the JAR -> editable by the user)
   2. config.properties inside the JAR (src/main/resources -> default values)
 */
public class ServerConfig {
    private static final String FILE_NAME = "config.properties";

    private final String ip;
    private final int port;

    private ServerConfig(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public static ServerConfig load() throws IOException {
        Properties props = new Properties();
        Path external = Path.of(FILE_NAME);

        if (Files.exists(external)) {
            try (InputStream in = new FileInputStream(external.toFile())) {
                props.load(in);
            }
        } else {
            try (InputStream in = ServerConfig.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
                if (in == null) {
                    throw new IOException(FILE_NAME + " not found");
                }
                props.load(in);
            }
        }

        String ip = props.getProperty("server.ip", "0.0.0.0").trim();
        int port = Integer.parseInt(props.getProperty("server.port", "3000").trim());
        return new ServerConfig(ip, port);
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
