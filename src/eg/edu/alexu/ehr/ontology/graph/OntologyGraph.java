package eg.edu.alexu.ehr.ontology.graph;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import eg.edu.alexu.ehr.ontology.api.wrapper.Ontology;
import eg.edu.alexu.ehr.ontology.api.wrapper.OntologyProperty;
import eg.edu.alexu.ehr.ontology.api.wrapper.object.entities.OntologyClass;
import eg.edu.alexu.ehr.ontology.api.wrapper.object.entities.OntologyDatatype;
import eg.edu.alexu.ehr.ontology.api.wrapper.object.entities.OntologyEntity;
import eg.edu.alexu.ehr.ontology.api.wrapper.object.values.OntologyIndividual;
import eg.edu.alexu.ehr.ontology.api.wrapper.object.values.OntologyValue;

public class OntologyGraph {
	private Ontology ontology;
	private OntologyGraphNode root;

	private Set<OntologyGraphNode> classes;
	private Set<OntologyGraphNode> datatypes;
	private Set<OntologyGraphNode> individuals;
	private Set<OntologyGraphNode> literals;
        
	private Map<OntologyGraphEdge, OntologyGraphEdge> individualToClassPropertyMap;
        private Map<OntologyEntity, OntologyGraphNode> entityMap;
	private Map<OntologyValue, OntologyGraphNode> valueMap;

	private int noOfedges;
	
	public OntologyGraph(Ontology ontology) {
		build(ontology);
	}
	
	public void build(Ontology ontology) {
		this.ontology = ontology;
		noOfedges = 0;
		Set<OntologyClass> classesSet = ontology.getClasses();
		Set<OntologyDatatype> datatypeSet = ontology.getDatatypes();
		Set<OntologyProperty> properties = ontology.getProperties();
		Set<OntologyIndividual> individualsSet = new HashSet<OntologyIndividual>();
		
		classes = new HashSet<OntologyGraphNode>(classesSet.size());
		datatypes = new HashSet<OntologyGraphNode>(datatypeSet.size());
		individuals = new HashSet<OntologyGraphNode>(individualsSet.size());
		literals = new HashSet<OntologyGraphNode>();
		individualToClassPropertyMap = new HashMap<OntologyGraphEdge, OntologyGraphEdge>();
		
		entityMap =
                        new HashMap<OntologyEntity, OntologyGraphNode>(classesSet.size());
		valueMap =
                        new HashMap<OntologyValue, OntologyGraphNode>(individualsSet.size());
		
		for (OntologyClass clss : classesSet) {
			OntologyGraphNode node = new OntologyGraphNode(clss);
			classes.add(node);
			entityMap.put(clss, node);
			
			Set<OntologyIndividual> individualsLocalSet = clss.getIndividuals(ontology);
			for (OntologyIndividual individual : individualsLocalSet) {
				node = new OntologyGraphNode(individual);
				if (!individuals.contains(node)) {
					individuals.add(node);
					valueMap.put(individual, node);
				}
			}
		}
		
		for (OntologyDatatype datatype : datatypeSet) {
			OntologyGraphNode node = new OntologyGraphNode(datatype);
			datatypes.add(node);
			entityMap.put(datatype, node);
		}
		
		for (OntologyProperty property : properties) {
			Set<OntologyClass> domains = property.getDomains(ontology);
			Set<OntologyEntity> ranges = property.getRanges(ontology);
			
			for (OntologyClass domain : domains) {
				OntologyGraphNode domainNode = entityMap.get(domain);
				for (OntologyEntity range : ranges) {
					OntologyGraphNode rangeNode;
					if (entityMap.containsKey(range)) {
						rangeNode = entityMap.get(range);
					}
					else {
						rangeNode = new OntologyGraphNode(range);
						addEntity(rangeNode, entityMap);
					}
						
					domainNode.addConnection(property, rangeNode);
					noOfedges++;
				}
			}
		}
		
		for (OntologyClass clss : classesSet) {
			Set<OntologyClass> equivalentClasses = clss.getEquivalentClasses(ontology);
			Set<OntologyClass> disjointClasses = clss.getDisjointClasses(ontology);
			Set<OntologyClass> subclasses = clss.getSubclasses(ontology);
			Set<OntologyIndividual> instances = clss.getIndividuals(ontology);
			
			OntologyGraphNode class1 = entityMap.get(clss);
			
			for (OntologyClass clss2 : equivalentClasses) {
				if (clss.equals(clss2))
					continue;
				OntologyGraphNode class2 = entityMap.get(clss2);
				class1.addConnection(EdgeType.EQUIVALENTTO, class2);
				class2.addConnection(EdgeType.EQUIVALENTTO, class1);
				noOfedges+=2;
			}
			
			for (OntologyClass clss2 : disjointClasses) {
				OntologyGraphNode class2 = entityMap.get(clss2);
				class1.addConnection(EdgeType.DISJOINTWITH, class2);
				class2.addConnection(EdgeType.DISJOINTWITH, class1);
				noOfedges+=2;
			}
			
			for (OntologyClass clss2 : subclasses) {
				OntologyGraphNode class2 = entityMap.get(clss2);
				class1.addConnection(EdgeType.SUBCLASS, class2);
				class2.addConnection(EdgeType.SUPERCLASS, class1);
				noOfedges+=2;
			}
			
			for (OntologyIndividual ind : instances) {
				OntologyGraphNode individual = valueMap.get(ind);
				class1.addConnection(EdgeType.INSTANCE, individual);
				individual.addConnection(EdgeType.INSTANCEOF, class1);
				noOfedges+=2;
				
				Map<OntologyProperty, Set<OntologyValue>> indValues = ind.getPropertyValues(ontology);
				Set<OntologyProperty> keys = indValues.keySet();

                                // Individual properties
				for (OntologyProperty property : keys) {
					Set<OntologyValue> values = indValues.get(property);
					for (OntologyValue value : values) {
						addValue(value, valueMap);
						OntologyGraphNode individual2 = valueMap.get(value);
						individual.addConnection(property, individual2);
                                                connectIndividualEdgeToClassEdge(individual, property);
						noOfedges++;
					}
				}
			}
		}
		
		root = classes.iterator().next();
		
		System.out.println("Number of nodes: " + (classes.size() + datatypes.size()));
		System.out.println("Number of edges: " + noOfedges);
	}

