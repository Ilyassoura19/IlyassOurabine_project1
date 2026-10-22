package ma.project.model;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/*
 Where is the server? Two sources, in this order:
   1. command-line arguments:   java -jar TCPClient.jar <ServerIPAddress> <PortNumber>
   2. config.properties at runtime (next to the JAR first, otherwise the default one inside the JAR)
 */
public class ClientConfig {
    private static final String FILE_NAME = "config.properties";

    private final String ip;
    private final int port;

    private ClientConfig(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public static ClientConfig resolve(List<String> args) throws IOException {
        if (args.size() >= 2) {
            try {
                return new ClientConfig(args.get(0), Integer.parseInt(args.get(1)));
            } catch (NumberFormatException e) {
                throw new IOException("Invalid port number: " + args.get(1)
                        + "  (usage: java -jar TCPClient.jar <ServerIPAddress> <PortNumber>)");
            }
        }
        return load();
    }

    private static ClientConfig load() throws IOException {
        Properties props = new Properties();
        Path external = Path.of(FILE_NAME);

        if (Files.exists(external)) {
            try (InputStream in = new FileInputStream(external.toFile())) {
                props.load(in);
            }
        } else {
            try (InputStream in = ClientConfig.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
                if (in == null) {
                    throw new IOException(FILE_NAME + " not found");
                }
                props.load(in);
            }
        }

        String ip = props.getProperty("server.ip", "localhost").trim();
        int port = Integer.parseInt(props.getProperty("server.port", "3000").trim());
        return new ClientConfig(ip, port);
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
