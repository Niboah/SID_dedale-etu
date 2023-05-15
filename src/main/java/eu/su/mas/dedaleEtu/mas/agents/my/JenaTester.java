package eu.su.mas.dedaleEtu.mas.agents.my;

import org.apache.commons.compress.utils.Lists;
import org.apache.jena.ontology.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.ResultBinding;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

public class JenaTester {
    private static final String P2 = "src/main/java/eu/su/mas/dedaleEtu/mas/agents/my/";
    private static final String P2Url="http://www.semanticweb.org/15arch6h/ontologies/2023/3/dedalu";

    private static final String nodeUrl=P2Url+"#node";
    private static final String objectUrl=P2Url+"#object";
    private static final String agentUrl=P2Url+"#agent";
    OntModel model;

    String OntologyFile;
    String NamingContext;
    OntDocumentManager dm;

    public JenaTester(String Filename) {
        this.OntologyFile = Filename;
        this.NamingContext = Paths.get(P2+Filename).toAbsolutePath().toString();
    }


    public void loadOntology() {
        //System.out.println("\n\n· Loading Ontology");
        model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        dm = model.getDocumentManager();

        // Two options to import: absolute path in file system or classpath URL
        dm.addAltEntry(NamingContext,NamingContext);
        model.read(NamingContext);
        System.out.println("Read");
    }

    public void releaseOntology(String name) throws FileNotFoundException {
      //  System.out.println("\n\n· Releasing Ontology");
        if (!model.isClosed()) {
            String sep = File.separator;
            Path resourcePath =
                    Paths.get("src/main/java/eu/su/mas/dedaleEtu/mas/agents/my"+ sep + "p2-agent"+ name +
                            "-modified.rdf").toAbsolutePath();
            // System.out.println(resourcePath);
            model.write(new FileOutputStream(resourcePath.toString(), false));
            model.close();
        }
    }

    public void getIndividuals() {
        System.out.println("\n\n· Listing all individuals");
        for (Iterator<Individual> i = model.listIndividuals(); i.hasNext(); ) {
            Individual dummy = i.next();
            System.out.println("Ontology has individual: ");
            System.out.println("   " + dummy);
            for (StmtIterator j = dummy.listProperties(); j.hasNext();) {
                Property pro= j.next().getPredicate();
                RDFNode value=dummy.getPropertyValue(pro);
                System.out.println(pro.getLocalName()+ " "+value);
            }
        }
    }

    public void getIndividualsByClass() {
        System.out.println("\n\n· Listing individuals per class");
        Iterator<OntClass> classesIt = model.listNamedClasses();
        while (classesIt.hasNext()) {
            OntClass actual = classesIt.next();
            System.out.println("Class: '" + actual.getURI() + "' has individuals:");
            OntClass pizzaClass = model.getOntClass(actual.getURI());
            for (Iterator<Individual> i = model.listIndividuals(pizzaClass); i.hasNext(); ) {
                System.out.println("    · " + i.next());
            }
        }
    }

    public void getPropertiesByClass(String className) {
        System.out.println("\n\n· Listing properties per class");

        // All properties for the "Pizza Four Seasons" class
        OntClass c = model.getOntClass(P2Url+"#"+className);
        System.out.println("Class: '" + c.getURI() + "' has properties:");
        OntClass myClass = model.getOntClass(c.getURI());
        Iterator<OntProperty> itProperties = myClass.listDeclaredProperties();

        while (itProperties.hasNext()) {
            OntProperty property = itProperties.next();
            System.out.println("    · Name :" + property.getLocalName());
            System.out.println("        · Domain :" + property.getDomain());
            System.out.println("        · Range :" + property.getRange());
            System.out.println("        · Inverse :" + property.getInverse());
            System.out.println("        · IsData :" + property.isDatatypeProperty());
            System.out.println("        · IsFunctional :" + property.isFunctionalProperty());
            System.out.println("        · IsObject :" + property.isObjectProperty());
            System.out.println("        · IsSymetric :" + property.isSymmetricProperty());
            System.out.println("        · IsTransitive :" + property.isTransitiveProperty());
        }
    }



