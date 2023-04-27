package eu.su.mas.dedaleEtu.mas.agents.dummies.sid.bdi;

public class Constants {
    public static String I_AM_REGISTERED = "IAmRegistered";
    public static String ONTOLOGY = "ontology";
    public static String QUERY_SITUATED_AGENT =
                    "PREFIX example: <http://example#> " +
                    "SELECT ?Agent where {" +
                    " ?Agent a example:Agent ."+
                    "}";
}