        private void connectIndividualEdgeToClassEdge(OntologyGraphNode individual,
                OntologyProperty property) {
            Set<OntologyGraphNode> classes
                    = individual.getNextNodes(EdgeType.INSTANCEOF);
            Map<OntologyGraphNode, OntologyGraphEdge> classPropertyEdges
                    = new HashMap<OntologyGraphNode, OntologyGraphEdge>(classes.size());
            for (OntologyGraphNode clss : classes) {
                classPropertyEdges.put(clss, clss.getEdges(property).iterator().next());
            }
            Set<OntologyGraphEdge> edges = individual.getEdges(property);
            for (OntologyGraphEdge edge : edges) {
                for (OntologyGraphNode clss : classes) {
                    OntologyGraphEdge propertyEdge = classPropertyEdges.get(clss);
                    individualToClassPropertyMap.put(edge, propertyEdge);
                    propertyEdge.incrementWeight();
                }
            }
        }
	
	private void addEntity(OntologyGraphNode entity, Map<OntologyEntity, OntologyGraphNode> map) {
		if (classes.contains(entity) || datatypes.contains(entity))
			return;
		NodeType nodeType = entity.getNodeType();
		if (nodeType == NodeType.CLASS)
			classes.add(entity);
		else
			datatypes.add(entity);
		map.put((OntologyEntity)entity.getObject(), entity);
	}
	
	private void addEntity(OntologyEntity entity, Map<OntologyEntity, OntologyGraphNode>  map) {
		OntologyGraphNode node = new OntologyGraphNode(entity);
		if (classes.contains(node) || datatypes.contains(node))
			return;
		NodeType nodeType = node.getNodeType();
		if (nodeType == NodeType.CLASS)
			classes.add(node);
		else
			datatypes.add(node);
		map.put(entity, node);
	}
	
	private void addValue(OntologyGraphNode value, Map<OntologyValue, OntologyGraphNode> map) {
		if (individuals.contains(value) || literals.contains(value))
			return;
		NodeType nodeType = value.getNodeType();
		if (nodeType == NodeType.INDIVIDUAL)
			individuals.add(value);
		else
			literals.add(value);
		map.put((OntologyValue)value.getObject(), value);
	}
	
	private void addValue(OntologyValue value, Map<OntologyValue, OntologyGraphNode> map) {
		OntologyGraphNode node = new OntologyGraphNode(value);
		if (individuals.contains(node) || literals.contains(node))
			return;
		NodeType nodeType = node.getNodeType();
		if (nodeType == NodeType.INDIVIDUAL)
			individuals.add(node);
		else
			literals.add(node);
		map.put(value, node);
	}

        @Override
        public String toString() {
		//String ret = "";
		StringBuffer strBuffer = new StringBuffer("");
		//strBuffer.append(" String Buffer");

                for (OntologyGraphNode subject : classes) {
                    Set<OntologyGraphEdge> edges = subject.getEdges();
                    for (OntologyGraphEdge predicate : edges) {
                        OntologyGraphNode object = predicate.getNextNode();
                        strBuffer.append(subject + " -- " + predicate + " -- " + object + "\n");
                    }
                }

                for (OntologyGraphNode subject : individuals) {
                    Set<OntologyGraphEdge> edges = subject.getEdges();
                    for (OntologyGraphEdge predicate : edges) {
                        OntologyGraphNode object = predicate.getNextNode();
                        strBuffer.append(subject + " -- " + predicate + " -- " + object + "\n");
                    }
                }
                
		return strBuffer.toString();
	}

        public void save(String filepath) {
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(filepath), "utf-8"))) {
			for (OntologyGraphNode subject : classes) {
				Set<OntologyGraphEdge> edges = subject.getEdges();
                                for (OntologyGraphEdge predicate : edges) {
                                    OntologyGraphNode object = predicate.getNextNode();
                                    writer.write(subject + " -- \"" + predicate.getWeight() + "\"@"
                                            + predicate + " -- " + object + "\n");
                                }
			}

                        for (OntologyGraphNode subject : individuals) {
				Set<OntologyGraphEdge> edges = subject.getEdges();
                                for (OntologyGraphEdge predicate : edges) {
                                    OntologyGraphNode object = predicate.getNextNode();
                                    writer.write(subject + " -- " + predicate.getWeight() + "\"@"
                                            + predicate + " -- " + object + "\n");
                                }
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