    public void getClasses() {
        System.out.println("\n\n· Listing classes in ontology");
        //List of ontology classes
        Iterator<OntClass> classesIt = model.listNamedClasses();
        List<OntClass> classes = Lists.newArrayList(classesIt);

        for (OntClass actual : classes) {
            System.out.println("Ontology has class: " + actual.getURI());
        }
    }




    /**
     * añadir propiedades connect a nodo para que forma una mapa.
     * @param id identificador de nodo
     * @param node_connect identificadors de nodos que connecta a nodo id
     */
    public void add_connect_node(String id, String node_connect) {
        OntClass nodeClass =model.getOntClass(nodeUrl);
        Individual node = model.getIndividual(nodeUrl+ id);
        Individual node2 =model.getIndividual(nodeUrl+node_connect);
        Property    connect= model.getProperty(P2Url+ "#connect");
        if (node==null) {
            nodeClass.createIndividual(nodeUrl+id);
            node = model.getIndividual(nodeUrl+ id);
        }

        if (node2==null) {
            nodeClass.createIndividual(nodeUrl+node_connect);
            node2 = model.getIndividual(nodeUrl+ node_connect);
        }

        node.addProperty(connect,node2);
        node2.addProperty(connect,node);

        OntClass nearTanker=model.getOntClass(P2Url+"#nearTanker");
        OntClass hasTanker=model.getOntClass(P2Url+"#hasTanker");
        if (node2.hasOntClass(hasTanker)) {
            node.addOntClass(nearTanker);
        }
        if (node.hasOntClass(hasTanker)) {
            node2.addOntClass(nearTanker);
        }



    }



    /**
     * añadir propiedad hasObject a nodo y inNodo object
     * @param id identificador del nodo
     * @param object identificador del agente
     */
    public void addObject(String id, String object, int weight) {
        OntClass objClass =model.getOntClass(objectUrl);
        Individual node=model.getIndividual(nodeUrl+id);
        OntClass nodeClass =model.getOntClass(nodeUrl);
        Individual object1= model.getIndividual(objectUrl+ object+weight);
        if (node==null) {
            nodeClass.createIndividual(nodeUrl + id);
            node = model.getIndividual(nodeUrl + id);
            Property identificador = model.getProperty(P2Url + "#id");
            node.addProperty(identificador, id);
        }
        if (object1==null) {
            objClass.createIndividual(objectUrl + object+weight);
            object1 = model.getIndividual(objectUrl + object+weight);
        }
        Property hasObject=model.getProperty(P2Url+"#hasObject");
        Property inNodo=model.getProperty(P2Url+"#objectInNode");
        object1.addProperty(inNodo,node);
        node.addProperty(hasObject,object1);
        Property objWeight=model.getProperty(P2Url+"#weight");
        object1.addProperty(objWeight, String.valueOf(weight));

    }


    /**
     * añadir propiedad hasAgent a nodo y inNodo a agent
     * @param pos identificador del nodo
     * @param name identificador del agente
     * @param rol el tipus del agente
     */

