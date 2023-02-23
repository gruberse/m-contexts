package at.lbg.dhp;

import org.apache.jena.query.*;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFWriter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        onlineMode();
        offlineMode();
    }

    public static void offlineMode() throws FileNotFoundException {
        Dataset dataset = RDFDataMgr.loadDataset("input_dataset.trig", Lang.TRIG);
        RDFWriter.create()
                .base("http://www.dhp.lbg.ac.at/JITAIs/")
                .lang(Lang.TRIG)
                .source(inference(dataset))
                .output(new FileOutputStream("src/main/resources/output_dataset.trig"));
    }

    public static void onlineMode() {
        RDFConnectionRemoteBuilder builder = RDFConnectionFuseki.create().destination("http://localhost:3030/JITAIs");
        try (RDFConnectionFuseki conn = (RDFConnectionFuseki) builder.build()) {
            conn.update("DELETE { ?s ?p ?o } WHERE { ?s ?p ?o }");
            conn.update("DELETE { GRAPH ?g { ?s ?p ?o } } WHERE { GRAPH ?g { ?s ?p ?o } }");

            Dataset dataset = RDFDataMgr.loadDataset("input_dataset.trig", Lang.TRIG);
            conn.loadDataset(inference(dataset));
        }
    }

    public static Dataset inference(Dataset dataset) {
        try(RDFConnection conn = RDFConnection.connect(dataset)){
            conn.update("""
                    prefix m:  <http://www.dke.uni-linz.ac.at/m-contexts#>
                    base <http://www.dhp.lbg.ac.at/JITAIs/>
                    INSERT {
                        GRAPH ?ctx { ?s ?p ?o }
                    }
                    WHERE {
                        GRAPH <Contexts> { ?ctx a m:ConcreteContext; m:subContextOf+ ?superCtx }
                        GRAPH ?superCtx { ?s ?p ?o }
                    }""");
        }
        return dataset;
    }
}