package ir.mnz.proformapricemanager;

public record CompanyConfig(
        String name,
        String server,
        int port,
        String database,
        String username,
        String password
) {

    public String jdbcUrl() {

        return "jdbc:sqlserver://"
                + server + ":" + port
                + ";databaseName=" + database
                + ";encrypt=false"
                + ";sslProtocol=TLSv1"
                + ";loginTimeout=5";
    }

    @Override
    public String toString() {
        return name;
    }
}
