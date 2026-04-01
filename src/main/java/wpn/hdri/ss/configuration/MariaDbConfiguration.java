package wpn.hdri.ss.configuration;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

/**
 * Connection settings for the MariaDB instance that stores domain events
 * in ERPNext-compatible tables.
 */
@Root(name = "mariadb")
public class MariaDbConfiguration {

    @Attribute(name = "host")
    private String host;

    @Attribute(name = "port", required = false)
    private int port = 3306;

    @Attribute(name = "database")
    private String database;

    @Attribute(name = "user")
    private String user;

    @Attribute(name = "password")
    private String password;

    public MariaDbConfiguration(
            @Attribute(name = "host")     String host,
            @Attribute(name = "port", required = false) int port,
            @Attribute(name = "database") String database,
            @Attribute(name = "user")     String user,
            @Attribute(name = "password") String password) {
        this.host     = host;
        this.port     = port > 0 ? port : 3306;
        this.database = database;
        this.user     = user;
        this.password = password;
    }

    public String jdbcUrl() {
        return "jdbc:mariadb://" + host + ":" + port + "/" + database;
    }

    public String getUser()     { return user; }
    public String getPassword() { return password; }
}
