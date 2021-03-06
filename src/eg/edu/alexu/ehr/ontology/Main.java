package eg.edu.alexu.ehr.ontology;
import eg.edu.alexu.ehr.ontology.api.wrapper.Ontology;
import eg.edu.alexu.ehr.ontology.graph.OntologyGraph;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class Main {
	public static void main(String []args) throws OWLOntologyCreationException {
		//File file = new File("pc.rdf");
		/*File file = new File("movieontology-instances.owl");
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(file);*/
		/*Set<OWLClass> classSet = ontology.getClassesInSignature();
		OWLClass []owlclasses = new OWLClass[classSet.size()];
		int index = 0;
		for (OWLClass c : classSet) {
			owlclasses[index++] = c;
			//Set<OWLIndividual> instances = ((OWLClass)c).getIndividuals(ontology);
			//for (OWLIndividual instance : instances)
				//System.out.println(c + " --> " + instance);
			System.out.println(c);
		}
		
		System.out.println();
		
		Set<OWLDataProperty> dataPropertySet = ontology.getDataPropertiesInSignature();//owlclasses[55].getDataPropertiesInSignature();
		Set<OWLObjectProperty> objectPropertySet = ontology.getObjectPropertiesInSignature();//owlclasses[55].getObjectPropertiesInSignature();
		
		System.out.println(dataPropertySet.size());
		System.out.println(objectPropertySet.size());
		for (OWLDataProperty p : dataPropertySet) {
			System.out.println(p + ": " + p.getDomains(ontology).size() + " - " + p.getRanges(ontology).size());
		}
		for (OWLObjectProperty p : objectPropertySet) {
			System.out.println(p + ": " + p.getDomains(ontology).size() + " - " + p.getRanges(ontology).size());
		}*/
		
		Ontology ontology = new Ontology("movieontology-instances.owl");
		//Ontology ontology = new Ontology("pc.rdf");
		OntologyGraph graph = new OntologyGraph(ontology);
                graph.save("Edges.txt");
		//System.out.println();
		//System.out.println(graph);
		//graph.save("Graph Edges.txt");
		
	}
}