    public void addAgent(String name, String rol, String pos) {
        OntClass agentClass =model.getOntClass(P2Url+"#"+rol);
        OntClass nodeClass =model.getOntClass(nodeUrl);
        Individual node=model.getIndividual(nodeUrl+pos);
        Individual agent1=model.getIndividual(P2Url+"#"+rol+ name);
        if (node==null) {
            nodeClass.createIndividual(nodeUrl+pos);
            node=model.getIndividual(nodeUrl+pos);
            Property identificador= model.getProperty(P2Url + "#id");
            node.addProperty(identificador,pos);
        }
        Property hasAgent=model.getProperty(P2Url+"#hasAgent");
        Property inNodo=model.getProperty(P2Url+"#inNode");
        boolean exist=false;
        RDFNode oldNode=null;
        // si no existe agente en la ontologia lo crea
        if (agent1==null) {
            agentClass.createIndividual(P2Url + "#"+rol+name);
            agent1= model.getIndividual(P2Url+"#"+rol+name);
            OntClass padre=model.getOntClass(agentUrl);
            agent1.addOntClass(padre);

            agent1.addProperty(inNodo,node);
            node.addProperty(hasAgent,agent1);

        }

        else {
            oldNode=agent1.getPropertyValue(inNodo);
            Individual node2=model.getIndividual(String.valueOf(oldNode.asResource()));
            node2.removeProperty(hasAgent,agent1);
            agent1.removeProperty(inNodo,oldNode);
            agent1.addProperty(inNodo,node);
            node.addProperty(hasAgent,agent1);
            exist=true;
        }
        Property connect=model.getProperty(P2Url+"#connect");
        OntClass nearTanker=model.getOntClass(P2Url+"#nearTanker");

        if (rol.equals("tanker")) {
            OntClass hasTanker=model.getOntClass(P2Url+"#hasTanker");

            node.addOntClass(hasTanker);
            NodeIterator nodeConnect;
            if (exist) {
                Individual node2=model.getIndividual(String.valueOf(oldNode.asResource()));
                nodeConnect=node2.listPropertyValues(connect);
                node2.removeOntClass(hasTanker);
                while (nodeConnect.hasNext()) {
                    RDFNode n=nodeConnect.nextNode();
                    Individual node1=model.getIndividual(String.valueOf(n.asResource()));
                    node1.removeOntClass(nearTanker);
                }
            }
            nodeConnect=node.listPropertyValues(connect);
            while (nodeConnect.hasNext()) {
                RDFNode n=nodeConnect.nextNode();
                Individual node1=model.getIndividual(String.valueOf(n.asResource()));
                node1.addOntClass(nearTanker);
            }

        }

        else if (rol.equals("recolector")) {
            OntClass canDrop=model.getOntClass(P2Url+"#canDrop");
            if (agent1.hasOntClass(canDrop)) {
                agent1.removeOntClass(canDrop);
            }
            if (node.hasOntClass(nearTanker)) {
                agent1.addOntClass(canDrop);
            }
        }

    }

    /**
     * canviar posicio del agente
     * @param oldPos posicio antiguo
     * @param newPos posicio nuevo
     * @param name nombre del agente
     */
    public void changePosition(String oldPos,String newPos, String name,String rol) {
        System.out.println("change position "+oldPos + " to "+newPos +" for agent "+ name);
        Individual node1=model.getIndividual(nodeUrl+oldPos);
        Individual node2=model.getIndividual(nodeUrl+newPos);
        Individual agent=model.getIndividual(P2Url+"#"+rol+name);
        Property hasAgent=model.getProperty(P2Url+"#hasAgent");
        Property inNodo=model.getProperty(P2Url+"#inNode");
        if (node2==null) {

            OntClass nodeClass =model.getOntClass(P2Url+"#node");
            nodeClass.createIndividual(nodeUrl+newPos);
            node2= model.getIndividual(nodeUrl+newPos);
            Property identificador= model.getProperty(P2Url + "#id");
            node2.addProperty(identificador,newPos);
        }
        agent.removeProperty(inNodo,node1);
        agent.addProperty(inNodo,node2);
        node1.removeProperty(hasAgent,agent);
        node2.addProperty(hasAgent,agent);
        Property connect= model.getProperty(P2Url+ "#connect");
        node1.addProperty(connect,node2);
        node2.addProperty(connect,node1);
        OntClass nearTanker= model.getOntClass(P2Url+"#nearTanker");
        if (rol.equals("tanker")) {
            OntClass hasTanker= model.getOntClass(P2Url+"#hasTanker");

            node1.removeOntClass(hasTanker);
            NodeIterator nodeConnect=node1.listPropertyValues(connect);
            while (nodeConnect.hasNext()) {
                RDFNode n=nodeConnect.nextNode();
                Individual nextNode=model.getIndividual(String.valueOf(n.asResource()));
                nextNode.removeOntClass(nearTanker);
            }
            node2.addOntClass(hasTanker);
            nodeConnect=node2.listPropertyValues(connect);
            while (nodeConnect.hasNext()) {
                RDFNode n=nodeConnect.nextNode();
                Individual nextNode=model.getIndividual(String.valueOf(n.asResource()));
                nextNode.addOntClass(nearTanker);
            }
        }

        else if (rol.equals("recolector")) {
            OntClass canDrop=model.getOntClass(P2Url+"#canDrop");
            if (agent.hasOntClass(canDrop)) {
                agent.removeOntClass(canDrop);
            }
            if (node2.hasOntClass(nearTanker)) {
                agent.addOntClass(canDrop);
            }
        }
    }
}